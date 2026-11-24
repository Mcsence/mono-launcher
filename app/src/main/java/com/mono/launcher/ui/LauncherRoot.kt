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
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.runtime.produceState
import com.mono.launcher.AppEntry
import androidx.compose.ui.platform.LocalContext
import com.mono.launcher.Focus
import com.mono.launcher.Gestures
import com.mono.launcher.IconPackInfo
import com.mono.launcher.Prefs
import kotlinx.coroutines.delay
import java.time.LocalDateTime

enum class Screen { HOME, DRAWER, SETTINGS }

@Composable
fun LauncherRoot(
    apps: List<AppEntry>,
    prefs: Prefs,
    iconPacks: List<IconPackInfo>,
    overlayGranted: Boolean,
    notificationAccess: Boolean,
    usageAccess: Boolean,
    accessibilityEnabled: Boolean,
    homeSignal: Int,
    onLaunch: (AppEntry) -> Unit,
    onInfo: (AppEntry) -> Unit,
    onUninstall: (AppEntry) -> Unit,
    onSetDefault: () -> Unit,
    onLockScreenChange: (Boolean) -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onAddWidget: (AppWidgetProviderInfo) -> Unit,
    onRemoveWidget: (Int) -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    /** App waiting behind the "breathe" pause because it has a daily limit. */
    var pausedApp by remember { mutableStateOf<AppEntry?>(null) }
    val context = LocalContext.current

    // Focus mode: re-checked every 20 s so it starts and ends on time.
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            delay(20_000L)
            value = LocalDateTime.now()
        }
    }
    val focusUntil = Focus.activeUntil(prefs, now)
    val visibleApps = remember(apps, focusUntil != null, prefs.focusApps) {
        if (focusUntil != null) apps.filter { it.key in prefs.focusApps } else apps
    }

    // Pressing Home while the launcher is already visible goes back to the home screen.
    LaunchedEffect(homeSignal) {
        screen = Screen.HOME
        menuApp = null
        pausedApp = null
    }
    // Back never leaves the launcher; it only closes the drawer or settings.
    BackHandler { screen = Screen.HOME }

    // Apps with a daily limit go through the pause screen first.
    val launch: (AppEntry) -> Unit = { app ->
        if (app.component.packageName in prefs.limits) {
            pausedApp = app
        } else {
            onLaunch(app)
            screen = Screen.HOME
        }
    }

    Box(Modifier.fillMaxSize()) {
        HomeScreen(
            apps = visibleApps,
            prefs = prefs,
            focusUntil = focusUntil?.toLocalTime(),
            onExitFocus = { focusUntil?.let { prefs.pauseFocusUntil(Focus.millis(it)) } },
            onOpenDrawer = { screen = Screen.DRAWER },
            onOpenSettings = { screen = Screen.SETTINGS },
            onLaunch = launch,
            onLongPress = { menuApp = it },
            onSwipeLeft = { Gestures.run(context, prefs.swipeLeft, apps, launch) },
            onSwipeRight = { Gestures.run(context, prefs.swipeRight, apps, launch) },
            onSwipeDown = { if (prefs.swipeDownNotifications) Gestures.openNotifications(context) },
            onDoubleTap = { if (prefs.doubleTapLock) Gestures.lockScreen() },
        )

        AnimatedVisibility(
            visible = screen == Screen.DRAWER,
            enter = slideInVertically { it / 3 } + fadeIn(),
            exit = slideOutVertically { it / 3 } + fadeOut(),
        ) {
            AppDrawer(
                apps = visibleApps,
                hiddenCount = apps.size - visibleApps.size,
                prefs = prefs,
                onOpenSettings = { screen = Screen.SETTINGS },
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
                notificationAccess = notificationAccess,
                usageAccess = usageAccess,
                accessibilityEnabled = accessibilityEnabled,
                onRequestAccessibility = onRequestAccessibility,
                onClose = { screen = Screen.HOME },
                onSetDefault = onSetDefault,
                onLockScreenChange = onLockScreenChange,
                onRequestOverlay = onRequestOverlay,
                onRequestNotificationAccess = onRequestNotificationAccess,
                onRequestUsageAccess = onRequestUsageAccess,
                onAddWidget = onAddWidget,
                onRemoveWidget = onRemoveWidget,
            )
        }

        pausedApp?.let { app ->
            PauseScreen(
                app = app,
                prefs = prefs,
                onOpen = {
                    pausedApp = null
                    onLaunch(app)
                    screen = Screen.HOME
                },
                onCancel = {
                    pausedApp = null
                    screen = Screen.HOME
                },
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
                onRename = { name ->
                    prefs.setLabel(app.key, name?.takeIf { it.trim() != app.originalLabel })
                    menuApp = null
                },
            )
        }
    }
}
