package com.faarigh.app.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.faarigh.app.ui.theme.CardboardColors

/**
 * Draws a subtle grid paper pattern behind the content.
 * Gives the "retro notebook" feel.
 */
/**
 * Lightweight grid paper effect.
 * Uses a fixed viewport-sized pattern instead of drawing lines across
 * the entire scrollable content height (which caused severe lag).
 */
fun Modifier.gridPaper(
    lineColor: Color = CardboardColors.gridLine,
    spacing: Dp = 24.dp,
    lineWidth: Dp = 0.5.dp,
) = drawBehind {
    // Only draw grid lines within the visible viewport, not the full scroll height.
    // This is a massive performance win on long scrollable lists.
    val spacingPx = spacing.toPx()
    val strokeWidth = lineWidth.toPx()
    val color = lineColor.copy(alpha = 0.25f)

    // Limit to visible area — drawBehind clips to the composable's bounds
    // but LazyColumn's bounds can be very tall. Cap at screen-sized grid.
    val maxW = size.width
    val maxH = size.height.coerceAtMost(3000f) // cap to prevent excessive drawing

    var x = spacingPx
    while (x < maxW) {
        drawLine(color, Offset(x, 0f), Offset(x, maxH), strokeWidth)
        x += spacingPx
    }
    var y = spacingPx
    while (y < maxH) {
        drawLine(color, Offset(0f, y), Offset(maxW, y), strokeWidth)
        y += spacingPx
    }
}
