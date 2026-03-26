package com.faarigh.app.service.accessibility

import java.util.Calendar

/**
 * Bank of reflective prompts for intervention screens.
 *
 * Research basis (One Sec PNAS study, Gamba et al. 2023):
 * - Deliberation messages ALONE are not effective
 * - But combined with time delay + dismiss option, they contribute to 57% usage reduction
 * - Must rotate to prevent habituation (50% ignore rate within a week for static messages)
 *
 * Framing guidelines:
 * - Gain-framed (not loss-framed): "You have your evening" not "You wasted 2 hours"
 * - Curious, not accusatory: "What brought you here?" not "Are you sure?"
 * - Non-judgmental: never shame, moralize, or catastrophize
 * - Autonomy-respecting: "Totally your call" energy
 */
object ReflectivePrompts {

    /**
     * Each prompt is a function that takes context and returns formatted text.
     * This allows prompts to include dynamic data (app name, count, time).
     */
    data class PromptContext(
        val appLabel: String,
        val openCountToday: Int,
        val currentHour: Int,
        val cumulativeMinutes: Long,
    )

    private val prompts: List<(PromptContext) -> String> = listOf(
        // ── Curiosity ────────────────────────────────────
        { ctx -> "What are you hoping to find on ${ctx.appLabel}?" },
        { _ -> "What brought you here right now?" },
        { _ -> "Is this where you want to be?" },
        { ctx -> "What will ${ctx.appLabel} give you that you need?" },

        // ── Time Awareness ───────────────────────────────
        { ctx ->
            val timeStr = formatTime(ctx.currentHour)
            if (ctx.currentHour >= 22 || ctx.currentHour < 6) {
                "It's $timeStr. You wanted better sleep."
            } else {
                "It's $timeStr. How do you want to spend this time?"
            }
        },
        { ctx ->
            val remaining = estimateEveningRemaining(ctx.currentHour)
            if (remaining != null) "You have about $remaining of your evening left." else "Take a breath. What matters right now?"
        },

        // ── Neutral Observation ──────────────────────────
        { ctx ->
            val ordinal = ordinal(ctx.openCountToday)
            "This is your $ordinal time opening ${ctx.appLabel} today."
        },
        { ctx ->
            if (ctx.cumulativeMinutes > 0) {
                "You've spent ${ctx.cumulativeMinutes} minutes on ${ctx.appLabel} today."
            } else {
                "First visit to ${ctx.appLabel} today. Make it count."
            }
        },
        { ctx ->
            if (ctx.openCountToday > 3) {
                "You've checked ${ctx.appLabel} ${ctx.openCountToday} times today."
            } else {
                "Just checking in. Still want to open ${ctx.appLabel}?"
            }
        },

        // ── Alternative Suggestions ──────────────────────
        { _ -> "You could also go for a short walk." },
        { _ -> "What about reading a few pages instead?" },
        { _ -> "How about stretching for a minute?" },

        // ── Gentle Encouragement ─────────────────────────
        { _ -> "Take a breath. Totally your call." },
        { _ -> "No judgment either way. What do you want?" },
        { _ -> "This is hard for everyone. You're doing well." },
        { _ -> "One conscious choice at a time." },
    )

    val size: Int get() = prompts.size

    /**
     * Get a formatted prompt by index.
     */
    fun getPrompt(index: Int, context: PromptContext): String {
        val safeIndex = index.coerceIn(0, prompts.lastIndex)
        return prompts[safeIndex](context)
    }

    /**
     * Get a prompt with raw parameters (convenience for service layer).
     */
    fun getPrompt(
        index: Int,
        appLabel: String,
        openCountToday: Int,
        currentHour: Int,
        cumulativeMinutes: Long,
    ): String = getPrompt(
        index,
        PromptContext(appLabel, openCountToday, currentHour, cumulativeMinutes),
    )

    // ── Helpers ──────────────────────────────────────────

    private fun formatTime(hour: Int): String {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val amPm = if (hour < 12) "AM" else "PM"
        return "$h $amPm"
    }

    private fun estimateEveningRemaining(hour: Int): String? {
        return when {
            hour in 18..20 -> "${22 - hour} hours"
            hour == 21 -> "about an hour"
            hour >= 22 || hour < 6 -> null // too late, use different prompt
            else -> null // daytime, not applicable
        }
    }

    private fun ordinal(n: Int): String = when {
        n % 100 in 11..13 -> "${n}th"
        n % 10 == 1 -> "${n}st"
        n % 10 == 2 -> "${n}nd"
        n % 10 == 3 -> "${n}rd"
        else -> "${n}th"
    }
}
