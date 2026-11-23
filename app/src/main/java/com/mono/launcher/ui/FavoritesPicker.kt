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
import com.mono.launcher.IconStyle
import com.mono.launcher.Prefs
import com.mono.launcher.normalize

/**
 * Full list of apps with a check per app. Checked apps show their position
 * in the favorites (1, 2, 3…); tapping toggles them.
 */
@Composable
fun FavoritesPicker(apps: List<AppEntry>, prefs: Prefs, onClose: () -> Unit) {
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
            Text("Favoris", color = White, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            Text(
                "${prefs.favorites.size}",
                color = Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 10.dp).align(Alignment.Bottom),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "OK",
                color = Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Accent)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
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
            items(filtered, key = { it.key }) { app ->
                val position = prefs.favorites.indexOf(app.key)
                val selected = position >= 0
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { prefs.toggleFavorite(app.key) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (prefs.iconStyle != IconStyle.NONE) {
                        AppIcon(app, prefs, 26.dp)
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        app.label,
                        color = if (selected) White else Muted,
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
                            .background(if (selected) Accent else Black)
                            .then(if (selected) Modifier else Modifier.border(1.5.dp, Muted, CircleShape)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Text("${position + 1}", color = Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
