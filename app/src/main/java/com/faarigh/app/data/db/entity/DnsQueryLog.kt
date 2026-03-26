package com.faarigh.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistent DNS query log — tracks every DNS query with domain, blocked status,
 * category, and timestamp. Enables Pi-hole style analytics.
 */
@Entity(
    tableName = "dns_query_log",
    indices = [
        Index("timestamp"),
        Index("domain"),
        Index("isBlocked"),
        Index("category"),
    ],
)
data class DnsQueryLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val isBlocked: Boolean,
    val category: String = "allowed",  // "ads", "explicit", "telemetry", "custom", "allowed"
    val timestamp: Long = System.currentTimeMillis(),
    val upstreamDns: String = "",
    val responseTimeMs: Long = 0,
)
