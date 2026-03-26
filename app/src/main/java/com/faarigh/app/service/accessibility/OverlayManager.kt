package com.faarigh.app.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.faarigh.app.ui.overlay.AppPauseOverlay
import com.faarigh.app.ui.overlay.BreathingPattern
import com.faarigh.app.ui.overlay.CognitiveDefusionOverlay
import com.faarigh.app.ui.overlay.InterventionTechnique
import com.faarigh.app.ui.overlay.NsfwShieldOverlay
import com.faarigh.app.ui.overlay.QuarantineOverlay
import com.faarigh.app.ui.overlay.ShortsBlockedOverlay
import com.faarigh.app.ui.overlay.UrgeSurfingOverlay
import com.faarigh.app.ui.overlay.ValuesCheckInOverlay

class OverlayManager(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null

    val isShowing: Boolean get() = overlayView != null

    fun show(
        packageName: String,
        appLabel: String,
        onProceed: () -> Unit,
        onGoBack: () -> Unit,
        escalationLevel: EscalationTracker.Level = EscalationTracker.Level.LIGHT,
        breathingPattern: BreathingPattern = BreathingPattern.SimplePause,
        promptText: String? = null,
        contextText: String? = null,
    ) {
        if (overlayView != null) return

        showComposeOverlay {
            AppPauseOverlay(
                appLabel = appLabel,
                breathingPattern = breathingPattern,
                escalationLevel = escalationLevel,
                promptText = promptText,
                contextText = contextText,
                onCancel = {
                    dismiss()
                    onGoBack()
                },
                onProceed = {
                    dismiss()
                    onProceed()
                },
            )
        }
    }

    /**
     * Show the NSFW-specific intervention — immediate decision screen
     * with a clear message about what was detected.
     */
    fun showNsfwIntervention(
        packageName: String,
        appLabel: String,
        onProceed: () -> Unit,
        onGoBack: () -> Unit,
        onFalsePositive: () -> Unit = {},
    ) {
        if (overlayView != null) return

        showComposeOverlay {
            NsfwShieldOverlay(
                onGoBack = {
                    dismiss()
                    onGoBack()
                },
                onContinueAnyway = {
                    dismiss()
                    onProceed()
                },
                onFalsePositive = {
                    dismiss()
                    onFalsePositive()
                },
            )
        }
    }

    /**
     * Show shorts blocked overlay — blocks content immediately with a message.
     * When user taps OK, overlay dismisses and onDismiss is called.
     */
    fun showShortsBlocked(
        platform: String,
        openCountToday: Int = 0,
        onDismiss: () -> Unit,
    ) {
        if (overlayView != null) return

        showComposeOverlay {
            ShortsBlockedOverlay(
                platform = platform,
                onOk = {
                    dismiss()
                    onDismiss()
                },
                openCountToday = openCountToday,
            )
        }
    }

    /**
     * Show quarantine overlay — blocks app with no bypass option.
     * User deliberately set this schedule.
     */
    fun showQuarantine(
        appLabel: String,
        reason: String,
        onDismiss: () -> Unit,
    ) {
        if (overlayView != null) return

        showComposeOverlay {
            QuarantineOverlay(
                appLabel = appLabel,
                reason = reason,
                onDismiss = {
                    dismiss()
                    onDismiss()
                },
            )
        }
    }

    /**
     * Show an intervention overlay based on the selected technique.
     *
     * Falls through to [AppPauseOverlay] for SIMPLE_PAUSE and PHYSIOLOGICAL_SIGH.
     * Dispatches to technique-specific overlays for COGNITIVE_DEFUSION,
     * URGE_SURFING, and WIND_DOWN values check-in.
     */
    fun showInterventionWithTechnique(
        technique: InterventionTechnique,
        packageName: String,
        appLabel: String,
        onProceed: () -> Unit,
        onGoBack: () -> Unit,
        escalationLevel: EscalationTracker.Level = EscalationTracker.Level.LIGHT,
        breathingPattern: BreathingPattern = BreathingPattern.SimplePause,
        promptText: String? = null,
        contextText: String? = null,
    ) {
        if (overlayView != null) return

        // Wind-down always uses the values check-in overlay
        if (escalationLevel == EscalationTracker.Level.WIND_DOWN) {
            showComposeOverlay {
                ValuesCheckInOverlay(
                    appName = appLabel,
                    appPackageName = packageName,
                    onProceed = {
                        dismiss()
                        onProceed()
                    },
                    onTurnBack = {
                        dismiss()
                        onGoBack()
                    },
                )
            }
            return
        }

        when (technique) {
            InterventionTechnique.SIMPLE_PAUSE,
            InterventionTechnique.PHYSIOLOGICAL_SIGH -> {
                // Fall through to existing breathing overlay
                show(
                    packageName = packageName,
                    appLabel = appLabel,
                    onProceed = onProceed,
                    onGoBack = onGoBack,
                    escalationLevel = escalationLevel,
                    breathingPattern = breathingPattern,
                    promptText = promptText,
                    contextText = contextText,
                )
            }

            InterventionTechnique.COGNITIVE_DEFUSION -> {
                showComposeOverlay {
                    CognitiveDefusionOverlay(
                        appName = appLabel,
                        appPackageName = packageName,
                        escalationContext = contextText,
                        onProceed = {
                            dismiss()
                            onProceed()
                        },
                        onTurnBack = {
                            dismiss()
                            onGoBack()
                        },
                    )
                }
            }

            InterventionTechnique.URGE_SURFING -> {
                showComposeOverlay {
                    UrgeSurfingOverlay(
                        appName = appLabel,
                        appPackageName = packageName,
                        escalationContext = contextText,
                        onProceed = {
                            dismiss()
                            onProceed()
                        },
                        onTurnBack = {
                            dismiss()
                            onGoBack()
                        },
                    )
                }
            }
        }
    }

    fun dismiss() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: IllegalArgumentException) {
                // View already removed
            }
        }
        overlayView = null
    }

    private fun showComposeOverlay(content: @Composable () -> Unit) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )

        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val composeView = ComposeView(service).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent(content)
        }

        overlayView = composeView
        windowManager.addView(composeView, params)
    }
}

/**
 * Minimal LifecycleOwner + SavedStateRegistryOwner for hosting Compose
 * inside a service overlay (no Activity available).
 */
private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    init {
        savedStateRegistryController.performRestore(null)
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
