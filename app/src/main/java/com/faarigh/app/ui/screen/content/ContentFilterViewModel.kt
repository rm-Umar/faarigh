package com.faarigh.app.ui.screen.content

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faarigh.app.data.db.dao.CategoryCount
import com.faarigh.app.data.db.dao.DomainCount
import com.faarigh.app.data.db.entity.BlockedDomain
import com.faarigh.app.data.repository.DnsStatsRepository
import com.faarigh.app.data.repository.DomainFilterRepository
import com.faarigh.app.service.vpn.FaarighVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ContentFilterViewModel @Inject constructor(
    private val application: Application,
    private val domainFilterRepository: DomainFilterRepository,
    private val dnsStatsRepo: DnsStatsRepository,
) : AndroidViewModel(application) {

    companion object {
        private const val DEBOUNCE_MS = 2000L
    }

    val allDomains: StateFlow<List<BlockedDomain>> = domainFilterRepository
        .getAllDomains()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledCount: StateFlow<Int> = domainFilterRepository
        .getEnabledCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val vpnRunning: StateFlow<Boolean> = FaarighVpnService.isRunningFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FaarighVpnService.isRunning)

    // ── DNS Stats ─────────────────────────────────────────────
    private val todayStart: Long get() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    val dnsTotal: StateFlow<Int> = dnsStatsRepo
        .getTotalQueries(todayStart)
        .distinctUntilChanged()
        .debounce(DEBOUNCE_MS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dnsBlocked: StateFlow<Int> = dnsStatsRepo
        .getBlockedQueries(todayStart)
        .distinctUntilChanged()
        .debounce(DEBOUNCE_MS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val topBlockedDomains: StateFlow<List<DomainCount>> = dnsStatsRepo
        .getTopBlockedDomains(todayStart, 8)
        .debounce(DEBOUNCE_MS * 2)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dnsCategoryBreakdown: StateFlow<List<CategoryCount>> = dnsStatsRepo
        .getBlockedByCategory(todayStart)
        .debounce(DEBOUNCE_MS * 2)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniqueDomains: StateFlow<Int> = dnsStatsRepo
        .getUniqueDomains(todayStart)
        .distinctUntilChanged()
        .debounce(DEBOUNCE_MS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun stopVpn() {
        val intent = Intent(application, FaarighVpnService::class.java).apply {
            action = FaarighVpnService.ACTION_STOP
        }
        application.startService(intent)
    }

    fun setCategoryEnabled(category: String, enabled: Boolean) {
        viewModelScope.launch {
            domainFilterRepository.setCategoryEnabled(category, enabled)
        }
    }

    fun addCustomDomain(domain: String) {
        if (domain.isBlank()) return
        viewModelScope.launch {
            domainFilterRepository.addDomain(domain, "custom")
        }
    }

    fun removeDomain(domain: String) {
        viewModelScope.launch {
            domainFilterRepository.removeDomain(domain)
        }
    }

    fun loadDefaults() {
        viewModelScope.launch {
            domainFilterRepository.loadDefaultBlocklists()
        }
    }
}
