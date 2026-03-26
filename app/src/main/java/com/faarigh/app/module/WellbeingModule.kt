package com.faarigh.app.module

import kotlinx.coroutines.flow.Flow

/**
 * Base interface for all Mindful feature modules.
 * Each module represents a self-contained feature (app interception, content filtering, etc.)
 * that can be enabled/disabled independently.
 *
 * This interface is designed for plug-and-play: implement it, register with ModuleRegistry,
 * and the UI (ModuleDetailScreen) renders config/stats/education automatically.
 */
interface WellbeingModule {
    /** Unique identifier for this module */
    val id: String

    /** Human-readable name */
    val name: String

    /** Short description of what this module does */
    val description: String

    /** Drawable resource ID for the module icon (R.drawable.ic_module_*) */
    val iconRes: Int

    /** Accent color as a Long (e.g., 0xFFA8D5A2) for theming */
    val accentColor: Long

    /** Reactive enabled state */
    val isEnabled: Flow<Boolean>

    /** Enable or disable this module */
    suspend fun setEnabled(enabled: Boolean)

    /** Configuration items this module exposes — rendered dynamically in ModuleDetailScreen */
    val configItems: List<ModuleConfigItem>

    /** Live stats for this module — rendered dynamically in ModuleDetailScreen */
    val statsItems: Flow<List<ModuleStat>>

    /** Education content shown before enabling */
    val educationCards: List<OnboardingCard>

    /** Called when the module should start operating */
    fun onStart()

    /** Called when the module should stop operating */
    fun onStop()
}

/**
 * Sealed class representing the types of configuration controls a module can expose.
 * ModuleDetailScreen renders these generically — no per-module branching needed.
 */
sealed class ModuleConfigItem {
    abstract val key: String
    abstract val label: String

    data class Toggle(
        override val key: String,
        override val label: String,
        val description: String,
        val value: Flow<Boolean>,
        val onToggle: suspend (Boolean) -> Unit,
    ) : ModuleConfigItem()

    data class Slider(
        override val key: String,
        override val label: String,
        val range: ClosedFloatingPointRange<Float>,
        val steps: Int,
        val value: Flow<Float>,
        val valueLabel: (Float) -> String,
        val onValueChange: suspend (Float) -> Unit,
    ) : ModuleConfigItem()

    data class Choice(
        override val key: String,
        override val label: String,
        val options: List<Pair<String, String>>,  // value to display label
        val value: Flow<String>,
        val onSelect: suspend (String) -> Unit,
    ) : ModuleConfigItem()

    /**
     * A set of toggleable items (e.g., per-app toggles for Shorts Blocker).
     */
    data class ToggleGroup(
        override val key: String,
        override val label: String,
        val items: List<Pair<String, String>>,  // id to display label
        val selectedItems: Flow<Set<String>>,
        val onToggleItem: suspend (String, Boolean) -> Unit,
    ) : ModuleConfigItem()

    /**
     * Plain informational text card.
     */
    data class Info(
        override val key: String,
        override val label: String,
    ) : ModuleConfigItem()
}

/**
 * A single stat displayed in the module detail screen.
 */
data class ModuleStat(
    val label: String,
    val value: String,
    val trend: String? = null,    // e.g., "up 12%" or "down 5%"
    val accentColor: Long? = null,
)
