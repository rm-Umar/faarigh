package com.faarigh.app.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faarigh.app.ui.theme.MonospaceFamily

// ═══════════════════════════════════════════════════════════════
// Retro Bumped Components — Cardboard Retro Design System
// Theme-aware: dark shadow on light mode, light shadow on dark mode
// Thick 5-6dp offset for prominent 3D raised effect
// ═══════════════════════════════════════════════════════════════

private val ButtonShape = RoundedCornerShape(4.dp)
private val CardShape = RoundedCornerShape(4.dp)
private val TagShape = RoundedCornerShape(3.dp)

/** Shadow color adapts to theme — dark on cream, cream on dark */
@Composable
private fun shadowColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFFD9D0C4) else Color(0xFF1A1A1A)

/** Border color adapts to theme */
@Composable
private fun borderColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFFD9D0C4).copy(alpha = 0.35f) else Color(0xFF1A1A1A).copy(alpha = 0.55f)

// ── RetroButton (filled) ─────────────────────────────────────

@Composable
fun RetroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    shadowOffsetX: Dp = 4.dp,
    shadowOffsetY: Dp = 5.dp,
) {
    var isPressed by remember { mutableStateOf(false) }
    val shadow = shadowColor()
    val border = borderColor()

    val animX by animateDpAsState(
        targetValue = if (isPressed && enabled) 1.dp else shadowOffsetX,
        animationSpec = tween(60), label = "sx",
    )
    val animY by animateDpAsState(
        targetValue = if (isPressed && enabled) 1.dp else shadowOffsetY,
        animationSpec = tween(60), label = "sy",
    )

    Box(modifier = modifier.padding(bottom = shadowOffsetY, end = shadowOffsetX)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = animX, y = animY)
                .clip(ButtonShape)
                .background(if (enabled) shadow else shadow.copy(alpha = 0.3f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ButtonShape)
                .background(if (enabled) color else color.copy(alpha = 0.5f))
                .border(2.dp, border, ButtonShape)
                .then(
                    if (enabled) Modifier.pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                            onClick()
                        })
                    } else Modifier,
                )
                .padding(vertical = 16.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = if (enabled) textColor else textColor.copy(alpha = 0.6f),
                fontFamily = MonospaceFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── RetroOutlinedButton ──────────────────────────────────────

@Composable
fun RetroOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    shadowOffsetX: Dp = 4.dp,
    shadowOffsetY: Dp = 5.dp,
) {
    var isPressed by remember { mutableStateOf(false) }
    val shadow = shadowColor()

    val animX by animateDpAsState(
        targetValue = if (isPressed) 1.dp else shadowOffsetX,
        animationSpec = tween(60), label = "sx",
    )
    val animY by animateDpAsState(
        targetValue = if (isPressed) 1.dp else shadowOffsetY,
        animationSpec = tween(60), label = "sy",
    )

    Box(modifier = modifier.padding(bottom = shadowOffsetY, end = shadowOffsetX)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = animX, y = animY)
                .clip(ButtonShape)
                .background(shadow),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ButtonShape)
                .background(surfaceColor)
                .border(2.dp, shadow.copy(alpha = 0.5f), ButtonShape)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    })
                }
                .padding(vertical = 16.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = textColor,
                fontFamily = MonospaceFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── RetroCard ────────────────────────────────────────────────

@Composable
fun RetroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    shadowOffsetX: Dp = 3.dp,
    shadowOffsetY: Dp = 3.dp,
    content: @Composable () -> Unit,
) {
    val shadow = shadowColor()
    val border = borderColor()
    val oxPx = with(LocalDensity.current) { shadowOffsetX.toPx() }
    val oyPx = with(LocalDensity.current) { shadowOffsetY.toPx() }
    val cornerPx = with(LocalDensity.current) { 4.dp.toPx() }

    // Single Box with drawBehind shadow — avoids extra composable layer
    Box(
        modifier = modifier
            .padding(bottom = shadowOffsetY, end = shadowOffsetX)
            .fillMaxWidth()
            .drawBehind {
                // Draw shadow rectangle offset behind
                drawRoundRect(
                    color = shadow.copy(alpha = 0.85f),
                    topLeft = Offset(oxPx, oyPx),
                    size = size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                )
            }
            .clip(CardShape)
            .background(surfaceColor)
            .border(1.5.dp, border, CardShape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .padding(16.dp),
    ) {
        content()
    }
}

// ── RetroHeading ─────────────────────────────────────────────

@Composable
fun RetroHeading(
    text: String,
    modifier: Modifier = Modifier,
) {
    val shadow = shadowColor()
    val border = borderColor()

    Box(modifier = modifier.padding(bottom = 3.dp, end = 3.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.5.dp, y = 3.dp)
                .clip(TagShape)
                .background(shadow.copy(alpha = 0.75f)),
        )
        Box(
            modifier = Modifier
                .clip(TagShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.5.dp, border, TagShape)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = MonospaceFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
            )
        }
    }
}

// ── RetroToggle ─────────────────────────────────────────────

@Composable
fun RetroToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    checkedColor: Color = MaterialTheme.colorScheme.primary,
) {
    val shadow = shadowColor()
    val trackColor = if (checked) checkedColor else MaterialTheme.colorScheme.surfaceContainerHigh
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(150), label = "thumb",
    )

    Box(
        modifier = modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.5.dp, shadow.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (checked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                .border(1.dp, shadow.copy(alpha = 0.3f), RoundedCornerShape(3.dp)),
        )
    }
}
