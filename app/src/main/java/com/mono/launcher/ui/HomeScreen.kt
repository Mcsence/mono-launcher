package com.mono.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.AppEntry
import com.mono.launcher.AppNotification
import com.mono.launcher.ClockStyle
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
import kotlin.math.abs

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
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeDown: () -> Unit,
    onDoubleTap: () -> Unit,
) {
    val favorites = rememberFavorites(apps, prefs).ifEmpty {
        // In focus mode with no favorite among the work apps, show the work apps.
        if (focusUntil != null) apps.take(8) else emptyList()
    }
    val openDrawer by rememberUpdatedState(onOpenDrawer)
    val openSettings by rememberUpdatedState(onOpenSettings)
    val swipeLeft by rememberUpdatedState(onSwipeLeft)
    val swipeRight by rememberUpdatedState(onSwipeRight)
    val swipeDown by rememberUpdatedState(onSwipeDown)
    val doubleTap by rememberUpdatedState(onDoubleTap)
    val media = NotificationState.media

    Column(
        Modifier
            .fillMaxSize()
            .background(HomeBackground)
            // Swipes anywhere: up = app drawer, down = notifications, left / right = chosen apps.
            .pointerInput(Unit) {
                var dx = 0f
                var dy = 0f
                val threshold = 60.dp.toPx()
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDragEnd = {
                        if (abs(dy) > abs(dx)) {
                            if (dy < -threshold) openDrawer() else if (dy > threshold) swipeDown()
                        } else {
                            if (dx > threshold) swipeRight() else if (dx < -threshold) swipeLeft()
                        }
                    },
                ) { _, amount ->
                    dx += amount.x
                    dy += amount.y
                }
            }
            // On an empty spot: double-tap = chosen action, long-press = settings.
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { doubleTap() },
                    onLongPress = { openSettings() },
                )
            }
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        if (prefs.showClock) ClockBlock(prefs)
        ScreenTimeTotal(prefs)
        Spacer(Modifier.height(if (prefs.showClock) 24.dp else 16.dp))

        if (prefs.widgets.isNotEmpty()) {
            WidgetStack(prefs.widgets)
            Spacer(Modifier.height(8.dp))
        }

        if (focusUntil != null) {
            FocusBanner(focusUntil, onExitFocus)
            Spacer(Modifier.height(20.dp))
        }

        if (prefs.favStyle != FavStyle.CATEGORIES) {
            SectionLabel(if (focusUntil != null) "Focus" else "Favoris")
            Spacer(Modifier.height(16.dp))
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                prefs.favStyle == FavStyle.CATEGORIES ->
                    CategoriesView(apps, favorites, prefs, onLaunch, onLongPress)
                favorites.isEmpty() && focusUntil != null -> Text(
                    "Aucune app autorisée. Ajoute-en dans Réglages › Mode focus.",
                    color = Muted,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                )
                favorites.isEmpty() -> Text(
                    "Appui long sur un espace vide › Réglages › Choisir les apps favorites.",
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

        // Minimal bottom handle: tap or swipe up for all apps.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onOpenDrawer)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Muted.copy(alpha = 0.5f))
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
            .background(HomeBackground)
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
        ClockBlock(prefs)
        Spacer(Modifier.height(32.dp))
        SectionLabel("Favoris")
        Spacer(Modifier.height(16.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                favorites.isEmpty() -> Text("Aucun favori pour l'instant.", color = Muted, fontSize = 16.sp)
                prefs.favStyle == FavStyle.TILES -> FavoriteTiles(favorites, prefs, onLaunch, onLongPress = {})
                else -> FavoriteList(favorites, prefs, showPreviews = false, onLaunch, onLongPress = {})
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

@Composable
private fun rememberFavorites(apps: List<AppEntry>, prefs: Prefs): List<AppEntry> =
    remember(apps, prefs.favorites) {
        val byKey = apps.associateBy { it.key }
        prefs.favorites.mapNotNull { byKey[it] }
    }

@Composable
private fun notificationFor(app: AppEntry, prefs: Prefs): AppNotification? =
    if (prefs.showNotifications) NotificationState.byPackage[app.component.packageName] else null

/** True when the app has a daily limit and today's use went past it. */
@Composable
fun isOverLimit(app: AppEntry, prefs: Prefs): Boolean {
    val limit = prefs.limits[app.component.packageName] ?: return false
    val used = ScreenTime.today[app.component.packageName] ?: return false
    return used >= limit * 60_000L
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
            color = OnAccent,
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

// ---------------------------------------------------------------- Clock

@Composable
private fun ClockBlock(prefs: Prefs) {
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1_000L)
        }
    }
    val scale = prefs.textSize.scale
    val hours = remember(now.hour) { "%02d".format(now.hour) }
    val minutes = remember(now.minute) { "%02d".format(now.minute) }
    val longDate = remember(now.toLocalDate()) {
        now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))
            .replaceFirstChar { it.uppercase() }
    }

    when (prefs.clockStyle) {
        ClockStyle.CLASSIC -> Column {
            Text(
                "$hours:$minutes",
                color = White,
                fontSize = (64 * scale).sp,
                lineHeight = (64 * scale).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-3 * scale).sp,
            )
            DateLine(longDate, scale)
        }

        // Bold hours, light minutes, date block on the right.
        ClockStyle.DUO -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            val muted = Muted
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(hours) }
                    withStyle(SpanStyle(fontWeight = FontWeight.Light, color = muted)) { append(":") }
                    withStyle(SpanStyle(fontWeight = FontWeight.Light)) { append(minutes) }
                },
                color = White,
                fontSize = (72 * scale).sp,
                lineHeight = (72 * scale).sp,
                letterSpacing = (-3 * scale).sp,
                modifier = Modifier.weight(1f),
            )
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    now.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase().trimEnd('.'),
                    color = Muted,
                    fontSize = (12 * scale).sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                )
                Text("${now.dayOfMonth}", color = White, fontSize = (24 * scale).sp, fontWeight = FontWeight.Bold)
                Text(
                    now.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault())).uppercase().trimEnd('.'),
                    color = Accent,
                    fontSize = (12 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
            }
        }

        // Hours on top, minutes in the accent color underneath.
        ClockStyle.STACKED -> Column {
            Text(
                hours,
                color = White,
                fontSize = (88 * scale).sp,
                lineHeight = (80 * scale).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-4 * scale).sp,
            )
            Text(
                minutes,
                color = Accent,
                fontSize = (88 * scale).sp,
                lineHeight = (84 * scale).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-4 * scale).sp,
            )
            DateLine(longDate, scale)
        }

        // One quiet line.
        ClockStyle.DISCREET -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$hours:$minutes", color = White, fontSize = (22 * scale).sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(5.dp).background(Accent, CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(longDate, color = Muted, fontSize = (15 * scale).sp)
        }
    }
}

@Composable
private fun DateLine(date: String, scale: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(Accent, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(date, color = Muted, fontSize = (14 * scale).sp, fontWeight = FontWeight.Medium)
    }
}

// ---------------------------------------------------------------- Favorites: list

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
            val over = isOverLimit(app, prefs)
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
                    AppIcon(app, prefs, 26.dp * scale, dimmed = over)
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            app.label,
                            color = when {
                                pressed -> Accent
                                over -> Muted.copy(alpha = 0.6f)
                                else -> White
                            },
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
                            Text(time, color = if (over) Accent else Muted, fontSize = (12 * scale).sp)
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

// ---------------------------------------------------------------- Favorites: tiles

/** Two-column tiles; the first favorite is highlighted with the accent color. */
@Composable
private fun FavoriteTiles(
    favorites: List<AppEntry>,
    prefs: Prefs,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        favorites.chunked(2).forEachIndexed { rowIndex, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

/** Light tile: thin outline instead of a solid block, gently shrinks when pressed. */
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
    val pressScale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "press")
    val notification = notificationFor(app, prefs)
    val over = isOverLimit(app, prefs)
    val shape = RoundedCornerShape(20.dp)
    val foreground = when {
        highlighted -> OnAccent
        over -> Muted
        else -> White
    }

    Column(
        modifier
            .height(88.dp * scale)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(shape)
            .background(if (highlighted) Accent else Raised.copy(alpha = 0.35f))
            .then(if (highlighted) Modifier else Modifier.border(1.dp, Raised, shape))
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
                AppIcon(app, prefs, 26.dp, onAccent = highlighted, dimmed = over)
            } else {
                Box(Modifier.size(7.dp).background(if (highlighted) OnAccent else Accent, CircleShape))
            }
            Spacer(Modifier.weight(1f))
            if (notification != null) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (highlighted) OnAccent else Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${notification.count.coerceAtMost(9)}",
                        color = if (highlighted) Accent else OnAccent,
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
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------- Favorites: categories

/** Big category titles ("Social", "Banque"…); tapping one unfolds its apps. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoriesView(
    apps: List<AppEntry>,
    favorites: List<AppEntry>,
    prefs: Prefs,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    val scale = prefs.textSize.scale
    val groups = remember(apps, favorites, prefs.categories) {
        val byKey = apps.associateBy { it.key }
        buildList {
            if (favorites.isNotEmpty()) add("Favoris" to favorites)
            prefs.categories.forEach { category ->
                val list = category.keys.mapNotNull { byKey[it] }
                if (list.isNotEmpty()) add(category.name to list)
            }
        }
    }
    var open by rememberSaveable { mutableIntStateOf(0) }

    if (groups.isEmpty()) {
        Text(
            "Crée des catégories dans Réglages › Catégories.",
            color = Muted,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )
        return
    }

    Column {
        groups.forEachIndexed { index, (name, list) ->
            val expanded = index == open
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        open = if (expanded) -1 else index
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name,
                    color = if (expanded) White else Muted.copy(alpha = 0.55f),
                    fontSize = (36 * scale).sp,
                    lineHeight = (40 * scale).sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                )
                if (expanded) {
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(9.dp).background(Accent, CircleShape))
                } else {
                    val unread = list.count { notificationFor(it, prefs) != null }
                    if (unread > 0) {
                        Spacer(Modifier.width(10.dp))
                        Text("$unread", color = Accent, fontSize = (14 * scale).sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(Modifier.padding(start = 2.dp, top = 4.dp, bottom = 12.dp)) {
                    list.forEach { app ->
                        val over = isOverLimit(app, prefs)
                        val notification = notificationFor(app, prefs)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onLongClick = { onLongPress(app) },
                                    onClick = { onLaunch(app) },
                                )
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (prefs.iconStyle != IconStyle.NONE) {
                                AppIcon(app, prefs, 22.dp * scale, dimmed = over)
                                Spacer(Modifier.width(12.dp))
                            }
                            Text(
                                app.label,
                                color = if (over) Muted.copy(alpha = 0.6f) else White,
                                fontSize = (19 * scale).sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (notification != null) {
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.size(6.dp).background(Accent, CircleShape))
                            }
                            appTime(app, prefs)?.let { time ->
                                Spacer(Modifier.width(10.dp))
                                Text(time, color = if (over) Accent else Muted, fontSize = (12 * scale).sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
