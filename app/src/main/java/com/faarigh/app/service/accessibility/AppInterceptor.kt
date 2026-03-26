package com.faarigh.app.service.accessibility

import android.util.Log

/**
 * Manages the logic of when to intercept an app launch, including cooldown tracking.
 *
 * Session-based tracking: once user proceeds in an app, they're in an "active session".
 * No re-intervention happens until they LEAVE the app (switch to home/another app)
 * AND the cooldown period has elapsed. This prevents:
 * - Comments/dialogs/share sheets re-triggering intervention
 * - Infinite scroll being interrupted mid-session
 */
class AppInterceptor {

    companion object {
        private const val TAG = "AppInterceptor"
    }

    val escalationTracker = EscalationTracker()

    // packageName -> timestamp of when session started (user proceeded)
    private val sessionStartTimes = mutableMapOf<String, Long>()

    // Packages currently in an active session (user proceeded and hasn't left)
    private val activeSessions = mutableSetOf<String>()

    // Configurable cooldown per app (packageName -> cooldown in ms)
    private val appCooldowns = mutableMapOf<String, Long>()

    // Default cooldown — configurable from preferences
    @Volatile
    private var defaultCooldownMs: Long = 1_800_000L // 30 minutes

    // ── Mid-session check-in tracking ────────────────────
    // Track which threshold the user has already been interrupted at during this session.
    // Key: packageName, Value: last threshold (in minutes) that triggered a check-in.
    // This prevents re-triggering at the same threshold after user proceeds.
    private val lastCheckInThreshold = mutableMapOf<String, Int>()
    // Minimum interval between mid-session check-ins (5 minutes)
    private val MIN_CHECKIN_INTERVAL_MS = 5 * 60_000L
    private val lastCheckInTime = mutableMapOf<String, Long>()

    fun setDefaultCooldown(cooldownMs: Long) {
        defaultCooldownMs = cooldownMs
    }

    fun setCooldown(packageName: String, cooldownMs: Long) {
        if (cooldownMs > 0) {
            appCooldowns[packageName] = cooldownMs
        } else {
            appCooldowns.remove(packageName)
        }
    }

    /**
     * Called when the foreground app changes. If the user left an active session
     * (switched to a different app or went home), end that session.
     */
    fun onForegroundChanged(newPackage: String) {
        // End sessions for all OTHER packages — user has left them
        val ended = activeSessions.filter { it != newPackage }
        ended.forEach { pkg ->
            activeSessions.remove(pkg)
            escalationTracker.onAppLeft(pkg)
            lastCheckInThreshold.remove(pkg)
            lastCheckInTime.remove(pkg)
            Log.d(TAG, "Session ended for $pkg (user left)")
        }
    }

    /**
     * Checks whether this app launch should be intercepted.
     * Returns true if an intervention should be shown.
     */
    fun shouldIntercept(
        packageName: String,
        targetPackages: Set<String>,
    ): Boolean {
        if (packageName !in targetPackages) return false

        // If user is in an active session for this app, never interrupt
        if (packageName in activeSessions) {
            Log.d(TAG, "Skipping $packageName (active session)")
            return false
        }

        // If cooldown hasn't elapsed since last session, skip
        if (isInCooldown(packageName)) {
            Log.d(TAG, "Skipping $packageName (in cooldown)")
            return false
        }

        return true
    }

    /**
     * Called when the user proceeds past the intervention.
     * Starts an active session — no more interventions until they leave & cooldown expires.
     */
    fun onProceeded(packageName: String) {
        activeSessions.add(packageName)
        sessionStartTimes[packageName] = System.currentTimeMillis()
        Log.d(TAG, "Session started for $packageName")
    }

    /**
     * Called when the user goes back from the intervention.
     * No session started — next open will trigger intervention again.
     */
    fun onTurnedBack(packageName: String) {
        // No session — if they come back, intervention fires again
    }

    /**
     * Check if a mid-session check-in should fire during active scrolling.
     *
     * This is the anti-doom-scroll mechanism. During an active session,
     * we periodically check if the user has crossed a new time threshold
     * (e.g., 20min, 40min, 60min). If they have, we interrupt with an
     * escalated intervention.
     *
     * @param checkInIntervalMin How often to check in (default: every 20 minutes)
     * @return true if we should show a mid-session intervention
     */
    fun shouldCheckInMidSession(
        packageName: String,
        targetPackages: Set<String>,
        checkInIntervalMin: Int = 20,
    ): Boolean {
        // Only check apps we're monitoring that have an active session
        if (packageName !in targetPackages) return false
        if (packageName !in activeSessions) return false

        val sessionStart = sessionStartTimes[packageName] ?: return false
        val now = System.currentTimeMillis()
        val sessionMinutes = ((now - sessionStart) / 60_000).toInt()

        // Which threshold are we at? (20, 40, 60, 80, ...)
        if (sessionMinutes < checkInIntervalMin) return false
        val currentThreshold = (sessionMinutes / checkInIntervalMin) * checkInIntervalMin

        // Have we already shown a check-in at this threshold?
        val lastThreshold = lastCheckInThreshold[packageName] ?: 0
        if (currentThreshold <= lastThreshold) return false

        // Debounce — don't fire more than once per 5 minutes
        val lastTime = lastCheckInTime[packageName] ?: 0L
        if (now - lastTime < MIN_CHECKIN_INTERVAL_MS) return false

        // Fire the check-in!
        lastCheckInThreshold[packageName] = currentThreshold
        lastCheckInTime[packageName] = now
        Log.d(TAG, "Mid-session check-in for $packageName at ${currentThreshold}min")
        return true
    }

    /**
     * Called when user proceeds past a mid-session check-in.
     * Resets the session timer so the next check-in is relative to now.
     */
    fun onMidSessionProceeded(packageName: String) {
        // Don't reset session start — keep cumulative tracking
        // The threshold tracking prevents re-triggering at the same level
        Log.d(TAG, "Mid-session proceeded for $packageName")
    }

    /**
     * Build escalation context for the given package. Delegates to EscalationTracker.
     */
    fun getEscalationContext(
        packageName: String,
        isWindDown: Boolean,
        mediumThresholdMin: Int = 20,
        deepThresholdMin: Int = 60,
    ): EscalationTracker.Context {
        return escalationTracker.getContext(
            packageName = packageName,
            isWindDown = isWindDown,
            mediumThresholdMin = mediumThresholdMin,
            deepThresholdMin = deepThresholdMin,
            promptBankSize = ReflectivePrompts.size,
        )
    }

    private fun isInCooldown(packageName: String): Boolean {
        val sessionStart = sessionStartTimes[packageName] ?: return false
        val cooldownMs = appCooldowns[packageName] ?: defaultCooldownMs
        return System.currentTimeMillis() - sessionStart < cooldownMs
    }
}
