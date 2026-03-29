package com.faarigh.app.ui.overlay

/**
 * Research-backed breathing patterns for intervention overlays.
 *
 * Based on:
 * - Stanford Cell Reports Medicine 2023: Physiological sigh is the
 *   "fastest known way to calm the nervous system in real time"
 * - Box breathing: well-established for stress reduction (military, first responders)
 * - Simple pause: pattern interrupt only (One Sec PNAS study — friction design)
 */
sealed class BreathingPattern(
    val name: String,
    val description: String,
    val totalDurationMs: Long,
    val phases: List<BreathPhase>,
) {
    /**
     * Physiological Sigh — Stanford-validated
     * Double inhale through nose + extended exhale through mouth.
     * One cycle ≈ 10 seconds. Shifts autonomic state in 1-3 cycles.
     */
    data object PhysiologicalSigh : BreathingPattern(
        name = "Physiological Sigh",
        description = "Double inhale, long exhale",
        totalDurationMs = 10_000,
        phases = listOf(
            BreathPhase("Inhale", 2500, 0.65f, 0.95f),
            BreathPhase("Inhale", 1000, 0.95f, 1.10f),   // double-inhale top-up
            BreathPhase("Exhale", 6500, 1.10f, 0.55f),    // extended exhale
        ),
    )

    /**
     * Box Breathing — 4-4-4-4 pattern
     * Used by Navy SEALs, first responders. Each phase 4 seconds.
     * One cycle = 16 seconds. Good for deeper calming.
     */
    data object BoxBreathing : BreathingPattern(
        name = "Box Breathing",
        description = "Inhale, hold, exhale, hold",
        totalDurationMs = 16_000,
        phases = listOf(
            BreathPhase("Inhale", 4000, 0.65f, 1.05f),
            BreathPhase("Hold", 4000, 1.05f, 1.05f),
            BreathPhase("Exhale", 4000, 1.05f, 0.65f),
            BreathPhase("Hold", 4000, 0.65f, 0.65f),
        ),
    )

    /**
     * Simple Pause — pattern interrupt only
     * NOT a breathing exercise. Just a 4-second delay to break the
     * automatic open-scroll loop. Based on friction design research.
     */
    data object SimplePause : BreathingPattern(
        name = "Pause",
        description = "A moment to reflect",
        totalDurationMs = 4_000,
        phases = listOf(
            BreathPhase("Breathe", 4000, 0.85f, 1.00f),
        ),
    )

    /** A pattern with user-scaled phase durations. */
    class Scaled(
        name: String,
        description: String,
        totalDurationMs: Long,
        phases: List<BreathPhase>,
    ) : BreathingPattern(name, description, totalDurationMs, phases)
}

/**
 * A single phase within a breathing pattern.
 *
 * @param label User-facing instruction (e.g., "Inhale", "Hold", "Exhale")
 * @param durationMs How long this phase lasts
 * @param startScale Circle animation start scale (0.0-1.5)
 * @param endScale Circle animation end scale (0.0-1.5)
 */
data class BreathPhase(
    val label: String,
    val durationMs: Long,
    val startScale: Float,
    val endScale: Float,
)
