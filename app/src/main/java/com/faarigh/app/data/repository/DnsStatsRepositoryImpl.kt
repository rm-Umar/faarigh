package com.faarigh.app.data.repository

import android.util.Log
import com.faarigh.app.data.db.dao.CategoryCount
import com.faarigh.app.data.db.dao.DnsQueryLogDao
import com.faarigh.app.data.db.dao.DnsStatsAggregateDao
import com.faarigh.app.data.db.dao.DomainCount
import com.faarigh.app.data.db.dao.HourlyCount
import com.faarigh.app.data.db.entity.DnsQueryLog
import com.faarigh.app.data.db.entity.DnsStatsAggregate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsStatsRepositoryImpl @Inject constructor(
    private val rawDao: DnsQueryLogDao,
    private val aggDao: DnsStatsAggregateDao,
) : DnsStatsRepository {

    companion object {
        private const val TAG = "DnsStats"
        private val SEVEN_DAYS_MS = TimeUnit.DAYS.toMillis(7)
    }

    override suspend fun logQuery(
        domain: String, isBlocked: Boolean, category: String,
        upstreamDns: String, responseTimeMs: Long,
    ) {
        rawDao.insert(DnsQueryLog(
            domain = domain, isBlocked = isBlocked, category = category,
            upstreamDns = upstreamDns, responseTimeMs = responseTimeMs,
        ))
    }

    override suspend fun logBatch(logs: List<DnsQueryLog>) = rawDao.insertAll(logs)

    // ── Smart queries: raw for recent, raw-only for now (aggregate union later) ──

    override fun getTotalQueries(since: Long): Flow<Int> =
        rawDao.getTotalQueries(since)

    override fun getBlockedQueries(since: Long): Flow<Int> =
        rawDao.getBlockedQueries(since)

    override fun getTopBlockedDomains(since: Long, limit: Int): Flow<List<DomainCount>> =
        rawDao.getTopBlockedDomains(since, limit)

    override fun getTopAllowedDomains(since: Long, limit: Int): Flow<List<DomainCount>> =
        rawDao.getTopAllowedDomains(since, limit)

    override fun getBlockedByCategory(since: Long): Flow<List<CategoryCount>> =
        rawDao.getBlockedByCategory(since)

    override fun getHourlyActivity(since: Long): Flow<List<HourlyCount>> =
        rawDao.getHourlyActivity(since)

    override fun getRecentQueries(limit: Int): Flow<List<DnsQueryLog>> =
        rawDao.getRecentQueries(limit)

    override fun getRecentBlocked(limit: Int): Flow<List<DnsQueryLog>> =
        rawDao.getRecentBlocked(limit)

    override fun getAvgResponseTime(since: Long): Flow<Double?> =
        rawDao.getAvgResponseTime(since)

    override fun getUniqueDomains(since: Long): Flow<Int> =
        rawDao.getUniqueDomains(since)

    override fun getUniqueBlockedDomains(since: Long): Flow<Int> =
        rawDao.getUniqueBlockedDomains(since)

    // ── Aggregate + Purge ──────────────────────────────────────

    override suspend fun aggregateAndPurge(keepRawDays: Int) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(keepRawDays.toLong())
        Log.i(TAG, "Aggregating raw DNS logs older than $keepRawDays days...")

        try {
            // Get all raw rows older than cutoff, grouped by hour
            val oldRows = rawDao.getRowsOlderThan(cutoff)
            if (oldRows.isEmpty()) {
                Log.i(TAG, "No raw rows to aggregate")
                return
            }

            // Group by hour
            val byHour = oldRows.groupBy { roundToHour(it.timestamp) }

            val aggregates = byHour.map { (hourTs, rows) ->
                val blocked = rows.filter { it.isBlocked }
                DnsStatsAggregate(
                    hourTimestamp = hourTs,
                    totalQueries = rows.size,
                    blockedQueries = blocked.size,
                    adsBlocked = blocked.count { it.category == "ads" },
                    explicitBlocked = blocked.count { it.category == "explicit" },
                    telemetryBlocked = blocked.count { it.category == "telemetry" },
                    customBlocked = blocked.count { it.category == "custom" },
                    uniqueDomains = rows.map { it.domain }.distinct().size,
                    avgResponseMs = rows.filter { it.responseTimeMs > 0 }
                        .map { it.responseTimeMs.toDouble() }
                        .average().takeIf { !it.isNaN() } ?: 0.0,
                    topBlockedDomain = blocked.groupBy { it.domain }
                        .maxByOrNull { it.value.size }?.key ?: "",
                    topAllowedDomain = rows.filter { !it.isBlocked }
                        .groupBy { it.domain }
                        .maxByOrNull { it.value.size }?.key ?: "",
                )
            }

            aggDao.insertAll(aggregates)
            rawDao.deleteOlderThan(cutoff)
            Log.i(TAG, "Aggregated ${oldRows.size} rows into ${aggregates.size} hourly summaries")
        } catch (e: Exception) {
            Log.e(TAG, "Aggregation failed: ${e.message}", e)
        }
    }

    override suspend fun cleanup(keepDays: Int) {
        // First aggregate old raw rows (keep 7 days raw)
        aggregateAndPurge(7)
        // Then purge old aggregates beyond keepDays
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(keepDays.toLong())
        aggDao.deleteOlderThan(cutoff)
    }

    private fun roundToHour(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
