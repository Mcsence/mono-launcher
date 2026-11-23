package com.mono.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.AppEntry
import com.mono.launcher.AppNotification
import com.mono.launcher.FavStyle
import com.mono.launcher.IconStyle
import com.mono.launcher.NotificationState
import com.mono.launcher.Prefs
import com.mono.launcher.ScreenTime
import com.mono.launcher.ScreenTimeMode
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    prefs: Prefs,
    focusUntil: LocalTime?,
    onExitFocus: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    val favorites = rememberFavorites(apps, prefs).ifEmpty {
        // In focus mode with no favorite among the work apps, show the work apps.
        if (focusUntil != null) apps.take(8) else emptyList()
    }
    val openDrawer by rememberUpdatedState(onOpenDrawer)
    val media = NotificationState.media

    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
            // Swipe up anywhere to open the app drawer.
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -120f) openDrawer() },
                ) { _, dy -> total += dy }
            }
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        if (prefs.showClock) {
            Clock(prefs.textSize.scale)
        }
        ScreenTimeTotal(prefs)
        Spacer(Modifier.height(if (prefs.showClock) 28.dp else 24.dp))

        if (focusUntil != null) {
            FocusBanner(focusUntil, onExitFocus)
            Spacer(Modifier.height(20.dp))
        }

        SectionLabel(if (focusUntil != null) "Focus" else "Favoris")
        Spacer(Modifier.height(16.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                favorites.isEmpty() && focusUntil != null -> Text(
                    "Aucune app autorisée. Ajoute-en dans Réglages › Mode focus.",
                    color = Muted,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                )
                favorites.isEmpty() -> Text(
                    "Va dans Réglages › Choisir les apps favorites\npour les épingler ici.",
                    color = Muted,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                )
                prefs.favStyle == FavStyle.LIST ->
                    FavoriteList(favorites, prefs, showPreviews = true, onLaunch, onLongPress)
                else -> FavoriteTiles(favorites, prefs, onLaunch, onLongPress)
            }
        }

        if (prefs.showMusic && media != null) {
            MusicPlayer(media, Modifier.padding(top = 12.dp))
        }

        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "↑  TOUTES LES APPS",
                color = White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Raised)
                    .clickable(onClick = onOpenDrawer)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "RÉGLAGES",
                color = Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

/**
 * Shown over the system lock screen: clock, favorites and music. Notification text is
 * not shown here (only a dot) so messages stay private while the phone is locked.
 */
@Composable
fun LockScreen(
    apps: List<AppEntry>,
    prefs: Prefs,
    onLaunch: (AppEntry) -> Unit,
    onUnlock: () -> Unit,
) {
    val favorites = rememberFavorites(apps, prefs)
    val unlock by rememberUpdatedState(onUnlock)
    val media = NotificationState.media

    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -120f) unlock() },
                ) { _, dy -> total += dy }
            }
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Clock(prefs.textSize.scale)
        Spacer(Modifier.height(32.dp))
        SectionLabel("Favoris")
        Spacer(Modifier.height(16.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                favorites.isEmpty() -> Text("Aucun favori pour l'instant.", color = Muted, fontSize = 16.sp)
                prefs.favStyle == FavStyle.LIST ->
                    FavoriteList(favorites, prefs, showPreviews = false, onLaunch, onLongPress = {})
                else -> FavoriteTiles(favorites, prefs, onLaunch, onLongPress = {})
            }
        }
        if (prefs.showMusic && media != null) {
            MusicPlayer(media, Modifier.padding(top = 12.dp))
        }
        Text(
            "↑  GLISSER POUR DÉVERROUILLER",
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .clickable { unlock() }
                .padding(vertical = 14.dp),
        )
    }
}

/** "Écran aujourd'hui · 2 h 14" under the clock. */
@Composable
private fun ScreenTimeTotal(prefs: Prefs) {
    if (prefs.screenTime == ScreenTimeMode.OFF) return
    val total = ScreenTime.total
    if (total < 60_000) return
    Text(
        "Écran aujourd'hui · ${ScreenTime.format(total)}",
        color = Muted,
        fontSize = (13 * prefs.textSize.scale).sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun FocusBanner(until: LocalTime, onExit: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Raised)
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(Accent, CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(
            "MODE FOCUS · jusqu'à ${until.format(DateTimeFormatter.ofPattern("HH:mm"))}",
            color = White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            "SORTIR",
            color = Black,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Accent)
                .clickable(onClick = onExit)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun appTime(app: AppEntry, prefs: Prefs): String? {
    if (prefs.screenTime != ScreenTimeMode.PER_APP) return null
    return ScreenTime.today[app.component.packageName]?.let(ScreenTime::format)
}

@Composable
private fun rememberFavorites(apps: List<AppEntry>, prefs: Prefs): List<AppEntry> =
    remember(apps, prefs.favorites) {
        val byKey = apps.associateBy { it.key }
        prefs.favorites.mapNotNull { byKey[it] }
    }

@Composable
private fun notificationFor(app: AppEntry, prefs: Prefs): AppNotification? =
    if (prefs.showNotifications) NotificationState.byPackage[app.component.packageName] else null

@Composable
private fun Clock(scale: Float) {
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1_000L)
        }
    }
    val time = remember(now.hour, now.minute) { now.format(DateTimeFormatter.ofPattern("HH:mm")) }
    val date = remember(now.toLocalDate()) {
        now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))
            .replaceFirstChar { it.uppercase() }
    }
    Column {
        Text(
            time,
            color = White,
            fontSize = (64 * scale).sp,
            lineHeight = (64 * scale).sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-3 * scale).sp,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(Accent, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(date, color = Muted, fontSize = (14 * scale).sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Big bold text list, one favorite per line, with the latest notification underneath. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteList(
    favorites: List<AppEntry>,
    prefs: Prefs,
    showPreviews: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    val scale = prefs.textSize.scale
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        favorites.forEach { app ->
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val notification = notificationFor(app, prefs)
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onLongClick = { onLongPress(app) },
                        onClick = { onLaunch(app) },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (prefs.iconStyle != IconStyle.NONE) {
                    AppIcon(app, prefs, 26.dp * scale)
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            app.label,
                            color = if (pressed) Accent else White,
                            fontSize = (28 * scale).sp,
                            lineHeight = (32 * scale).sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (notification != null && !showPreviews) {
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.size(7.dp).background(Accent, CircleShape))
                        }
                        appTime(app, prefs)?.let { time ->
                            Spacer(Modifier.width(10.dp))
                            Text(time, color = Muted, fontSize = (12 * scale).sp)
                        }
                    }
                    if (notification != null && showPreviews) {
                        NotificationPreview(notification, scale, onClick = {
                            if (!NotificationState.open(notification)) onLaunch(app)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationPreview(notification: AppNotification, scale: Float, onClick: () -> Unit) {
    val text = buildString {
        append(notification.title)
        if (notification.text.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(notification.text)
        }
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(Accent, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            color = Muted,
            fontSize = (13 * scale).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (notification.count > 1) {
            Spacer(Modifier.width(6.dp))
            Text("+${notification.count - 1}", color = Accent, fontSize = (12 * scale).sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Two-column tiles; the first favorite is highlighted with the accent color. */
@Composable
private fun FavoriteTiles(
    favorites: List<AppEntry>,
    prefs: Prefs,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        favorites.chunked(2).forEachIndexed { rowIndex, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { colIndex, app ->
                    Tile(
                        app = app,
                        highlighted = rowIndex == 0 && colIndex == 0,
                        prefs = prefs,
                        onClick = { onLaunch(app) },
                        onLongClick = { onLongPress(app) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tile(
    app: AppEntry,
    highlighted: Boolean,
    prefs: Prefs,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = prefs.textSize.scale
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val notification = notificationFor(app, prefs)
    val background = when {
        highlighted -> Accent
        pressed -> RaisedPressed
        else -> Raised
    }
    val foreground = if (highlighted) Black else White

    Column(
        modifier
            .height(92.dp * scale)
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (prefs.iconStyle != IconStyle.NONE) {
                AppIcon(app, prefs, 28.dp, onAccent = highlighted)
            } else {
                Box(Modifier.size(8.dp).background(if (highlighted) Black else Accent, CircleShape))
            }
            Spacer(Modifier.weight(1f))
            if (notification != null) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (highlighted) Black else Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${notification.count.coerceAtMost(9)}",
                        color = if (highlighted) Accent else Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Text(
            app.label,
            color = foreground,
            fontSize = (15 * scale).sp,
            lineHeight = (17 * scale).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
