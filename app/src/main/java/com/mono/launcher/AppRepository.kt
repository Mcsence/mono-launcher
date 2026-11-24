package com.mono.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
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
    /** Full icon, or a white glyph to tint when [isGlyph] (accent-colored icons). */
    val icon: ImageBitmap?,
    val isGlyph: Boolean,
    /** Name given by the app itself, kept when the user renames it. */
    val originalLabel: String = label,
) {
    val letter: String = letterOf(label)
}

/**
 * Last loaded app list, kept for the whole process so that a re-created home screen
 * (e.g. after unlocking) shows the apps immediately instead of an empty frame.
 */
object AppCache {
    @Volatile
    var apps: List<AppEntry> = emptyList()
}

/** "#" first, then alphabetical in the phone's language. */
fun sortApps(apps: List<AppEntry>): List<AppEntry> {
    val collator = Collator.getInstance()
    return apps.sortedWith(
        compareBy<AppEntry> { if (it.letter == "#") 0 else 1 }
            .thenComparator { a, b -> collator.compare(a.label, b.label) }
    )
}

/** Applies the user's custom names, then re-sorts. */
fun applyLabels(apps: List<AppEntry>, labels: Map<String, String>): List<AppEntry> =
    if (labels.isEmpty()) apps else sortApps(apps.map { app -> labels[app.key]?.let { app.copy(label = it) } ?: app })

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

    /**
     * Every launchable app of the current user, sorted "#" first then alphabetically.
     * [onlyKeys] restricts loading (and icon rendering) to those apps.
     */
    fun load(config: IconConfig, onlyKeys: Set<String>? = null): List<AppEntry> {
        val px = (48 * context.resources.displayMetrics.density).toInt()
        val pack = if (config.pack.isNotEmpty() && config.style != IconStyle.THEMED) {
            IconPacks.load(context, config.pack)
        } else {
            null
        }
        return sortApps(launcherApps.getActivityList(null, Process.myUserHandle())
            .filter { it.componentName.packageName != context.packageName }
            .filter { onlyKeys == null || it.componentName.flattenToShortString() in onlyKeys }
            .map { info ->
                val (icon, isGlyph) = renderIcon(info, config, pack, px)
                AppEntry(
                    key = info.componentName.flattenToShortString(),
                    label = info.label.toString(),
                    component = info.componentName,
                    user = info.user,
                    icon = icon,
                    isGlyph = isGlyph,
                )
            })
    }

    private fun renderIcon(
        info: LauncherActivityInfo,
        config: IconConfig,
        pack: IconPack?,
        px: Int,
    ): Pair<ImageBitmap?, Boolean> {
        if (config.style == IconStyle.NONE) return null to false
        return runCatching {
            val system = info.getIcon(0)
            if (config.style == IconStyle.THEMED) {
                // Android 13+ apps can ship a one-color version of their icon.
                val mono = if (Build.VERSION.SDK_INT >= 33 && system is AdaptiveIconDrawable) system.monochrome else null
                return@runCatching mono?.let { IconRenderer.glyph(it, px).asImageBitmap() } to true
            }
            val fromPack = pack?.drawableFor(info.componentName)
            if (fromPack != null) return@runCatching fromPack.toBitmap(px, px).asImageBitmap() to false
            IconRenderer.shaped(system, config.shape, px).asImageBitmap() to false
        }.getOrElse { null to false }
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

/** Draws launcher icons into bitmaps with the user's chosen shape. */
object IconRenderer {
    /** Adaptive icon layers are 108dp with the visible 72dp in the middle: draw 1.5x and crop. */
    private fun layerBounds(px: Int): Rect {
        val inset = px / 4
        return Rect(-inset, -inset, px + inset, px + inset)
    }

    /** White one-color glyph, tinted later with the accent color. */
    fun glyph(drawable: Drawable, px: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val d = drawable.mutate()
        d.bounds = layerBounds(px)
        d.setTint(android.graphics.Color.WHITE)
        d.draw(Canvas(bitmap))
        return bitmap
    }

    fun shaped(drawable: Drawable, shape: IconShape, px: Int): Bitmap {
        if (shape == IconShape.SYSTEM) return drawable.toBitmap(px, px)
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.clipPath(path(shape, px.toFloat()))
        if (drawable is AdaptiveIconDrawable) {
            val bounds = layerBounds(px)
            drawable.background?.let { it.bounds = bounds; it.draw(canvas) }
            drawable.foreground?.let { it.bounds = bounds; it.draw(canvas) }
        } else {
            // Old-style icon: put it on a dark plate of the chosen shape.
            canvas.drawColor(0xFF1C1C1C.toInt())
            val margin = (px * 0.16f).toInt()
            drawable.setBounds(margin, margin, px - margin, px - margin)
            drawable.draw(canvas)
        }
        return bitmap
    }

    private fun path(shape: IconShape, s: Float): Path = Path().apply {
        when (shape) {
            IconShape.SYSTEM, IconShape.CIRCLE -> addCircle(s / 2, s / 2, s / 2, Path.Direction.CW)
            IconShape.SQUIRCLE -> addRoundRect(0f, 0f, s, s, s * 0.3f, s * 0.3f, Path.Direction.CW)
            IconShape.TEARDROP -> {
                val r = s / 2
                val small = s * 0.12f
                // top-left, top-right, bottom-right, bottom-left
                addRoundRect(RectF(0f, 0f, s, s), floatArrayOf(r, r, r, r, small, small, r, r), Path.Direction.CW)
            }
        }
    }
}
