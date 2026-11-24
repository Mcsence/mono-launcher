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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.AppEntry
import com.mono.launcher.IconShape
import com.mono.launcher.IconStyle
import com.mono.launcher.Prefs

private val GrayscaleFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

fun IconShape.composeShape(): Shape = when (this) {
    IconShape.SYSTEM, IconShape.CIRCLE -> CircleShape
    IconShape.SQUIRCLE -> RoundedCornerShape(30)
    IconShape.TEARDROP -> RoundedCornerShape(
        topStartPercent = 50,
        topEndPercent = 50,
        bottomEndPercent = 12,
        bottomStartPercent = 50,
    )
}

/**
 * App icon following the user's icon settings. [onAccent] = drawn on an accent-colored
 * tile, so the icon switches to black / grayscale to stay readable.
 */
@Composable
fun AppIcon(app: AppEntry, prefs: Prefs, size: Dp, onAccent: Boolean = false, dimmed: Boolean = false) {
    val style = prefs.iconStyle
    if (style == IconStyle.NONE) return
    if (style == IconStyle.THEMED || app.isGlyph) {
        val tint = when {
            onAccent -> OnAccent
            dimmed -> Muted
            else -> Accent
        }
        Box(
            Modifier
                .size(size)
                .clip(prefs.iconShape.composeShape())
                .background(if (onAccent) Color(0x1F000000) else Raised),
            contentAlignment = Alignment.Center,
        ) {
            val glyph = app.icon
            if (app.isGlyph && glyph != null) {
                Image(
                    bitmap = glyph,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(tint),
                )
            } else {
                // App has no one-color icon: show its initial instead.
                Text(
                    app.label.take(1).uppercase(),
                    color = tint,
                    fontSize = (size.value * 0.45f).sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        return
    }
    val bitmap = app.icon ?: return
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier.size(size),
        alpha = if (dimmed) 0.4f else 1f,
        colorFilter = if (style == IconStyle.MONO || onAccent || dimmed) GrayscaleFilter else null,
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
                    color = if (isSelected) OnAccent else White,
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
    prefs: Prefs,
    favoriteIndex: Int,
    favoriteCount: Int,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMove: (Int) -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
    onRename: (String?) -> Unit,
) {
    val isFavorite = favoriteIndex >= 0
    var renaming by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(app.label) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(renaming) { if (renaming) runCatching { focus.requestFocus() } }
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
                AppIcon(app, prefs, 40.dp)
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
            if (app.label != app.originalLabel) {
                Text(app.originalLabel, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(16.dp))
            if (renaming) {
                BasicTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = LocalMonoFont.current,
                    ),
                    cursorBrush = SolidColor(Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onRename(newName) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Raised)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("Enregistrer", filled = true) { onRename(newName) }
                    if (app.label != app.originalLabel) {
                        PillButton("Nom d'origine", filled = false) { onRename(null) }
                    }
                    PillButton("Annuler", filled = false) { renaming = false }
                }
            } else {
                SheetAction(
                    if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                    highlight = true,
                    onClick = onToggleFavorite,
                )
                if (isFavorite && favoriteIndex > 0) SheetAction("Monter", onClick = { onMove(-1) })
                if (isFavorite && favoriteIndex < favoriteCount - 1) SheetAction("Descendre", onClick = { onMove(1) })
                SheetAction("Renommer", onClick = { renaming = true })
                SheetAction("Infos sur l'app", onClick = onInfo)
                SheetAction("Désinstaller", onClick = onUninstall)
            }
        }
    }
}

@Composable
private fun PillButton(text: String, filled: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (filled) OnAccent else White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) Accent else Raised)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
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
