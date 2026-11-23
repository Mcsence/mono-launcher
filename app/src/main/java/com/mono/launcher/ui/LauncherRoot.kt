package com.mono.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mono.launcher.AppEntry
import com.mono.launcher.IconPackInfo
import com.mono.launcher.Prefs

enum class Screen { HOME, DRAWER, SETTINGS }

@Composable
fun LauncherRoot(
    apps: List<AppEntry>,
    prefs: Prefs,
    iconPacks: List<IconPackInfo>,
    overlayGranted: Boolean,
    homeSignal: Int,
    onLaunch: (AppEntry) -> Unit,
    onInfo: (AppEntry) -> Unit,
    onUninstall: (AppEntry) -> Unit,
    onSetDefault: () -> Unit,
    onLockScreenChange: (Boolean) -> Unit,
    onRequestOverlay: () -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }

    // Pressing Home while the launcher is already visible goes back to the home screen.
    LaunchedEffect(homeSignal) {
        screen = Screen.HOME
        menuApp = null
    }
    // Back never leaves the launcher; it only closes the drawer or settings.
    BackHandler { screen = Screen.HOME }

    val launch: (AppEntry) -> Unit = {
        onLaunch(it)
        screen = Screen.HOME
    }

    Box(Modifier.fillMaxSize().background(Black)) {
        HomeScreen(
            apps = apps,
            prefs = prefs,
            onOpenDrawer = { screen = Screen.DRAWER },
            onOpenSettings = { screen = Screen.SETTINGS },
            onLaunch = launch,
            onLongPress = { menuApp = it },
        )

        AnimatedVisibility(
            visible = screen == Screen.DRAWER,
            enter = slideInVertically { it / 3 } + fadeIn(),
            exit = slideOutVertically { it / 3 } + fadeOut(),
        ) {
            AppDrawer(
                apps = apps,
                prefs = prefs,
                onClose = { screen = Screen.HOME },
                onLaunch = launch,
                onLongPress = { menuApp = it },
            )
        }

        AnimatedVisibility(
            visible = screen == Screen.SETTINGS,
            enter = slideInHorizontally { it / 4 } + fadeIn(),
            exit = slideOutHorizontally { it / 4 } + fadeOut(),
        ) {
            SettingsScreen(
                apps = apps,
                prefs = prefs,
                iconPacks = iconPacks,
                overlayGranted = overlayGranted,
                onClose = { screen = Screen.HOME },
                onSetDefault = onSetDefault,
                onLockScreenChange = onLockScreenChange,
                onRequestOverlay = onRequestOverlay,
            )
        }

        menuApp?.let { app ->
            AppActionsSheet(
                app = app,
                prefs = prefs,
                favoriteIndex = prefs.favorites.indexOf(app.key),
                favoriteCount = prefs.favorites.size,
                onDismiss = { menuApp = null },
                onToggleFavorite = {
                    prefs.toggleFavorite(app.key)
                    menuApp = null
                },
                onMove = { prefs.moveFavorite(app.key, it) },
                onInfo = {
                    onInfo(app)
                    menuApp = null
                },
                onUninstall = {
                    onUninstall(app)
                    menuApp = null
                },
            )
        }
    }
}
