package com.faarigh.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "app_schedules", indices = [Index("packageName")])
data class AppSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val type: String,           // "schedule", "daily_limit", "focus", "detox"
    val startHour: Int = 0,
    val startMin: Int = 0,
    val endHour: Int = 7,
    val endMin: Int = 0,
    val daysOfWeek: String = "1,2,3,4,5,6,7",
    val dailyLimitMin: Int = 0,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
)
