package com.faarigh.app.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faarigh.app.R

private val RetroFont = FontFamily(Font(R.font.jetbrainsmono_regular))
private val RetroFontBold = FontFamily(Font(R.font.jetbrainsmono_bold))
private val DarkBg = Color(0xFF0D0D0D)
private val TextPrimary = Color(0xFFF5E6D3)
private val TextMuted = Color(0xFFA89882)
private val AccentAmber = Color(0xFFD4915E)
private val AccentGreen = Color(0xFF5A8A54)

@Composable
fun CategoryLimitOverlay(
    appLabel: String,
    category: String,
    usageFormatted: String,
    limitFormatted: String,
    onGoBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg.copy(alpha = 0.97f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Time display
            Text(
                text = usageFormatted,
                fontFamily = RetroFontBold,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = AccentAmber,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "of $category today",
                fontFamily = RetroFont,
                fontSize = 14.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Your limit: $limitFormatted",
                fontFamily = RetroFont,
                fontSize = 12.sp,
                color = TextMuted.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = "You wanted to keep $category\nunder $limitFormatted today.",
                fontFamily = RetroFont,
                fontSize = 14.sp,
                color = TextPrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(48.dp))

            // Go back — prominent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentGreen)
                    .clickable(onClick = onGoBack)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Go back",
                    fontFamily = RetroFontBold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(20.dp))

            // Continue anyway — de-emphasized
            Text(
                "Open $appLabel anyway",
                fontFamily = RetroFont,
                fontSize = 12.sp,
                color = TextMuted.copy(alpha = 0.5f),
                modifier = Modifier
                    .clickable(onClick = onContinue)
                    .padding(vertical = 8.dp),
            )
        }
    }
}
