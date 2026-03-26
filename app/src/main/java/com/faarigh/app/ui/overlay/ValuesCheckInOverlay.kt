package com.faarigh.app.ui.overlay

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faarigh.app.R
import com.faarigh.app.ui.component.RetroButton
import com.faarigh.app.ui.component.RetroOutlinedButton
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Hardcoded overlay colors ──
private val BgColor = Color(0xFFF5F0E8)
private val TextPrimary = Color(0xFF2D2B28)
private val TextMuted = Color(0xFF7A756E)
private val AccentGreen = Color(0xFF5A8A54)
private val AccentPurple = Color(0xFF9C7EDB)
private val BorderColor = Color(0xFFE8E2D8)
private val CardBg = Color(0xFFFFFFFF)

private val RetroFont = FontFamily(Font(R.font.vt323_regular))

/**
 * Wind-down values check-in overlay.
 *
 * A calm, text-based intervention used during nighttime wind-down hours.
 * Connects the user's app-opening impulse to their stated value of
 * better rest, without shaming or blocking.
 */
@Composable
fun ValuesCheckInOverlay(
    appName: String,
    appPackageName: String,
    onProceed: () -> Unit,
    onTurnBack: () -> Unit,
) {
    var contentVisible by remember { mutableStateOf(false) }

    val currentTime = remember {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    }

    LaunchedEffect(Unit) {
        delay(300L) // brief pause before fade-in
        contentVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .drawBehind {
                // Subtle purple glow for nighttime mood
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentPurple.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.3f),
                        radius = size.width * 0.7f,
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentGreen.copy(alpha = 0.04f), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.7f),
                        radius = size.width * 0.5f,
                    ),
                )
            },
    ) {
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(800)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // ── Current time ──
                Text(
                    text = currentTime,
                    color = AccentPurple,
                    fontFamily = RetroFont,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(32.dp))

                // ── Values reminder ──
                Text(
                    text = "You mentioned wanting better rest.",
                    color = TextMuted,
                    fontFamily = RetroFont,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(20.dp))

                // ── Reflective question ──
                Text(
                    text = "How does opening $appName serve\nwhat matters to you right now?",
                    color = TextPrimary,
                    fontFamily = RetroFont,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(48.dp))

                // ── Decision buttons ──
                RetroButton(
                    text = "You're right",
                    onClick = onTurnBack,
                    modifier = Modifier.fillMaxWidth(),
                    color = AccentGreen,
                    textColor = Color.White,
                )

                Spacer(Modifier.height(12.dp))

                RetroOutlinedButton(
                    text = "Open $appName anyway",
                    onClick = onProceed,
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = BorderColor,
                    textColor = TextPrimary,
                    surfaceColor = CardBg,
                )
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
