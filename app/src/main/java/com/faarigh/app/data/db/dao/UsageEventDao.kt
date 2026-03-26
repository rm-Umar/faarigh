package com.faarigh.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.faarigh.app.data.db.entity.UsageEvent
import kotlinx.coroutines.flow.Flow

data class ActionCount(
    val action: String,
    val count: Int,
)

@Dao
interface UsageEventDao {

    @Insert
    suspend fun insert(event: UsageEvent)

    @Query("SELECT * FROM usage_events WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getEventsSince(since: Long): Flow<List<UsageEvent>>

    @Query(
        "SELECT action, COUNT(*) as count FROM usage_events " +
        "WHERE timestamp >= :since GROUP BY action"
    )
    fun getActionCounts(since: Long): Flow<List<ActionCount>>

    @Query(
        "SELECT * FROM usage_events WHERE packageName = :packageName " +
        "AND timestamp >= :since ORDER BY timestamp DESC"
    )
    fun getEventsForApp(packageName: String, since: Long): Flow<List<UsageEvent>>

    @Query("SELECT COUNT(*) FROM usage_events WHERE timestamp >= :since")
    fun getTotalCount(since: Long): Flow<Int>

    @Query("DELETE FROM usage_events")
    suspend fun deleteAll()
}
