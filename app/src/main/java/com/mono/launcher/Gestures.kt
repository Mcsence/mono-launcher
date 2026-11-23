package com.mono.launcher

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import android.view.accessibility.AccessibilityEvent

/** What a swipe can open: nothing, the camera, the SMS app, or any app (by key). */
object Gestures {
    const val NONE = ""
    const val CAMERA = "@camera"
    const val MESSAGES = "@messages"

    fun label(target: String, apps: List<AppEntry>): String = when (target) {
        NONE -> "Rien"
        CAMERA -> "Appareil photo"
        MESSAGES -> "Messages"
        else -> apps.firstOrNull { it.key == target }?.label ?: "App introuvable"
    }

    fun run(context: Context, target: String, apps: List<AppEntry>, launch: (AppEntry) -> Unit) {
        when (target) {
            NONE -> Unit
            CAMERA -> runCatching {
                context.startActivity(
                    Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            MESSAGES -> {
                val pkg = Telephony.Sms.getDefaultSmsPackage(context) ?: return
                context.packageManager.getLaunchIntentForPackage(pkg)?.let { runCatching { context.startActivity(it) } }
            }
            else -> apps.firstOrNull { it.key == target }?.let(launch)
        }
    }

    /** Pulls down the notification shade. */
    @SuppressLint("WrongConstant")
    fun openNotifications(context: Context) {
        if (MonoAccessibilityService.perform(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)) return
        runCatching {
            val statusBar = context.getSystemService("statusbar")
            statusBar?.javaClass?.getMethod("expandNotificationsPanel")?.invoke(statusBar)
        }
    }

    /** Turns the screen off (needs the accessibility service, Android 9+). */
    fun lockScreen(): Boolean =
        Build.VERSION.SDK_INT >= 28 && MonoAccessibilityService.perform(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
}

/**
 * Android only lets apps turn the screen off through an accessibility service.
 * This one listens to nothing and only performs "lock screen" / "open notifications".
 */
class MonoAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        @Volatile
        private var instance: MonoAccessibilityService? = null

        fun perform(action: Int): Boolean = instance?.performGlobalAction(action) == true

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val mine = ComponentName(context, MonoAccessibilityService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == mine }
        }
    }
}
