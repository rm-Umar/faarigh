package com.faarigh.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_domains")
data class BlockedDomain(
    @PrimaryKey val domain: String,
    val category: String, // "explicit", "ads", "telemetry", "shorts", "custom"
    val isBuiltIn: Boolean = true,
    val isEnabled: Boolean = true,
)
