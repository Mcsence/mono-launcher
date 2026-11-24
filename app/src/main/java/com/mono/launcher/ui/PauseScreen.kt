package com.mono.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.AppEntry
import com.mono.launcher.Prefs
import com.mono.launcher.ScreenTime
import kotlinx.coroutines.delay

/** Short pause before an app with a daily limit; longer once the limit is reached. */
fun pauseSecondsFor(app: AppEntry, prefs: Prefs): Int? {
    val limit = prefs.limits[app.component.packageName] ?: return null
    val used = ScreenTime.today[app.component.packageName] ?: 0L
    return if (used >= limit * 60_000L) 20 else 5
}

@Composable
fun PauseScreen(app: AppEntry, prefs: Prefs, onOpen: () -> Unit, onCancel: () -> Unit) {
    val pkg = app.component.packageName
    val limit = prefs.limits[pkg] ?: 0
    val used = ScreenTime.today[pkg] ?: 0L
    val over = used >= limit * 60_000L
    val total = if (over) 20 else 5
    var remaining by remember(app.key) { mutableIntStateOf(total) }
    LaunchedEffect(app.key) {
        while (remaining > 0) {
            delay(1_000L)
            remaining--
        }
    }
    BackHandler(onBack = onCancel)

    val breathing = rememberInfiniteTransition(label = "breath")
    val scale by breathing.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
            .swallowTouches()
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(160.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape)
                    .background(if (over) Raised else Accent.copy(alpha = 0.25f))
            )
            Text(
                if (remaining > 0) "$remaining" else "·",
                color = White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            if (scale > 0.95f) "Expire…" else "Inspire…",
            color = Muted,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(24.dp))
        Text(app.label, color = White, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            if (over) {
                "Limite de $limit min dépassée · ${ScreenTime.format(used)} aujourd'hui"
            } else {
                "${ScreenTime.format(used).takeIf { used >= 60_000 } ?: "0 min"} sur $limit min aujourd'hui"
            },
            color = if (over) Accent else Muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Text(
            "Je n'en ai pas besoin",
            color = OnAccent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Accent)
                .clickable(onClick = onCancel)
                .padding(vertical = 16.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (remaining > 0) "Ouvrir quand même (${remaining}s)" else "Ouvrir quand même",
            color = if (remaining > 0) Muted.copy(alpha = 0.5f) else White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = remaining == 0, onClick = onOpen)
                .padding(vertical = 14.dp),
        )
    }
}
