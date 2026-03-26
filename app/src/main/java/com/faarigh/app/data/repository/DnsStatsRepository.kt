package com.faarigh.app.data.repository

import com.faarigh.app.data.db.dao.CategoryCount
import com.faarigh.app.data.db.dao.DomainCount
import com.faarigh.app.data.db.dao.HourlyCount
import com.faarigh.app.data.db.entity.DnsQueryLog
import kotlinx.coroutines.flow.Flow

interface DnsStatsRepository {
    suspend fun logQuery(domain: String, isBlocked: Boolean, category: String, upstreamDns: String, responseTimeMs: Long)
    suspend fun logBatch(logs: List<DnsQueryLog>)

    // Smart queries — use raw table for recent data, aggregate for older
    fun getTotalQueries(since: Long): Flow<Int>
    fun getBlockedQueries(since: Long): Flow<Int>
    fun getTopBlockedDomains(since: Long, limit: Int = 10): Flow<List<DomainCount>>
    fun getTopAllowedDomains(since: Long, limit: Int = 10): Flow<List<DomainCount>>
    fun getBlockedByCategory(since: Long): Flow<List<CategoryCount>>
    fun getHourlyActivity(since: Long): Flow<List<HourlyCount>>
    fun getRecentQueries(limit: Int = 30): Flow<List<DnsQueryLog>>
    fun getRecentBlocked(limit: Int = 30): Flow<List<DnsQueryLog>>
    fun getAvgResponseTime(since: Long): Flow<Double?>
    fun getUniqueDomains(since: Long): Flow<Int>
    fun getUniqueBlockedDomains(since: Long): Flow<Int>

    // Aggregate + purge: compress old rows, delete raw
    suspend fun aggregateAndPurge(keepRawDays: Int = 7)
    suspend fun cleanup(keepDays: Int = 90)
}
