package com.faarigh.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.faarigh.app.data.db.entity.AppSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface AppScheduleDao {

    @Query("SELECT * FROM app_schedules ORDER BY createdAt DESC")
    fun getAllSchedules(): Flow<List<AppSchedule>>

    @Query("SELECT * FROM app_schedules WHERE isEnabled = 1")
    fun getActiveSchedules(): Flow<List<AppSchedule>>

    @Query("SELECT * FROM app_schedules WHERE packageName = :packageName")
    fun getSchedulesForApp(packageName: String): Flow<List<AppSchedule>>

    @Query("SELECT * FROM app_schedules WHERE id = :id")
    suspend fun getById(id: Long): AppSchedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: AppSchedule): Long

    @Update
    suspend fun update(schedule: AppSchedule)

    @Delete
    suspend fun delete(schedule: AppSchedule)

    @Query("DELETE FROM app_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE app_schedules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
