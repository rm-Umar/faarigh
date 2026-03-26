package com.faarigh.app.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faarigh.app.R
import com.faarigh.app.ui.component.RetroButton
import com.faarigh.app.ui.component.RetroOutlinedButton
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

// ── Hardcoded overlay colors ──
private val BgColor = Color(0xFFF5F0E8)
private val TextPrimary = Color(0xFF2D2B28)
private val TextMuted = Color(0xFF7A756E)
private val AccentGreen = Color(0xFF5A8A54)
private val AccentPurple = Color(0xFF9C7EDB)
private val BorderColor = Color(0xFFE8E2D8)
private val CardBg = Color(0xFFFFFFFF)
private val WaveColor = Color(0xFFA8D5A2) // CardboardColors.accentGreen

private val RetroFont = FontFamily(Font(R.font.vt323_regular))

/**
 * Urge Surfing intervention overlay with wave animation.
 *
 * Based on mindfulness-based relapse prevention (MBRP):
 * urges are like waves — they rise, peak, and naturally fall.
 * The user watches the wave animate to internalize this.
 *
 * Total duration: ~15 seconds (2s intro + 10s wave + 2s outro + buttons)
 */
@Composable
fun UrgeSurfingOverlay(
    appName: String,
    appPackageName: String,
    escalationContext: String?,
    onProceed: () -> Unit,
    onTurnBack: () -> Unit,
) {
    // Phase: 0 = intro, 1 = wave rising, 2 = peak message, 3 = wave falling, 4 = outro, 5 = buttons
    var phase by remember { mutableIntStateOf(0) }
    var showButtons by remember { mutableStateOf(false) }

    // Wave progress: 0f (flat) -> 1f (full cycle complete)
    val waveProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Phase 0: Intro text
        phase = 0
        delay(2000L)

        // Phase 1-3: Wave animation (10 seconds total)
        phase = 1
        waveProgress.animateTo(
            targetValue = 0.5f,
            animationSpec = tween(durationMillis = 5000, easing = LinearEasing),
        )

        // Phase 2: Peak message
        phase = 2
        waveProgress.animateTo(
            targetValue = 0.7f,
            animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
        )

        // Phase 3: Wave falling
        phase = 3
        waveProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing),
        )

        // Phase 4: Outro
        phase = 4
        delay(2000L)

        // Phase 5: Buttons
        phase = 5
        showButtons = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .drawBehind {
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
            // ── Escalation context ──
            if (escalationContext != null) {
                Text(
                    text = escalationContext,
                    color = TextMuted,
                    fontFamily = RetroFont,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
            }

            // ── Intro text ──
            AnimatedVisibility(
                visible = phase >= 0,
                enter = fadeIn(tween(600)),
                exit = fadeOut(tween(400)),
            ) {
                Text(
                    text = "You're feeling pulled to $appName.\nThat's normal.",
                    color = TextPrimary,
                    fontFamily = RetroFont,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Wave animation ──
            AnimatedVisibility(
                visible = phase in 1..4,
                enter = fadeIn(tween(600)),
                exit = fadeOut(tween(400)),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                ) {
                    val progress = waveProgress.value
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2

                    // Amplitude envelope: rises then falls (peaks at progress ~0.5)
                    val envelope = if (progress <= 0.5f) {
                        progress * 2f // 0 -> 1
                    } else {
                        (1f - progress) * 2f // 1 -> 0
                    }
                    val maxAmplitude = h * 0.35f
                    val amplitude = maxAmplitude * envelope

                    val path = Path()
                    val steps = 200
                    for (i in 0..steps) {
                        val fraction = i.toFloat() / steps
                        // Only draw up to the current progress point
                        val drawFraction = fraction * progress
                        val x = fraction * w
                        val y = centerY - amplitude * sin(drawFraction * 2 * PI).toFloat()

                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = WaveColor,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Peak message ──
            AnimatedVisibility(
                visible = phase == 2,
                enter = fadeIn(tween(400)),
                exit = fadeOut(tween(400)),
            ) {
                Text(
                    text = "The urge is peaking. Stay with it.",
                    color = TextPrimary,
                    fontFamily = RetroFont,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Outro message ──
            AnimatedVisibility(
                visible = phase >= 4,
                enter = fadeIn(tween(400)),
                exit = fadeOut(tween(400)),
            ) {
                Text(
                    text = "It's passing. How do you feel?",
                    color = TextPrimary,
                    fontFamily = RetroFont,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Decision buttons ──
            AnimatedVisibility(
                visible = showButtons,
                enter = fadeIn(tween(400)),
                exit = fadeOut(tween(400)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(40.dp))

                    RetroButton(
                        text = "I'm good",
                        onClick = onTurnBack,
                        modifier = Modifier.fillMaxWidth(),
                        color = AccentGreen,
                        textColor = Color.White,
                    )

                    Spacer(Modifier.height(12.dp))

                    RetroOutlinedButton(
                        text = "Open $appName",
                        onClick = onProceed,
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = BorderColor,
                        textColor = TextPrimary,
                        surfaceColor = CardBg,
                    )
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
