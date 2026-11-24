package com.mono.launcher

import android.Manifest
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import androidx.compose.runtime.CompositionLocalProvider
import com.mono.launcher.ui.LocalWidgetHost
import com.mono.launcher.ui.applyMonoWindow
import com.mono.launcher.ui.paletteFor
import android.content.Intent
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.remember
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import com.mono.launcher.ui.LauncherRoot
import com.mono.launcher.ui.MonoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var repo: AppRepository
    private lateinit var prefs: Prefs
    private val apps = mutableStateOf(AppCache.apps)
    private val iconPacks = mutableStateOf<List<IconPackInfo>>(emptyList())
    private val overlayGranted = mutableStateOf(true)
    private val notificationAccess = mutableStateOf(true)
    private val usageAccess = mutableStateOf(true)
    private val accessibilityEnabled = mutableStateOf(false)
    private var refreshJob: Job? = null

    /** Incremented when Home is pressed while we're already in front: closes drawer/settings. */
    private val homeSignal = mutableIntStateOf(0)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private lateinit var widgetHost: AppWidgetHost
    private var pendingWidgetId = -1

    private val bindWidget =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) configureOrSaveWidget(pendingWidgetId) else cancelPendingWidget()
        }

    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs.get(this)
        applyMonoWindow(prefs.theme)
        super.onCreate(savedInstanceState)

        repo = AppRepository(this)
        repo.launcherApps.registerCallback(packageCallback)
        widgetHost = AppWidgetHost(applicationContext, WIDGET_HOST_ID)

        // Light / dark / wallpaper: status bar icons and window background follow the theme.
        lifecycleScope.launch {
            snapshotFlow { prefs.theme }.drop(1).collect { applyMonoWindow(it) }
        }

        // Re-render icons when the icon style, shape or pack changes.
        lifecycleScope.launch {
            snapshotFlow { prefs.iconConfig() }.drop(1).collect { refresh() }
        }

        // Markets and news: every 15 min while Lumen is on screen, and right away when settings change.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    runCatching { Feeds.refresh(prefs) }
                    delay(15 * 60_000L)
                }
            }
        }
        lifecycleScope.launch {
            snapshotFlow { listOf(prefs.showMarkets, prefs.tickers, prefs.showNews, prefs.newsSources) }
                .drop(1)
                .collect { runCatching { Feeds.refresh(prefs) } }
        }

        // Screen time: refreshed every minute while Mono is on screen.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    val needed = prefs.screenTime != ScreenTimeMode.OFF || prefs.limits.isNotEmpty()
                    if (needed && ScreenTime.hasAccess(this@MainActivity)) {
                        ScreenTime.today = withContext(Dispatchers.Default) {
                            runCatching { ScreenTime.compute(this@MainActivity) }.getOrDefault(emptyMap())
                        }
                    }
                    delay(60_000L)
                }
            }
        }

        setContent {
            MonoTheme(
                accent = Color(prefs.accent),
                palette = paletteFor(prefs.theme, prefs.wallpaperDim.alpha),
                font = prefs.font.family,
            ) {
                val shownApps = remember(apps.value, prefs.customLabels) {
                    applyLabels(apps.value, prefs.customLabels)
                }
                CompositionLocalProvider(LocalWidgetHost provides widgetHost) {
                LauncherRoot(
                    apps = shownApps,
                    prefs = prefs,
                    iconPacks = iconPacks.value,
                    overlayGranted = overlayGranted.value,
                    notificationAccess = notificationAccess.value,
                    usageAccess = usageAccess.value,
                    accessibilityEnabled = accessibilityEnabled.value,
                    onRequestAccessibility = {
                        runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    },
                    homeSignal = homeSignal.intValue,
                    onLaunch = repo::launch,
                    onInfo = repo::openInfo,
                    onUninstall = repo::uninstall,
                    onSetDefault = {
                        runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                    },
                    onLockScreenChange = ::setLockScreen,
                    onRequestOverlay = ::requestOverlay,
                    onRequestNotificationAccess = {
                        runCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    },
                    onRequestUsageAccess = {
                        runCatching { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                    },
                    onAddWidget = ::addWidget,
                    onRemoveWidget = ::removeWidget,
                )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { widgetHost.startListening() }
    }

    override fun onStop() {
        runCatching { widgetHost.stopListening() }
        super.onStop()
    }

    // ---- Widgets: allocate an id, get permission to bind it, let the widget configure itself. ----

    private fun addWidget(info: AppWidgetProviderInfo) {
        val manager = AppWidgetManager.getInstance(this)
        val id = widgetHost.allocateAppWidgetId()
        pendingWidgetId = id
        if (manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null)) {
            configureOrSaveWidget(id)
        } else {
            bindWidget.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
            )
        }
    }

    private fun configureOrSaveWidget(id: Int) {
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)
        if (info == null) {
            cancelPendingWidget()
            return
        }
        if (info.configure != null) {
            val started = runCatching {
                widgetHost.startAppWidgetConfigureActivityForResult(this, id, 0, REQUEST_CONFIGURE_WIDGET, null)
            }.isSuccess
            if (!started) saveWidget(id, info)
        } else {
            saveWidget(id, info)
        }
    }

    private fun saveWidget(id: Int, info: AppWidgetProviderInfo) {
        val density = resources.displayMetrics.density
        val heightDp = (info.minHeight / density).toInt().coerceIn(90, 260)
        prefs.addWidget(id, heightDp)
        pendingWidgetId = -1
    }

    private fun cancelPendingWidget() {
        if (pendingWidgetId != -1) runCatching { widgetHost.deleteAppWidgetId(pendingWidgetId) }
        pendingWidgetId = -1
    }

    private fun removeWidget(id: Int) {
        runCatching { widgetHost.deleteAppWidgetId(id) }
        prefs.removeWidget(id)
    }

    @Deprecated("Needed for AppWidgetHost.startAppWidgetConfigureActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CONFIGURE_WIDGET) return
        val id = pendingWidgetId
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)
        if (resultCode == RESULT_OK && info != null) saveWidget(id, info) else cancelPendingWidget()
    }

    override fun onResume() {
        super.onResume()
        overlayGranted.value = Settings.canDrawOverlays(this)
        notificationAccess.value = NotificationState.hasAccess(this)
        usageAccess.value = ScreenTime.hasAccess(this)
        accessibilityEnabled.value = MonoAccessibilityService.isEnabled(this)
        if (notificationAccess.value) {
            // Make sure Android has (re)connected the listener, e.g. after an update.
            runCatching {
                NotificationListenerService.requestRebind(ComponentName(this, NotifService::class.java))
            }
        }
        if (prefs.lockScreen) LockService.start(this)
        refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeSignal.intValue++
    }

    override fun onDestroy() {
        repo.launcherApps.unregisterCallback(packageCallback)
        super.onDestroy()
    }

    private fun setLockScreen(enabled: Boolean) {
        prefs.updateLockScreen(enabled)
        if (enabled) {
            if (Build.VERSION.SDK_INT >= 33) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            LockService.start(this)
            if (!Settings.canDrawOverlays(this)) requestOverlay()
        } else {
            LockService.stop(this)
        }
    }

    /** Opening Mono over the lock screen needs "Display over other apps". */
    private fun requestOverlay() {
        runCatching {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
    }

    companion object {
        private const val WIDGET_HOST_ID = 1024
        private const val REQUEST_CONFIGURE_WIDGET = 42
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch {
            val config = prefs.iconConfig()
            val (loaded, packs) = withContext(Dispatchers.Default) {
                repo.load(config) to IconPacks.list(this@MainActivity)
            }
            apps.value = loaded
            AppCache.apps = loaded
            iconPacks.value = packs
        }
    }
}
