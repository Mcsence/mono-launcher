package com.mono.launcher

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class FavStyle(val label: String) { LIST("Liste"), TILES("Tuiles"), CATEGORIES("Catégories") }

/** A named group of apps shown as a big title on the home screen. */
data class Category(val name: String, val keys: List<String>)

/** A real Android widget placed on the home screen. */
data class WidgetEntry(val id: Int, val heightDp: Int)

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
    var showNotifications by mutableStateOf(sp.getBoolean(K_SHOW_NOTIFICATIONS, true))
        private set
    var showMusic by mutableStateOf(sp.getBoolean(K_SHOW_MUSIC, true))
        private set

    var screenTime by mutableStateOf(enumPref(K_SCREEN_TIME, ScreenTimeMode.OFF))
        private set

    var focusEnabled by mutableStateOf(sp.getBoolean(K_FOCUS, false))
        private set
    /** Apps that stay visible during focus mode. */
    var focusApps by mutableStateOf(
        sp.getString(K_FOCUS_APPS, "").orEmpty().split('\n').filter { it.isNotBlank() }
    )
        private set
    /** Minutes since midnight. */
    var focusStart by mutableStateOf(sp.getInt(K_FOCUS_START, 9 * 60))
        private set
    var focusEnd by mutableStateOf(sp.getInt(K_FOCUS_END, 18 * 60))
        private set
    /** Days of week, 1 = Monday … 7 = Sunday. */
    var focusDays by mutableStateOf(
        sp.getString(K_FOCUS_DAYS, "1,2,3,4,5").orEmpty().split(',').mapNotNull { it.toIntOrNull() }.toSet()
    )
        private set
    /** Focus is ignored until this time (epoch ms), set by "Sortir". */
    var focusPausedUntil by mutableStateOf(sp.getLong(K_FOCUS_PAUSED, 0L))
        private set

    fun updateScreenTime(value: ScreenTimeMode) {
        screenTime = value
        sp.edit().putString(K_SCREEN_TIME, value.name).apply()
    }

    fun updateFocusEnabled(value: Boolean) {
        focusEnabled = value
        focusPausedUntil = 0L
        sp.edit().putBoolean(K_FOCUS, value).putLong(K_FOCUS_PAUSED, 0L).apply()
    }

    fun toggleFocusApp(key: String) {
        focusApps = if (key in focusApps) focusApps - key else focusApps + key
        sp.edit().putString(K_FOCUS_APPS, focusApps.joinToString("\n")).apply()
    }

    fun updateFocusStart(minutes: Int) {
        focusStart = minutes
        sp.edit().putInt(K_FOCUS_START, minutes).apply()
    }

    fun updateFocusEnd(minutes: Int) {
        focusEnd = minutes
        sp.edit().putInt(K_FOCUS_END, minutes).apply()
    }

    fun toggleFocusDay(day: Int) {
        focusDays = if (day in focusDays) focusDays - day else focusDays + day
        sp.edit().putString(K_FOCUS_DAYS, focusDays.sorted().joinToString(",")).apply()
    }

    fun pauseFocusUntil(epochMillis: Long) {
        focusPausedUntil = epochMillis
        sp.edit().putLong(K_FOCUS_PAUSED, epochMillis).apply()
    }

    /** Gesture targets: [Gestures.NONE], [Gestures.CAMERA], [Gestures.MESSAGES] or an app key. */
    var swipeLeft by mutableStateOf(sp.getString(K_SWIPE_LEFT, Gestures.MESSAGES).orEmpty())
        private set
    var swipeRight by mutableStateOf(sp.getString(K_SWIPE_RIGHT, Gestures.CAMERA).orEmpty())
        private set
    var swipeDownNotifications by mutableStateOf(sp.getBoolean(K_SWIPE_DOWN, true))
        private set
    var doubleTapLock by mutableStateOf(sp.getBoolean(K_DOUBLE_TAP, false))
        private set

    fun updateSwipeLeft(target: String) {
        swipeLeft = target
        sp.edit().putString(K_SWIPE_LEFT, target).apply()
    }

    fun updateSwipeRight(target: String) {
        swipeRight = target
        sp.edit().putString(K_SWIPE_RIGHT, target).apply()
    }

    fun updateSwipeDownNotifications(value: Boolean) {
        swipeDownNotifications = value
        sp.edit().putBoolean(K_SWIPE_DOWN, value).apply()
    }

    fun updateDoubleTapLock(value: Boolean) {
        doubleTapLock = value
        sp.edit().putBoolean(K_DOUBLE_TAP, value).apply()
    }

    var theme by mutableStateOf(enumPref(K_THEME, ThemeMode.DARK))
        private set
    var wallpaperDim by mutableStateOf(enumPref(K_WALLPAPER_DIM, WallpaperDim.MEDIUM))
        private set
    var font by mutableStateOf(enumPref(K_FONT, FontChoice.GROTESK))
        private set
    var clockStyle by mutableStateOf(enumPref(K_CLOCK_STYLE, ClockStyle.DUO))
        private set

    fun updateTheme(value: ThemeMode) {
        theme = value
        sp.edit().putString(K_THEME, value.name).apply()
    }

    fun updateWallpaperDim(value: WallpaperDim) {
        wallpaperDim = value
        sp.edit().putString(K_WALLPAPER_DIM, value.name).apply()
    }

    fun updateFont(value: FontChoice) {
        font = value
        sp.edit().putString(K_FONT, value.name).apply()
    }

    fun updateClockStyle(value: ClockStyle) {
        clockStyle = value
        sp.edit().putString(K_CLOCK_STYLE, value.name).apply()
    }

    // ---- Categories ----

    var categories by mutableStateOf(parseCategories(sp.getString(K_CATEGORIES, null)))
        private set

    fun addCategory(name: String): Int {
        saveCategories(categories + Category(name.trim(), emptyList()))
        return categories.lastIndex
    }

    fun renameCategory(index: Int, name: String) {
        if (index !in categories.indices || name.isBlank()) return
        saveCategories(categories.toMutableList().apply { this[index] = this[index].copy(name = name.trim()) })
    }

    fun deleteCategory(index: Int) {
        if (index !in categories.indices) return
        saveCategories(categories.toMutableList().apply { removeAt(index) })
    }

    fun toggleCategoryApp(index: Int, key: String) {
        if (index !in categories.indices) return
        val category = categories[index]
        val keys = if (key in category.keys) category.keys - key else category.keys + key
        saveCategories(categories.toMutableList().apply { this[index] = category.copy(keys = keys) })
    }

    private fun saveCategories(list: List<Category>) {
        categories = list
        val raw = list.joinToString("\n") { c -> c.name + LABEL_SEP + c.keys.joinToString(ITEM_SEP) }
        sp.edit().putString(K_CATEGORIES, raw).apply()
    }

    private fun parseCategories(raw: String?): List<Category> =
        raw.orEmpty().split('\n').filter { it.isNotBlank() }.map { line ->
            val parts = line.split(LABEL_SEP, limit = 2)
            Category(parts[0], parts.getOrNull(1).orEmpty().split(ITEM_SEP).filter { it.isNotBlank() })
        }

    // ---- Daily limits (minutes per day, by package name) ----

    var limits by mutableStateOf(
        sp.getString(K_LIMITS, "").orEmpty().split('\n').mapNotNull { line ->
            val parts = line.split(LABEL_SEP, limit = 2)
            val minutes = parts.getOrNull(1)?.toIntOrNull()
            if (parts.size == 2 && minutes != null) parts[0] to minutes else null
        }.toMap()
    )
        private set

    fun setLimit(packageName: String, minutes: Int?) {
        val map = limits.toMutableMap()
        if (minutes == null || minutes <= 0) map.remove(packageName) else map[packageName] = minutes
        limits = map
        sp.edit().putString(K_LIMITS, map.entries.joinToString("\n") { "${it.key}$LABEL_SEP${it.value}" }).apply()
    }

    // ---- Widgets ----

    var widgets by mutableStateOf(
        sp.getString(K_WIDGETS, "").orEmpty().split(',').mapNotNull { item ->
            val parts = item.split(':')
            val id = parts.getOrNull(0)?.toIntOrNull()
            val height = parts.getOrNull(1)?.toIntOrNull()
            if (id != null && height != null) WidgetEntry(id, height) else null
        }
    )
        private set

    fun addWidget(id: Int, heightDp: Int) = saveWidgets(widgets + WidgetEntry(id, heightDp))

    fun removeWidget(id: Int) = saveWidgets(widgets.filter { it.id != id })

    fun setWidgetHeight(id: Int, heightDp: Int) =
        saveWidgets(widgets.map { if (it.id == id) it.copy(heightDp = heightDp) else it })

    private fun saveWidgets(list: List<WidgetEntry>) {
        widgets = list
        sp.edit().putString(K_WIDGETS, list.joinToString(",") { "${it.id}:${it.heightDp}" }).apply()
    }

    /** User-chosen names, by app key. */
    var customLabels by mutableStateOf(parseLabels(sp.getString(K_LABELS, null)))
        private set

    fun updateShowNotifications(value: Boolean) {
        showNotifications = value
        sp.edit().putBoolean(K_SHOW_NOTIFICATIONS, value).apply()
    }

    fun updateShowMusic(value: Boolean) {
        showMusic = value
        sp.edit().putBoolean(K_SHOW_MUSIC, value).apply()
    }

    /** Renames an app; a blank or null [label] restores its original name. */
    fun setLabel(key: String, label: String?) {
        val map = customLabels.toMutableMap()
        if (label.isNullOrBlank()) map.remove(key) else map[key] = label.trim()
        customLabels = map
        sp.edit()
            .putString(K_LABELS, map.entries.joinToString("\n") { "${it.key}$LABEL_SEP${it.value}" })
            .apply()
    }

    private fun parseLabels(raw: String?): Map<String, String> =
        raw.orEmpty().split('\n').mapNotNull { line ->
            val parts = line.split(LABEL_SEP, limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()

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
        private const val K_SHOW_NOTIFICATIONS = "show_notifications"
        private const val K_SHOW_MUSIC = "show_music"
        private const val K_LABELS = "custom_labels"
        private const val K_SCREEN_TIME = "screen_time"
        private const val K_THEME = "theme"
        private const val K_WALLPAPER_DIM = "wallpaper_dim"
        private const val K_FONT = "font"
        private const val K_CLOCK_STYLE = "clock_style"
        private const val K_SWIPE_LEFT = "swipe_left"
        private const val K_SWIPE_RIGHT = "swipe_right"
        private const val K_SWIPE_DOWN = "swipe_down_notifications"
        private const val K_DOUBLE_TAP = "double_tap_lock"
        private const val K_FOCUS = "focus_enabled"
        private const val K_FOCUS_APPS = "focus_apps"
        private const val K_FOCUS_START = "focus_start"
        private const val K_FOCUS_END = "focus_end"
        private const val K_FOCUS_DAYS = "focus_days"
        private const val K_FOCUS_PAUSED = "focus_paused_until"
        private const val LABEL_SEP = "\u001F"
        private const val ITEM_SEP = "\u001E"
        private const val K_CATEGORIES = "categories"
        private const val K_LIMITS = "daily_limits"
        private const val K_WIDGETS = "widgets"

        @Volatile
        private var instance: Prefs? = null

        fun get(context: Context): Prefs =
            instance ?: synchronized(this) {
                instance ?: Prefs(context.applicationContext).also { instance = it }
            }
    }
}
