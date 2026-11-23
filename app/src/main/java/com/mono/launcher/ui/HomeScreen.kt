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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.AppEntry
import com.mono.launcher.FavStyle
import com.mono.launcher.IconStyle
import com.mono.launcher.Prefs
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    prefs: Prefs,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    val favorites = remember(apps, prefs.favorites) {
        val byKey = apps.associateBy { it.key }
        prefs.favorites.mapNotNull { byKey[it] }
    }
    val openDrawer by rememberUpdatedState(onOpenDrawer)

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
            Spacer(Modifier.height(32.dp))
        } else {
            Spacer(Modifier.height(24.dp))
        }

        SectionLabel("Favoris")
        Spacer(Modifier.height(16.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                favorites.isEmpty() -> Text(
                    "Glisse vers le haut, puis appuie longuement\nsur une app pour l'épingler ici.",
                    color = Muted,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                )
                prefs.favStyle == FavStyle.LIST -> FavoriteList(favorites, prefs, onLaunch, onLongPress)
                else -> FavoriteTiles(favorites, prefs, onLaunch, onLongPress)
            }
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

/** Big bold text list, one favorite per line. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteList(
    favorites: List<AppEntry>,
    prefs: Prefs,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    val scale = prefs.textSize.scale
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        favorites.forEach { app ->
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
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
                    AppIcon(app, prefs.iconStyle, 26.dp * scale)
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    app.label,
                    color = if (pressed) Accent else White,
                    fontSize = (28 * scale).sp,
                    lineHeight = (32 * scale).sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
        if (prefs.iconStyle != IconStyle.NONE) {
            AppIcon(app, if (highlighted) IconStyle.MONO else prefs.iconStyle, 28.dp)
        } else {
            Box(Modifier.size(8.dp).background(if (highlighted) Black else Accent, CircleShape))
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
