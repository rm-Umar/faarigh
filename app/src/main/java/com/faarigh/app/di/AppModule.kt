package com.faarigh.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.faarigh.app.data.db.FaarighDatabase
import com.faarigh.app.data.db.dao.AppScheduleDao
import com.faarigh.app.data.db.dao.BlockedDomainDao
import com.faarigh.app.data.db.dao.DnsQueryLogDao
import com.faarigh.app.data.db.dao.DnsStatsAggregateDao
import com.faarigh.app.data.db.dao.InterceptedAppDao
import com.faarigh.app.data.db.dao.InterventionEventDao
import com.faarigh.app.data.db.dao.UsageEventDao
import com.faarigh.app.data.repository.AppInterceptionRepository
import com.faarigh.app.data.repository.AppInterceptionRepositoryImpl
import com.faarigh.app.data.repository.AppScheduleRepository
import com.faarigh.app.data.repository.AppScheduleRepositoryImpl
import com.faarigh.app.data.repository.DnsStatsRepository
import com.faarigh.app.data.repository.DnsStatsRepositoryImpl
import com.faarigh.app.data.repository.DomainFilterRepository
import com.faarigh.app.data.repository.DomainFilterRepositoryImpl
import com.faarigh.app.data.repository.UsageRepository
import com.faarigh.app.data.repository.UsageRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FaarighDatabase =
        Room.databaseBuilder(context, FaarighDatabase::class.java, "faarigh.db")
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            // Don't wipe DB on schema changes — data should persist across updates
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()

    // Migration 4→5: no schema change, just preserves data
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No-op — cooldownSec default changed in Kotlin, not in SQL schema
        }
    }

    // Migration 5→6: add escalation tracking columns to intervention_events
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE intervention_events ADD COLUMN escalationLevel TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE intervention_events ADD COLUMN promptShown TEXT DEFAULT NULL")
        }
    }

    // Migration 6→7: add app_schedules table for quarantine system
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS app_schedules (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "packageName TEXT NOT NULL, " +
                    "appLabel TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "startHour INTEGER NOT NULL DEFAULT 0, " +
                    "startMin INTEGER NOT NULL DEFAULT 0, " +
                    "endHour INTEGER NOT NULL DEFAULT 7, " +
                    "endMin INTEGER NOT NULL DEFAULT 0, " +
                    "daysOfWeek TEXT NOT NULL DEFAULT '1,2,3,4,5,6,7', " +
                    "dailyLimitMin INTEGER NOT NULL DEFAULT 0, " +
                    "isEnabled INTEGER NOT NULL DEFAULT 1, " +
                    "createdAt INTEGER NOT NULL, " +
                    "expiresAt INTEGER)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_app_schedules_packageName ON app_schedules (packageName)")
        }
    }

    @Provides
    fun provideInterceptedAppDao(db: FaarighDatabase): InterceptedAppDao =
        db.interceptedAppDao()

    @Provides
    fun provideBlockedDomainDao(db: FaarighDatabase): BlockedDomainDao =
        db.blockedDomainDao()

    @Provides
    fun provideUsageEventDao(db: FaarighDatabase): UsageEventDao =
        db.usageEventDao()

    @Provides
    fun provideDnsQueryLogDao(db: FaarighDatabase): DnsQueryLogDao =
        db.dnsQueryLogDao()

    @Provides
    fun provideDnsStatsAggregateDao(db: FaarighDatabase): DnsStatsAggregateDao =
        db.dnsStatsAggregateDao()

    @Provides
    fun provideInterventionEventDao(db: FaarighDatabase): InterventionEventDao =
        db.interventionEventDao()

    @Provides
    fun provideAppScheduleDao(db: FaarighDatabase): AppScheduleDao =
        db.appScheduleDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppInterceptionRepository(
        impl: AppInterceptionRepositoryImpl,
    ): AppInterceptionRepository

    @Binds
    @Singleton
    abstract fun bindDomainFilterRepository(
        impl: DomainFilterRepositoryImpl,
    ): DomainFilterRepository

    @Binds
    @Singleton
    abstract fun bindUsageRepository(
        impl: UsageRepositoryImpl,
    ): UsageRepository

    @Binds
    @Singleton
    abstract fun bindDnsStatsRepository(
        impl: DnsStatsRepositoryImpl,
    ): DnsStatsRepository

    @Binds
    @Singleton
    abstract fun bindAppScheduleRepository(
        impl: AppScheduleRepositoryImpl,
    ): AppScheduleRepository
}
