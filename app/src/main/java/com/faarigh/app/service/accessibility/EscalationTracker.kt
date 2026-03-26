package com.faarigh.app.service.accessibility

import android.util.Log
import java.util.Calendar

/**
 * Tracks daily cumulative app usage and determines the appropriate
 * escalation level for interventions.
 *
 * Based on One Sec PNAS study + Stanford physiological sigh research:
 * - LIGHT: First open — minimal friction, just a pause
 * - MEDIUM: After 20min cumulative — physiological sigh + reflective prompt
 * - DEEP: After user-set limit — full intervention with alternatives
 * - WIND_DOWN: Late night — auto-escalate for sleep protection
 *
 * All state is in-memory and resets daily. No persistence needed —
 * each day is a fresh start (research-backed: avoids shame accumulation).
 */
class EscalationTracker {

    companion object {
        private const val TAG = "EscalationTracker"
    }

    enum class Level { LIGHT, MEDIUM, DEEP, WIND_DOWN }

    data class Context(
        val level: Level,
        val openCountToday: Int,
        val cumulativeMinutesToday: Long,
        val promptIndex: Int,
    )

    // packageName -> cumulative usage time in ms today
    private val dailyUsageMs = mutableMapOf<String, Long>()

    // packageName -> open count today
    private val dailyOpenCount = mutableMapOf<String, Int>()

    // packageName -> last prompt index shown (for rotation)
    private val lastPromptIndex = mutableMapOf<String, Int>()

    // packageName -> timestamp when current session started
    private val sessionStartMs = mutableMapOf<String, Long>()

    // Day tracking for auto-reset
    private var lastResetDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

    /**
     * Reset all counters if a new day has started.
     */
    private fun resetIfNewDay() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (today != lastResetDay) {
            Log.d(TAG, "New day detected, resetting escalation counters")
            dailyUsageMs.clear()
            dailyOpenCount.clear()
            lastPromptIndex.clear()
            sessionStartMs.clear()
            lastResetDay = today
        }
    }

    /**
     * Seed today's usage from UsageStatsManager so escalation accounts for
     * time spent before the accessibility service was running (e.g. after install).
     * Only seeds if UsageStatsManager shows MORE usage than we've tracked — never overwrites.
     */
    fun seedFromUsageStats(packageName: String, usageMs: Long) {
        resetIfNewDay()
        val current = dailyUsageMs[packageName] ?: 0L
        if (usageMs > current) {
            dailyUsageMs[packageName] = usageMs
        }
    }

    /**
     * Called when user opens/returns to an app (from AppInterceptor).
     */
    fun onAppOpened(packageName: String) {
        resetIfNewDay()
        dailyOpenCount[packageName] = (dailyOpenCount[packageName] ?: 0) + 1
        sessionStartMs[packageName] = System.currentTimeMillis()
    }

    /**
     * Called when user leaves an app (switches away).
     * Accumulates the session time into daily totals.
     */
    fun onAppLeft(packageName: String) {
        val start = sessionStartMs.remove(packageName) ?: return
        val elapsed = System.currentTimeMillis() - start
        dailyUsageMs[packageName] = (dailyUsageMs[packageName] ?: 0L) + elapsed
        Log.d(TAG, "$packageName session: ${elapsed / 1000}s, cumulative today: ${(dailyUsageMs[packageName] ?: 0) / 60_000}min")
    }

    /**
     * Determine the appropriate escalation level for this app right now.
     *
     * @param mediumThresholdMin Minutes of cumulative use before MEDIUM (default 20)
     * @param deepThresholdMin Minutes of cumulative use before DEEP (default 60)
     * @param isWindDown Whether the current time is in the user's wind-down window
     */
    fun getLevel(
        packageName: String,
        isWindDown: Boolean,
        mediumThresholdMin: Int = 20,
        deepThresholdMin: Int = 60,
    ): Level {
        resetIfNewDay()

        // Wind-down always escalates
        if (isWindDown) return Level.WIND_DOWN

        val cumulativeMin = (dailyUsageMs[packageName] ?: 0L) / 60_000

        return when {
            cumulativeMin >= deepThresholdMin -> Level.DEEP
            cumulativeMin >= mediumThresholdMin -> Level.MEDIUM
            else -> Level.LIGHT
        }
    }

    /**
     * Get the next prompt index for rotation (never repeats consecutively).
     */
    fun getNextPromptIndex(packageName: String, bankSize: Int): Int {
        if (bankSize <= 0) return 0
        val last = lastPromptIndex[packageName] ?: -1
        var next = (last + 1) % bankSize
        // Extra shuffle: if only 2+ prompts, skip one occasionally for variety
        if (bankSize > 3 && (dailyOpenCount[packageName] ?: 0) % 3 == 0) {
            next = (next + 1) % bankSize
        }
        lastPromptIndex[packageName] = next
        return next
    }

    fun getOpenCountToday(packageName: String): Int {
        resetIfNewDay()
        return dailyOpenCount[packageName] ?: 0
    }

    fun getCumulativeMinutesToday(packageName: String): Long {
        resetIfNewDay()
        return (dailyUsageMs[packageName] ?: 0L) / 60_000
    }

    /**
     * Build the full escalation context for an app intervention.
     */
    fun getContext(
        packageName: String,
        isWindDown: Boolean,
        mediumThresholdMin: Int = 20,
        deepThresholdMin: Int = 60,
        promptBankSize: Int = 15,
    ): Context {
        onAppOpened(packageName)
        return Context(
            level = getLevel(packageName, isWindDown, mediumThresholdMin, deepThresholdMin),
            openCountToday = getOpenCountToday(packageName),
            cumulativeMinutesToday = getCumulativeMinutesToday(packageName),
            promptIndex = getNextPromptIndex(packageName, promptBankSize),
        )
    }
}
