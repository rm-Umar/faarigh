package com.faarigh.app.data.repository

import com.faarigh.app.data.db.dao.ActionCount
import com.faarigh.app.data.db.dao.UsageEventDao
import com.faarigh.app.data.db.entity.UsageEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepositoryImpl @Inject constructor(
    private val dao: UsageEventDao,
) : UsageRepository {

    override fun getEventsSince(since: Long): Flow<List<UsageEvent>> =
        dao.getEventsSince(since)

    override fun getActionCounts(since: Long): Flow<List<ActionCount>> =
        dao.getActionCounts(since)

    override fun getEventsForApp(packageName: String, since: Long): Flow<List<UsageEvent>> =
        dao.getEventsForApp(packageName, since)

    override fun getTotalCount(since: Long): Flow<Int> =
        dao.getTotalCount(since)

    override suspend fun logEvent(
        packageName: String,
        action: String,
        interventionType: String,
    ) {
        dao.insert(
            UsageEvent(
                packageName = packageName,
                action = action,
                interventionType = interventionType,
            )
        )
    }

    override suspend fun clearAll() {
        dao.deleteAll()
    }
}
