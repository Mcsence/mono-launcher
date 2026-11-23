package com.mono.launcher

import android.Manifest
import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
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
    private val apps = mutableStateOf<List<AppEntry>>(emptyList())
    private val iconPacks = mutableStateOf<List<IconPackInfo>>(emptyList())
    private val overlayGranted = mutableStateOf(true)
    private var refreshJob: Job? = null

    /** Incremented when Home is pressed while we're already in front: closes drawer/settings. */
    private val homeSignal = mutableIntStateOf(0)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(SystemBarStyle.dark(transparent), SystemBarStyle.dark(transparent))
        super.onCreate(savedInstanceState)

        repo = AppRepository(this)
        prefs = Prefs.get(this)
        repo.launcherApps.registerCallback(packageCallback)

        // Re-render icons when the icon style, shape or pack changes.
        lifecycleScope.launch {
            snapshotFlow { prefs.iconConfig() }.drop(1).collect { refresh() }
        }

        setContent {
            MonoTheme(accent = Color(prefs.accent)) {
                LauncherRoot(
                    apps = apps.value,
                    prefs = prefs,
                    iconPacks = iconPacks.value,
                    overlayGranted = overlayGranted.value,
                    homeSignal = homeSignal.intValue,
                    onLaunch = repo::launch,
                    onInfo = repo::openInfo,
                    onUninstall = repo::uninstall,
                    onSetDefault = {
                        runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                    },
                    onLockScreenChange = ::setLockScreen,
                    onRequestOverlay = ::requestOverlay,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayGranted.value = Settings.canDrawOverlays(this)
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

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch {
            val config = prefs.iconConfig()
            val (loaded, packs) = withContext(Dispatchers.Default) {
                repo.load(config) to IconPacks.list(this@MainActivity)
            }
            apps.value = loaded
            iconPacks.value = packs
        }
    }
}
