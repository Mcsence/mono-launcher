package com.mono.launcher

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

enum class ScreenTimeMode(val label: String) { OFF("Masqué"), TOTAL("Total"), PER_APP("Par app") }

/** Today's time spent in each app, from Android's usage statistics. */
object ScreenTime {
    /** Package name -> milliseconds in the foreground since midnight. */
    var today by mutableStateOf<Map<String, Long>>(emptyMap())

    val total: Long get() = today.values.sum()

    fun hasAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Adds up foreground time per app from today's open/close events. */
    fun compute(context: Context): Map<String, Long> {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(start, now)
        val totals = HashMap<String, Long>()
        val openedAt = HashMap<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                EVENT_RESUMED -> openedAt.putIfAbsent(event.packageName, event.timeStamp)
                EVENT_PAUSED, EVENT_STOPPED -> openedAt.remove(event.packageName)?.let { opened ->
                    totals.merge(event.packageName, event.timeStamp - opened) { a, b -> a + b }
                }
            }
        }
        openedAt.forEach { (pkg, opened) -> totals.merge(pkg, now - opened) { a, b -> a + b } }
        totals.remove(context.packageName)
        return totals.filterValues { it >= 60_000 }
    }

    /** "23 min" or "2 h 14". */
    fun format(millis: Long): String {
        val minutes = millis / 60_000
        return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${"%02d".format(minutes % 60)}"
    }

    // Same values as UsageEvents.Event.ACTIVITY_RESUMED / ACTIVITY_PAUSED / ACTIVITY_STOPPED.
    private const val EVENT_RESUMED = 1
    private const val EVENT_PAUSED = 2
    private const val EVENT_STOPPED = 23
}

/** Focus mode: during the scheduled hours only the "work" apps are visible. */
object Focus {
    /** End of the focus period we're in right now, or null if not in a scheduled period. */
    fun windowEnd(prefs: Prefs, now: LocalDateTime): LocalDateTime? {
        if (!prefs.focusEnabled || prefs.focusStart == prefs.focusEnd) return null
        val start = LocalTime.of(prefs.focusStart / 60, prefs.focusStart % 60)
        val end = LocalTime.of(prefs.focusEnd / 60, prefs.focusEnd % 60)
        val time = now.toLocalTime()
        val days = prefs.focusDays
        return if (start < end) {
            if (now.dayOfWeek.value in days && time >= start && time < end) now.toLocalDate().atTime(end) else null
        } else {
            // Overnight period, e.g. 22:00 -> 07:00.
            when {
                time >= start && now.dayOfWeek.value in days -> now.toLocalDate().plusDays(1).atTime(end)
                time < end && now.minusDays(1).dayOfWeek.value in days -> now.toLocalDate().atTime(end)
                else -> null
            }
        }
    }

    /** End of the current focus period, unless the user chose to leave it. */
    fun activeUntil(prefs: Prefs, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val end = windowEnd(prefs, now) ?: return null
        return if (millis(now) < prefs.focusPausedUntil) null else end
    }

    fun millis(time: LocalDateTime): Long = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
