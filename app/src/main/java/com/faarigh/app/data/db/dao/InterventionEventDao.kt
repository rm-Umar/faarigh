package com.faarigh.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.faarigh.app.data.db.entity.InterventionEvent
import kotlinx.coroutines.flow.Flow

data class ModuleActionCount(
    val action: String,
    val count: Int,
)

data class AppInterventionRow(
    val appPackage: String,
    val appName: String,
    val interventionCount: Int,
    val allowedCount: Int,
    val blockedCount: Int,
)

data class InterventionHourlyCount(
    val hour: Int,
    val count: Int,
)

data class IntentionalUseResult(
    val blocked: Int,
    val allowed: Int,
)

data class DailyIntentionalRow(
    val dayEpoch: Long,
    val blocked: Int,
    val allowed: Int,
)

@Dao
interface InterventionEventDao {

    @Insert
    suspend fun insert(event: InterventionEvent)

    // ── Date range queries ───────────────────────────────────

    @Query("SELECT * FROM intervention_events WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getEventsForDateRange(start: Long, end: Long): Flow<List<InterventionEvent>>

    @Query("SELECT COUNT(*) FROM intervention_events WHERE moduleId = :moduleId AND timestamp >= :start AND timestamp <= :end")
    fun getEventCountByModule(moduleId: String, start: Long, end: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM intervention_events WHERE action = :action AND timestamp >= :start AND timestamp <= :end")
    fun getEventCountByAction(action: String, start: Long, end: Long): Flow<Int>

    // ── Allowed vs Blocked per module ────────────────────────

    @Query("""
        SELECT action, COUNT(*) as count
        FROM intervention_events
        WHERE moduleId = :moduleId AND timestamp >= :start AND timestamp <= :end
            AND action IN ('allowed', 'blocked')
        GROUP BY action
    """)
    fun getAllowedVsBlockedByModule(moduleId: String, start: Long, end: Long): Flow<List<ModuleActionCount>>

    // ── Most intervened apps ─────────────────────────────────

    @Query("""
        SELECT appPackage, appName,
            COUNT(*) as interventionCount,
            SUM(CASE WHEN action = 'allowed' THEN 1 ELSE 0 END) as allowedCount,
            SUM(CASE WHEN action = 'blocked' THEN 1 ELSE 0 END) as blockedCount
        FROM intervention_events
        WHERE timestamp >= :start AND timestamp <= :end
        GROUP BY appPackage
        ORDER BY interventionCount DESC
        LIMIT :limit
    """)
    fun getMostInterventedApps(start: Long, end: Long, limit: Int = 10): Flow<List<AppInterventionRow>>

    // ── NSFW specific ────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM intervention_events WHERE moduleId = 'nsfw_detection' AND timestamp >= :start AND timestamp <= :end")
    fun getNsfwEventCount(start: Long, end: Long): Flow<Int>

    @Query("""
        SELECT action, COUNT(*) as count
        FROM intervention_events
        WHERE moduleId = 'nsfw_detection' AND timestamp >= :start AND timestamp <= :end
            AND action IN ('allowed', 'blocked')
        GROUP BY action
    """)
    fun getNsfwAllowedVsBlocked(start: Long, end: Long): Flow<List<ModuleActionCount>>

    // ── Total pause time ─────────────────────────────────────

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM intervention_events WHERE action = 'allowed' AND timestamp >= :start AND timestamp <= :end")
    fun getTotalPauseTime(start: Long, end: Long): Flow<Long>

    // ── Hourly breakdown ─────────────────────────────────────

    @Query("""
        SELECT CAST(((timestamp / 1000) % 86400) / 3600 AS INTEGER) AS hour,
            COUNT(*) AS count
        FROM intervention_events
        WHERE timestamp >= :start AND timestamp <= :end
        GROUP BY hour
        ORDER BY hour
    """)
    fun getHourlyEventCounts(start: Long, end: Long): Flow<List<InterventionHourlyCount>>

    // ── Today counts (convenience) ───────────────────────────

    @Query("SELECT COUNT(*) FROM intervention_events WHERE timestamp >= :since")
    fun getTotalCountSince(since: Long): Flow<Int>

    @Query("""
        SELECT action, COUNT(*) as count
        FROM intervention_events
        WHERE timestamp >= :since AND action IN ('allowed', 'blocked')
        GROUP BY action
    """)
    fun getActionCountsSince(since: Long): Flow<List<ModuleActionCount>>

    // ── Intentional use ratio ──────────────────────────────────

    @Query("SELECT SUM(CASE WHEN action = 'blocked' THEN 1 ELSE 0 END) as blocked, SUM(CASE WHEN action = 'allowed' THEN 1 ELSE 0 END) as allowed FROM intervention_events WHERE timestamp >= :start AND timestamp <= :end")
    fun getIntentionalUseRatio(start: Long, end: Long): Flow<IntentionalUseResult?>

    @Query("SELECT (timestamp / 86400000) as dayEpoch, SUM(CASE WHEN action = 'blocked' THEN 1 ELSE 0 END) as blocked, SUM(CASE WHEN action = 'allowed' THEN 1 ELSE 0 END) as allowed FROM intervention_events WHERE timestamp >= :start AND timestamp <= :end GROUP BY dayEpoch ORDER BY dayEpoch")
    fun getDailyIntentionalTrend(start: Long, end: Long): Flow<List<DailyIntentionalRow>>

    // ── Cleanup ──────────────────────────────────────────────

    @Query("DELETE FROM intervention_events WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM intervention_events")
    suspend fun deleteAll()
}
