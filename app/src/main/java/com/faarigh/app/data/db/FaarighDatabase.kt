package com.faarigh.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.faarigh.app.data.db.dao.AppScheduleDao
import com.faarigh.app.data.db.dao.BlockedDomainDao
import com.faarigh.app.data.db.dao.DnsQueryLogDao
import com.faarigh.app.data.db.dao.DnsStatsAggregateDao
import com.faarigh.app.data.db.dao.InterceptedAppDao
import com.faarigh.app.data.db.dao.InterventionEventDao
import com.faarigh.app.data.db.dao.UsageEventDao
import com.faarigh.app.data.db.entity.AppSchedule
import com.faarigh.app.data.db.entity.BlockedDomain
import com.faarigh.app.data.db.entity.DnsQueryLog
import com.faarigh.app.data.db.entity.DnsStatsAggregate
import com.faarigh.app.data.db.entity.InterceptedApp
import com.faarigh.app.data.db.entity.InterventionEvent
import com.faarigh.app.data.db.entity.UsageEvent

@Database(
    entities = [
        InterceptedApp::class,
        BlockedDomain::class,
        UsageEvent::class,
        DnsQueryLog::class,
        DnsStatsAggregate::class,
        InterventionEvent::class,
        AppSchedule::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class FaarighDatabase : RoomDatabase() {
    abstract fun interceptedAppDao(): InterceptedAppDao
    abstract fun blockedDomainDao(): BlockedDomainDao
    abstract fun usageEventDao(): UsageEventDao
    abstract fun dnsQueryLogDao(): DnsQueryLogDao
    abstract fun dnsStatsAggregateDao(): DnsStatsAggregateDao
    abstract fun interventionEventDao(): InterventionEventDao
    abstract fun appScheduleDao(): AppScheduleDao
}
