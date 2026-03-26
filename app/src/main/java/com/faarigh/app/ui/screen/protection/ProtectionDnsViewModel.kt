package com.faarigh.app.ui.screen.protection

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faarigh.app.data.repository.DnsStatsRepository
import com.faarigh.app.service.vpn.FaarighVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ProtectionDnsViewModel @Inject constructor(
    private val application: Application,
    dnsStatsRepo: DnsStatsRepository,
) : AndroidViewModel(application) {

    private val todayStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    val vpnRunning: StateFlow<Boolean> = FaarighVpnService.isRunningFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FaarighVpnService.isRunning)

    val dnsTotal: StateFlow<Int> = dnsStatsRepo
        .getTotalQueries(todayStart)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dnsBlocked: StateFlow<Int> = dnsStatsRepo
        .getBlockedQueries(todayStart)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun stopVpn() {
        val intent = Intent(application, FaarighVpnService::class.java).apply {
            action = FaarighVpnService.ACTION_STOP
        }
        application.startService(intent)
    }
}
