package com.faarigh.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_events")
data class UsageEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String, // "proceeded", "turned_back"
    val interventionType: String = "pause",
    val sessionDurationSec: Int = 0,
)
