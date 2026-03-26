package com.faarigh.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intercepted_apps")
data class InterceptedApp(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val isEnabled: Boolean = true,
    val breathingDurationSec: Int = 10,
    val cooldownSec: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
)
