package com.faarigh.app.module

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central registry for all wellbeing modules.
 * Modules register here and can be looked up by ID.
 */
@Singleton
class ModuleRegistry @Inject constructor() {

    companion object {
        private const val TAG = "ModuleRegistry"
    }

    private val modules = mutableMapOf<String, WellbeingModule>()

    fun register(module: WellbeingModule) {
        modules[module.id] = module
        Log.d(TAG, "Registered module: ${module.id} (${module.name})")
    }

    fun unregister(moduleId: String) {
        modules.remove(moduleId)
    }

    fun getModule(id: String): WellbeingModule? = modules[id]

    fun getAllModules(): List<WellbeingModule> = modules.values.toList()
}
