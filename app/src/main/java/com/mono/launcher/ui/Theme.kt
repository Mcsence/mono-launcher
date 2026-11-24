package com.mono.launcher.ui

import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import com.mono.launcher.ThemeMode

/** Colors of one theme. Names below (Black, White…) read from the active palette. */
data class Palette(
    /** Background of full-screen panels (drawer, settings). */
    val background: Color,
    /** Background of the home and lock screens (transparent-ish over the wallpaper). */
    val home: Color,
    val foreground: Color,
    val panel: Color,
    val raised: Color,
    val raisedPressed: Color,
    val muted: Color,
    val isLight: Boolean,
)

val DarkPalette = Palette(
    background = Color(0xFF000000),
    home = Color(0xFF000000),
    foreground = Color(0xFFFFFFFF),
    panel = Color(0xFF111111),
    raised = Color(0xFF1C1C1C),
    raisedPressed = Color(0xFF2A2A2A),
    muted = Color(0xFF8C8C8C),
    isLight = false,
)

val LightPalette = Palette(
    background = Color(0xFFFFFFFF),
    home = Color(0xFFFFFFFF),
    foreground = Color(0xFF0A0A0A),
    panel = Color(0xFFF5F5F5),
    raised = Color(0xFFEFEFEF),
    raisedPressed = Color(0xFFE0E0E0),
    muted = Color(0xFF6E6E6E),
    isLight = true,
)

/** Dark theme drawn over the phone's wallpaper, darkened by [dim] (0..1). */
fun wallpaperPalette(dim: Float) = DarkPalette.copy(
    background = Color(0xF0000000),
    home = Color.Black.copy(alpha = dim),
    raised = Color(0x33FFFFFF),
    raisedPressed = Color(0x4DFFFFFF),
    muted = Color(0xFFB0B0B0),
)

fun paletteFor(mode: ThemeMode, dim: Float): Palette = when (mode) {
    ThemeMode.DARK -> DarkPalette
    ThemeMode.LIGHT -> LightPalette
    ThemeMode.WALLPAPER -> wallpaperPalette(dim)
}

val LocalPalette = staticCompositionLocalOf { DarkPalette }
val LocalMonoFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

// "Black" = page background and "White" = main text; they swap in the light theme.
val Black: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.background
val White: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.foreground
val HomeBackground: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.home
val Panel: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.panel
val Raised: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.raised
val RaisedPressed: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.raisedPressed
val Muted: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.muted

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

/** Text / icons drawn on top of the accent color: black on bright accents, white on dark ones. */
val OnAccent: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onPrimary

@Composable
fun MonoTheme(
    accent: Color,
    palette: Palette = DarkPalette,
    font: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit,
) {
    // A white accent would vanish on the light theme: use black instead.
    val effectiveAccent = if (palette.isLight && accent.luminance() > 0.9f) Color(0xFF0A0A0A) else accent
    val onAccent = if (effectiveAccent.luminance() > 0.4f) Color(0xFF000000) else Color(0xFFFFFFFF)
    val colors = if (palette.isLight) {
        lightColorScheme(
            primary = effectiveAccent,
            onPrimary = onAccent,
            background = palette.background,
            onBackground = palette.foreground,
            surface = palette.panel,
            onSurface = palette.foreground,
        )
    } else {
        darkColorScheme(
            primary = effectiveAccent,
            onPrimary = onAccent,
            background = palette.background,
            onBackground = palette.foreground,
            surface = palette.panel,
            onSurface = palette.foreground,
        )
    }
    val base = Typography()
    val typography = base.copy(
        bodyLarge = base.bodyLarge.copy(fontFamily = font),
        bodyMedium = base.bodyMedium.copy(fontFamily = font),
        bodySmall = base.bodySmall.copy(fontFamily = font),
        titleLarge = base.titleLarge.copy(fontFamily = font),
        labelLarge = base.labelLarge.copy(fontFamily = font),
    )
    CompositionLocalProvider(LocalPalette provides palette, LocalMonoFont provides font) {
        MaterialTheme(colorScheme = colors, typography = typography, content = content)
    }
}

/** Status bar icons, window background and wallpaper visibility for the chosen theme. */
fun ComponentActivity.applyMonoWindow(mode: ThemeMode, isHome: Boolean = false) {
    val transparent = android.graphics.Color.TRANSPARENT
    if (mode == ThemeMode.LIGHT) {
        enableEdgeToEdge(SystemBarStyle.light(transparent, transparent), SystemBarStyle.light(transparent, transparent))
    } else {
        enableEdgeToEdge(SystemBarStyle.dark(transparent), SystemBarStyle.dark(transparent))
    }
    if (mode == ThemeMode.WALLPAPER) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawable(ColorDrawable(transparent))
    } else {
        // The home window stays a wallpaper window (set in its theme); an opaque background covers it.
        if (!isHome) window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        val bg = if (mode == ThemeMode.LIGHT) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        window.setBackgroundDrawable(ColorDrawable(bg))
    }
}
