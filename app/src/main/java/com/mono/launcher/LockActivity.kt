package com.mono.launcher

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    private val apps = mutableStateOf<List<AppEntry>>(emptyList())

    private val unlockedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(SystemBarStyle.dark(transparent), SystemBarStyle.dark(transparent))
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        repo = AppRepository(this)
        prefs = Prefs.get(this)
        ContextCompat.registerReceiver(
            this,
            unlockedReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            MonoTheme(accent = Color(prefs.accent)) {
                // In focus mode, only the work apps are shown here too.
                val inFocus = Focus.activeUntil(prefs) != null
                val shown = remember(apps.value, inFocus, prefs.focusApps) {
                    if (inFocus) apps.value.filter { it.key in prefs.focusApps } else apps.value
                }
                LockScreen(
                    apps = shown,
                    prefs = prefs,
                    onLaunch = { app -> unlockThen { repo.launch(app) } },
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

    override fun onDestroy() {
        runCatching { unregisterReceiver(unlockedReceiver) }
        super.onDestroy()
    }

    private fun loadFavorites() {
        lifecycleScope.launch {
            val keys = prefs.favorites.toSet()
            val loaded = withContext(Dispatchers.Default) { repo.load(prefs.iconConfig(), onlyKeys = keys) }
            apps.value = applyLabels(loaded, prefs.customLabels)
        }
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
