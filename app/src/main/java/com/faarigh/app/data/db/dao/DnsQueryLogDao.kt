package com.faarigh.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.faarigh.app.data.db.entity.DnsQueryLog
import kotlinx.coroutines.flow.Flow

data class DomainCount(val domain: String, val count: Int)
data class CategoryCount(val category: String, val count: Int)
data class HourlyCount(val hour: Int, val total: Int, val blocked: Int)

@Dao
interface DnsQueryLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: DnsQueryLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<DnsQueryLog>)

    // ── Totals ──────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM dns_query_log WHERE timestamp >= :since")
    fun getTotalQueries(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM dns_query_log WHERE isBlocked = 1 AND timestamp >= :since")
    fun getBlockedQueries(since: Long): Flow<Int>

    // ── Top blocked domains ────────────────────────────────

    @Query("""
        SELECT domain, COUNT(*) AS count
        FROM dns_query_log
        WHERE isBlocked = 1 AND timestamp >= :since
        GROUP BY domain
        ORDER BY count DESC
        LIMIT :limit
    """)
    fun getTopBlockedDomains(since: Long, limit: Int = 10): Flow<List<DomainCount>>

    // ── Top allowed (queried) domains ──────────────────────

    @Query("""
        SELECT domain, COUNT(*) AS count
        FROM dns_query_log
        WHERE isBlocked = 0 AND timestamp >= :since
        GROUP BY domain
        ORDER BY count DESC
        LIMIT :limit
    """)
    fun getTopAllowedDomains(since: Long, limit: Int = 10): Flow<List<DomainCount>>

    // ── Category breakdown ─────────────────────────────────

    @Query("""
        SELECT category, COUNT(*) AS count
        FROM dns_query_log
        WHERE isBlocked = 1 AND timestamp >= :since
        GROUP BY category
        ORDER BY count DESC
    """)
    fun getBlockedByCategory(since: Long): Flow<List<CategoryCount>>

    // ── Hourly activity (for timeline graph) ───────────────

    @Query("""
        SELECT
            CAST(((timestamp / 1000) % 86400) / 3600 AS INTEGER) AS hour,
            COUNT(*) AS total,
            SUM(CASE WHEN isBlocked = 1 THEN 1 ELSE 0 END) AS blocked
        FROM dns_query_log
        WHERE timestamp >= :since
        GROUP BY hour
        ORDER BY hour
    """)
    fun getHourlyActivity(since: Long): Flow<List<HourlyCount>>

    // ── Recent queries ─────────────────────────────────────

    @Query("SELECT * FROM dns_query_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentQueries(limit: Int = 50): Flow<List<DnsQueryLog>>

    @Query("SELECT * FROM dns_query_log WHERE isBlocked = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentBlocked(limit: Int = 50): Flow<List<DnsQueryLog>>

    // ── Average response time ──────────────────────────────

    @Query("SELECT AVG(responseTimeMs) FROM dns_query_log WHERE isBlocked = 0 AND responseTimeMs > 0 AND timestamp >= :since")
    fun getAvgResponseTime(since: Long): Flow<Double?>

    // ── Unique domains ─────────────────────────────────────

    @Query("SELECT COUNT(DISTINCT domain) FROM dns_query_log WHERE timestamp >= :since")
    fun getUniqueDomains(since: Long): Flow<Int>

    @Query("SELECT COUNT(DISTINCT domain) FROM dns_query_log WHERE isBlocked = 1 AND timestamp >= :since")
    fun getUniqueBlockedDomains(since: Long): Flow<Int>

    // ── Cleanup ────────────────────────────────────────────

    @Query("SELECT * FROM dns_query_log WHERE timestamp < :before")
    suspend fun getRowsOlderThan(before: Long): List<DnsQueryLog>

    @Query("DELETE FROM dns_query_log WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM dns_query_log")
    suspend fun deleteAll()
}
