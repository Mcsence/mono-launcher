package com.mono.launcher.ui

import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.AppEntry
import com.mono.launcher.FeedState
import com.mono.launcher.Feeds
import com.mono.launcher.NotificationState
import com.mono.launcher.Prefs
import com.mono.launcher.Quote
import com.mono.launcher.ScreenTime
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Reference for the screen-time gauge when no daily limit is set: 4 hours. */
private const val SCREEN_TIME_GAUGE_MS = 4 * 60 * 60_000L

/**
 * Lock screen, "cards" style: clock, then a grid of thin cards (screen time, markets,
 * top headline, notifications, battery) and the music player. No apps, and never the
 * text of a notification, only how many and from which apps.
 */
@Composable
fun LockScreen(
    apps: List<AppEntry>,
    prefs: Prefs,
    onOpenLink: (String) -> Unit,
    onUnlock: () -> Unit,
) {
    val unlock by rememberUpdatedState(onUnlock)
    val context = LocalContext.current
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1_000L)
        }
    }
    val battery = remember(now.minute) {
        context.getSystemService(BatteryManager::class.java)
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 0..100 }
    }
    val notifications = NotificationState.byPackage
    val notifCount = notifications.values.sumOf { it.count }
    val notifApps = remember(notifications, apps) {
        val labels = apps.associate { it.component.packageName to it.label }
        notifications.keys.mapNotNull { labels[it] }.take(2).joinToString(", ")
    }
    val quotes = if (prefs.showMarkets) FeedState.quotes else emptyList()
    val headline = if (prefs.showNews) FeedState.headlines.firstOrNull() else null
    val screenTotal = ScreenTime.total
    val media = NotificationState.media
    val scale = prefs.textSize.scale

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
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Clock: bold hours, light minutes, date block on the right.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            val muted = Muted
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("%02d".format(now.hour)) }
                    withStyle(SpanStyle(fontWeight = FontWeight.Light, color = muted)) { append(":") }
                    withStyle(SpanStyle(fontWeight = FontWeight.Light)) { append("%02d".format(now.minute)) }
                },
                color = White,
                fontSize = (72 * scale).sp,
                lineHeight = (72 * scale).sp,
                letterSpacing = (-3).sp,
                modifier = Modifier.weight(1f),
            )
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    now.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase().trimEnd('.'),
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                )
                Text("${now.dayOfMonth}", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    now.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault())).uppercase().trimEnd('.'),
                    color = Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Cards
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCard("ÉCRAN", Modifier.weight(1f)) {
                    Text(
                        if (screenTotal >= 60_000) ScreenTime.format(screenTotal) else "—",
                        color = White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Gauge((screenTotal.toFloat() / SCREEN_TIME_GAUGE_MS).coerceIn(0f, 1f))
                }
                quotes.getOrNull(0)?.let { QuoteCard(it, Modifier.weight(1f)) }
                    ?: InfoCard("BATTERIE", Modifier.weight(1f)) { BigValue(battery?.let { "$it %" } ?: "—") }
            }

            headline?.let { news ->
                InfoCard("À LA UNE · ${news.source.uppercase()}", Modifier.fillMaxWidth(), onClick = { onOpenLink(news.link) }) {
                    Text(
                        news.title,
                        color = White,
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val ago = Feeds.ago(news.published)
                    if (ago.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(ago, color = Muted, fontSize = 11.sp)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quotes.getOrNull(1)?.let { QuoteCard(it, Modifier.weight(1f)) }
                    ?: InfoCard("BATTERIE", Modifier.weight(1f)) { BigValue(battery?.let { "$it %" } ?: "—") }
                InfoCard("NOTIFS", Modifier.weight(1f)) {
                    BigValue(if (notifCount > 0) "$notifCount" else "0")
                    Text(
                        if (notifCount > 0) notifApps else "Rien de neuf",
                        color = Muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (quotes.size > 2 && quotes.getOrNull(0) != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuoteCard(quotes[2], Modifier.weight(1f))
                    InfoCard("BATTERIE", Modifier.weight(1f)) { BigValue(battery?.let { "$it %" } ?: "—") }
                }
            }
        }

        if (prefs.showMusic && media != null) {
            MusicPlayer(media, Modifier.padding(top = 8.dp))
        }
        Text(
            "POSE TON DOIGT · OU GLISSE ↑ POUR LE CODE",
            color = Muted,
            fontSize = 11.sp,
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

/** Thin outlined card with a small uppercase label. */
@Composable
private fun InfoCard(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(Raised.copy(alpha = 0.35f))
            .border(1.dp, Raised, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            label,
            color = Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun BigValue(text: String) {
    Text(text, color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
}

@Composable
private fun QuoteCard(quote: Quote, modifier: Modifier = Modifier) {
    InfoCard(quote.name.uppercase(), modifier) {
        Text(Feeds.formatPrice(quote), color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(
            Feeds.formatChange(quote),
            color = if (quote.changePercent >= 0) Accent else Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Thin progress bar in the accent color. */
@Composable
private fun Gauge(progress: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Raised)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Accent)
        )
    }
}
