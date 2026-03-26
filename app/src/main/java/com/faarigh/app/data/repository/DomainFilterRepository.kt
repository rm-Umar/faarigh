package com.faarigh.app.data.repository

import com.faarigh.app.data.db.entity.BlockedDomain
import kotlinx.coroutines.flow.Flow

interface DomainFilterRepository {
    fun getAllDomains(): Flow<List<BlockedDomain>>
    fun getDomainsByCategory(category: String): Flow<List<BlockedDomain>>
    fun getEnabledCount(): Flow<Int>
    suspend fun getEnabledDomains(): Set<String>
    suspend fun getEnabledDomainsWithCategories(): Map<String, String>
    suspend fun getEnabledByCategory(category: String): Set<String>
    suspend fun addDomain(domain: String, category: String)
    suspend fun removeDomain(domain: String)
    suspend fun setCategoryEnabled(category: String, enabled: Boolean)
    suspend fun loadDefaultBlocklists()
}
