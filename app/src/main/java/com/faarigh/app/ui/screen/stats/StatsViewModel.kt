package com.faarigh.app.ui.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faarigh.app.data.db.dao.ActionCount
import com.faarigh.app.data.db.dao.DailyIntentionalRow
import com.faarigh.app.data.db.dao.IntentionalUseResult
import com.faarigh.app.data.db.entity.UsageEvent
import com.faarigh.app.data.repository.AppInterventionStat
import com.faarigh.app.data.repository.DashboardOverview
import com.faarigh.app.data.repository.DnsStatsRepository
import com.faarigh.app.data.repository.ModuleStats
import com.faarigh.app.data.repository.StatsRepository
import com.faarigh.app.data.repository.UsageRepository
import com.faarigh.app.data.tracking.AppUsageStat
import com.faarigh.app.service.vpn.FaarighVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.LocalDate
import java.util.Calendar
import javax.inject.Inject

enum class TimeRange(val label: String, val daysBack: Int) {
    TODAY("Today", 0),
    WEEK("This Week", 7),
    MONTH("This Month", 30),
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val dnsStatsRepo: DnsStatsRepository,
    private val statsRepository: StatsRepository,
) : ViewModel() {

    companion object {
        private const val DEBOUNCE_MS = 2000L
    }

    private val _selectedRange = MutableStateFlow(TimeRange.TODAY)
    val selectedRange: StateFlow<TimeRange> = _selectedRange.asStateFlow()

    // Selected date for weekly chart drill-down (null = today)
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val effectiveDate: kotlinx.coroutines.flow.Flow<LocalDate> =
        _selectedDate.map { it ?: LocalDate.now() }

    // ── Screen time stats (reactive to selectedDate AND selectedRange) ─────────

    /**
     * When a specific date is selected (chart drill-down), show that single day.
     * When no specific date is selected:
     *   - TODAY  -> show today
     *   - WEEK   -> sum last 7 days
     *   - MONTH  -> sum last 30 days
     */
    val screenTimeForDate: StateFlow<Duration> = combine(_selectedDate, _selectedRange) { date, range ->
        date to range
    }.flatMapLatest { (date, range) ->
        if (date != null) {
            // Specific date selected from chart
            statsRepository.getTotalScreenTime(date)
        } else {
            when (range) {
                TimeRange.TODAY -> statsRepository.getTotalScreenTime(LocalDate.now())
                TimeRange.WEEK -> statsRepository.getTotalScreenTimeForRange(
                    LocalDate.now().minusDays(6), LocalDate.now().plusDays(1)
                )
                TimeRange.MONTH -> statsRepository.getTotalScreenTimeForRange(
                    LocalDate.now().minusDays(29), LocalDate.now().plusDays(1)
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Duration.ZERO)

    val weeklyScreenTime: StateFlow<List<Pair<LocalDate, Duration>>> = statsRepository
        .getWeeklyScreenTime()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Top apps: when a specific date is selected, show that date.
     * Otherwise respect the range.
     */
    val topApps: StateFlow<List<AppUsageStat>> = combine(_selectedDate, _selectedRange) { date, range ->
        date to range
    }.flatMapLatest { (date, range) ->
        if (date != null) {
            statsRepository.getTopApps(date, 10)
        } else {
            when (range) {
                TimeRange.TODAY -> statsRepository.getTopApps(LocalDate.now(), 10)
                TimeRange.WEEK -> statsRepository.getTopAppsForRange(
                    LocalDate.now().minusDays(6), LocalDate.now().plusDays(1), 10
                )
                TimeRange.MONTH -> statsRepository.getTopAppsForRange(
                    LocalDate.now().minusDays(29), LocalDate.now().plusDays(1), 10
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryBreakdownUsage: StateFlow<Map<String, Duration>> = effectiveDate
        .flatMapLatest { statsRepository.getCategoryBreakdown(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * Unlock count: single date when selected, otherwise range-aware.
     */
    val unlockCount: StateFlow<Int> = combine(_selectedDate, _selectedRange) { date, range ->
        date to range
    }.flatMapLatest { (date, range) ->
        if (date != null) {
            statsRepository.getUnlockCount(date)
        } else {
            when (range) {
                TimeRange.TODAY -> statsRepository.getUnlockCount(LocalDate.now())
                TimeRange.WEEK -> statsRepository.getUnlockCountForRange(
                    LocalDate.now().minusDays(6), LocalDate.now().plusDays(1)
                )
                TimeRange.MONTH -> statsRepository.getUnlockCountForRange(
                    LocalDate.now().minusDays(29), LocalDate.now().plusDays(1)
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Intervention stats (reactive to _selectedRange) ───────────────────────
    val events: StateFlow<List<UsageEvent>> = _selectedRange
        .flatMapLatest { usageRepository.getEventsSince(getStartTime(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actionCounts: StateFlow<List<ActionCount>> = _selectedRange
        .flatMapLatest { usageRepository.getActionCounts(getStartTime(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val interventionCount: StateFlow<Int> = _selectedRange
        .flatMapLatest { statsRepository.getInterventionCountSince(getStartTime(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val interventionSuccessRate: StateFlow<Float> = _selectedRange
        .flatMapLatest { statsRepository.getInterventionSuccessRateSince(getStartTime(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val appPauseStats: StateFlow<ModuleStats> = _selectedRange
        .flatMapLatest { statsRepository.getModuleStats("app_pause", it.daysBack) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModuleStats(0, 0, 0, 0f))

    val nsfwStats: StateFlow<ModuleStats> = _selectedRange
        .flatMapLatest { statsRepository.getModuleStats("nsfw_detection", it.daysBack) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModuleStats(0, 0, 0, 0f))

    val shortsStats: StateFlow<ModuleStats> = _selectedRange
        .flatMapLatest { statsRepository.getModuleStats("shorts_blocker", it.daysBack) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModuleStats(0, 0, 0, 0f))

    val mostPausedApps: StateFlow<List<AppInterventionStat>> = _selectedRange
        .flatMapLatest { statsRepository.getMostPausedApps(it.daysBack.coerceAtLeast(1), 5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Intentional use stats (reactive to _selectedRange) ─────────────────
    val intentionalUseRatio: StateFlow<IntentionalUseResult?> = _selectedRange
        .flatMapLatest { statsRepository.getIntentionalUseRatioSince(getStartTime(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyIntentionalTrend: StateFlow<List<DailyIntentionalRow>> = _selectedRange
        .flatMapLatest { statsRepository.getDailyIntentionalTrend(it.daysBack.coerceAtLeast(7)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val consciousChoicesToday: StateFlow<Int> = _selectedRange
        .flatMapLatest { statsRepository.getConsciousChoicesSince(getStartTime(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Dashboard overview ───────────────────────────────────
    val dashboardOverview: StateFlow<DashboardOverview?> = statsRepository
        .getDashboardOverview()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── DNS stats (kept minimal for quick stat card) ─────────
    val vpnRunning = FaarighVpnService.isRunningFlow

    val dnsBlocked: StateFlow<Int> = _selectedRange
        .flatMapLatest { dnsStatsRepo.getBlockedQueries(getStartTime(it)).distinctUntilChanged().debounce(DEBOUNCE_MS) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dnsTotal: StateFlow<Int> = _selectedRange
        .flatMapLatest { dnsStatsRepo.getTotalQueries(getStartTime(it)).distinctUntilChanged().debounce(DEBOUNCE_MS) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Usage stats permission ───────────────────────────────
    val hasUsagePermission: Boolean
        get() = statsRepository.hasUsageStatsPermission()

    // ── Collapsible sections state ──────────────────────────
    private val _expandedSections = MutableStateFlow(setOf("usage")) // "usage" expanded by default
    val expandedSections: StateFlow<Set<String>> = _expandedSections.asStateFlow()

    fun toggleSection(section: String) {
        _expandedSections.value = _expandedSections.value.let { set ->
            if (section in set) set - section else set + section
        }
    }

    fun setTimeRange(range: TimeRange) {
        _selectedRange.value = range
        // Clear date selection when switching range so range-based totals show
        _selectedDate.value = null
    }

    fun setSelectedDate(date: LocalDate?) {
        _selectedDate.value = date
    }

    private fun getStartTime(range: TimeRange): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (range.daysBack > 0) cal.add(Calendar.DAY_OF_YEAR, -range.daysBack)
        return cal.timeInMillis
    }
}
