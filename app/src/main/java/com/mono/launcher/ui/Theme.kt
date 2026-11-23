package com.mono.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Black & white palette + one accent color chosen by the user.
val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val Panel = Color(0xFF111111)
val Raised = Color(0xFF1C1C1C)
val RaisedPressed = Color(0xFF2A2A2A)
val Muted = Color(0xFF8C8C8C)

/** Lime, orange, cyan, pink, violet, white. */
val AccentPresets = listOf(
    0xFFD7FF3C,
    0xFFFF6A2B,
    0xFF3CE6FF,
    0xFFFF3D8B,
    0xFF9C7CFF,
    0xFFFFFFFF,
)

val Accent: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

@Composable
fun MonoTheme(accent: Color, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            onPrimary = Black,
            background = Black,
            onBackground = White,
            surface = Panel,
            onSurface = White,
        ),
        content = content,
    )
}
