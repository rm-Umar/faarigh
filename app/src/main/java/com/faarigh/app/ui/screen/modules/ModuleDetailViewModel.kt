package com.faarigh.app.ui.screen.modules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.faarigh.app.di.PopulatedModuleRegistry
import com.faarigh.app.module.WellbeingModule
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Generic ViewModel for the module detail screen.
 * Simply looks up the module from the registry -- all config/state
 * is exposed through the WellbeingModule interface itself.
 *
 * Injects PopulatedModuleRegistry (not raw ModuleRegistry) to guarantee
 * all modules have been registered before lookup.
 */
@HiltViewModel
class ModuleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    populatedRegistry: PopulatedModuleRegistry,
) : ViewModel() {

    private val moduleId: String = savedStateHandle.get<String>("moduleId") ?: ""

    /** The module instance, or null if not found in the registry */
    val module: WellbeingModule? = populatedRegistry.registry.getModule(moduleId)
}
