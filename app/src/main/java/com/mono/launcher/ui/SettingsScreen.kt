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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.FavStyle
import com.mono.launcher.IconStyle
import com.mono.launcher.Prefs
import com.mono.launcher.TextSize

@Composable
fun SettingsScreen(prefs: Prefs, onClose: () -> Unit, onSetDefault: () -> Unit) {
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
            Box(Modifier.padding(start = 6.dp, top = 14.dp).size(10.dp).background(Accent, CircleShape))
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

        Section("Favoris") {
            Segmented(FavStyle.entries, prefs.favStyle, { it.label }, prefs::updateFavStyle)
        }

        Section("Icônes") {
            Segmented(IconStyle.entries, prefs.iconStyle, { it.label }, prefs::updateIconStyle)
        }

        Section("Taille du texte") {
            Segmented(TextSize.entries, prefs.textSize, { it.label }, prefs::updateTextSize)
        }

        Section("Horloge") {
            Segmented(listOf(true, false), prefs.showClock, { if (it) "Affichée" else "Masquée" }, prefs::updateShowClock)
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
        Text("Mono · v1.0", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(32.dp))
    SectionLabel(title)
    Spacer(Modifier.height(14.dp))
    content()
}
