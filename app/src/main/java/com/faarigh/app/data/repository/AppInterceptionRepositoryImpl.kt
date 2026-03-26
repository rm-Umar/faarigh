package com.faarigh.app.data.repository

import com.faarigh.app.data.db.dao.InterceptedAppDao
import com.faarigh.app.data.db.entity.InterceptedApp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInterceptionRepositoryImpl @Inject constructor(
    private val dao: InterceptedAppDao,
) : AppInterceptionRepository {

    override fun getAllApps(): Flow<List<InterceptedApp>> = dao.getAll()

    override fun getEnabledApps(): Flow<List<InterceptedApp>> = dao.getEnabled()

    override suspend fun getEnabledPackageNames(): Set<String> =
        dao.getEnabledPackageNames().toSet()

    override suspend fun getByPackage(packageName: String): InterceptedApp? =
        dao.getByPackage(packageName)

    override suspend fun addApp(packageName: String, label: String) {
        dao.upsert(InterceptedApp(packageName = packageName, appLabel = label))
    }

    override suspend fun removeApp(packageName: String) {
        val app = dao.getByPackage(packageName) ?: return
        dao.delete(app)
    }

    override suspend fun toggleApp(packageName: String, enabled: Boolean) {
        dao.setEnabled(packageName, enabled)
    }

    override suspend fun updateConfig(app: InterceptedApp) {
        dao.upsert(app)
    }
}
