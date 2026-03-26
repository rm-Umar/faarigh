package com.faarigh.app.ui.screen.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faarigh.app.data.db.entity.AppSchedule
import com.faarigh.app.data.repository.AppScheduleRepository
import com.faarigh.app.data.repository.StatsRepository
import com.faarigh.app.util.InstalledApp
import com.faarigh.app.util.PackageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

data class BuiltInCategory(
    val name: String,
    val packages: Set<String>,
)

@HiltViewModel
class CategoryLimitsViewModel @Inject constructor(
    application: Application,
    private val scheduleRepository: AppScheduleRepository,
    private val statsRepository: StatsRepository,
) : AndroidViewModel(application) {

    companion object {
        val SOCIAL_MEDIA = BuiltInCategory(
            "Social Media",
            setOf(
                "com.instagram.android", "com.facebook.katana", "com.twitter.android",
                "com.x.android", "com.snapchat.android", "com.zhiliaoapp.musically",
                "com.reddit.frontpage", "com.linkedin.android", "com.pinterest",
            ),
        )
        val ENTERTAINMENT = BuiltInCategory(
            "Entertainment",
            setOf(
                "com.google.android.youtube", "com.netflix.mediaclient",
                "com.disney.disneyplus", "com.spotify.music", "tv.twitch.android.app",
            ),
        )
        val COMMUNICATION = BuiltInCategory(
            "Communication",
            setOf(
                "com.whatsapp", "org.telegram.messenger", "com.facebook.orca",
                "com.discord",
            ),
        )
        val GAMING = BuiltInCategory("Games", emptySet()) // filled from system category

        val BUILT_IN = listOf(SOCIAL_MEDIA, ENTERTAINMENT, COMMUNICATION, GAMING)
    }

    // -- Category limits (minutes) --
    private val _categoryLimits = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categoryLimits: StateFlow<Map<String, Int>> = _categoryLimits.asStateFlow()

    // -- Category usage today --
    val categoryUsageToday: StateFlow<Map<String, Duration>> = statsRepository
        .getCategoryBreakdown(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // -- Custom categories: Map<categoryName, Set<packageNames>> --
    private val _customCategories = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val customCategories: StateFlow<Map<String, Set<String>>> = _customCategories.asStateFlow()

    // -- All installed apps (for custom category picker) --
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    init {
        loadCategoryLimits()
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = PackageUtils.getInstalledApps(getApplication())
        }
    }

    private fun loadCategoryLimits() {
        viewModelScope.launch {
            val schedules = scheduleRepository.getAllSchedules().first()
            val categoryMap = mutableMapOf<String, Int>()
            val customCats = mutableMapOf<String, Set<String>>()
            schedules.filter { it.type == "daily_limit" && it.appLabel.startsWith("Category:") }
                .forEach { sched ->
                    val category = sched.appLabel.removePrefix("Category:")
                    categoryMap[category] = sched.dailyLimitMin
                    // Custom categories store package names in daysOfWeek field
                    if (category !in BUILT_IN.map { it.name } && sched.daysOfWeek.isNotBlank()) {
                        customCats[category] = sched.daysOfWeek.split(",").toSet()
                    }
                }
            _categoryLimits.value = categoryMap
            _customCategories.value = customCats
        }
    }

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

    fun addCustomCategory(name: String, packages: Set<String>, limitMin: Int) {
        viewModelScope.launch {
            val appLabel = "Category:$name"
            val schedule = AppSchedule(
                packageName = "category_$name",
                appLabel = appLabel,
                type = "daily_limit",
                dailyLimitMin = limitMin,
                daysOfWeek = packages.joinToString(","),
            )
            scheduleRepository.addSchedule(schedule)
            _categoryLimits.value = _categoryLimits.value.toMutableMap().apply {
                put(name, limitMin)
            }
            _customCategories.value = _customCategories.value.toMutableMap().apply {
                put(name, packages)
            }
        }
    }
}
