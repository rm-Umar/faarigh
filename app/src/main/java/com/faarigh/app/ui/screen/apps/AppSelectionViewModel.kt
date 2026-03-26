package com.faarigh.app.ui.screen.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faarigh.app.data.db.entity.AppSchedule
import com.faarigh.app.data.db.entity.InterceptedApp
import com.faarigh.app.data.repository.AppInterceptionRepository
import com.faarigh.app.data.repository.AppScheduleRepository
import com.faarigh.app.data.repository.StatsRepository
import com.faarigh.app.util.InstalledApp
import com.faarigh.app.util.PackageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class AppSelectionViewModel @Inject constructor(
    application: Application,
    private val repository: AppInterceptionRepository,
    private val scheduleRepository: AppScheduleRepository,
    private val statsRepository: StatsRepository,
) : AndroidViewModel(application) {

    val interceptedApps: StateFlow<List<InterceptedApp>> = repository
        .getAllApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Debounced search query (300ms delay)
    private val debouncedSearchQuery: StateFlow<String> = _searchQuery
        .debounce(300L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Filtered apps reactive to debounced search
    val filteredApps: StateFlow<List<InstalledApp>> = combine(
        _installedApps,
        debouncedSearchQuery,
    ) { apps, query ->
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // -- Per-app config sheet state --
    private val _configSheetApp = MutableStateFlow<ConfigSheetState?>(null)
    val configSheetApp: StateFlow<ConfigSheetState?> = _configSheetApp.asStateFlow()

    // -- Category limits --
    private val _categoryLimits = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categoryLimits: StateFlow<Map<String, Int>> = _categoryLimits.asStateFlow()

    // -- Category usage today --
    val categoryUsageToday: StateFlow<Map<String, Duration>> = statsRepository
        .getCategoryBreakdown(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        loadInstalledApps()
        loadCategoryLimits()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _installedApps.value = PackageUtils.getInstalledApps(getApplication())
            _isLoading.value = false
        }
    }

    private fun loadCategoryLimits() {
        viewModelScope.launch {
            val schedules = scheduleRepository.getAllSchedules().first()
            val categoryMap = mutableMapOf<String, Int>()
            schedules.filter { it.type == "daily_limit" && it.appLabel.startsWith("Category:") }
                .forEach { sched ->
                    val category = sched.appLabel.removePrefix("Category:")
                    categoryMap[category] = sched.dailyLimitMin
                }
            _categoryLimits.value = categoryMap
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleApp(packageName: String, label: String, selected: Boolean) {
        viewModelScope.launch {
            if (selected) {
                repository.addApp(packageName, label)
            } else {
                repository.removeApp(packageName)
            }
        }
    }

    // ── Per-App Config Sheet ──────────────────────────────

    fun openConfigSheet(packageName: String, appLabel: String) {
        viewModelScope.launch {
            val isMonitored = interceptedApps.value.any { it.packageName == packageName }
            val schedules = scheduleRepository.getSchedulesForApp(packageName).first()
            val dailyLimit = schedules.find { it.type == "daily_limit" }
            val schedule = schedules.find { it.type == "schedule" }

            _configSheetApp.value = ConfigSheetState(
                packageName = packageName,
                appLabel = appLabel,
                isMonitored = isMonitored,
                dailyLimitMin = dailyLimit?.dailyLimitMin ?: 0,
                dailyLimitId = dailyLimit?.id,
                scheduleEnabled = schedule?.isEnabled ?: false,
                scheduleId = schedule?.id,
                scheduleStartHour = schedule?.startHour ?: 22,
                scheduleStartMin = schedule?.startMin ?: 0,
                scheduleEndHour = schedule?.endHour ?: 7,
                scheduleEndMin = schedule?.endMin ?: 0,
            )
        }
    }

    fun closeConfigSheet() {
        _configSheetApp.value = null
    }

    fun updateMonitored(packageName: String, label: String, monitored: Boolean) {
        viewModelScope.launch {
            if (monitored) {
                repository.addApp(packageName, label)
            } else {
                repository.removeApp(packageName)
            }
            _configSheetApp.value = _configSheetApp.value?.copy(isMonitored = monitored)
        }
    }

    fun updateDailyLimit(packageName: String, label: String, limitMin: Int) {
        viewModelScope.launch {
            val current = _configSheetApp.value ?: return@launch
            if (limitMin == 0) {
                current.dailyLimitId?.let { scheduleRepository.deleteSchedule(it) }
                _configSheetApp.value = current.copy(dailyLimitMin = 0, dailyLimitId = null)
            } else {
                if (current.dailyLimitId != null) {
                    val existing = AppSchedule(
                        id = current.dailyLimitId,
                        packageName = packageName,
                        appLabel = label,
                        type = "daily_limit",
                        dailyLimitMin = limitMin,
                    )
                    scheduleRepository.updateSchedule(existing)
                    _configSheetApp.value = current.copy(dailyLimitMin = limitMin)
                } else {
                    val newSchedule = AppSchedule(
                        packageName = packageName,
                        appLabel = label,
                        type = "daily_limit",
                        dailyLimitMin = limitMin,
                    )
                    val id = scheduleRepository.addSchedule(newSchedule)
                    _configSheetApp.value = current.copy(dailyLimitMin = limitMin, dailyLimitId = id)
                }
            }
        }
    }

    fun updateScheduleBlock(
        packageName: String,
        label: String,
        enabled: Boolean,
        startHour: Int = 22,
        startMin: Int = 0,
        endHour: Int = 7,
        endMin: Int = 0,
    ) {
        viewModelScope.launch {
            val current = _configSheetApp.value ?: return@launch
            if (!enabled) {
                current.scheduleId?.let { scheduleRepository.deleteSchedule(it) }
                _configSheetApp.value = current.copy(scheduleEnabled = false, scheduleId = null)
            } else {
                if (current.scheduleId != null) {
                    val existing = AppSchedule(
                        id = current.scheduleId,
                        packageName = packageName,
                        appLabel = label,
                        type = "schedule",
                        startHour = startHour,
                        startMin = startMin,
                        endHour = endHour,
                        endMin = endMin,
                    )
                    scheduleRepository.updateSchedule(existing)
                    _configSheetApp.value = current.copy(
                        scheduleStartHour = startHour,
                        scheduleStartMin = startMin,
                        scheduleEndHour = endHour,
                        scheduleEndMin = endMin,
                    )
                } else {
                    val newSchedule = AppSchedule(
                        packageName = packageName,
                        appLabel = label,
                        type = "schedule",
                        startHour = startHour,
                        startMin = startMin,
                        endHour = endHour,
                        endMin = endMin,
                    )
                    val id = scheduleRepository.addSchedule(newSchedule)
                    _configSheetApp.value = current.copy(
                        scheduleEnabled = true,
                        scheduleId = id,
                        scheduleStartHour = startHour,
                        scheduleStartMin = startMin,
                        scheduleEndHour = endHour,
                        scheduleEndMin = endMin,
                    )
                }
            }
        }
    }

    // ── Category Limits ──────────────────────────────────

    fun updateCategoryLimit(category: String, limitMin: Int) {
        viewModelScope.launch {
            val appLabel = "Category:$category"
            val schedules = scheduleRepository.getAllSchedules().first()
            val existing = schedules.find {
                it.type == "daily_limit" && it.appLabel == appLabel
            }
            if (limitMin == 0) {
                existing?.id?.let { scheduleRepository.deleteSchedule(it) }
            } else {
                if (existing != null) {
                    scheduleRepository.updateSchedule(existing.copy(dailyLimitMin = limitMin))
                } else {
                    scheduleRepository.addSchedule(
                        AppSchedule(
                            packageName = "category_$category",
                            appLabel = appLabel,
                            type = "daily_limit",
                            dailyLimitMin = limitMin,
                        ),
                    )
                }
            }
            _categoryLimits.value = _categoryLimits.value.toMutableMap().apply {
                if (limitMin == 0) remove(category) else put(category, limitMin)
            }
        }
    }
}

data class ConfigSheetState(
    val packageName: String,
    val appLabel: String,
    val isMonitored: Boolean,
    val dailyLimitMin: Int = 0,
    val dailyLimitId: Long? = null,
    val scheduleEnabled: Boolean = false,
    val scheduleId: Long? = null,
    val scheduleStartHour: Int = 22,
    val scheduleStartMin: Int = 0,
    val scheduleEndHour: Int = 7,
    val scheduleEndMin: Int = 0,
)
