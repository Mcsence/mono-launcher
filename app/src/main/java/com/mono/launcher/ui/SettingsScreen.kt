package com.mono.launcher.ui

import android.app.TimePickerDialog
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import com.mono.launcher.ClockStyle
import com.mono.launcher.FontChoice
import com.mono.launcher.ScreenTime
import com.mono.launcher.ThemeMode
import com.mono.launcher.WallpaperDim
import androidx.compose.ui.platform.LocalContext
import com.mono.launcher.Gestures
import com.mono.launcher.ScreenTimeMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.AppEntry
import com.mono.launcher.FavStyle
import com.mono.launcher.IconPackInfo
import com.mono.launcher.IconShape
import com.mono.launcher.IconStyle
import com.mono.launcher.Prefs
import com.mono.launcher.TextSize

@Composable
fun SettingsScreen(
    apps: List<AppEntry>,
    prefs: Prefs,
    iconPacks: List<IconPackInfo>,
    overlayGranted: Boolean,
    notificationAccess: Boolean,
    usageAccess: Boolean,
    accessibilityEnabled: Boolean,
    onRequestAccessibility: () -> Unit,
    onClose: () -> Unit,
    onSetDefault: () -> Unit,
    onLockScreenChange: (Boolean) -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onAddWidget: (AppWidgetProviderInfo) -> Unit,
    onRemoveWidget: (Int) -> Unit,
) {
    var picking by remember { mutableStateOf<Picker?>(null) }
    var editingCategory by remember { mutableStateOf<Int?>(null) }
    /** Category whose name is being typed; -1 = a new one. */
    var namingCategory by remember { mutableStateOf<Int?>(null) }
    var limitApp by remember { mutableStateOf<AppEntry?>(null) }
    val context = LocalContext.current
    val isLight = LocalPalette.current.isLight

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Black)
                .swallowTouches()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Réglages", color = White, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                Box(Modifier.padding(start = 6.dp, top = 12.dp).size(9.dp).background(Accent, CircleShape))
                Spacer(Modifier.weight(1f))
                Text(
                    "FERMER",
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onClose)
                        .padding(12.dp),
                )
            }

            Section("Thème") {
                Segmented(ThemeMode.entries, prefs.theme, { it.label }, prefs::updateTheme)
                if (prefs.theme == ThemeMode.WALLPAPER) {
                    Spacer(Modifier.height(12.dp))
                    SubLabel("Assombrir le fond d'écran")
                    Segmented(WallpaperDim.entries, prefs.wallpaperDim, { it.label }, prefs::updateWallpaperDim)
                }
            }

            Section("Police") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Raised)
                ) {
                    FontChoice.entries.forEach { choice ->
                        val selected = prefs.font == choice
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { prefs.updateFont(choice) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                choice.label,
                                color = if (selected) White else Muted,
                                fontFamily = choice.family,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Text("14:32", color = Muted, fontFamily = choice.family, fontSize = 18.sp, fontWeight = FontWeight.Light)
                            Spacer(Modifier.width(12.dp))
                            Box(Modifier.size(8.dp).background(if (selected) Accent else Raised, CircleShape))
                        }
                    }
                }
            }

            Section("Horloge") {
                Segmented(ClockStyle.entries, prefs.clockStyle, { it.label }, prefs::updateClockStyle)
                Spacer(Modifier.height(8.dp))
                Segmented(listOf(true, false), prefs.showClock, { if (it) "Affichée" else "Masquée" }, prefs::updateShowClock)
            }

            Section("Favoris") {
                ActionRow("Choisir les apps favorites", "${prefs.favorites.size}") { picking = Picker.FAVORITES }
                Spacer(Modifier.height(10.dp))
                Segmented(FavStyle.entries, prefs.favStyle, { it.label }, prefs::updateFavStyle)
            }

            Section("Catégories") {
                prefs.categories.forEachIndexed { index, category ->
                    GestureRow(category.name, "${category.keys.size} apps") { editingCategory = index }
                    Spacer(Modifier.height(8.dp))
                }
                ActionRow("Nouvelle catégorie", "+") { namingCategory = -1 }
                if (prefs.categories.isEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Social", "Banque", "Mobilité", "Travail").forEach { name ->
                            Text(
                                name,
                                color = White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Raised)
                                    .clickable { editingCategory = prefs.addCategory(name) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
                Hint("Sur l'accueil : Favoris › style « Catégories ». Touche un titre pour le déplier.")
            }

            Section("Widgets") {
                prefs.widgets.forEach { widget ->
                    WidgetRow(
                        label = remember(widget.id) { widgetLabel(context, widget.id) },
                        heightDp = widget.heightDp,
                        onHeight = { prefs.setWidgetHeight(widget.id, it) },
                        onRemove = { onRemoveWidget(widget.id) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                ActionRow("Ajouter un widget", "+") { picking = Picker.WIDGET }
                Hint("Les widgets s'affichent sous l'horloge.")
            }

            Section("Limites quotidiennes") {
                prefs.limits.forEach { (pkg, minutes) ->
                    val app = apps.firstOrNull { it.component.packageName == pkg }
                    val used = ScreenTime.today[pkg]?.let(ScreenTime::format) ?: "0 min"
                    GestureRow(app?.label ?: pkg, "$used / $minutes min") { limitApp = app }
                    Spacer(Modifier.height(8.dp))
                }
                ActionRow("Ajouter une limite", "+") { picking = Picker.LIMIT_APP }
                if (prefs.limits.isNotEmpty() && !usageAccess) {
                    Spacer(Modifier.height(10.dp))
                    ActionRow("Autoriser l'accès aux données d'utilisation", "!", highlight = true, onClick = onRequestUsageAccess)
                }
                Hint("Avant d'ouvrir une app limitée : 5 s de respiration. Limite dépassée : l'app passe en gris et la pause dure 20 s.")
            }

            Section("Notifications sous les favoris") {
                Segmented(listOf(true, false), prefs.showNotifications, { if (it) "Affichées" else "Masquées" }, prefs::updateShowNotifications)
            }

            Section("Lecteur musique") {
                Segmented(listOf(true, false), prefs.showMusic, { if (it) "Affiché" else "Masqué" }, prefs::updateShowMusic)
                if ((prefs.showNotifications || prefs.showMusic) && !notificationAccess) {
                    Spacer(Modifier.height(10.dp))
                    ActionRow("Autoriser l'accès aux notifications", "!", highlight = true, onClick = onRequestNotificationAccess)
                    Hint("Nécessaire pour les aperçus et le lecteur. Rien ne quitte ton téléphone.")
                }
            }

            Section("Gestes") {
                GestureRow("Glisser vers la droite", Gestures.label(prefs.swipeRight, apps)) { picking = Picker.SWIPE_RIGHT }
                Spacer(Modifier.height(8.dp))
                GestureRow("Glisser vers la gauche", Gestures.label(prefs.swipeLeft, apps)) { picking = Picker.SWIPE_LEFT }
                Spacer(Modifier.height(14.dp))
                Text("Glisser vers le bas", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Segmented(listOf(true, false), prefs.swipeDownNotifications, { if (it) "Notifications" else "Rien" }, prefs::updateSwipeDownNotifications)
                Spacer(Modifier.height(14.dp))
                Text("Double-tap sur l'accueil", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Segmented(listOf(true, false), prefs.doubleTapLock, { if (it) "Éteindre l'écran" else "Rien" }, prefs::updateDoubleTapLock)
                if (prefs.doubleTapLock && !accessibilityEnabled) {
                    Spacer(Modifier.height(10.dp))
                    ActionRow("Activer « Lumen : gestes » dans l'accessibilité", "!", highlight = true, onClick = onRequestAccessibility)
                    Hint(
                        "Android exige un service d'accessibilité pour éteindre l'écran ; Lumen ne lit rien à l'écran. " +
                            "Si l'option est grisée : Paramètres › Applications › Lumen › ⋮ › « Autoriser les paramètres restreints »."
                    )
                }
                Hint("Glisse depuis le milieu de l'écran : depuis les bords, Android fait « retour ».")
            }

            Section("Temps d'écran") {
                Segmented(ScreenTimeMode.entries, prefs.screenTime, { it.label }, prefs::updateScreenTime)
                if (prefs.screenTime != ScreenTimeMode.OFF && !usageAccess) {
                    Spacer(Modifier.height(10.dp))
                    ActionRow("Autoriser l'accès aux données d'utilisation", "!", highlight = true, onClick = onRequestUsageAccess)
                    Hint("Android compte le temps passé dans chaque app ; Lumen l'affiche simplement.")
                } else if (prefs.screenTime != ScreenTimeMode.OFF) {
                    Hint("Total sous l'horloge ; « Par app » l'affiche aussi à côté de chaque app.")
                }
            }

            Section("Mode focus") {
                Segmented(listOf(true, false), prefs.focusEnabled, { if (it) "Programmé" else "Désactivé" }, prefs::updateFocusEnabled)
                if (prefs.focusEnabled) {
                    Spacer(Modifier.height(10.dp))
                    ActionRow("Apps autorisées pendant le focus", "${prefs.focusApps.size}") { picking = Picker.FOCUS }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        TimeChip("De", prefs.focusStart) { pickTime(context, isLight, prefs.focusStart, prefs::updateFocusStart) }
                        TimeChip("à", prefs.focusEnd) { pickTime(context, isLight, prefs.focusEnd, prefs::updateFocusEnd) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("L", "M", "M", "J", "V", "S", "D").forEachIndexed { index, letter ->
                            val day = index + 1
                            val on = day in prefs.focusDays
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (on) Accent else Raised)
                                    .clickable { prefs.toggleFocusDay(day) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(letter, color = if (on) OnAccent else Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Hint("Pendant ces heures, seules les apps autorisées sont visibles. « Sortir » sur l'accueil coupe le focus jusqu'à la fin de la plage.")
                }
            }

            Section("Couleur d'accent") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccentPresets.forEach { value ->
                        val selected = prefs.accent == value
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(value))
                                .then(if (selected) Modifier.border(3.dp, White, CircleShape) else Modifier)
                                .clickable { prefs.updateAccent(value) }
                        )
                    }
                }
            }

            Section("Icônes") {
                Segmented(IconStyle.entries, prefs.iconStyle, { it.label }, prefs::updateIconStyle)
                if (prefs.iconStyle == IconStyle.THEMED) {
                    Hint("Les icônes prennent ta couleur d'accent. Les apps sans version une couleur affichent leur initiale.")
                }
            }

            if (prefs.iconStyle != IconStyle.NONE) {
                Section("Forme des icônes") {
                    Segmented(IconShape.entries, prefs.iconShape, { it.label }, prefs::updateIconShape)
                }

                Section("Pack d'icônes") {
                    if (prefs.iconStyle == IconStyle.THEMED) {
                        Hint("Les packs ne s'appliquent pas en mode Accent.")
                    } else if (iconPacks.isEmpty()) {
                        Hint("Aucun pack installé. Cherche « icon pack » sur le Play Store, installe-en un, puis reviens ici.")
                    } else {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Raised)
                        ) {
                            PackRow("Aucun", prefs.iconPack.isEmpty()) { prefs.updateIconPack("") }
                            iconPacks.forEach { pack ->
                                PackRow(pack.label, prefs.iconPack == pack.packageName) {
                                    prefs.updateIconPack(pack.packageName)
                                }
                            }
                        }
                    }
                }
            }

            Section("Taille du texte") {
                Segmented(TextSize.entries, prefs.textSize, { it.label }, prefs::updateTextSize)
            }

            Section("Écran verrouillé") {
                Segmented(listOf(true, false), prefs.lockScreen, { if (it) "Favoris" else "Désactivé" }, onLockScreenChange)
                Hint("Tes favoris s'affichent quand tu allumes l'écran. Toucher une app demande ton empreinte ou ton code.")
                if (prefs.lockScreen && !overlayGranted) {
                    Spacer(Modifier.height(10.dp))
                    ActionRow("Autoriser l'affichage par-dessus les apps", "!", highlight = true, onClick = onRequestOverlay)
                }
            }

            Spacer(Modifier.height(40.dp))
            Text(
                "Définir comme écran d'accueil",
                color = OnAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Accent)
                    .clickable(onClick = onSetDefault)
                    .padding(vertical = 18.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text("Lumen", color = Muted, fontSize = 12.sp)
        }

        when (picking) {
            Picker.FAVORITES -> AppPicker(
                title = "Favoris",
                apps = apps,
                prefs = prefs,
                selected = prefs.favorites,
                numbered = true,
                onToggle = prefs::toggleFavorite,
                onClose = { picking = null },
            )
            Picker.FOCUS -> AppPicker(
                title = "Apps focus",
                apps = apps,
                prefs = prefs,
                selected = prefs.focusApps,
                numbered = false,
                onToggle = prefs::toggleFocusApp,
                onClose = { picking = null },
            )
            Picker.SWIPE_RIGHT -> GestureTargetPicker(
                title = "Glisser à droite",
                apps = apps,
                prefs = prefs,
                current = prefs.swipeRight,
                onPick = prefs::updateSwipeRight,
                onClose = { picking = null },
            )
            Picker.SWIPE_LEFT -> GestureTargetPicker(
                title = "Glisser à gauche",
                apps = apps,
                prefs = prefs,
                current = prefs.swipeLeft,
                onPick = prefs::updateSwipeLeft,
                onClose = { picking = null },
            )
            Picker.LIMIT_APP -> GestureTargetPicker(
                title = "Limiter une app",
                apps = apps,
                prefs = prefs,
                current = "",
                specials = emptyList(),
                onPick = { key -> limitApp = apps.firstOrNull { it.key == key } },
                onClose = { picking = null },
            )
            Picker.WIDGET -> WidgetPicker(onPick = onAddWidget, onClose = { picking = null })
            null -> Unit
        }

        editingCategory?.let { index ->
            val category = prefs.categories.getOrNull(index)
            if (category == null) {
                editingCategory = null
            } else {
                AppPicker(
                    title = category.name,
                    apps = apps,
                    prefs = prefs,
                    selected = category.keys,
                    numbered = false,
                    onToggle = { prefs.toggleCategoryApp(index, it) },
                    onClose = { editingCategory = null },
                    onRename = { namingCategory = index },
                    onDelete = {
                        prefs.deleteCategory(index)
                        editingCategory = null
                    },
                )
            }
        }

        namingCategory?.let { index ->
            NameDialog(
                title = if (index == -1) "Nouvelle catégorie" else "Renommer",
                initial = prefs.categories.getOrNull(index)?.name.orEmpty(),
                onConfirm = { name ->
                    if (index == -1) editingCategory = prefs.addCategory(name) else prefs.renameCategory(index, name)
                    namingCategory = null
                },
                onDismiss = { namingCategory = null },
            )
        }

        limitApp?.let { app ->
            LimitDialog(
                appLabel = app.label,
                current = prefs.limits[app.component.packageName],
                onPick = { minutes ->
                    prefs.setLimit(app.component.packageName, minutes)
                    limitApp = null
                },
                onDismiss = { limitApp = null },
            )
        }
    }
}

private enum class Picker { FAVORITES, FOCUS, SWIPE_LEFT, SWIPE_RIGHT, LIMIT_APP, WIDGET }

@Composable
private fun SubLabel(text: String) {
    Text(text, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun WidgetRow(label: String, heightDp: Int, onHeight: (Int) -> Unit, onRemove: () -> Unit) {
    val sizes = listOf(110, 170, 240)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Raised)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "RETIRER",
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onRemove)
                    .padding(8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Segmented(
            sizes,
            sizes.minBy { kotlin.math.abs(it - heightDp) },
            { when (it) { 110 -> "Petit"; 170 -> "Moyen"; else -> "Grand" } },
            onHeight,
        )
    }
}

/** Small dialog with one text field, for naming a category. */
@Composable
private fun NameDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text(title, color = White, fontWeight = FontWeight.Bold) },
        text = {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = TextStyle(color = White, fontSize = 18.sp, fontFamily = LocalMonoFont.current),
                cursorBrush = SolidColor(Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(name) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Raised)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        },
        confirmButton = {
            Text(
                "OK",
                color = Accent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = name.isNotBlank()) { onConfirm(name) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        },
        dismissButton = {
            Text(
                "Annuler",
                color = Muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        },
    )
}

/** Choose a daily limit in minutes, or remove it. */
@Composable
private fun LimitDialog(appLabel: String, current: Int?, onPick: (Int?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text("Limite pour $appLabel", color = White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                listOf(15, 30, 45, 60, 90, 120).forEach { minutes ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onPick(minutes) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (minutes < 60) "$minutes min" else "${minutes / 60} h${if (minutes % 60 > 0) " ${minutes % 60}" else ""}",
                            color = if (minutes == current) Accent else White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        if (minutes == current) Box(Modifier.size(8.dp).background(Accent, CircleShape))
                    }
                }
            }
        },
        confirmButton = {
            if (current != null) {
                Text(
                    "Retirer la limite",
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onPick(null) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        },
        dismissButton = {
            Text(
                "Annuler",
                color = Muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        },
    )
}

@Composable
private fun GestureRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Raised)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(value, color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.width(4.dp))
        Text("›", color = Muted, fontSize = 22.sp)
    }
}

private fun formatMinutes(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

private fun pickTime(context: Context, isLight: Boolean, current: Int, onPicked: (Int) -> Unit) {
    TimePickerDialog(
        context,
        if (isLight) android.R.style.Theme_Material_Light_Dialog_Alert else android.R.style.Theme_Material_Dialog_Alert,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        current / 60,
        current % 60,
        true,
    ).show()
}

@Composable
private fun TimeChip(label: String, minutes: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Raised)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Muted, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(formatMinutes(minutes), color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(28.dp))
    SectionLabel(title)
    Spacer(Modifier.height(12.dp))
    content()
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        color = Muted,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** Full-width row with a label and a small badge, e.g. "Choisir les apps favorites   5". */
@Composable
private fun ActionRow(label: String, badge: String, highlight: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Raised)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (highlight) Accent else White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(Accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(badge, color = OnAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(4.dp))
        Text("›", color = Muted, fontSize = 22.sp)
    }
}

@Composable
private fun PackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (selected) White else Muted,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (selected) Box(Modifier.size(8.dp).background(Accent, CircleShape))
    }
}
