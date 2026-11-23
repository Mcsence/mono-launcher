package com.mono.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.text.Collator
import java.text.Normalizer

data class AppEntry(
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
) {
    val letter: String = letterOf(label)
}

private val diacritics = Regex("\\p{Mn}+")

/** Lowercase, accent-free form used for search and grouping ("Égalité" -> "egalite"). */
fun normalize(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(diacritics, "").lowercase()

/** Section letter shown in the drawer: A–Z, or "#" for digits and symbols. */
fun letterOf(label: String): String {
    val c = normalize(label.trim()).firstOrNull()?.uppercaseChar() ?: return "#"
    return if (c in 'A'..'Z') c.toString() else "#"
}

class AppRepository(private val context: Context) {
    val launcherApps: LauncherApps = context.getSystemService(LauncherApps::class.java)

    /** Every launchable app of the current user, sorted "#" first then alphabetically. */
    fun load(): List<AppEntry> {
        val collator = Collator.getInstance()
        val iconPx = (48 * context.resources.displayMetrics.density).toInt()
        return launcherApps.getActivityList(null, Process.myUserHandle())
            .filter { it.componentName.packageName != context.packageName }
            .map { info ->
                AppEntry(
                    key = info.componentName.flattenToShortString(),
                    label = info.label.toString(),
                    component = info.componentName,
                    user = info.user,
                    icon = info.getIcon(0).toBitmap(iconPx, iconPx).asImageBitmap(),
                )
            }
            .sortedWith(
                compareBy<AppEntry> { if (it.letter == "#") 0 else 1 }
                    .thenComparator { a, b -> collator.compare(a.label, b.label) }
            )
    }

    fun launch(app: AppEntry) {
        runCatching { launcherApps.startMainActivity(app.component, app.user, null, null) }
    }

    fun openInfo(app: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
    }

    fun uninstall(app: AppEntry) {
        runCatching {
            val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.component.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
