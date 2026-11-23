package com.mono.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Xml
import org.xmlpull.v1.XmlPullParser

data class IconPackInfo(val packageName: String, val label: String)

/** An installed icon pack: maps app components to drawables inside the pack. */
class IconPack(
    private val res: Resources,
    private val packageName: String,
    private val drawables: Map<String, String>,
) {
    fun drawableFor(component: ComponentName): Drawable? {
        val name = drawables[component.flattenToString()] ?: return null
        val id = res.getIdentifier(name, "drawable", packageName)
        if (id == 0) return null
        return runCatching { res.getDrawable(id, null) }.getOrNull()
    }
}

/**
 * Icon packs use the de-facto standard launcher format (ADW / Nova): an
 * `appfilter.xml` listing `<item component="ComponentInfo{pkg/cls}" drawable="name"/>`.
 */
object IconPacks {
    val INTENT_ACTIONS = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.gau.go.launcherex.theme",
    )

    fun list(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        return INTENT_ACTIONS
            .flatMap { action -> runCatching { pm.queryIntentActivities(Intent(action), 0) }.getOrDefault(emptyList()) }
            .map { it.activityInfo.packageName }
            .distinct()
            .map { pkg ->
                val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                    .getOrDefault(pkg)
                IconPackInfo(pkg, label)
            }
            .sortedBy { it.label.lowercase() }
    }

    fun load(context: Context, packageName: String): IconPack? = runCatching {
        val res = context.packageManager.getResourcesForApplication(packageName)
        val xmlId = res.getIdentifier("appfilter", "xml", packageName)
        val parser: XmlPullParser = if (xmlId != 0) {
            res.getXml(xmlId)
        } else {
            Xml.newPullParser().apply {
                setInput(context.createPackageContext(packageName, 0).assets.open("appfilter.xml"), "UTF-8")
            }
        }
        val map = HashMap<String, String>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG || parser.name != "item") continue
            val component = parser.getAttributeValue(null, "component") ?: continue
            val drawable = parser.getAttributeValue(null, "drawable") ?: continue
            val flat = component.removePrefix("ComponentInfo{").removeSuffix("}")
            ComponentName.unflattenFromString(flat)?.let { map.putIfAbsent(it.flattenToString(), drawable) }
        }
        IconPack(res, packageName, map)
    }.getOrNull()
}
