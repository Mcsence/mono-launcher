package com.mono.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.AppEntry
import com.mono.launcher.IconStyle
import com.mono.launcher.Prefs
import com.mono.launcher.ScreenTime
import com.mono.launcher.ScreenTimeMode
import com.mono.launcher.normalize
import kotlinx.coroutines.launch

private sealed interface DrawerRow {
    data class Header(val letter: String) : DrawerRow
    data class App(val app: AppEntry) : DrawerRow
}

@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    hiddenCount: Int,
    prefs: Prefs,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val q = normalize(query.trim())
        if (q.isEmpty()) {
            apps
        } else {
            apps.filter { normalize(it.label).contains(q) || normalize(it.originalLabel).contains(q) }
        }
    }
    // Flat list: letter header followed by its apps (apps are already sorted).
    val rows = remember(filtered) {
        buildList<DrawerRow> {
            filtered.groupBy { it.letter }.forEach { (letter, group) ->
                add(DrawerRow.Header(letter))
                group.forEach { add(DrawerRow.App(it)) }
            }
        }
    }
    val headerIndex = remember(rows) {
        buildMap {
            rows.forEachIndexed { i, row -> if (row is DrawerRow.Header) put(row.letter, i) }
        }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var bubble by remember { mutableStateOf<String?>(null) }
    val scale = prefs.textSize.scale

    LaunchedEffect(query) { listState.scrollToItem(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Black)
            .swallowTouches()
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Apps", color = White, fontSize = (32 * scale).sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                Text(
                    "${filtered.size}",
                    color = Accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 14.dp).align(Alignment.Bottom),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "RÉGLAGES",
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenSettings)
                        .padding(12.dp),
                )
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

            if (hiddenCount > 0) {
                Text(
                    "Mode focus · $hiddenCount apps masquées",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp),
                )
            }

            SearchField(
                value = query,
                onValueChange = { query = it },
                onGo = { filtered.firstOrNull()?.let(onLaunch) },
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 24.dp, end = 56.dp, bottom = 32.dp),
            ) {
                items(
                    rows,
                    key = { row ->
                        when (row) {
                            is DrawerRow.Header -> "h:${row.letter}"
                            is DrawerRow.App -> row.app.key
                        }
                    },
                ) { row ->
                    when (row) {
                        is DrawerRow.Header -> Text(
                            row.letter,
                            color = Accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                        )
                        is DrawerRow.App -> AppRow(
                            app = row.app,
                            prefs = prefs,
                            scale = scale,
                            isFavorite = prefs.isFavorite(row.app.key),
                            onClick = { onLaunch(row.app) },
                            onLongClick = { onLongPress(row.app) },
                        )
                    }
                }
            }
        }

        if (query.isBlank()) {
            AlphabetScroller(
                available = headerIndex.keys,
                onLetter = { letter ->
                    headerIndex[letter]?.let { index -> scope.launch { listState.scrollToItem(index) } }
                },
                onActiveChange = { bubble = it },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
            )
        }

        bubble?.let { letter ->
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(Accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter, color = OnAccent, fontSize = 56.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onGo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = LocalMonoFont.current,
        ),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Raised)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).background(Accent, CircleShape))
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text("Rechercher une app", color = Muted, fontSize = 18.sp)
                    }
                    innerTextField()
                }
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: AppEntry,
    prefs: Prefs,
    scale: Float,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val over = isOverLimit(app, prefs)
        if (prefs.iconStyle != IconStyle.NONE) {
            AppIcon(app, prefs, 26.dp * scale, dimmed = over)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            app.label,
            color = when {
                pressed -> Accent
                over -> Muted.copy(alpha = 0.6f)
                else -> White
            },
            fontSize = (17 * scale).sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (isFavorite) {
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(6.dp).background(Accent, CircleShape))
        }
        if (prefs.screenTime == ScreenTimeMode.PER_APP) {
            ScreenTime.today[app.component.packageName]?.let { millis ->
                Spacer(Modifier.weight(1f))
                Text(ScreenTime.format(millis), color = Muted, fontSize = 12.sp)
            }
        }
    }
}
