package com.faarigh.app.data.repository

import com.faarigh.app.data.db.dao.ActionCount
import com.faarigh.app.data.db.entity.UsageEvent
import kotlinx.coroutines.flow.Flow

interface UsageRepository {
    fun getEventsSince(since: Long): Flow<List<UsageEvent>>
    fun getActionCounts(since: Long): Flow<List<ActionCount>>
    fun getEventsForApp(packageName: String, since: Long): Flow<List<UsageEvent>>
    fun getTotalCount(since: Long): Flow<Int>
    suspend fun logEvent(packageName: String, action: String, interventionType: String = "pause")
    suspend fun clearAll()
}
