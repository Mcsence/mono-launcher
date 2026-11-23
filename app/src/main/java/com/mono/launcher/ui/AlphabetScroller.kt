package com.mono.launcher.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LETTERS = listOf("#") + ('A'..'Z').map { it.toString() }

/**
 * Vertical A–Z strip. Touch or drag along it to jump to a letter; letters with
 * no app jump to the next letter that has one.
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
    val currentAvailable by rememberUpdatedState(available)
    val currentOnLetter by rememberUpdatedState(onLetter)
    val currentOnActiveChange by rememberUpdatedState(onActiveChange)

    fun pick(y: Float) {
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
        LETTERS.forEach { letter ->
            Box(Modifier.height(18.dp), contentAlignment = Alignment.Center) {
                Text(
                    letter,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        letter == active -> Accent
                        letter in available -> White
                        else -> Muted.copy(alpha = 0.35f)
                    },
                )
            }
        }
    }
}
