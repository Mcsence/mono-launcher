package com.mono.launcher

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

enum class ThemeMode(val label: String) { DARK("Sombre"), LIGHT("Clair"), WALLPAPER("Fond d'écran") }

/** How much the wallpaper is darkened behind the home screen. */
enum class WallpaperDim(val label: String, val alpha: Float) {
    LIGHT("Léger", 0.25f), MEDIUM("Moyen", 0.5f), STRONG("Fort", 0.72f)
}

enum class ClockStyle(val label: String) {
    CLASSIC("Classique"), DUO("Duo"), STACKED("Empilée"), DISCREET("Discrète")
}

/** Bundled open-source fonts (SIL Open Font License, from Google Fonts). */
enum class FontChoice(val label: String, private val resId: Int?) {
    SYSTEM("Système", null),
    GROTESK("Grotesk", R.font.space_grotesk),
    MANROPE("Manrope", R.font.manrope),
    SYNE("Syne", R.font.syne),
    UNBOUNDED("Unbounded", R.font.unbounded);

    /** Variable fonts: one file, each weight picked on its "wght" axis. */
    @OptIn(ExperimentalTextApi::class)
    val family: FontFamily by lazy {
        val id = resId ?: return@lazy FontFamily.Default
        FontFamily(
            listOf(
                FontWeight.Light,
                FontWeight.Normal,
                FontWeight.Medium,
                FontWeight.SemiBold,
                FontWeight.Bold,
                FontWeight.ExtraBold,
                FontWeight.Black,
            ).map { weight -> Font(id, weight) }
        )
    }
}
