package com.faarigh.app.data.repository

import com.faarigh.app.data.db.dao.AppScheduleDao
import com.faarigh.app.data.db.entity.AppSchedule
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppScheduleRepositoryImpl @Inject constructor(
    private val dao: AppScheduleDao,
) : AppScheduleRepository {

    override fun getAllSchedules(): Flow<List<AppSchedule>> = dao.getAllSchedules()

    override fun getActiveSchedules(): Flow<List<AppSchedule>> = dao.getActiveSchedules()

    override fun getSchedulesForApp(packageName: String): Flow<List<AppSchedule>> =
        dao.getSchedulesForApp(packageName)

    override suspend fun addSchedule(schedule: AppSchedule): Long = dao.insert(schedule)

    override suspend fun updateSchedule(schedule: AppSchedule) = dao.update(schedule)

    override suspend fun deleteSchedule(id: Long) = dao.deleteById(id)

    override suspend fun toggleSchedule(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    override fun isAppCurrentlyBlocked(packageName: String, schedules: List<AppSchedule>): String? {
        val now = Calendar.getInstance()
        val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK) // 1=Sunday
        // Convert to ISO day: 1=Monday..7=Sunday
        val isoDow = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek - 1
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val currentTime = System.currentTimeMillis()

        for (schedule in schedules) {
            if (schedule.packageName != packageName) continue
            if (!schedule.isEnabled) continue
            if (schedule.expiresAt != null && currentTime > schedule.expiresAt) continue

            val activeDays = schedule.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (isoDow !in activeDays) continue

            when (schedule.type) {
                "schedule", "focus", "detox" -> {
                    val startMin = schedule.startHour * 60 + schedule.startMin
                    val endMin = schedule.endHour * 60 + schedule.endMin

                    val isBlocked = if (startMin <= endMin) {
                        nowMin in startMin until endMin
                    } else {
                        // Overnight range
                        nowMin >= startMin || nowMin < endMin
                    }

                    if (isBlocked) {
                        val endFormatted = "%02d:%02d".format(schedule.endHour, schedule.endMin)
                        return when (schedule.type) {
                            "focus" -> "${schedule.appLabel} is blocked during focus mode until $endFormatted"
                            "detox" -> "${schedule.appLabel} is in detox until $endFormatted"
                            else -> "${schedule.appLabel} is blocked until $endFormatted"
                        }
                    }
                }
                // "daily_limit" would need usage tracking — skip for now
            }
        }
        return null
    }
}
