package com.faarigh.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.faarigh.app.data.db.entity.BlockedDomain
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedDomainDao {

    @Query("SELECT * FROM blocked_domains ORDER BY category, domain")
    fun getAll(): Flow<List<BlockedDomain>>

    @Query("SELECT domain FROM blocked_domains WHERE isEnabled = 1")
    suspend fun getEnabledDomains(): List<String>

    @Query("SELECT * FROM blocked_domains WHERE isEnabled = 1")
    suspend fun getEnabledDomainsWithInfo(): List<BlockedDomain>

    @Query("SELECT domain FROM blocked_domains WHERE isEnabled = 1 AND category = :category")
    suspend fun getEnabledByCategory(category: String): List<String>

    @Query("SELECT * FROM blocked_domains WHERE category = :category")
    fun getByCategory(category: String): Flow<List<BlockedDomain>>

    @Query("SELECT COUNT(*) FROM blocked_domains WHERE isEnabled = 1")
    fun getEnabledCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(domain: BlockedDomain)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(domains: List<BlockedDomain>)

    @Delete
    suspend fun delete(domain: BlockedDomain)

    @Query("UPDATE blocked_domains SET isEnabled = :enabled WHERE category = :category")
    suspend fun setCategoryEnabled(category: String, enabled: Boolean)
}
