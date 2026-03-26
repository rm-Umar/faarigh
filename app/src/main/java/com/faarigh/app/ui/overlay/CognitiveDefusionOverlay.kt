package com.faarigh.app.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faarigh.app.R
import com.faarigh.app.ui.component.RetroButton
import com.faarigh.app.ui.component.RetroOutlinedButton
import com.faarigh.app.ui.theme.CardboardColors
import com.faarigh.app.ui.theme.MonospaceFamily
import kotlinx.coroutines.delay

// ── Hardcoded overlay colors (no MaterialTheme in overlay context) ──
private val BgColor = Color(0xFFF5F0E8)
private val TextPrimary = Color(0xFF2D2B28)
private val TextMuted = Color(0xFF7A756E)
private val AccentGreen = Color(0xFF5A8A54)
private val AccentPurple = Color(0xFF9C7EDB)
private val BorderColor = Color(0xFFE8E2D8)
private val CardBg = Color(0xFFFFFFFF)

private val RetroFont = FontFamily(Font(R.font.vt323_regular))

/**
 * ACT-based cognitive defusion intervention overlay.
 *
 * Shows sequential text prompts that fade in/out like passing thoughts,
 * helping the user observe the urge to open the app without acting on it.
 *
 * Based on Acceptance and Commitment Therapy (ACT) defusion techniques:
 * observing thoughts as mental events rather than commands to act.
 */
@Composable
fun CognitiveDefusionOverlay(
    appName: String,
    appPackageName: String,
    escalationContext: String?,
    onProceed: () -> Unit,
    onTurnBack: () -> Unit,
) {
    val prompts = remember {
        listOf(
            "Notice the pull to open $appName.",
            "What does that urge feel like?",
            "That's just a thought. You can watch it pass.",
        )
    }

    var currentPromptIndex by remember { mutableIntStateOf(-1) }
    var showButtons by remember { mutableStateOf(false) }

    // Animated y-offset for the drifting-up effect
    val driftOffset = remember { Animatable(0f) }

    // Drive the prompt sequence
    LaunchedEffect(Unit) {
        for (i in prompts.indices) {
            currentPromptIndex = i
            driftOffset.snapTo(20f) // start slightly below
            driftOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600),
            )
            delay(3000L) // stay visible for 3 seconds
        }
        // After all prompts, show buttons
        showButtons = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .drawBehind {
                // Subtle radial glows matching AppPauseOverlay
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

            // ── Sequential prompts ──
            Spacer(Modifier.height(48.dp))

            for (i in prompts.indices) {
                AnimatedVisibility(
                    visible = currentPromptIndex >= i,
                    enter = fadeIn(tween(600)),
                    exit = fadeOut(tween(400)),
                ) {
                    Text(
                        text = prompts[i],
                        color = TextPrimary,
                        fontFamily = RetroFont,
                        fontSize = if (i == prompts.lastIndex) 28.sp else 24.sp,
                        fontWeight = if (i == prompts.lastIndex) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        lineHeight = if (i == prompts.lastIndex) 38.sp else 32.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (currentPromptIndex == i) {
                                    Modifier.offset { IntOffset(0, driftOffset.value.toInt()) }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }

                if (i < prompts.lastIndex) {
                    Spacer(Modifier.height(20.dp))
                }
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
                    Spacer(Modifier.height(48.dp))

                    RetroButton(
                        text = "Let it pass",
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
