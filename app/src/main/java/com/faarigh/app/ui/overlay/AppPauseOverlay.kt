package com.faarigh.app.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faarigh.app.R
import com.faarigh.app.service.accessibility.EscalationTracker
import com.faarigh.app.ui.component.RetroButton
import com.faarigh.app.ui.component.RetroOutlinedButton

// ── Hardcoded Cardboard Retro overlay colors ────────────────
private val BgColor = Color(0xFFF5F0E8)
private val CardBg = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF2D2B28)
private val TextMuted = Color(0xFF7A756E)
private val AccentGreen = Color(0xFF5A8A54)
private val AccentPurple = Color(0xFF9C7EDB)
private val BorderColor = Color(0xFFE8E2D8)
private val ShadowColor = Color(0xFF1A1A1A)

private val RetroFont = FontFamily(Font(R.font.vt323_regular))
private val PixelFont = FontFamily(Font(R.font.press_start_2p))

private val OverlayShape = RoundedCornerShape(4.dp)

/**
 * Full-screen App Pause intervention overlay with progressive friction.
 *
 * Animates through the provided [breathingPattern] phases, then reveals
 * decision buttons. Visual intensity scales with [escalationLevel].
 */
@Composable
fun AppPauseOverlay(
    appLabel: String,
    breathingPattern: BreathingPattern,
    escalationLevel: EscalationTracker.Level,
    promptText: String?,
    contextText: String?,
    onCancel: () -> Unit,
    onProceed: () -> Unit,
) {
    var breathingComplete by remember { mutableStateOf(false) }
    var currentPhaseLabel by remember { mutableStateOf(breathingPattern.phases.firstOrNull()?.label ?: "") }
    // Animate scale using Animatable for smooth transitions
    val animatedScale = remember { Animatable(breathingPattern.phases.firstOrNull()?.startScale ?: 0.85f) }

    // Animate through all breathing phases sequentially
    LaunchedEffect(breathingPattern) {
        for (phase in breathingPattern.phases) {
            currentPhaseLabel = phase.label
            animatedScale.animateTo(
                targetValue = phase.endScale,
                animationSpec = tween(
                    durationMillis = phase.durationMs.toInt(),
                    easing = EaseInOutCubic,
                ),
            )
        }
        breathingComplete = true
    }

    // Subtle pulsing ring (runs continuously during breathing)
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ringAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .drawBehind {
                // Subtle radial glows
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentGreen.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(size.width * 0.35f, size.height * 0.15f),
                        radius = size.width * 0.6f,
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentPurple.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.8f),
                        radius = size.width * 0.5f,
                    ),
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ── Context text (MEDIUM / DEEP / WIND_DOWN) ──
            if (contextText != null && escalationLevel != EscalationTracker.Level.LIGHT) {
                Text(
                    text = contextText,
                    color = TextMuted,
                    fontFamily = RetroFont,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
            }

            // ── Wind-down banner ──
            if (escalationLevel == EscalationTracker.Level.WIND_DOWN) {
                Text(
                    text = "Your wind-down time has started",
                    color = AccentPurple,
                    fontFamily = RetroFont,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Breathing circle ──
            if (!breathingComplete) {
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val baseRadius = size.minDimension / 2
                        val scale = animatedScale.value

                        val circleColor = when (escalationLevel) {
                            EscalationTracker.Level.WIND_DOWN -> AccentPurple
                            else -> AccentGreen
                        }

                        // Outer ring
                        drawCircle(
                            color = circleColor.copy(alpha = ringAlpha * 0.25f),
                            radius = baseRadius * scale * 1.1f,
                            center = center,
                            style = Stroke(width = 1.5f),
                        )

                        // Middle ring
                        drawCircle(
                            color = circleColor.copy(alpha = ringAlpha * 0.4f),
                            radius = baseRadius * scale * 0.85f,
                            center = center,
                            style = Stroke(width = 2f),
                        )

                        // Inner filled glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    circleColor.copy(alpha = 0.25f),
                                    circleColor.copy(alpha = 0.08f),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = baseRadius * scale * 0.65f,
                            ),
                            radius = baseRadius * scale * 0.65f,
                            center = center,
                        )

                        // Core glow
                        drawCircle(
                            color = circleColor.copy(alpha = 0.35f),
                            radius = baseRadius * scale * 0.35f,
                            center = center,
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Phase label
                Text(
                    text = currentPhaseLabel,
                    color = TextPrimary,
                    fontFamily = RetroFont,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                // App name hint
                Text(
                    text = "Opening $appLabel",
                    color = TextMuted,
                    fontFamily = RetroFont,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }

            // ── Prompt card (MEDIUM / DEEP / WIND_DOWN) ──
            if (promptText != null && escalationLevel != EscalationTracker.Level.LIGHT) {
                Spacer(Modifier.height(24.dp))

                val promptFontSize = when (escalationLevel) {
                    EscalationTracker.Level.DEEP -> 20.sp
                    else -> 17.sp
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg, OverlayShape)
                        .border(1.5.dp, BorderColor, OverlayShape)
                        .padding(20.dp),
                ) {
                    Text(
                        text = promptText,
                        color = TextPrimary,
                        fontFamily = RetroFont,
                        fontSize = promptFontSize,
                        textAlign = TextAlign.Center,
                        lineHeight = promptFontSize * 1.4f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── Early "Go back" button (visible during breathing) ──
            AnimatedVisibility(
                visible = !breathingComplete,
                enter = fadeIn(tween(600)),
                exit = fadeOut(tween(300)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(32.dp))
                    Text(
                        "\u2190 Go back",
                        fontFamily = RetroFont,
                        fontSize = 13.sp,
                        color = TextMuted.copy(alpha = 0.45f),
                        modifier = Modifier
                            .clickable(onClick = onCancel)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }

            // ── Decision buttons (appear after breathing completes) ──
            AnimatedVisibility(
                visible = breathingComplete,
                enter = fadeIn(tween(400)),
                exit = fadeOut(tween(400)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(40.dp))

                    when (escalationLevel) {
                        EscalationTracker.Level.DEEP -> DeepButtons(appLabel, onCancel, onProceed)
                        else -> StandardButtons(appLabel, onCancel, onProceed)
                    }
                }
            }
        }

        // ── Brand footer ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "\u0641\u0627\u0631\u063A",
                color = TextMuted.copy(alpha = 0.6f),
                fontSize = 28.sp,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                BorderColor,
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
    }
}

// ── Standard buttons (LIGHT / MEDIUM / WIND_DOWN) ──────────

@Composable
private fun StandardButtons(
    appLabel: String,
    onCancel: () -> Unit,
    onProceed: () -> Unit,
) {
    // Primary: "Not right now" — filled green
    RetroButton(
        text = "Not right now",
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
        color = AccentGreen,
        textColor = Color.White,
    )

    Spacer(Modifier.height(12.dp))

    // Secondary: "Open [app]" — outlined
    RetroOutlinedButton(
        text = "Open $appLabel",
        onClick = onProceed,
        modifier = Modifier.fillMaxWidth(),
        borderColor = BorderColor,
        textColor = TextPrimary,
        surfaceColor = CardBg,
    )
}

// ── Deep buttons (DEEP level — "Go back" very prominent) ───

@Composable
private fun DeepButtons(
    appLabel: String,
    onCancel: () -> Unit,
    onProceed: () -> Unit,
) {
    // Primary: "Go back" — large, prominent green
    RetroButton(
        text = "Go back",
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
        color = AccentGreen,
        textColor = Color.White,
    )

    Spacer(Modifier.height(20.dp))

    // Secondary: "Continue anyway" — small, muted, de-emphasized
    RetroOutlinedButton(
        text = "Continue anyway",
        onClick = onProceed,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
            .alpha(0.6f),
        borderColor = BorderColor.copy(alpha = 0.4f),
        textColor = TextMuted.copy(alpha = 0.6f),
        surfaceColor = BgColor,
        shadowOffsetX = 2.dp,
        shadowOffsetY = 2.dp,
    )
}
