package com.mono.launcher

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mono.launcher.ui.LockScreen
import com.mono.launcher.ui.MonoTheme
import com.mono.launcher.ui.applyMonoWindow
import com.mono.launcher.ui.paletteFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Mono's favorites shown on top of the system lock screen. Started by [LockService]
 * when the screen turns off; closes itself as soon as the phone is unlocked.
 */
class LockActivity : ComponentActivity() {
    private lateinit var repo: AppRepository
    private lateinit var prefs: Prefs
    private val apps = mutableStateOf(AppCache.apps)

    private val unlockedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs.get(this)
        applyMonoWindow(prefs.theme)
        super.onCreate(savedInstanceState)
        // No sideways task-switch animation when this screen appears or goes away.
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
        window.setWindowAnimations(0)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        repo = AppRepository(this)
        ContextCompat.registerReceiver(
            this,
            unlockedReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            MonoTheme(
                accent = Color(prefs.accent),
                palette = paletteFor(prefs.theme, prefs.wallpaperDim.alpha),
                font = prefs.font.family,
            ) {
                LockScreen(
                    apps = apps.value,
                    prefs = prefs,
                    onOpenLink = { url ->
                        unlockThen {
                            runCatching {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }
                    },
                    onUnlock = { unlockThen { } },
                )
            }
        }
        loadFavorites()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        loadFavorites()
    }

    /** Close instantly: a fade-out would briefly show whatever is behind (wallpaper, old screen). */
    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(unlockedReceiver) }
        super.onDestroy()
    }

    /** App names (for the notification summary) and fresh markets / news. */
    private fun loadFavorites() {
        lifecycleScope.launch {
            if (apps.value.isEmpty()) {
                val loaded = withContext(Dispatchers.Default) {
                    repo.load(IconConfig(IconStyle.NONE, IconShape.SYSTEM, ""))
                }
                apps.value = applyLabels(loaded, prefs.customLabels)
            }
        }
        lifecycleScope.launch { runCatching { Feeds.refresh(prefs) } }
    }

    /** Asks for fingerprint / PIN if needed, then runs [action] and closes. */
    private fun unlockThen(action: () -> Unit) {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (!keyguard.isKeyguardLocked) {
            action()
            finish()
            return
        }
        keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                action()
                finish()
            }
        })
    }
}
