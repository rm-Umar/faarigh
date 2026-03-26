package com.faarigh.app.data.repository

import com.faarigh.app.data.blocklist.DefaultBlocklists
import com.faarigh.app.data.db.dao.BlockedDomainDao
import com.faarigh.app.data.db.entity.BlockedDomain
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainFilterRepositoryImpl @Inject constructor(
    private val dao: BlockedDomainDao,
) : DomainFilterRepository {

    override fun getAllDomains(): Flow<List<BlockedDomain>> = dao.getAll()

    override fun getDomainsByCategory(category: String): Flow<List<BlockedDomain>> =
        dao.getByCategory(category)

    override fun getEnabledCount(): Flow<Int> = dao.getEnabledCount()

    override suspend fun getEnabledDomains(): Set<String> =
        dao.getEnabledDomains().toSet()

    override suspend fun getEnabledDomainsWithCategories(): Map<String, String> =
        dao.getEnabledDomainsWithInfo().associate { it.domain to it.category }

    override suspend fun getEnabledByCategory(category: String): Set<String> =
        dao.getEnabledByCategory(category).toSet()

    override suspend fun addDomain(domain: String, category: String) {
        dao.upsert(
            BlockedDomain(
                domain = domain.lowercase().trim(),
                category = category,
                isBuiltIn = false,
            )
        )
    }

    override suspend fun removeDomain(domain: String) {
        dao.delete(BlockedDomain(domain = domain, category = ""))
    }

    override suspend fun setCategoryEnabled(category: String, enabled: Boolean) {
        dao.setCategoryEnabled(category, enabled)
    }

    override suspend fun loadDefaultBlocklists() {
        val allDefaults = DefaultBlocklists.EXPLICIT.map {
            BlockedDomain(domain = it, category = "explicit", isBuiltIn = true)
        } + DefaultBlocklists.ADS.map {
            BlockedDomain(domain = it, category = "ads", isBuiltIn = true)
        } + DefaultBlocklists.TELEMETRY.map {
            BlockedDomain(domain = it, category = "telemetry", isBuiltIn = true)
        }
        dao.upsertAll(allDefaults)
    }
}
