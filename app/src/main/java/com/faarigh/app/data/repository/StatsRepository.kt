package com.faarigh.app.data.repository

import com.faarigh.app.data.db.dao.AppInterventionRow
import com.faarigh.app.data.db.dao.DailyIntentionalRow
import com.faarigh.app.data.db.dao.IntentionalUseResult
import com.faarigh.app.data.db.dao.InterventionEventDao
import com.faarigh.app.data.db.dao.InterventionHourlyCount
import com.faarigh.app.data.db.dao.ModuleActionCount
import com.faarigh.app.data.tracking.AppUsageStat
import com.faarigh.app.data.tracking.UsageTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

// ── Data classes ─────────────────────────────────────────────

data class ModuleStats(
    val shown: Int,
    val allowed: Int,
    val blocked: Int,
    val successRate: Float, // blocked / (allowed + blocked)
)

data class AppInterventionStat(
    val packageName: String,
    val appName: String,
    val interventionCount: Int,
    val allowedCount: Int,
    val blockedCount: Int,
)

data class DnsOverview(
    val totalQueries: Int,
    val blockedQueries: Int,
    val blockRate: Float,
)

data class DashboardOverview(
    val screenTimeToday: Duration,
    val screenTimeYesterday: Duration,
    val interventionsToday: Int,
    val successRateToday: Float,
    val dnsQueriesTotal: Int,
    val dnsQueriesBlocked: Int,
    val topApps: List<AppUsageStat>,
    val unlockCount: Int,
)

@Singleton
class StatsRepository @Inject constructor(
    private val usageTracker: UsageTracker,
    private val interventionDao: InterventionEventDao,
    private val dnsStatsRepo: DnsStatsRepository,
) {

    // ══════════════════════════════════════════════════════════
    // SCREEN TIME
    // ══════════════════════════════════════════════════════════

    fun getTotalScreenTime(date: LocalDate): Flow<Duration> =
        usageTracker.getTotalScreenTime(date)

    fun getWeeklyScreenTime(): Flow<List<Pair<LocalDate, Duration>>> =
        usageTracker.getWeeklyScreenTime()

    fun getPerAppUsage(date: LocalDate): Flow<List<AppUsageStat>> =
        usageTracker.getPerAppUsage(date)

    fun getTopApps(date: LocalDate, limit: Int): Flow<List<AppUsageStat>> =
        usageTracker.getTopApps(date, limit)

    fun getCategoryBreakdown(date: LocalDate): Flow<Map<String, Duration>> =
        usageTracker.getCategoryBreakdown(date)

    fun getUnlockCount(date: LocalDate): Flow<Int> =
        usageTracker.getUnlockCount(date)

    fun hasUsageStatsPermission(): Boolean =
        usageTracker.hasUsageStatsPermission()

    // ══════════════════════════════════════════════════════════
    // INTERVENTIONS
    // ══════════════════════════════════════════════════════════

    fun getTodayInterventionCount(): Flow<Int> =
        interventionDao.getTotalCountSince(todayStartMs())

    fun getInterventionSuccessRate(days: Int): Flow<Float> {
        val (start, end) = daysRange(days)
        return interventionDao.getActionCountsSince(start).map { counts ->
            val allowed = counts.find { it.action == "allowed" }?.count ?: 0
            val blocked = counts.find { it.action == "blocked" }?.count ?: 0
            val total = allowed + blocked
            if (total > 0) blocked.toFloat() / total else 0f
        }
    }

    fun getModuleStats(moduleId: String, days: Int): Flow<ModuleStats> {
        val (start, end) = daysRange(days)
        return interventionDao.getAllowedVsBlockedByModule(moduleId, start, end).map { counts ->
            val allowed = counts.find { it.action == "allowed" }?.count ?: 0
            val blocked = counts.find { it.action == "blocked" }?.count ?: 0
            val total = allowed + blocked
            ModuleStats(
                shown = total,
                allowed = allowed,
                blocked = blocked,
                successRate = if (total > 0) blocked.toFloat() / total else 0f,
            )
        }
    }

    fun getIntentionalUseRatio(days: Int): Flow<IntentionalUseResult?> {
        val (start, end) = daysRange(days)
        return interventionDao.getIntentionalUseRatio(start, end)
    }

    fun getDailyIntentionalTrend(days: Int): Flow<List<DailyIntentionalRow>> {
        val (start, end) = daysRange(days)
        return interventionDao.getDailyIntentionalTrend(start, end)
    }

    fun getConsciousChoicesToday(): Flow<Int> =
        interventionDao.getEventCountByAction("blocked", todayStartMs(), todayStartMs() + 24 * 60 * 60 * 1000L)

    fun getHourlyActivity(date: LocalDate): Flow<List<InterventionHourlyCount>> {
        val zone = ZoneId.systemDefault()
        val startMs = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return interventionDao.getHourlyEventCounts(startMs, endMs)
    }

    fun getMostPausedApps(days: Int, limit: Int): Flow<List<AppInterventionStat>> {
        val (start, end) = daysRange(days)
        return interventionDao.getMostInterventedApps(start, end, limit).map { rows ->
            rows.map { row ->
                AppInterventionStat(
                    packageName = row.appPackage,
                    appName = row.appName,
                    interventionCount = row.interventionCount,
                    allowedCount = row.allowedCount,
                    blockedCount = row.blockedCount,
                )
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // NSFW SPECIFIC
    // ══════════════════════════════════════════════════════════

    fun getNsfwDetections(days: Int): Flow<Int> {
        val (start, end) = daysRange(days)
        return interventionDao.getNsfwEventCount(start, end)
    }

    fun getNsfwAllowRate(days: Int): Flow<Float> {
        val (start, end) = daysRange(days)
        return interventionDao.getNsfwAllowedVsBlocked(start, end).map { counts ->
            val allowed = counts.find { it.action == "allowed" }?.count ?: 0
            val blocked = counts.find { it.action == "blocked" }?.count ?: 0
            val total = allowed + blocked
            if (total > 0) allowed.toFloat() / total else 0f
        }
    }

    fun getTotalPauseTime(days: Int): Flow<Duration> {
        val (start, end) = daysRange(days)
        return interventionDao.getTotalPauseTime(start, end).map { ms ->
            Duration.ofMillis(ms)
        }
    }

    // ══════════════════════════════════════════════════════════
    // DNS SPECIFIC
    // ══════════════════════════════════════════════════════════

    fun getDnsStats(days: Int): Flow<DnsOverview> {
        val since = daysRange(days).first
        return combine(
            dnsStatsRepo.getTotalQueries(since),
            dnsStatsRepo.getBlockedQueries(since),
        ) { total, blocked ->
            DnsOverview(
                totalQueries = total,
                blockedQueries = blocked,
                blockRate = if (total > 0) blocked.toFloat() / total else 0f,
            )
        }
    }

    // ══════════════════════════════════════════════════════════
    // COMBINED DASHBOARD OVERVIEW
    // ══════════════════════════════════════════════════════════

    fun getDashboardOverview(): Flow<DashboardOverview> {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val todayStart = todayStartMs()
        val dnsSince = todayStart

        // combine() with 6 flows requires the array overload
        val screenTodayFlow = usageTracker.getTotalScreenTime(today)
        val screenYesterdayFlow = usageTracker.getTotalScreenTime(yesterday)
        val interventionCountFlow = interventionDao.getTotalCountSince(todayStart)
        val actionCountsFlow = interventionDao.getActionCountsSince(todayStart)
        val dnsTotalFlow = dnsStatsRepo.getTotalQueries(dnsSince)
        val dnsBlockedFlow = dnsStatsRepo.getBlockedQueries(dnsSince)

        return combine(
            screenTodayFlow,
            screenYesterdayFlow,
            interventionCountFlow,
            actionCountsFlow,
            dnsTotalFlow,
            dnsBlockedFlow,
        ) { values ->
            val screenToday = values[0] as Duration
            val screenYesterday = values[1] as Duration
            val interventionCount = values[2] as Int
            @Suppress("UNCHECKED_CAST")
            val actionCounts = values[3] as List<ModuleActionCount>
            val dnsTotal = values[4] as Int
            val dnsBlocked = values[5] as Int

            val allowed = actionCounts.find { it.action == "allowed" }?.count ?: 0
            val blocked = actionCounts.find { it.action == "blocked" }?.count ?: 0
            val total = allowed + blocked
            val successRate = if (total > 0) blocked.toFloat() / total else 0f

            DashboardOverview(
                screenTimeToday = screenToday,
                screenTimeYesterday = screenYesterday,
                interventionsToday = interventionCount,
                successRateToday = successRate,
                dnsQueriesTotal = dnsTotal,
                dnsQueriesBlocked = dnsBlocked,
                topApps = emptyList(), // loaded separately to avoid too many combines
                unlockCount = 0,
            )
        }
    }

    // ══════════════════════════════════════════════════════════
    // RANGE-AWARE QUERIES (for stats screen time-range chips)
    // ══════════════════════════════════════════════════════════

    fun getInterventionCountSince(startMs: Long): Flow<Int> =
        interventionDao.getTotalCountSince(startMs)

    fun getInterventionSuccessRateSince(startMs: Long): Flow<Float> =
        interventionDao.getActionCountsSince(startMs).map { counts ->
            val allowed = counts.find { it.action == "allowed" }?.count ?: 0
            val blocked = counts.find { it.action == "blocked" }?.count ?: 0
            val total = allowed + blocked
            if (total > 0) blocked.toFloat() / total else 0f
        }

    fun getConsciousChoicesSince(startMs: Long): Flow<Int> {
        val endMs = System.currentTimeMillis()
        return interventionDao.getEventCountByAction("blocked", startMs, endMs)
    }

    fun getIntentionalUseRatioSince(startMs: Long): Flow<IntentionalUseResult?> {
        val endMs = System.currentTimeMillis()
        return interventionDao.getIntentionalUseRatio(startMs, endMs)
    }

    /**
     * Sum screen time across a date range (inclusive of start, exclusive of end).
     */
    fun getTotalScreenTimeForRange(startDate: LocalDate, endDate: LocalDate): Flow<Duration> = flow {
        if (!usageTracker.hasUsageStatsPermission()) {
            emit(Duration.ZERO)
            return@flow
        }
        val zone = ZoneId.systemDefault()
        val startMs = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = endDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val totalMs = usageTracker.calculateForegroundTimeForRange(startMs, endMs)
        emit(Duration.ofMillis(totalMs))
    }.flowOn(Dispatchers.IO)

    /**
     * Get top apps across a date range.
     */
    fun getTopAppsForRange(startDate: LocalDate, endDate: LocalDate, limit: Int): Flow<List<AppUsageStat>> = flow {
        if (!usageTracker.hasUsageStatsPermission()) {
            emit(emptyList())
            return@flow
        }
        val zone = ZoneId.systemDefault()
        val startMs = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = endDate.atStartOfDay(zone).toInstant().toEpochMilli()
        emit(usageTracker.getTopAppsForRange(startMs, endMs, limit))
    }.flowOn(Dispatchers.IO)

    /**
     * Get unlock count summed across a date range.
     */
    fun getUnlockCountForRange(startDate: LocalDate, endDate: LocalDate): Flow<Int> = flow {
        if (!usageTracker.hasUsageStatsPermission()) {
            emit(0)
            return@flow
        }
        var total = 0
        var d = startDate
        while (d.isBefore(endDate)) {
            total += usageTracker.getUnlockCountForDate(d)
            d = d.plusDays(1)
        }
        emit(total)
    }.flowOn(Dispatchers.IO)

    // ── Helpers ──────────────────────────────────────────────

    private fun todayStartMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun daysRange(days: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val end = cal.timeInMillis + 24 * 60 * 60 * 1000L
        if (days > 0) cal.add(Calendar.DAY_OF_YEAR, -days)
        return cal.timeInMillis to end
    }
}
