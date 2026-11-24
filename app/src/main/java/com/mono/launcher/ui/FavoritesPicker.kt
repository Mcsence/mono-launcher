package com.mono.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.AppEntry
import com.mono.launcher.Gestures
import com.mono.launcher.IconStyle
import com.mono.launcher.Prefs
import com.mono.launcher.normalize

/** Single choice for a swipe: nothing, camera, messages, or any app. */
@Composable
fun GestureTargetPicker(
    title: String,
    apps: List<AppEntry>,
    prefs: Prefs,
    current: String,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
    specials: List<String> = listOf(Gestures.NONE, Gestures.CAMERA, Gestures.MESSAGES),
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val q = normalize(query.trim())
        if (q.isEmpty()) apps else apps.filter { normalize(it.label).contains(q) }
    }
    BackHandler(onBack = onClose)

    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
            .swallowTouches()
            .systemBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = White, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
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
        SearchField(
            value = query,
            onValueChange = { query = it },
            onGo = {},
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            if (query.isBlank()) {
                items(specials, key = { "special:$it" }) { target ->
                    TargetRow(Gestures.label(target, apps), selected = target == current, app = null, prefs = prefs) {
                        onPick(target)
                        onClose()
                    }
                }
            }
            items(filtered, key = { it.key }) { app ->
                TargetRow(app.label, selected = app.key == current, app = app, prefs = prefs) {
                    onPick(app.key)
                    onClose()
                }
            }
        }
    }
}

@Composable
private fun SmallTextButton(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

@Composable
private fun TargetRow(label: String, selected: Boolean, app: AppEntry?, prefs: Prefs, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (app != null && prefs.iconStyle != IconStyle.NONE) {
            AppIcon(app, prefs, 26.dp)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            label,
            color = if (selected) Accent else White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) Box(Modifier.size(8.dp).background(Accent, CircleShape))
    }
}

/**
 * Full list of apps with a check per app; tapping toggles them. With [numbered],
 * checked apps show their position (1, 2, 3…), otherwise a check mark.
 */
@Composable
fun AppPicker(
    title: String,
    apps: List<AppEntry>,
    prefs: Prefs,
    selected: List<String>,
    numbered: Boolean,
    onToggle: (String) -> Unit,
    onClose: () -> Unit,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val q = normalize(query.trim())
        if (q.isEmpty()) apps else apps.filter { normalize(it.label).contains(q) }
    }
    BackHandler(onBack = onClose)

    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
            .swallowTouches()
            .systemBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = White, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            Text(
                "${selected.size}",
                color = Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 10.dp).align(Alignment.Bottom),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "OK",
                color = OnAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Accent)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }

        if (onRename != null || onDelete != null) {
            Row(Modifier.padding(start = 16.dp, top = 4.dp)) {
                onRename?.let { SmallTextButton("RENOMMER", it) }
                onDelete?.let { SmallTextButton("SUPPRIMER", it) }
            }
        }

        SearchField(
            value = query,
            onValueChange = { query = it },
            onGo = {},
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            items(filtered, key = { it.key }) { app ->
                val position = selected.indexOf(app.key)
                val isSelected = position >= 0
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggle(app.key) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (prefs.iconStyle != IconStyle.NONE) {
                        AppIcon(app, prefs, 26.dp)
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        app.label,
                        color = if (isSelected) White else Muted,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Accent else Black)
                            .then(if (isSelected) Modifier else Modifier.border(1.5.dp, Muted, CircleShape)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Text(
                                if (numbered) "${position + 1}" else "✓",
                                color = OnAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
