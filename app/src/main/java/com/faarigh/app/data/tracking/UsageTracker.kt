package com.faarigh.app.data.tracking

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val category: String,
    val usageTime: Duration,
    val openCount: Int,
)

@Singleton
class UsageTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private val SOCIAL_MEDIA_PACKAGES = setOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.facebook.orca",
            "com.twitter.android",
            "com.twitter.android.lite",     // Twitter Lite
            "com.x.android",                // X (rebranded Twitter)
            "com.x.twitter",                // X alternate package
            "com.twitter.twitterx",         // Twitter X transitional
            "com.snapchat.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.reddit.frontpage",
            "com.linkedin.android",
            "com.pinterest",
            "com.tumblr",
            "org.telegram.messenger",
            "com.discord",
        )

        private val ENTERTAINMENT_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.netflix.mediaclient",
            "com.spotify.music",
            "com.amazon.avod.thirdpartyclient",
            "com.disney.disneyplus",
            "tv.twitch.android.app",
        )

        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.brave.browser",
            "com.opera.browser",
            "com.microsoft.emmx",
            "com.duckduckgo.mobile.android",
        )

        private val COMMUNICATION_PACKAGES = setOf(
            "com.whatsapp",
            "com.google.android.apps.messaging",
            "com.google.android.gm",
            "com.microsoft.office.outlook",
            "com.Slack",
        )

        private val PRODUCTIVITY_PACKAGES = setOf(
            "com.google.android.apps.docs",
            "com.google.android.apps.docs.editors.sheets",
            "com.google.android.apps.docs.editors.slides",
            "com.microsoft.office.word",
            "com.microsoft.office.excel",
            "com.google.android.calendar",
            "com.todoist",
            "com.notion.id",
        )

        private val IGNORED_PACKAGES = setOf(
            // System UI
            "com.android.systemui",
            "com.android.providers.settings",
            // Launchers
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",         // Samsung launcher
            "com.microsoft.launcher",
            "com.teslacoilsw.launcher",             // Nova launcher
            "com.actionlauncher.playstore",
            // Google services
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.vending",
            // Permission/installer UIs
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            // Keyboard apps
            "com.google.android.inputmethod.latin",  // Gboard
            "com.samsung.android.honeyboard",        // Samsung keyboard
            "com.swiftkey.languageprovider",
            "com.touchtype.swiftkey",                // SwiftKey
            "com.android.inputmethod.latin",
        )
    }

    /**
     * Check if the app has usage stats permission.
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Get total screen time for a specific date.
     */
    fun getTotalScreenTime(date: LocalDate): Flow<Duration> = flow {
        if (!hasUsageStatsPermission()) {
            emit(Duration.ZERO)
            return@flow
        }
        val totalMs = getScreenTimeForDate(date)
        emit(Duration.ofMillis(totalMs))
    }.flowOn(Dispatchers.IO)

    /**
     * Calculate screen time using INTERVAL_DAILY for accurate per-day stats.
     * INTERVAL_BEST can return multi-day buckets causing inflated totals.
     */
    /**
     * Calculate screen time from usage events (MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND).
     * This is the most accurate method — it counts actual foreground durations
     * within the exact time range, matching Digital Wellbeing's approach.
     */
    private fun getScreenTimeForDate(date: LocalDate): Long {
        val (startMs, endMs) = dayRange(date)
        return calculateForegroundTimeFromEvents(startMs, endMs)
    }

    /**
     * Calculate total foreground screen time from raw usage events.
     * Uses a 12-hour lookback to capture sessions that started before [startMs]
     * (e.g. crossing midnight), then clips each session to [startMs, endMs].
     * This matches Digital Wellbeing's midnight-reset behaviour.
     */
    private fun calculateForegroundTimeFromEvents(startMs: Long, endMs: Long): Long {
        // Delegate to getPerAppForegroundTime and sum across all apps
        return getPerAppForegroundTime(startMs, endMs).values.sum()
    }

    /**
     * Calculate per-app foreground time using raw events, with a 12-hour lookback to
     * catch sessions that started before [startMs] (e.g. a session crossing midnight).
     * Only time within [startMs, endMs] is counted — matching Digital Wellbeing's
     * midnight reset behaviour. INTERVAL_DAILY is intentionally NOT used here because
     * it can return multi-day accumulated data that inflates totals by 3-4 hours.
     */
    private fun getPerAppForegroundTime(startMs: Long, endMs: Long): Map<String, Long> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        // Look back 12 h so we can detect sessions already in progress at startMs
        val lookbackMs = startMs - 12 * 60 * 60 * 1000L
        val events = usageStatsManager.queryEvents(lookbackMs, endMs)
        val event = UsageEvents.Event()

        val foregroundStart = mutableMapOf<String, Long>()
        val foregroundTotals = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg in IGNORED_PACKAGES) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (!foregroundStart.containsKey(pkg)) {
                        foregroundStart[pkg] = event.timeStamp
                    }
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val start = foregroundStart.remove(pkg) ?: continue
                    // Clip to [startMs, endMs] so pre-midnight time is excluded
                    val effectiveStart = start.coerceAtLeast(startMs)
                    val effectiveEnd = event.timeStamp.coerceAtMost(endMs)
                    if (effectiveEnd > effectiveStart) {
                        val dur = effectiveEnd - effectiveStart
                        foregroundTotals[pkg] = (foregroundTotals[pkg] ?: 0L) + dur
                    }
                }
            }
        }

        // Sessions still in foreground at query time
        val now = System.currentTimeMillis().coerceAtMost(endMs)
        for ((pkg, start) in foregroundStart) {
            val effectiveStart = start.coerceAtLeast(startMs)
            val dur = (now - effectiveStart).coerceAtLeast(0)
            if (dur > 0) foregroundTotals[pkg] = (foregroundTotals[pkg] ?: 0L) + dur
        }

        return foregroundTotals
    }

    /**
     * Get weekly screen time as a list of (date, duration) pairs for the last 7 days.
     */
    fun getWeeklyScreenTime(): Flow<List<Pair<LocalDate, Duration>>> = flow {
        if (!hasUsageStatsPermission()) {
            emit(emptyList())
            return@flow
        }
        val today = LocalDate.now()
        val result = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val totalMs = getScreenTimeForDate(date)
            date to Duration.ofMillis(totalMs)
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    /**
     * Get per-app usage for a specific date, sorted by most used.
     */
    fun getPerAppUsage(date: LocalDate): Flow<List<AppUsageStat>> = flow {
        if (!hasUsageStatsPermission()) {
            emit(emptyList())
            return@flow
        }
        val (startMs, endMs) = dayRange(date)
        val perApp = getPerAppForegroundTime(startMs, endMs)
        val openCounts = countAppOpens(startMs, endMs)

        val result = perApp
            .filter { it.value > 60_000 }
            .entries
            .sortedByDescending { it.value }
            .map { (pkg, timeMs) ->
                AppUsageStat(
                    packageName = pkg,
                    appName = getAppLabel(pkg),
                    category = categorizeApp(pkg),
                    usageTime = Duration.ofMillis(timeMs),
                    openCount = openCounts[pkg] ?: 0,
                )
            }
        emit(result)
    }.flowOn(Dispatchers.IO)

    /**
     * Get top N apps by usage for a specific date.
     */
    fun getTopApps(date: LocalDate, limit: Int): Flow<List<AppUsageStat>> = flow {
        if (!hasUsageStatsPermission()) {
            emit(emptyList())
            return@flow
        }
        val (startMs, endMs) = dayRange(date)
        val perApp = getPerAppForegroundTime(startMs, endMs)
        val openCounts = countAppOpens(startMs, endMs)

        val result = perApp
            .filter { it.value > 60_000 }
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (pkg, timeMs) ->
                AppUsageStat(
                    packageName = pkg,
                    appName = getAppLabel(pkg),
                    category = categorizeApp(pkg),
                    usageTime = Duration.ofMillis(timeMs),
                    openCount = openCounts[pkg] ?: 0,
                )
            }
        emit(result)
    }.flowOn(Dispatchers.IO)

    /**
     * Get usage broken down by category for a specific date.
     */
    fun getCategoryBreakdown(date: LocalDate): Flow<Map<String, Duration>> = flow {
        if (!hasUsageStatsPermission()) {
            emit(emptyMap())
            return@flow
        }
        val (startMs, endMs) = dayRange(date)
        val perApp = getPerAppForegroundTime(startMs, endMs)

        val breakdown = perApp
            .filter { it.value >= 10_000 } // Include apps used for 10+ seconds
            .entries
            .groupBy { categorizeApp(it.key) }
            .mapValues { (_, entries) ->
                Duration.ofMillis(entries.sumOf { it.value })
            }
            .toSortedMap(compareByDescending { it.length })

        emit(breakdown)
    }.flowOn(Dispatchers.IO)

    /**
     * Count phone unlocks/screen-on events for today.
     */
    fun getUnlockCount(date: LocalDate): Flow<Int> = flow {
        if (!hasUsageStatsPermission()) {
            emit(0)
            return@flow
        }
        val (startMs, endMs) = dayRange(date)
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(startMs, endMs)
        var count = 0
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) {
                count++
            }
        }
        emit(count)
    }.flowOn(Dispatchers.IO)

    // ── Range-based queries (called from StatsRepository) ───

    /**
     * Calculate total foreground time for an arbitrary millisecond range.
     * Must be called on a background thread.
     */
    fun calculateForegroundTimeForRange(startMs: Long, endMs: Long): Long =
        calculateForegroundTimeFromEvents(startMs, endMs)

    /**
     * Get top N apps across an arbitrary millisecond range.
     * Must be called on a background thread.
     */
    fun getTopAppsForRange(startMs: Long, endMs: Long, limit: Int): List<AppUsageStat> {
        val perApp = getPerAppForegroundTime(startMs, endMs)
        val openCounts = countAppOpens(startMs, endMs)
        return perApp
            .filter { it.value > 60_000 }
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (pkg, timeMs) ->
                AppUsageStat(
                    packageName = pkg,
                    appName = getAppLabel(pkg),
                    category = categorizeApp(pkg),
                    usageTime = Duration.ofMillis(timeMs),
                    openCount = openCounts[pkg] ?: 0,
                )
            }
    }

    /**
     * Get unlock count for a single date (non-flow, for summing across ranges).
     * Must be called on a background thread.
     */
    fun getUnlockCountForDate(date: LocalDate): Int {
        if (!hasUsageStatsPermission()) return 0
        val (startMs, endMs) = dayRange(date)
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(startMs, endMs)
        var count = 0
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) {
                count++
            }
        }
        return count
    }

    // ── Internals ────────────────────────────────────────────

    private fun countAppOpens(startMs: Long, endMs: Long): Map<String, Int> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(startMs, endMs)
        val counts = mutableMapOf<String, Int>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val pkg = event.packageName
                if (pkg !in IGNORED_PACKAGES) {
                    counts[pkg] = (counts[pkg] ?: 0) + 1
                }
            }
        }
        return counts
    }

    private fun categorizeApp(packageName: String): String {
        // First check hardcoded lists — these override system categories
        val inSocial = packageName in SOCIAL_MEDIA_PACKAGES
        val inEntertainment = packageName in ENTERTAINMENT_PACKAGES
        val inBrowser = packageName in BROWSER_PACKAGES
        val inComm = packageName in COMMUNICATION_PACKAGES
        val inProd = packageName in PRODUCTIVITY_PACKAGES

        android.util.Log.d(
            "UsageTracker",
            "Categorizing $packageName: social=$inSocial, entertainment=$inEntertainment, " +
                "browser=$inBrowser, comm=$inComm, prod=$inProd",
        )

        if (inSocial) return "Social Media"
        if (inEntertainment) return "Entertainment"
        if (inBrowser) return "Browser"
        if (inComm) return "Communication"
        if (inProd) return "Productivity"

        // Try using PackageManager category (API 26+)
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val systemCategory = when (appInfo.category) {
                ApplicationInfo.CATEGORY_SOCIAL -> "Social Media"
                ApplicationInfo.CATEGORY_VIDEO -> "Entertainment"
                ApplicationInfo.CATEGORY_AUDIO -> "Entertainment"
                ApplicationInfo.CATEGORY_IMAGE -> "Entertainment"
                ApplicationInfo.CATEGORY_GAME -> "Games"
                ApplicationInfo.CATEGORY_NEWS -> "News"
                ApplicationInfo.CATEGORY_MAPS -> "Productivity"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                else -> "Other"
            }
            android.util.Log.d(
                "UsageTracker",
                "System category for $packageName: ${appInfo.category} -> $systemCategory",
            )
            return systemCategory
        } catch (_: Exception) {
            return "Other"
        }
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            info.loadLabel(pm).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    private fun dayRange(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val startMs = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return startMs to endMs
    }
}
