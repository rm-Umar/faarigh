package com.faarigh.app.ui.screen.modules

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faarigh.app.di.PopulatedModuleRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI model for the modules list screen. Derived from WellbeingModule.
 */
data class ModuleInfo(
    val id: String,
    val name: String,
    val description: String,
    val iconRes: Int,
    val accentColor: Color,
    val isEnabled: Boolean = false,
)

@HiltViewModel
class ModulesViewModel @Inject constructor(
    private val populatedRegistry: PopulatedModuleRegistry,
    @ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    private val registry get() = populatedRegistry.registry

    /** All registered modules, with live enabled state */
    val modules: StateFlow<List<ModuleInfo>> = run {
        val allModules = registry.getAllModules()
        if (allModules.isEmpty()) {
            MutableStateFlow(emptyList<ModuleInfo>()).asStateFlow()
        } else {
            // Combine all module isEnabled flows into a single list
            combine(allModules.map { it.isEnabled }) { enabledArray ->
                allModules.mapIndexed { index, module ->
                    ModuleInfo(
                        id = module.id,
                        name = module.name,
                        description = module.description,
                        iconRes = module.iconRes,
                        accentColor = Color(module.accentColor.toInt()),
                        isEnabled = enabledArray[index],
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), allModules.map { module ->
                ModuleInfo(
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

    // Track which module wants to show education before enabling
    private val _pendingEducation = MutableStateFlow<String?>(null)
    val pendingEducation: StateFlow<String?> = _pendingEducation.asStateFlow()

    /**
     * Called when user taps the toggle. If enabling for the first time,
     * shows education cards instead of enabling immediately.
     */
    fun toggleModule(moduleId: String) {
        val current = modules.value.find { it.id == moduleId } ?: return
        val newEnabled = !current.isEnabled

        if (newEnabled && !hasSeenEducation(moduleId)) {
            _pendingEducation.value = moduleId
            return
        }

        applyToggle(moduleId, newEnabled)
    }

    /** Called after user finishes education cards and taps "Enable" */
    fun confirmEnableAfterEducation(moduleId: String) {
        markEducationSeen(moduleId)
        _pendingEducation.value = null
        applyToggle(moduleId, true)
    }

    /** Called if user dismisses education without enabling */
    fun dismissEducation() {
        _pendingEducation.value = null
    }

    private fun applyToggle(moduleId: String, enabled: Boolean) {
        val module = registry.getModule(moduleId) ?: return
        viewModelScope.launch {
            module.setEnabled(enabled)
        }
    }

    private fun hasSeenEducation(moduleId: String): Boolean {
        return appContext.getSharedPreferences("faarigh_education", android.content.Context.MODE_PRIVATE)
            .getBoolean("seen_$moduleId", false)
    }

    private fun markEducationSeen(moduleId: String) {
        appContext.getSharedPreferences("faarigh_education", android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("seen_$moduleId", true).apply()
    }
}
