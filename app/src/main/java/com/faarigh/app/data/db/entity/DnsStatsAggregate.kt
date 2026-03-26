package com.faarigh.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Pre-computed hourly DNS stats summary.
 * Raw DnsQueryLog rows older than 7 days are aggregated into these summaries,
 * then the raw rows are purged. This keeps storage under control while
 * preserving historical stats for weeks/months.
 *
 * Storage: ~100 bytes per row × 24 hours × 30 days = ~72KB/month
 * vs raw: ~5MB/day × 30 days = ~150MB/month
 */
@Entity(
    tableName = "dns_stats_aggregate",
    indices = [Index("hourTimestamp")],
)
data class DnsStatsAggregate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hourTimestamp: Long,         // Rounded to the hour (millis)
    val totalQueries: Int,
    val blockedQueries: Int,
    val adsBlocked: Int = 0,
    val explicitBlocked: Int = 0,
    val telemetryBlocked: Int = 0,
    val customBlocked: Int = 0,
    val uniqueDomains: Int = 0,
    val avgResponseMs: Double = 0.0,
    val topBlockedDomain: String = "",   // Most blocked domain this hour
    val topAllowedDomain: String = "",   // Most queried domain this hour
)
