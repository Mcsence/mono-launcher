package com.mono.launcher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.mono.launcher.WidgetEntry
import com.mono.launcher.normalize

/** The host that owns the widgets placed on the home screen (provided by MainActivity). */
val LocalWidgetHost = staticCompositionLocalOf<AppWidgetHost?> { null }

/** Widgets stacked under the clock, each with its own height. */
@Composable
fun WidgetStack(widgets: List<WidgetEntry>, modifier: Modifier = Modifier) {
    val host = LocalWidgetHost.current ?: return
    val context = LocalContext.current
    val manager = remember { AppWidgetManager.getInstance(context) }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp - 48
    Column(modifier) {
        widgets.forEach { entry ->
            val info = manager.getAppWidgetInfo(entry.id) ?: return@forEach
            key(entry.id) {
                AndroidView(
                    factory = { ctx ->
                        host.createView(ctx, entry.id, info).apply {
                            @Suppress("DEPRECATION")
                            updateAppWidgetSize(null, screenWidthDp, entry.heightDp, screenWidthDp, entry.heightDp)
                        }
                    },
                    update = { view ->
                        @Suppress("DEPRECATION")
                        view.updateAppWidgetSize(null, screenWidthDp, entry.heightDp, screenWidthDp, entry.heightDp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(entry.heightDp.dp)
                        .clip(RoundedCornerShape(20.dp)),
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

private data class WidgetChoice(
    val info: AppWidgetProviderInfo,
    val label: String,
    val appLabel: String,
    val icon: ImageBitmap?,
)

/** All widgets offered by installed apps, grouped by app. */
@Composable
fun WidgetPicker(onPick: (AppWidgetProviderInfo) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val choices = remember {
        val pm = context.packageManager
        val density = context.resources.displayMetrics.densityDpi
        AppWidgetManager.getInstance(context).installedProviders.map { info ->
            val appLabel = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
            }.getOrDefault(info.provider.packageName)
            WidgetChoice(
                info = info,
                label = info.loadLabel(pm),
                appLabel = appLabel,
                icon = runCatching { info.loadIcon(context, density)?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull(),
            )
        }.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.label.lowercase() }))
    }
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = normalize(query.trim())
        if (q.isEmpty()) choices else choices.filter { normalize(it.label + " " + it.appLabel).contains(q) }
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
            Text("Widgets", color = White, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
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
            items(filtered, key = { it.info.provider.flattenToString() }) { choice ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onPick(choice.info)
                            onClose()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    choice.icon?.let {
                        Image(it, contentDescription = null, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(14.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            choice.label,
                            color = White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(choice.appLabel, color = Muted, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Name of a placed widget, for the settings list. */
fun widgetLabel(context: android.content.Context, id: Int): String {
    val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(id) ?: return "Widget supprimé"
    return info.loadLabel(context.packageManager)
}
