package com.faarigh.app.data.repository

import com.faarigh.app.data.db.entity.AppSchedule
import kotlinx.coroutines.flow.Flow

interface AppScheduleRepository {
    fun getAllSchedules(): Flow<List<AppSchedule>>
    fun getActiveSchedules(): Flow<List<AppSchedule>>
    fun getSchedulesForApp(packageName: String): Flow<List<AppSchedule>>
    suspend fun addSchedule(schedule: AppSchedule): Long
    suspend fun updateSchedule(schedule: AppSchedule)
    suspend fun deleteSchedule(id: Long)
    suspend fun toggleSchedule(id: Long, enabled: Boolean)

    /**
     * Check if a package is currently blocked by any active schedule.
     * Returns a human-readable reason string if blocked, null if not blocked.
     */
    fun isAppCurrentlyBlocked(packageName: String, schedules: List<AppSchedule>): String?
}
