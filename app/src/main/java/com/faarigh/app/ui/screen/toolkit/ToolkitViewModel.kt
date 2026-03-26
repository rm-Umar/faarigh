package com.faarigh.app.ui.screen.toolkit

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faarigh.app.data.repository.AppInterceptionRepository
import com.faarigh.app.data.repository.DnsStatsRepository
import com.faarigh.app.di.PopulatedModuleRegistry
import com.faarigh.app.service.vpn.FaarighVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ToolkitModuleInfo(
    val id: String,
    val name: String,
    val description: String,
    val iconRes: Int,
    val accentColor: Color,
    val isEnabled: Boolean = false,
)

data class ToolkitAppIcon(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)

@HiltViewModel
class ToolkitViewModel @Inject constructor(
    private val application: Application,
    private val populatedRegistry: PopulatedModuleRegistry,
    appInterceptionRepository: AppInterceptionRepository,
    dnsStatsRepo: DnsStatsRepository,
) : AndroidViewModel(application) {

    private val registry get() = populatedRegistry.registry

    private val todayStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    // ── Modules ──────────────────────────────────────────────

    val modules: StateFlow<List<ToolkitModuleInfo>> = run {
        val allModules = registry.getAllModules()
        if (allModules.isEmpty()) {
            MutableStateFlow(emptyList<ToolkitModuleInfo>()).asStateFlow()
        } else {
            combine(allModules.map { it.isEnabled }) { enabledArray ->
                allModules.mapIndexed { index, module ->
                    ToolkitModuleInfo(
                        id = module.id,
                        name = module.name,
                        description = module.description,
                        iconRes = module.iconRes,
                        accentColor = Color(module.accentColor.toInt()),
                        isEnabled = enabledArray[index],
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), allModules.map { module ->
                ToolkitModuleInfo(
                    id = module.id,
                    name = module.name,
                    description = module.description,
                    iconRes = module.iconRes,
                    accentColor = Color(module.accentColor.toInt()),
                    isEnabled = false,
                )
            })
        }
    }

    // Education flow
    private val _pendingEducation = MutableStateFlow<String?>(null)
    val pendingEducation: StateFlow<String?> = _pendingEducation.asStateFlow()

    fun toggleModule(moduleId: String) {
        val current = modules.value.find { it.id == moduleId } ?: return
        val newEnabled = !current.isEnabled
        if (newEnabled && !hasSeenEducation(moduleId)) {
            _pendingEducation.value = moduleId
            return
        }
        applyToggle(moduleId, newEnabled)
    }

    fun confirmEnableAfterEducation(moduleId: String) {
        markEducationSeen(moduleId)
        _pendingEducation.value = null
        applyToggle(moduleId, true)
    }

    fun dismissEducation() {
        val moduleId = _pendingEducation.value ?: return
        markEducationSeen(moduleId)
        _pendingEducation.value = null
        applyToggle(moduleId, true)
    }

    fun getModuleCards(moduleId: String): List<com.faarigh.app.module.OnboardingCard> =
        registry.getModule(moduleId)?.educationCards ?: emptyList()

    fun getModuleName(moduleId: String): String =
        registry.getModule(moduleId)?.name ?: moduleId

    private fun applyToggle(moduleId: String, enabled: Boolean) {
        val module = registry.getModule(moduleId) ?: return
        viewModelScope.launch { module.setEnabled(enabled) }
    }

    private fun hasSeenEducation(moduleId: String): Boolean {
        return application.getSharedPreferences("faarigh_education", android.content.Context.MODE_PRIVATE)
            .getBoolean("seen_$moduleId", false)
    }

    private fun markEducationSeen(moduleId: String) {
        application.getSharedPreferences("faarigh_education", android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("seen_$moduleId", true).apply()
    }

    // ── DNS Stats ────────────────────────────────────────────

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

    // ── Monitored Apps ───────────────────────────────────────

    val monitoredApps: StateFlow<List<ToolkitAppIcon>> = appInterceptionRepository
        .getEnabledApps()
        .map { apps ->
            apps.map { app ->
                ToolkitAppIcon(
                    packageName = app.packageName,
                    label = app.appLabel,
                    icon = loadAppIcon(app.packageName),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Tooltip expansion state ──────────────────────────────

    private val _expandedTooltips = MutableStateFlow<Set<String>>(emptySet())
    val expandedTooltips: StateFlow<Set<String>> = _expandedTooltips.asStateFlow()

    fun toggleTooltip(moduleId: String) {
        _expandedTooltips.value = _expandedTooltips.value.let { set ->
            if (moduleId in set) set - moduleId else set + moduleId
        }
    }

    private fun loadAppIcon(packageName: String): ImageBitmap? {
        return try {
            val pm = application.packageManager
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
