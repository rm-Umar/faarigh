package com.faarigh.app.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.faarigh.app.ui.component.RetroButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Cardboard Retro overlay colors (no MaterialTheme in overlay context)
private val OverlayBg = Color(0xFFF5F0E8)
private val OverlayCard = Color(0xFFFFFFFF)
private val OverlayText = Color(0xFF2D2B28)
private val OverlayTextMuted = Color(0xFF7A756E)
private val OverlayAccent = Color(0xFF8B6914)  // Amber-brown for quarantine
private val OverlayBorder = Color(0xFFD9D4CC)

/**
 * Full-screen overlay shown when a quarantined app is opened.
 * No bypass option — the user deliberately set this schedule.
 */
@Composable
fun QuarantineOverlay(
    appLabel: String,
    reason: String,
    onDismiss: () -> Unit,
) {
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
            // Lock icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(OverlayAccent.copy(alpha = 0.08f))
                    .border(1.5.dp, OverlayAccent.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Scheduled pause",
                    tint = OverlayAccent,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "App Paused",
                color = OverlayText,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = reason,
                color = OverlayTextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "You chose to set this limit.\nThis is your conscious choice.",
                color = OverlayTextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(24.dp))

            RetroButton(
                text = "OK",
                onClick = onDismiss,
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
