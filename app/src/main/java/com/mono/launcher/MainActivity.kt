package com.mono.launcher

import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.mono.launcher.ui.LauncherRoot
import com.mono.launcher.ui.MonoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var repo: AppRepository
    private lateinit var prefs: Prefs
    private val apps = mutableStateOf<List<AppEntry>>(emptyList())

    /** Incremented when Home is pressed while we're already in front: closes drawer/settings. */
    private val homeSignal = mutableIntStateOf(0)

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
        prefs = Prefs(this)
        repo.launcherApps.registerCallback(packageCallback)

        setContent {
            MonoTheme(accent = Color(prefs.accent)) {
                LauncherRoot(
                    apps = apps.value,
                    prefs = prefs,
                    homeSignal = homeSignal.intValue,
                    onLaunch = repo::launch,
                    onInfo = repo::openInfo,
                    onUninstall = repo::uninstall,
                    onSetDefault = {
                        runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
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

    private fun refresh() {
        lifecycleScope.launch {
            apps.value = withContext(Dispatchers.Default) { repo.load() }
        }
    }
}
