package com.faarigh.app.data.repository

import com.faarigh.app.data.db.entity.InterceptedApp
import kotlinx.coroutines.flow.Flow

interface AppInterceptionRepository {
    fun getAllApps(): Flow<List<InterceptedApp>>
    fun getEnabledApps(): Flow<List<InterceptedApp>>
    suspend fun getEnabledPackageNames(): Set<String>
    suspend fun getByPackage(packageName: String): InterceptedApp?
    suspend fun addApp(packageName: String, label: String)
    suspend fun removeApp(packageName: String)
    suspend fun toggleApp(packageName: String, enabled: Boolean)
    suspend fun updateConfig(app: InterceptedApp)
}
