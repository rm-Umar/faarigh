package com.faarigh.app.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.faarigh.app.ui.component.RetroButton
import com.faarigh.app.ui.component.RetroOutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Cardboard Retro Overlay Colors ──────────────────────────
private val OverlayBg = Color(0xFFF5F0E8)
private val OverlayCard = Color(0xFFFFFFFF)
private val OverlayText = Color(0xFF2D2B28)
private val OverlayTextMuted = Color(0xFF7A756E)
private val OverlayAccent = Color(0xFF9C7EDB)       // Purple for NSFW
private val OverlayBorder = Color(0xFFD9D4CC)
private val OverlayButtonBg = Color(0xFFFAF7F2)
private val OverlayGreen = Color(0xFF4CAF50)

/**
 * Full-screen NSFW content notice overlay — Cardboard Retro style.
 */
@Composable
fun NsfwShieldOverlay(
    onGoBack: () -> Unit,
    onContinueAnyway: () -> Unit,
    onFalsePositive: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Outlined card panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(OverlayCard)
                    .border(1.dp, OverlayBorder, RoundedCornerShape(4.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Notice icon
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(OverlayAccent.copy(alpha = 0.08f))
                        .border(1.5.dp, OverlayAccent.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.GppGood,
                        contentDescription = "Content notice",
                        tint = OverlayAccent,
                        modifier = Modifier.size(44.dp),
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Content notice",
                    color = OverlayText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "This content may not align with your goals.\nWould you like to continue?",
                    color = OverlayTextMuted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 23.sp,
                )

                Spacer(Modifier.height(28.dp))

                // Primary: Go back (green, prominent)
                RetroButton(
                    text = "Go back",
                    onClick = onGoBack,
                    modifier = Modifier.fillMaxWidth(),
                    color = OverlayGreen,
                    textColor = Color.White,
                )

                Spacer(Modifier.height(12.dp))

                // Secondary: Continue (outlined, muted, smaller shadow)
                RetroOutlinedButton(
                    text = "Continue",
                    onClick = onContinueAnyway,
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = OverlayBorder,
                    textColor = OverlayTextMuted,
                    surfaceColor = OverlayButtonBg,
                    shadowOffsetX = 3.dp,
                    shadowOffsetY = 3.dp,
                )

                Spacer(Modifier.height(16.dp))

                // False positive as a small text link
                Text(
                    text = "Report false positive",
                    color = OverlayTextMuted.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onFalsePositive() },
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "\u0641\u0627\u0631\u063A",
                    color = OverlayTextMuted.copy(alpha = 0.4f),
                    fontSize = 24.sp,
                )
            }
        }

        Text(
            text = "Adjust sensitivity in Settings",
            color = OverlayTextMuted.copy(alpha = 0.5f),
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
        )
    }
}
