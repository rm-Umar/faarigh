package com.faarigh.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.faarigh.app.data.db.entity.DnsStatsAggregate
import kotlinx.coroutines.flow.Flow

@Dao
interface DnsStatsAggregateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(aggregates: List<DnsStatsAggregate>)

    @Query("SELECT SUM(totalQueries) FROM dns_stats_aggregate WHERE hourTimestamp >= :since")
    fun getTotalQueries(since: Long): Flow<Int?>

    @Query("SELECT SUM(blockedQueries) FROM dns_stats_aggregate WHERE hourTimestamp >= :since")
    fun getBlockedQueries(since: Long): Flow<Int?>

    @Query("""
        SELECT SUM(adsBlocked) as ads, SUM(explicitBlocked) as explicit,
               SUM(telemetryBlocked) as telemetry, SUM(customBlocked) as custom
        FROM dns_stats_aggregate WHERE hourTimestamp >= :since
    """)
    fun getCategoryTotals(since: Long): Flow<AggregateCategoryRow?>

    @Query("""
        SELECT hourTimestamp as hour, totalQueries as total, blockedQueries as blocked
        FROM dns_stats_aggregate
        WHERE hourTimestamp >= :since
        ORDER BY hourTimestamp
    """)
    fun getHourlyActivity(since: Long): Flow<List<AggregateHourlyRow>>

    @Query("SELECT SUM(uniqueDomains) FROM dns_stats_aggregate WHERE hourTimestamp >= :since")
    fun getUniqueDomains(since: Long): Flow<Int?>

    @Query("SELECT AVG(avgResponseMs) FROM dns_stats_aggregate WHERE avgResponseMs > 0 AND hourTimestamp >= :since")
    fun getAvgResponseTime(since: Long): Flow<Double?>

    @Query("DELETE FROM dns_stats_aggregate WHERE hourTimestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}

data class AggregateCategoryRow(
    val ads: Int?,
    val explicit: Int?,
    val telemetry: Int?,
    val custom: Int?,
)

data class AggregateHourlyRow(
    val hour: Long,
    val total: Int,
    val blocked: Int,
)
