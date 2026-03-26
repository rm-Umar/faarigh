package com.faarigh.app.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.content.Context
import com.faarigh.app.data.db.dao.InterventionEventDao
import com.faarigh.app.data.db.entity.AppSchedule
import com.faarigh.app.data.db.entity.InterventionEvent
import com.faarigh.app.data.preferences.ModulePreferences
import com.faarigh.app.data.repository.AppInterceptionRepository
import com.faarigh.app.data.repository.AppScheduleRepository
import com.faarigh.app.data.repository.UsageRepository
import com.faarigh.app.ui.overlay.BreathingPattern
import com.faarigh.app.service.content.NsfwClassifier
import java.util.Calendar
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

class FaarighAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FaarighA11y"
        private const val NSFW_ALLOW_COOLDOWN_MS = 1_800_000L // 30 min after user allows

        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.android.settings",
            "com.android.providers.settings",
            "com.google.android.dialer",
            "com.google.android.apps.messaging",
            "com.google.android.gms",           // Google Play Services
            "com.google.android.gsf",           // Google Services Framework
            "com.android.vending",              // Play Store
            "com.android.permissioncontroller", // Permission settings
            "com.google.android.permissioncontroller",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
        )

        var instance: FaarighAccessibilityService? = null
            private set
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ServiceEntryPoint {
        fun appInterceptionRepository(): AppInterceptionRepository
        fun usageRepository(): UsageRepository
        fun modulePreferences(): ModulePreferences
        fun interventionEventDao(): InterventionEventDao
        fun appScheduleRepository(): AppScheduleRepository
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val scanExecutor = Executors.newSingleThreadExecutor()

    private lateinit var appInterceptionRepo: AppInterceptionRepository
    private lateinit var usageRepo: UsageRepository
    private lateinit var modulePrefs: ModulePreferences
    private lateinit var interventionDao: InterventionEventDao
    private lateinit var appScheduleRepo: AppScheduleRepository

    private val appInterceptor = AppInterceptor()
    private val shortsDetector = ShortsDetector()
    private lateinit var overlayManager: OverlayManager
    private lateinit var nsfwClassifier: NsfwClassifier

    private var targetPackages = emptySet<String>()
    private var lastInterceptedPackage: String? = null
    private var currentForegroundPackage: String? = null

    // NSFW scanning state — loaded from preferences
    var nsfwScanningEnabled: Boolean = true
    var shortsBlockingEnabled: Boolean = true
    private var appPauseEnabled: Boolean = true
    private var nsfwThreshold: Float = 0.70f
    private var scanCooldownMs: Long = 2000L
    private var breathingDurationSec: Int = 3
    private var lastScanTime = 0L
    private var consecutiveCleanScans = 0
    private var isScanning = false
    private var nsfwAllowedUntil = 0L
    // Per-app false positive tracking — if user reports false positive,
    // raise threshold for that app for 2 hours
    private val falsePositiveApps = mutableMapOf<String, Long>() // pkg -> until timestamp
    private val FALSE_POSITIVE_WINDOW_MS = 7_200_000L // 2 hours

    // Per-app shorts blocking — which packages should have shorts detection
    private var shortsBlockerApps: Set<String> = ShortsDetector.MONITORED_PACKAGES

    // Shorts detection cooldown — debounce to avoid firing multiple times in quick succession
    private var lastShortsBackTime = 0L
    private val SHORTS_BACK_COOLDOWN_MS = 8_000L // 8 seconds debounce (give app time to navigate away)

    // Shorts open count — resets daily
    private var shortsOpenCountToday = 0
    private var shortsCountDate = 0 // day of year when count was last reset

    // Wind-down schedule state
    private var windDownEnabled: Boolean = false
    private var windDownStartHour: Int = 22
    private var windDownStartMin: Int = 0
    private var windDownEndHour: Int = 7
    private var windDownEndMin: Int = 0

    // Escalation thresholds
    private var mediumThresholdMin: Int = 20
    private var deepThresholdMin: Int = 60

    // User-preferred breathing pattern
    private var preferredBreathingPattern: String = "simple"

    // Quarantine schedules
    private var activeSchedules: List<AppSchedule> = emptyList()

    // Track intervention show time for duration calculation
    private var interventionShownAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ServiceEntryPoint::class.java,
        )
        appInterceptionRepo = entryPoint.appInterceptionRepository()
        usageRepo = entryPoint.usageRepository()
        modulePrefs = entryPoint.modulePreferences()
        interventionDao = entryPoint.interventionEventDao()
        appScheduleRepo = entryPoint.appScheduleRepository()

        overlayManager = OverlayManager(this)

        // Initialize NSFW classifier
        nsfwClassifier = NsfwClassifier(applicationContext)
        nsfwClassifier.initialize()

        // Load initial preferences synchronously so values are ready immediately
        try {
            runBlocking {
                breathingDurationSec = modulePrefs.breathingDurationSec.first()
                nsfwThreshold = modulePrefs.nsfwThreshold.first()
                scanCooldownMs = modulePrefs.nsfwScanIntervalMs.first()
                nsfwScanningEnabled = modulePrefs.nsfwEnabled.first()
                shortsBlockingEnabled = modulePrefs.shortsBlockerEnabled.first()
                shortsBlockerApps = modulePrefs.shortsBlockerApps.first()
                appPauseEnabled = modulePrefs.appPauseEnabled.first()
                windDownEnabled = modulePrefs.windDownEnabled.first()
                windDownStartHour = modulePrefs.windDownStartHour.first()
                windDownStartMin = modulePrefs.windDownStartMin.first()
                windDownEndHour = modulePrefs.windDownEndHour.first()
                windDownEndMin = modulePrefs.windDownEndMin.first()
                mediumThresholdMin = modulePrefs.mediumThresholdMin.first()
                deepThresholdMin = modulePrefs.deepThresholdMin.first()
                preferredBreathingPattern = modulePrefs.preferredBreathingPattern.first()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load initial preferences, using defaults: ${e.message}")
        }

        // Observe preference changes in real time
        serviceScope.launch {
            modulePrefs.breathingDurationSec.collectLatest { breathingDurationSec = it }
        }
        serviceScope.launch {
            modulePrefs.nsfwThreshold.collectLatest { nsfwThreshold = it }
        }
        serviceScope.launch {
            modulePrefs.nsfwScanIntervalMs.collectLatest { scanCooldownMs = it }
        }
        serviceScope.launch {
            modulePrefs.nsfwEnabled.collectLatest { nsfwScanningEnabled = it }
        }
        serviceScope.launch {
            modulePrefs.shortsBlockerEnabled.collectLatest { shortsBlockingEnabled = it }
        }
        serviceScope.launch {
            modulePrefs.appPauseEnabled.collectLatest { appPauseEnabled = it }
        }
        serviceScope.launch {
            modulePrefs.appPauseCooldownMin.collectLatest { cooldownMin ->
                appInterceptor.setDefaultCooldown(cooldownMin * 60_000L)
            }
        }
        serviceScope.launch {
            modulePrefs.shortsBlockerApps.collectLatest { shortsBlockerApps = it }
        }
        serviceScope.launch {
            modulePrefs.windDownEnabled.collectLatest { windDownEnabled = it }
        }
        serviceScope.launch {
            modulePrefs.windDownStartHour.collectLatest { windDownStartHour = it }
        }
        serviceScope.launch {
            modulePrefs.windDownStartMin.collectLatest { windDownStartMin = it }
        }
        serviceScope.launch {
            modulePrefs.windDownEndHour.collectLatest { windDownEndHour = it }
        }
        serviceScope.launch {
            modulePrefs.windDownEndMin.collectLatest { windDownEndMin = it }
        }
        serviceScope.launch {
            modulePrefs.mediumThresholdMin.collectLatest { mediumThresholdMin = it }
        }
        serviceScope.launch {
            modulePrefs.deepThresholdMin.collectLatest { deepThresholdMin = it }
        }
        serviceScope.launch {
            modulePrefs.preferredBreathingPattern.collectLatest { preferredBreathingPattern = it }
        }

        // Observe active quarantine schedules
        serviceScope.launch {
            appScheduleRepo.getActiveSchedules().collectLatest { schedules ->
                activeSchedules = schedules
                Log.d(TAG, "Active schedules updated: ${schedules.size} schedules")
            }
        }

        serviceScope.launch {
            appInterceptionRepo.getEnabledApps().collectLatest { apps ->
                targetPackages = apps.map { it.packageName }.toSet()
                apps.forEach { app ->
                    // cooldownSec < 300 (5 min) means "use global default"
                    // This handles legacy rows that had default 60s
                    val cooldownMs = if (app.cooldownSec >= 300) app.cooldownSec * 1000L else 0L
                    appInterceptor.setCooldown(app.packageName, cooldownMs)
                }
                Log.d(TAG, "Target packages updated: ${targetPackages.size} apps")
            }
        }

        // Seed escalation tracker with today's actual usage from UsageStatsManager
        // so thresholds account for time used before the service started (e.g. after install)
        seedEscalationTrackerFromUsageStats()

        Log.i(TAG, "Accessibility service connected")
    }

    /**
     * Seed the escalation tracker with today's per-app usage so that thresholds account
     * for time spent before the service started. Uses event-based calculation (NOT
     * INTERVAL_DAILY which inflates times by 3-4 h). Only counts foreground time from
     * midnight today — matching Digital Wellbeing's midnight reset.
     */
    private fun seedEscalationTrackerFromUsageStats() {
        try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val cal = java.util.Calendar.getInstance()
            val endTime = cal.timeInMillis
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val startTime = cal.timeInMillis
            // Look back 12 h to catch sessions crossing midnight, but only count [startTime, endTime]
            val lookbackMs = startTime - 12 * 60 * 60 * 1000L
            val events = usm.queryEvents(lookbackMs, endTime)
            val event = android.app.usage.UsageEvents.Event()
            val foregroundStart = mutableMapOf<String, Long>()
            val foregroundTotals = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                when (event.eventType) {
                    android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        if (!foregroundStart.containsKey(pkg)) foregroundStart[pkg] = event.timeStamp
                    }
                    android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        val start = foregroundStart.remove(pkg) ?: continue
                        val effectiveStart = start.coerceAtLeast(startTime)
                        val effectiveEnd = event.timeStamp.coerceAtMost(endTime)
                        if (effectiveEnd > effectiveStart) {
                            val dur = effectiveEnd - effectiveStart
                            foregroundTotals[pkg] = (foregroundTotals[pkg] ?: 0L) + dur
                        }
                    }
                }
            }
            // Sessions still open at endTime
            val now = System.currentTimeMillis().coerceAtMost(endTime)
            for ((pkg, start) in foregroundStart) {
                val effectiveStart = start.coerceAtLeast(startTime)
                val dur = (now - effectiveStart).coerceAtLeast(0)
                if (dur > 0) foregroundTotals[pkg] = (foregroundTotals[pkg] ?: 0L) + dur
            }

            var seeded = 0
            foregroundTotals.forEach { (pkg, usageMs) ->
                if (usageMs > 0) {
                    appInterceptor.escalationTracker.seedFromUsageStats(pkg, usageMs)
                    seeded++
                }
            }
            Log.d(TAG, "Seeded escalation tracker from events ($seeded apps)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to seed escalation tracker: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        if (packageName == this.packageName || packageName in IGNORED_PACKAGES) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                currentForegroundPackage = packageName
                consecutiveCleanScans = 0
                // Only trigger intervention for Activity transitions,
                // skip dialogs/sheets/popups (comments, share sheets, etc.)
                val className = event.className?.toString() ?: ""
                val isDialogOrPopup = className.contains("Dialog", ignoreCase = true) ||
                    className.contains("Sheet", ignoreCase = true) ||
                    className.contains("Popup", ignoreCase = true) ||
                    className.contains("PopupWindow", ignoreCase = true)
                if (!isDialogOrPopup) {
                    handleWindowStateChanged(packageName)
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowContentChanged(event, packageName)
                // Smart NSFW scanning: scan when content changes in non-trusted apps
                if (nsfwScanningEnabled && !overlayManager.isShowing) {
                    maybeRunNsfwScan(packageName)
                }
            }
        }
    }

    private fun handleWindowStateChanged(packageName: String) {
        if (overlayManager.isShowing) return

        // Notify interceptor of foreground change — ends sessions for apps user left
        appInterceptor.onForegroundChanged(packageName)

        // ── Quarantine check — BEFORE other interventions ──
        val blockReason = appScheduleRepo.isAppCurrentlyBlocked(packageName, activeSchedules)
        if (blockReason != null) {
            Log.d(TAG, "Quarantine blocking $packageName: $blockReason")
            logInterventionEvent(
                moduleId = "quarantine",
                appPackage = packageName,
                appName = getAppLabel(packageName),
                action = "blocked",
            )
            overlayManager.showQuarantine(
                appLabel = getAppLabel(packageName),
                reason = blockReason,
                onDismiss = {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                },
            )
            return
        }

        if (shortsBlockingEnabled && packageName in shortsBlockerApps && packageName in ShortsDetector.FULL_SHORT_FORM_PACKAGES) {
            if (appInterceptor.shouldIntercept(packageName, targetPackages + ShortsDetector.FULL_SHORT_FORM_PACKAGES)) {
                showIntervention(packageName, "TikTok")
                return
            }
        }

        if (appPauseEnabled && appInterceptor.shouldIntercept(packageName, targetPackages)) {
            showIntervention(packageName, getAppLabel(packageName))
            return
        }

        // NSFW scan on app open (not just content change)
        if (nsfwScanningEnabled && !overlayManager.isShowing) {
            maybeRunNsfwScan(packageName)
        }
    }

    private fun handleWindowContentChanged(event: AccessibilityEvent, packageName: String) {
        if (overlayManager.isShowing) return

        // Mid-session doom-scroll check: periodically interrupt during active sessions
        if (appPauseEnabled && appInterceptor.shouldCheckInMidSession(packageName, targetPackages, mediumThresholdMin)) {
            Log.d(TAG, "Mid-session check-in triggered for $packageName")
            showIntervention(packageName, getAppLabel(packageName))
            return
        }

        // Shorts detection with cooldown to prevent spam
        if (shortsBlockingEnabled && packageName in shortsBlockerApps) {
            val now = System.currentTimeMillis()
            if (now - lastShortsBackTime > SHORTS_BACK_COOLDOWN_MS) {
                try {
                    val rootNode = rootInActiveWindow
                    if (rootNode != null) {
                        val result = shortsDetector.checkForShorts(event, rootNode)
                        try { rootNode.recycle() } catch (_: Exception) {}
                        if (result.detected) {
                            Log.d(TAG, "Shorts detected: ${result.platform} - ${result.detail}")
                            lastShortsBackTime = now
                            logInterventionEvent(
                                moduleId = "shorts_blocker",
                                appPackage = packageName,
                                appName = getAppLabel(packageName),
                                action = "blocked",
                            )
                            // Track daily shorts open count (reset on new day)
                            val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                            if (today != shortsCountDate) {
                                shortsOpenCountToday = 0
                                shortsCountDate = today
                            }
                            shortsOpenCountToday++

                            // Show overlay immediately to cover shorts content.
                            // On dismiss, press BACK to navigate within the app
                            // (e.g. YouTube shorts tab → YouTube home tab).
                            overlayManager.showShortsBlocked(
                                platform = result.platform,
                                openCountToday = shortsOpenCountToday,
                                onDismiss = {
                                    lastShortsBackTime = System.currentTimeMillis()
                                    // BACK navigates within the app (shorts → home tab)
                                    performGlobalAction(GLOBAL_ACTION_BACK)
                                },
                            )
                            return
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Shorts detection error: ${e.message}")
                }
            }
        }
    }

    /**
     * Smart NSFW scanning: take a screenshot and run the classifier.
     * Only scans when:
     * - Content actually changed (triggered by TYPE_WINDOW_CONTENT_CHANGED)
     * - Not in a trusted/system app
     * - Enough time has passed since last scan
     * - Not already scanning
     * - API 30+ (takeScreenshot available on Android R+)
     */
    private fun maybeRunNsfwScan(packageName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        if (isScanning) return
        if (packageName in IGNORED_PACKAGES || packageName == this.packageName) return
        if (packageName == "com.faarigh.app") return
        // Skip if user allowed NSFW within the cooldown window
        if (System.currentTimeMillis() < nsfwAllowedUntil) return

        val now = System.currentTimeMillis()
        // Increase cooldown if recent scans were clean
        val effectiveCooldown = if (consecutiveCleanScans > 3) {
            scanCooldownMs * 3 // Scan less often if content is clean
        } else {
            scanCooldownMs
        }

        if (now - lastScanTime < effectiveCooldown) return

        lastScanTime = now
        isScanning = true
        Log.d(TAG, "NSFW scan starting for $packageName")

        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            scanExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    try {
                        val hwBuf = screenshot.hardwareBuffer
                        val bitmap = Bitmap.wrapHardwareBuffer(hwBuf, screenshot.colorSpace)
                        if (bitmap != null) {
                            val softBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                            bitmap.recycle()
                            hwBuf.close()

                            if (softBitmap != null) {
                                // Crop to center 80% to remove status bar & nav bar
                                val cropTop = (softBitmap.height * 0.1f).toInt()
                                val cropBottom = (softBitmap.height * 0.1f).toInt()
                                val cropped = Bitmap.createBitmap(
                                    softBitmap, 0, cropTop,
                                    softBitmap.width,
                                    softBitmap.height - cropTop - cropBottom,
                                )
                                softBitmap.recycle()

                                val result = nsfwClassifier.classifyDetailed(cropped)
                                cropped.recycle()

                                if (result.error) {
                                    Log.w(TAG, "NSFW classifier error for $packageName")
                                } else {
                                    // Use higher threshold if user reported false positive for this app
                                    val fpUntil = falsePositiveApps[packageName] ?: 0L
                                    val effectiveThreshold = if (System.currentTimeMillis() < fpUntil) {
                                        0.60f // Much stricter after false positive report
                                    } else {
                                        nsfwThreshold
                                    }

                                    Log.d(TAG, "NSFW explicit=${"%3f".format(result.explicitScore)} sexy=${"%.3f".format(result.sexyScore)} pkg=$packageName (threshold: $effectiveThreshold)")

                                    if (result.explicitScore >= effectiveThreshold) {
                                        consecutiveCleanScans = 0
                                        Log.w(TAG, "*** NSFW DETECTED (explicit: ${result.explicitScore}) in $packageName ***")
                                        serviceScope.launch {
                                            if (!overlayManager.isShowing) {
                                                showNsfwIntervention(packageName, getAppLabel(packageName))
                                            }
                                        }
                                    } else {
                                        consecutiveCleanScans++
                                    }
                                }
                            } else {
                                Log.w(TAG, "Failed to copy bitmap to software format")
                            }
                        } else {
                            Log.w(TAG, "Failed to wrap hardware buffer to bitmap")
                            hwBuf.close()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "NSFW scan error: ${e.message}", e)
                    } finally {
                        isScanning = false
                    }
                }

                override fun onFailure(errorCode: Int) {
                    Log.w(TAG, "Screenshot failed with code: $errorCode")
                    isScanning = false
                }
            },
        )
    }

    private fun showIntervention(packageName: String, appLabel: String) {
        if (overlayManager.isShowing) return
        lastInterceptedPackage = packageName
        interventionShownAt = System.currentTimeMillis()

        // Log "shown" event
        logInterventionEvent(
            moduleId = "app_pause",
            appPackage = packageName,
            appName = appLabel,
            action = "shown",
        )

        // Determine escalation context
        val isWindDown = isInWindDownWindow()
        val escalation = appInterceptor.getEscalationContext(
            packageName = packageName,
            isWindDown = isWindDown,
            mediumThresholdMin = mediumThresholdMin,
            deepThresholdMin = deepThresholdMin,
        )

        // Choose breathing pattern: use user's preferred pattern for LIGHT,
        // escalate to more intensive patterns for higher levels
        val userPattern = when (preferredBreathingPattern) {
            "sigh" -> BreathingPattern.PhysiologicalSigh
            "box" -> BreathingPattern.BoxBreathing
            else -> BreathingPattern.SimplePause
        }
        val breathingPattern = when (escalation.level) {
            EscalationTracker.Level.LIGHT -> userPattern
            EscalationTracker.Level.MEDIUM,
            EscalationTracker.Level.WIND_DOWN -> BreathingPattern.PhysiologicalSigh
            EscalationTracker.Level.DEEP -> BreathingPattern.BoxBreathing
        }

        // Get reflective prompt (only for MEDIUM/DEEP/WIND_DOWN)
        val promptText = when (escalation.level) {
            EscalationTracker.Level.LIGHT -> null
            else -> {
                val cal = Calendar.getInstance()
                ReflectivePrompts.getPrompt(
                    index = escalation.promptIndex,
                    appLabel = appLabel,
                    openCountToday = escalation.openCountToday,
                    currentHour = cal.get(Calendar.HOUR_OF_DAY),
                    cumulativeMinutes = escalation.cumulativeMinutesToday,
                )
            }
        }

        // Build context text
        val contextText = if (escalation.cumulativeMinutesToday > 0) {
            "You've spent ${escalation.cumulativeMinutesToday}min here today"
        } else {
            val ordinal = when {
                escalation.openCountToday % 100 in 11..13 -> "${escalation.openCountToday}th"
                escalation.openCountToday % 10 == 1 -> "${escalation.openCountToday}st"
                escalation.openCountToday % 10 == 2 -> "${escalation.openCountToday}nd"
                escalation.openCountToday % 10 == 3 -> "${escalation.openCountToday}rd"
                else -> "${escalation.openCountToday}th"
            }
            "${ordinal} time today"
        }

        overlayManager.show(
            packageName = packageName,
            appLabel = appLabel,
            escalationLevel = escalation.level,
            breathingPattern = breathingPattern,
            promptText = promptText,
            contextText = contextText,
            onProceed = {
                val pauseDuration = System.currentTimeMillis() - interventionShownAt
                appInterceptor.onProceeded(packageName)
                serviceScope.launch {
                    usageRepo.logEvent(packageName, "proceeded")
                }
                logInterventionEvent(
                    moduleId = "app_pause",
                    appPackage = packageName,
                    appName = appLabel,
                    action = "allowed",
                    durationMs = pauseDuration,
                )
                lastInterceptedPackage = null
            },
            onGoBack = {
                val pauseDuration = System.currentTimeMillis() - interventionShownAt
                appInterceptor.onTurnedBack(packageName)
                serviceScope.launch {
                    usageRepo.logEvent(packageName, "turned_back")
                }
                logInterventionEvent(
                    moduleId = "app_pause",
                    appPackage = packageName,
                    appName = appLabel,
                    action = "blocked",
                    durationMs = pauseDuration,
                )
                performGlobalAction(GLOBAL_ACTION_HOME)
                lastInterceptedPackage = null
            },
        )
    }

    /**
     * Check if current time falls within the user's wind-down window.
     * Handles overnight ranges (e.g. 22:00 -> 07:00).
     */
    private fun isInWindDownWindow(): Boolean {
        if (!windDownEnabled) return false
        val cal = Calendar.getInstance()
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMin = windDownStartHour * 60 + windDownStartMin
        val endMin = windDownEndHour * 60 + windDownEndMin

        return if (startMin <= endMin) {
            // Same-day range (e.g. 20:00 -> 23:00)
            nowMin in startMin until endMin
        } else {
            // Overnight range (e.g. 22:00 -> 07:00)
            nowMin >= startMin || nowMin < endMin
        }
    }

    private fun showNsfwIntervention(packageName: String, appLabel: String) {
        if (overlayManager.isShowing) return
        interventionShownAt = System.currentTimeMillis()

        // Log NSFW "shown" event
        logInterventionEvent(
            moduleId = "nsfw_detection",
            appPackage = packageName,
            appName = appLabel,
            action = "shown",
        )

        overlayManager.showNsfwIntervention(
            packageName = packageName,
            appLabel = appLabel,
            onProceed = {
                val pauseDuration = System.currentTimeMillis() - interventionShownAt
                // 30 min cooldown — they made a conscious choice
                nsfwAllowedUntil = System.currentTimeMillis() + NSFW_ALLOW_COOLDOWN_MS
                consecutiveCleanScans = 0
                serviceScope.launch {
                    usageRepo.logEvent(packageName, "proceeded", interventionType = "nsfw")
                }
                logInterventionEvent(
                    moduleId = "nsfw_detection",
                    appPackage = packageName,
                    appName = appLabel,
                    action = "allowed",
                    durationMs = pauseDuration,
                )
                Log.i(TAG, "NSFW allowed for 30 min in $packageName")
            },
            onGoBack = {
                val pauseDuration = System.currentTimeMillis() - interventionShownAt
                serviceScope.launch {
                    usageRepo.logEvent(packageName, "turned_back", interventionType = "nsfw")
                }
                logInterventionEvent(
                    moduleId = "nsfw_detection",
                    appPackage = packageName,
                    appName = appLabel,
                    action = "blocked",
                    durationMs = pauseDuration,
                )
                performGlobalAction(GLOBAL_ACTION_HOME)
            },
            onFalsePositive = {
                // Raise threshold for this app for 2 hours
                falsePositiveApps[packageName] = System.currentTimeMillis() + FALSE_POSITIVE_WINDOW_MS
                consecutiveCleanScans = 10 // Also slow down scanning
                serviceScope.launch {
                    usageRepo.logEvent(packageName, "false_positive", interventionType = "nsfw")
                }
                Log.i(TAG, "False positive reported for $packageName — raising threshold for 2h")
            },
        )
    }

    private fun logInterventionEvent(
        moduleId: String,
        appPackage: String,
        appName: String,
        action: String,
        durationMs: Long = 0,
    ) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                interventionDao.insert(
                    InterventionEvent(
                        moduleId = moduleId,
                        appPackage = appPackage,
                        appName = appName,
                        action = action,
                        durationMs = durationMs,
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log intervention event: ${e.message}")
            }
        }
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            info.loadLabel(pm).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        overlayManager.dismiss()
        nsfwClassifier.close()
        serviceScope.cancel()
        scanExecutor.shutdown()
    }
}
