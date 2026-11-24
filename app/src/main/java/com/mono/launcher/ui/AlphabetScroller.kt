package com.mono.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.PI

private val LETTERS = listOf("#") + ('A'..'Z').map { it.toString() }

/** How many letters above and below the finger are pushed by the wave. */
private const val WAVE_RADIUS = 5f

/**
 * Vertical A–Z strip. Touch or drag along it to jump to a letter; letters with
 * no app jump to the next letter that has one. Letters near the finger swell and
 * slide out to the left in a wave.
 */
@Composable
fun AlphabetScroller(
    available: Set<String>,
    onLetter: (String) -> Unit,
    onActiveChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var active by remember { mutableStateOf<String?>(null) }
    var heightPx by remember { mutableIntStateOf(1) }
    var fingerY by remember { mutableFloatStateOf(0f) }
    val currentAvailable by rememberUpdatedState(available)
    val currentOnLetter by rememberUpdatedState(onLetter)
    val currentOnActiveChange by rememberUpdatedState(onActiveChange)

    // 0 = at rest, 1 = finger down: the wave grows and settles with a spring.
    val strength by animateFloatAsState(
        targetValue = if (active != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "wave",
    )
    val shiftPx = with(LocalDensity.current) { 36.dp.toPx() }

    fun pick(y: Float) {
        fingerY = y
        val index = ((y / heightPx) * LETTERS.size).toInt().coerceIn(0, LETTERS.lastIndex)
        val letter = LETTERS[index]
        if (letter == active) return
        active = letter
        currentOnActiveChange(letter)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val target = LETTERS.drop(index).firstOrNull { it in currentAvailable }
            ?: LETTERS.take(index).lastOrNull { it in currentAvailable }
        target?.let(currentOnLetter)
    }

    Column(
        modifier
            .width(28.dp)
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    pick(down.position.y)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        pick(change.position.y)
                    }
                    active = null
                    currentOnActiveChange(null)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val slot = heightPx / LETTERS.size.toFloat()
        LETTERS.forEachIndexed { index, letter ->
            // Distance from the finger, in letters; a smooth bump that fades out at WAVE_RADIUS.
            val distance = abs((index + 0.5f) * slot - fingerY) / slot
            val bump = if (distance < WAVE_RADIUS) ((cos(distance / WAVE_RADIUS * PI) + 1) / 2).toFloat() else 0f
            val wave = bump * strength
            Box(Modifier.height(18.dp), contentAlignment = Alignment.Center) {
                Text(
                    letter,
                    fontSize = 11.sp,
                    fontWeight = if (letter == active) FontWeight.Black else FontWeight.Bold,
                    color = when {
                        letter == active -> Accent
                        letter in available -> White
                        else -> Muted.copy(alpha = 0.35f)
                    },
                    modifier = Modifier.graphicsLayer {
                        val grow = 1f + wave * 1.1f
                        scaleX = grow
                        scaleY = grow
                        translationX = -wave * shiftPx
                    },
                )
            }
        }
    }
}
