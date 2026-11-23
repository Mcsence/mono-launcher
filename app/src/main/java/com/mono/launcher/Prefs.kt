package com.mono.launcher

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class FavStyle(val label: String) { LIST("Liste"), TILES("Tuiles") }

enum class IconStyle(val label: String) {
    NONE("Aucune"), MONO("N&B"), COLOR("Couleur"), THEMED("Accent")
}

enum class IconShape(val label: String) {
    SYSTEM("Origine"), CIRCLE("Rond"), SQUIRCLE("Arrondi"), TEARDROP("Goutte")
}

enum class TextSize(val label: String, val scale: Float) {
    XSMALL("XS", 0.75f), SMALL("S", 0.88f), MEDIUM("M", 1f), LARGE("L", 1.15f)
}

/** What the icon bitmaps depend on; changing any of these reloads the app list. */
data class IconConfig(val style: IconStyle, val shape: IconShape, val pack: String)

/**
 * User settings, persisted in SharedPreferences and exposed as Compose state.
 * One shared instance so the home screen, the lock screen and the service agree.
 */
class Prefs private constructor(context: Context) {
    private val sp = context.getSharedPreferences("mono_prefs", Context.MODE_PRIVATE)

    var accent by mutableStateOf(sp.getLong(K_ACCENT, DEFAULT_ACCENT))
        private set
    var favStyle by mutableStateOf(enumPref(K_FAV_STYLE, FavStyle.LIST))
        private set
    var iconStyle by mutableStateOf(enumPref(K_ICON_STYLE, IconStyle.MONO))
        private set
    var iconShape by mutableStateOf(enumPref(K_ICON_SHAPE, IconShape.SYSTEM))
        private set
    /** Package name of the selected icon pack, or "" for none. */
    var iconPack by mutableStateOf(sp.getString(K_ICON_PACK, "").orEmpty())
        private set
    var textSize by mutableStateOf(enumPref(K_TEXT_SIZE, TextSize.MEDIUM))
        private set
    var showClock by mutableStateOf(sp.getBoolean(K_SHOW_CLOCK, true))
        private set
    var lockScreen by mutableStateOf(sp.getBoolean(K_LOCK_SCREEN, false))
        private set

    /** Ordered list of favorite app keys (flattened component names). */
    var favorites by mutableStateOf(
        sp.getString(K_FAVORITES, "").orEmpty().split('\n').filter { it.isNotBlank() }
    )
        private set

    fun iconConfig() = IconConfig(iconStyle, iconShape, iconPack)

    fun updateAccent(value: Long) {
        accent = value
        sp.edit().putLong(K_ACCENT, value).apply()
    }

    fun updateFavStyle(value: FavStyle) {
        favStyle = value
        sp.edit().putString(K_FAV_STYLE, value.name).apply()
    }

    fun updateIconStyle(value: IconStyle) {
        iconStyle = value
        sp.edit().putString(K_ICON_STYLE, value.name).apply()
    }

    fun updateIconShape(value: IconShape) {
        iconShape = value
        sp.edit().putString(K_ICON_SHAPE, value.name).apply()
    }

    fun updateIconPack(value: String) {
        iconPack = value
        sp.edit().putString(K_ICON_PACK, value).apply()
    }

    fun updateTextSize(value: TextSize) {
        textSize = value
        sp.edit().putString(K_TEXT_SIZE, value.name).apply()
    }

    fun updateShowClock(value: Boolean) {
        showClock = value
        sp.edit().putBoolean(K_SHOW_CLOCK, value).apply()
    }

    fun updateLockScreen(value: Boolean) {
        lockScreen = value
        sp.edit().putBoolean(K_LOCK_SCREEN, value).apply()
    }

    fun isFavorite(key: String) = key in favorites

    fun toggleFavorite(key: String) {
        saveFavorites(if (key in favorites) favorites - key else favorites + key)
    }

    fun moveFavorite(key: String, delta: Int) {
        val from = favorites.indexOf(key)
        val to = from + delta
        if (from < 0 || to !in favorites.indices) return
        saveFavorites(favorites.toMutableList().apply { add(to, removeAt(from)) })
    }

    private fun saveFavorites(list: List<String>) {
        favorites = list
        sp.edit().putString(K_FAVORITES, list.joinToString("\n")).apply()
    }

    private inline fun <reified T : Enum<T>> enumPref(key: String, default: T): T =
        sp.getString(key, null)?.let { name -> enumValues<T>().firstOrNull { it.name == name } }
            ?: default

    companion object {
        const val DEFAULT_ACCENT = 0xFFD7FF3C
        private const val K_ACCENT = "accent"
        private const val K_FAV_STYLE = "fav_style"
        private const val K_ICON_STYLE = "icon_style"
        private const val K_ICON_SHAPE = "icon_shape"
        private const val K_ICON_PACK = "icon_pack"
        private const val K_TEXT_SIZE = "text_size"
        private const val K_SHOW_CLOCK = "show_clock"
        private const val K_LOCK_SCREEN = "lock_screen"
        private const val K_FAVORITES = "favorites"

        @Volatile
        private var instance: Prefs? = null

        fun get(context: Context): Prefs =
            instance ?: synchronized(this) {
                instance ?: Prefs(context.applicationContext).also { instance = it }
            }
    }
}
