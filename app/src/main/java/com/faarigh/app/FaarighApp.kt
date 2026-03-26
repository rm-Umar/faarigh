package com.faarigh.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.faarigh.app.data.repository.DomainFilterRepository
import com.faarigh.app.di.PopulatedModuleRegistry
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FaarighApp : Application() {

    companion object {
        const val CHANNEL_VPN = "faarigh_vpn"
        const val CHANNEL_ALERTS = "faarigh_alerts"
    }

    @Inject lateinit var domainFilterRepo: DomainFilterRepository

    // Eager injection triggers module registration in ModuleRegistry
    @Inject lateinit var populatedModuleRegistry: PopulatedModuleRegistry

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // Auto-load default blocklists if empty (handles fresh install + DB migration wipes)
        appScope.launch {
            val existing = domainFilterRepo.getEnabledDomains()
            if (existing.isEmpty()) {
                domainFilterRepo.loadDefaultBlocklists()
                android.util.Log.i("FaarighApp", "Auto-loaded default blocklists")
            }
        }
    }

    private fun createNotificationChannels() {
        val vpnChannel = NotificationChannel(
            CHANNEL_VPN,
            "DNS Filter Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when the local DNS filter is active"
            setShowBadge(false)
        }

        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Content Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when content is detected"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(vpnChannel)
        manager.createNotificationChannel(alertsChannel)
    }
}
