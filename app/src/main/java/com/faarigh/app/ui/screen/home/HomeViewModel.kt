package com.faarigh.app.ui.screen.home

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faarigh.app.data.db.dao.ActionCount
import com.faarigh.app.data.db.dao.HourlyCount
import com.faarigh.app.data.db.entity.InterceptedApp
import com.faarigh.app.data.preferences.ModulePreferences
import com.faarigh.app.data.repository.AppInterceptionRepository
import com.faarigh.app.data.repository.DashboardOverview
import com.faarigh.app.data.repository.DnsStatsRepository
import com.faarigh.app.data.repository.DomainFilterRepository
import com.faarigh.app.data.repository.StatsRepository
import com.faarigh.app.data.repository.UsageRepository
import com.faarigh.app.data.tracking.AppUsageStat
import com.faarigh.app.service.accessibility.FaarighAccessibilityService
import com.faarigh.app.service.vpn.FaarighVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.LocalDate
import java.util.Calendar
import javax.inject.Inject

data class AppIconData(val packageName: String, val label: String, val icon: ImageBitmap?)

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    usageRepository: UsageRepository,
    domainFilterRepository: DomainFilterRepository,
    appInterceptionRepository: AppInterceptionRepository,
    dnsStatsRepo: DnsStatsRepository,
    private val statsRepository: StatsRepository,
    modulePreferences: ModulePreferences,
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

    val todayActionCounts: StateFlow<List<ActionCount>> = usageRepository
        .getActionCounts(todayStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTotalCount: StateFlow<Int> = usageRepository
        .getTotalCount(todayStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val blockedDomainsCount: StateFlow<Int> = domainFilterRepository
        .getEnabledCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isAccessibilityEnabled: Boolean
        get() = FaarighAccessibilityService.instance != null

    val vpnRunning: StateFlow<Boolean> = FaarighVpnService.isRunningFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val dnsTotal: StateFlow<Int> = dnsStatsRepo
        .getTotalQueries(todayStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dnsBlocked: StateFlow<Int> = dnsStatsRepo
        .getBlockedQueries(todayStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Hourly DNS activity for the area chart
    val hourlyActivity: StateFlow<List<HourlyCount>> = dnsStatsRepo
        .getHourlyActivity(todayStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monitored apps with icons for the app row
    val monitoredApps: StateFlow<List<AppIconData>> = appInterceptionRepository
        .getEnabledApps()
        .map { apps ->
            apps.map { app ->
                AppIconData(
                    packageName = app.packageName,
                    label = app.appLabel,
                    icon = loadAppIcon(app.packageName),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── New stats from StatsRepository ───────────────────────

    val screenTimeToday: StateFlow<Duration> = statsRepository
        .getTotalScreenTime(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Duration.ZERO)

    val screenTimeYesterday: StateFlow<Duration> = statsRepository
        .getTotalScreenTime(LocalDate.now().minusDays(1))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Duration.ZERO)

    val topAppsToday: StateFlow<List<AppUsageStat>> = statsRepository
        .getTopApps(LocalDate.now(), 5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyScreenTime: StateFlow<List<Pair<java.time.LocalDate, Duration>>> = statsRepository
        .getWeeklyScreenTime()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryBreakdown: StateFlow<Map<String, Duration>> = statsRepository
        .getCategoryBreakdown(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val interventionCountToday: StateFlow<Int> = statsRepository
        .getTodayInterventionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val consciousChoicesToday: StateFlow<Int> = statsRepository
        .getConsciousChoicesToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val interventionSuccessRate: StateFlow<Float> = statsRepository
        .getInterventionSuccessRate(0)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val unlockCount: StateFlow<Int> = statsRepository
        .getUnlockCount(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val hasUsagePermission: Boolean
        get() = statsRepository.hasUsageStatsPermission()

    // ── Module enabled states ────────────────────────────────
    val appPauseEnabled: StateFlow<Boolean> = modulePreferences.appPauseEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val shortsBlockerEnabled: StateFlow<Boolean> = modulePreferences.shortsBlockerEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val nsfwEnabled: StateFlow<Boolean> = modulePreferences.nsfwEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val dnsFilterEnabled: StateFlow<Boolean> = modulePreferences.dnsFilterEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private fun loadAppIcon(packageName: String): ImageBitmap? {
        return try {
            val pm = getApplication<Application>().packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val bmp = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val w = drawable.intrinsicWidth.coerceAtLeast(1)
                val h = drawable.intrinsicHeight.coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, w, h)
                drawable.draw(canvas)
                bmp
            }
            bmp.asImageBitmap()
        } catch (_: Exception) { null }
    }
}
