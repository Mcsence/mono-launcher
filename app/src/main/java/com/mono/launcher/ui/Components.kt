package com.mono.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.AppEntry
import com.mono.launcher.IconStyle

private val GrayscaleFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun AppIcon(app: AppEntry, style: IconStyle, size: Dp) {
    if (style == IconStyle.NONE) return
    Image(
        bitmap = app.icon,
        contentDescription = null,
        modifier = Modifier.size(size),
        colorFilter = if (style == IconStyle.MONO) GrayscaleFilter else null,
    )
}

/** Small uppercase label with an accent bar, e.g. "— FAVORIS". */
@Composable
fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 14.dp, height = 3.dp).background(Accent))
        Spacer(Modifier.width(10.dp))
        Text(
            text.uppercase(),
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
        )
    }
}

/** Segmented control: the selected option is filled with the accent color. */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Raised)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Accent else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    color = if (isSelected) Black else White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Stops taps from falling through an overlay to the home screen underneath. */
fun Modifier.swallowTouches(): Modifier = pointerInput(Unit) {
    awaitEachGesture { awaitFirstDown(requireUnconsumed = false) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    app: AppEntry,
    favoriteIndex: Int,
    favoriteCount: Int,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMove: (Int) -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    val isFavorite = favoriteIndex >= 0
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Panel,
        contentColor = White,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Muted)
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, IconStyle.COLOR, 40.dp)
                Spacer(Modifier.width(14.dp))
                Text(
                    app.label,
                    color = White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(16.dp))
            SheetAction(
                if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                highlight = true,
                onClick = onToggleFavorite,
            )
            if (isFavorite && favoriteIndex > 0) SheetAction("Monter", onClick = { onMove(-1) })
            if (isFavorite && favoriteIndex < favoriteCount - 1) SheetAction("Descendre", onClick = { onMove(1) })
            SheetAction("Infos sur l'app", onClick = onInfo)
            SheetAction("Désinstaller", onClick = onUninstall)
        }
    }
}

@Composable
private fun SheetAction(text: String, highlight: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        color = if (highlight) Accent else White,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}
