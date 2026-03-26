package com.faarigh.app.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.faarigh.app.ui.component.RetroButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Cardboard Retro colors (overlay can't use MaterialTheme)
private val OverlayBg = Color(0xFFF5F0E8)
private val OverlayCard = Color(0xFFFFFFFF)
private val OverlayText = Color(0xFF2D2B28)
private val OverlayTextMuted = Color(0xFF7A756E)
private val OverlayAccent = Color(0xFFE57373)  // Coral for shorts
private val OverlayBorder = Color(0xFFD9D4CC)

private val shortsTips = listOf(
    "Short-form video is designed to keep you scrolling with variable rewards.",
    "Each swipe triggers a small dopamine hit, making it hard to stop.",
    "Taking a break now helps reset your reward system.",
    "Algorithms learn your weaknesses and exploit them to maximize watch time.",
)

/**
 * Full-screen overlay shown when shorts/reels are detected.
 * Blocks the content immediately and shows a friendly message.
 */
@Composable
fun ShortsBlockedOverlay(
    platform: String,
    onOk: () -> Unit,
    openCountToday: Int = 0,
) {
    val tip = remember { shortsTips.random() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(OverlayCard)
                .border(1.dp, OverlayBorder, RoundedCornerShape(4.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Block icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(OverlayAccent.copy(alpha = 0.08f))
                    .border(1.5.dp, OverlayAccent.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Block,
                    contentDescription = "Shorts detected",
                    tint = OverlayAccent,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Shorts detected",
                color = OverlayText,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "You chose to filter short-form videos${if (platform.isNotEmpty()) " on $platform" else ""}. This is your conscious choice.",
                color = OverlayTextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
            )

            // Open count context
            if (openCountToday > 0) {
                Spacer(Modifier.height(6.dp))

                val ordinal = when {
                    openCountToday % 100 in 11..13 -> "${openCountToday}th"
                    openCountToday % 10 == 1 -> "${openCountToday}st"
                    openCountToday % 10 == 2 -> "${openCountToday}nd"
                    openCountToday % 10 == 3 -> "${openCountToday}rd"
                    else -> "${openCountToday}th"
                }

                Text(
                    text = "This is your ${ordinal} time today",
                    color = OverlayAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Random tip
            Text(
                text = tip,
                color = OverlayTextMuted.copy(alpha = 0.75f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
            )

            Spacer(Modifier.height(24.dp))

            RetroButton(
                text = "Got it",
                onClick = onOk,
                modifier = Modifier.fillMaxWidth(),
                color = OverlayAccent,
                textColor = Color.White,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "\u0641\u0627\u0631\u063A",
                color = OverlayTextMuted.copy(alpha = 0.3f),
                fontSize = 20.sp,
            )
        }
    }
}
