package com.mono.launcher

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat

data class AppNotification(
    val title: String,
    val text: String,
    val count: Int,
    val intent: PendingIntent?,
)

data class MediaInfo(
    val packageName: String,
    val title: String,
    val artist: String,
    val art: ImageBitmap?,
    val playing: Boolean,
)

/** Live notifications and now-playing media, filled by [NotifService], read by the UI. */
object NotificationState {
    var byPackage by mutableStateOf<Map<String, AppNotification>>(emptyMap())
    var media by mutableStateOf<MediaInfo?>(null)
    internal var controller: MediaController? = null

    fun playPause() {
        val c = controller ?: return
        if (media?.playing == true) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    /** Opens what the notification points to (e.g. the conversation). False if it has no target. */
    fun open(notification: AppNotification): Boolean {
        val intent = notification.intent ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= 34) {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                intent.send(null, 0, null, null, null, null, options.toBundle())
            } else {
                intent.send()
            }
        }.isSuccess
    }

    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}

/**
 * Android only lets an app see notifications and control music players through a
 * notification listener, which the user enables in "Notification access".
 */
class NotifService : NotificationListenerService() {
    private var sessionManager: MediaSessionManager? = null

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers -> pickController(controllers) }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = updateMedia()
        override fun onPlaybackStateChanged(state: PlaybackState?) = updateMedia()
        override fun onSessionDestroyed() {
            pickController(runCatching { sessionManager?.getActiveSessions(component()) }.getOrNull())
        }
    }

    private fun component() = ComponentName(this, NotifService::class.java)

    override fun onListenerConnected() {
        rebuild()
        val manager = getSystemService(MediaSessionManager::class.java)
        sessionManager = manager
        runCatching {
            manager.addOnActiveSessionsChangedListener(sessionsListener, component())
            pickController(manager.getActiveSessions(component()))
        }
    }

    override fun onListenerDisconnected() {
        runCatching { sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener) }
        setController(null)
        NotificationState.byPackage = emptyMap()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = rebuild()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = rebuild()

    private fun rebuild() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        NotificationState.byPackage = active
            .filter {
                it.packageName != packageName &&
                    it.isClearable &&
                    (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0
            }
            .groupBy { it.packageName }
            .mapValues { (_, list) ->
                val latest = list.maxBy { it.postTime }
                val extras = latest.notification.extras
                AppNotification(
                    title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                    text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
                    count = list.size,
                    intent = latest.notification.contentIntent,
                )
            }
            .filterValues { it.title.isNotBlank() || it.text.isNotBlank() }
    }

    private fun pickController(controllers: List<MediaController>?) {
        val playing = controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        setController(playing ?: controllers?.firstOrNull())
    }

    private fun setController(controller: MediaController?) {
        NotificationState.controller?.unregisterCallback(controllerCallback)
        NotificationState.controller = controller
        controller?.registerCallback(controllerCallback)
        updateMedia()
    }

    private fun updateMedia() {
        val controller = NotificationState.controller
        val metadata = controller?.metadata
        if (controller == null || metadata == null) {
            NotificationState.media = null
            return
        }
        val art = (metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART))
            ?.let { Bitmap.createScaledBitmap(it, 128, 128, true).asImageBitmap() }
        NotificationState.media = MediaInfo(
            packageName = controller.packageName,
            title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = (metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)).orEmpty(),
            art = art,
            playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
        )
    }
}
