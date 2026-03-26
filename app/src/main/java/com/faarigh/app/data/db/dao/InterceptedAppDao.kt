package com.faarigh.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.faarigh.app.data.db.entity.InterceptedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface InterceptedAppDao {

    @Query("SELECT * FROM intercepted_apps ORDER BY appLabel ASC")
    fun getAll(): Flow<List<InterceptedApp>>

    @Query("SELECT * FROM intercepted_apps WHERE isEnabled = 1")
    fun getEnabled(): Flow<List<InterceptedApp>>

    @Query("SELECT packageName FROM intercepted_apps WHERE isEnabled = 1")
    suspend fun getEnabledPackageNames(): List<String>

    @Query("SELECT * FROM intercepted_apps WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): InterceptedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: InterceptedApp)

    @Delete
    suspend fun delete(app: InterceptedApp)

    @Query("UPDATE intercepted_apps SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)
}
