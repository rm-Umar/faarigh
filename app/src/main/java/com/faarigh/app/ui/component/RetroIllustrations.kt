package com.faarigh.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════
// Pixel/8-bit Style Decorative Elements — Cardboard Retro
// Simple Canvas drawings using drawRect for pixel-art style icons
// ═══════════════════════════════════════════════════════════════

/**
 * A simple shield icon drawn with Canvas using pixel/blocky rectangles (8-bit style).
 */
@Composable
fun PixelShield(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier.size(size)) {
        val px = this.size.width / 10f // pixel unit

        // Shield outline — blocky shape
        // Top row (wide)
        drawRect(color, Offset(2 * px, 0f), Size(6 * px, px))
        // Second row
        drawRect(color, Offset(1 * px, 1 * px), Size(8 * px, px))
        // Body rows
        for (row in 2..5) {
            drawRect(color, Offset(1 * px, row * px), Size(8 * px, px))
        }
        // Tapering rows
        drawRect(color, Offset(2 * px, 6 * px), Size(6 * px, px))
        drawRect(color, Offset(3 * px, 7 * px), Size(4 * px, px))
        drawRect(color, Offset(4 * px, 8 * px), Size(2 * px, px))

        // Inner cutout (lighter)
        val inner = color.copy(alpha = 0.3f)
        for (row in 2..4) {
            drawRect(inner, Offset(3 * px, row * px), Size(4 * px, px))
        }
        drawRect(inner, Offset(4 * px, 5 * px), Size(2 * px, px))

        // Checkmark inside shield
        val check = Color.White.copy(alpha = 0.9f)
        drawRect(check, Offset(3.5f * px, 3.5f * px), Size(px, px))
        drawRect(check, Offset(4.5f * px, 4.5f * px), Size(px, px))
        drawRect(check, Offset(5.5f * px, 3.5f * px), Size(px, px))
        drawRect(check, Offset(6.5f * px, 2.5f * px), Size(px, px))
    }
}

/**
 * A simple phone icon in pixel style.
 */
@Composable
fun PixelPhone(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier.size(size)) {
        val px = this.size.width / 10f

        // Phone body
        for (row in 0..9) {
            drawRect(color, Offset(3 * px, row * px), Size(4 * px, px))
        }

        // Screen (lighter inner area)
        val screen = color.copy(alpha = 0.25f)
        for (row in 2..6) {
            drawRect(screen, Offset(4 * px, row * px), Size(2 * px, px))
        }

        // Top speaker dot
        drawRect(Color.White.copy(alpha = 0.6f), Offset(4.5f * px, 0.5f * px), Size(px, px * 0.5f))

        // Bottom button
        drawRect(Color.White.copy(alpha = 0.5f), Offset(4.5f * px, 8 * px), Size(px, px))
    }
}

/**
 * A pixel heart icon.
 */
@Composable
fun PixelHeart(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.error,
) {
    Canvas(modifier = modifier.size(size)) {
        val px = this.size.width / 10f

        // Classic pixel heart shape
        // Row 0: two bumps
        drawRect(color, Offset(1 * px, 1 * px), Size(2 * px, px))
        drawRect(color, Offset(5 * px, 1 * px), Size(2 * px, px))
        // Row 1: expanded
        drawRect(color, Offset(0f, 2 * px), Size(4 * px, px))
        drawRect(color, Offset(4 * px, 2 * px), Size(4 * px, px))
        // Row 2: full width
        drawRect(color, Offset(0f, 3 * px), Size(8 * px, px))
        // Row 3
        drawRect(color, Offset(1 * px, 4 * px), Size(6 * px, px))
        // Row 4
        drawRect(color, Offset(2 * px, 5 * px), Size(4 * px, px))
        // Row 5: tip
        drawRect(color, Offset(3 * px, 6 * px), Size(2 * px, px))

        // Highlight
        val hi = Color.White.copy(alpha = 0.35f)
        drawRect(hi, Offset(1 * px, 2 * px), Size(px, px))
        drawRect(hi, Offset(2 * px, 2 * px), Size(px, px * 0.5f))
    }
}

/**
 * A pixel star icon.
 */
@Composable
fun PixelStar(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.tertiary,
) {
    Canvas(modifier = modifier.size(size)) {
        val px = this.size.width / 10f

        // Star shape — 5-point pixel approximation
        // Top point
        drawRect(color, Offset(4 * px, 0f), Size(2 * px, px))
        drawRect(color, Offset(4 * px, 1 * px), Size(2 * px, px))
        // Middle wide bar
        drawRect(color, Offset(0f, 2 * px), Size(10 * px, px))
        drawRect(color, Offset(1 * px, 3 * px), Size(8 * px, px))
        // Inner body
        drawRect(color, Offset(2 * px, 4 * px), Size(6 * px, px))
        drawRect(color, Offset(3 * px, 5 * px), Size(4 * px, px))
        // Two legs
        drawRect(color, Offset(2 * px, 6 * px), Size(2 * px, px))
        drawRect(color, Offset(6 * px, 6 * px), Size(2 * px, px))
        drawRect(color, Offset(1 * px, 7 * px), Size(2 * px, px))
        drawRect(color, Offset(7 * px, 7 * px), Size(2 * px, px))

        // Center highlight
        val hi = Color.White.copy(alpha = 0.3f)
        drawRect(hi, Offset(4 * px, 2 * px), Size(2 * px, px))
        drawRect(hi, Offset(4 * px, 3 * px), Size(2 * px, px))
    }
}

/**
 * A dashed divider line made of small squares instead of a continuous line.
 * Pixel-art style separator.
 */
@Composable
fun PixelDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    squareSize: Dp = 3.dp,
    gap: Dp = 5.dp,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(squareSize),
    ) {
        val sqPx = squareSize.toPx()
        val gapPx = gap.toPx()
        val step = sqPx + gapPx
        var x = 0f

        while (x < size.width) {
            drawRect(
                color = color,
                topLeft = Offset(x, 0f),
                size = Size(sqPx, sqPx),
            )
            x += step
        }
    }
}
