package com.mono.launcher.ui

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
    onClose: () -> Unit,
    onSetDefault: () -> Unit,
    onLockScreenChange: (Boolean) -> Unit,
    onRequestOverlay: () -> Unit,
) {
    var picking by remember { mutableStateOf(false) }

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

            Section("Favoris") {
                ActionRow("Choisir les apps favorites", "${prefs.favorites.size}") { picking = true }
                Spacer(Modifier.height(10.dp))
                Segmented(FavStyle.entries, prefs.favStyle, { it.label }, prefs::updateFavStyle)
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

            Section("Horloge") {
                Segmented(listOf(true, false), prefs.showClock, { if (it) "Affichée" else "Masquée" }, prefs::updateShowClock)
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
                color = Black,
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
            Text("Mono", color = Muted, fontSize = 12.sp)
        }

        if (picking) {
            FavoritesPicker(apps = apps, prefs = prefs, onClose = { picking = false })
        }
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
            Text(badge, color = Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
