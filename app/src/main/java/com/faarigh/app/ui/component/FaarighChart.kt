package com.faarigh.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Faarigh Design System — Chart Components
 *
 * FaarighAreaChart: Gradient-filled area chart with smooth curves
 * FaarighBarChart: Vertical bar chart for hourly/categorical data
 * FaarighStackedBarChart: Stacked bars for category breakdowns
 */

// ── Area Chart ────────────────────────────────────────────────

data class AreaChartSeries(
    val data: List<Float>,
    val color: Color,
    val fillAlpha: Float = 0.20f,
)

@Composable
fun FaarighAreaChart(
    series: List<AreaChartSeries>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(160.dp),
    showDots: Boolean = false,
) {
    if (series.all { it.data.size < 2 }) return

    val globalMax = series.maxOf { s -> s.data.maxOrNull() ?: 0f }.coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        series.forEach { s ->
            drawAreaSeries(s, globalMax, showDots)
        }
    }
}

private fun DrawScope.drawAreaSeries(
    series: AreaChartSeries,
    maxVal: Float,
    showDots: Boolean,
) {
    val data = series.data
    if (data.size < 2) return

    val stepX = size.width / (data.size - 1)
    val points = data.mapIndexed { i, v ->
        Offset(i * stepX, size.height - (v / maxVal * size.height * 0.9f))
    }

    // Smooth path using cubic bezier
    val linePath = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 0 until points.size - 1) {
            val cp1x = (points[i].x + points[i + 1].x) / 2
            cubicTo(
                cp1x, points[i].y,
                cp1x, points[i + 1].y,
                points[i + 1].x, points[i + 1].y,
            )
        }
    }

    // Fill area
    val fillPath = Path().apply {
        addPath(linePath)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                series.color.copy(alpha = series.fillAlpha),
                Color.Transparent,
            ),
        ),
    )

    // Stroke line
    drawPath(
        path = linePath,
        color = series.color,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
    )

    // Dots
    if (showDots) {
        points.forEach { pt ->
            drawCircle(color = series.color, radius = 3.dp.toPx(), center = pt)
        }
    }
}

// ── Bar Chart ──────────────────────────────────────────────────

data class BarChartEntry(
    val value: Float,
    val label: String = "",
)

@Composable
fun FaarighBarChart(
    entries: List<BarChartEntry>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(120.dp),
    barColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    cornerRadius: Float = 6f,
    selectedIndex: Int = -1,
    selectedColor: Color = barColor,
    onBarTap: ((Int) -> Unit)? = null,
) {
    if (entries.isEmpty()) return
    val maxVal = entries.maxOf { it.value }.coerceAtLeast(1f)

    val tapModifier = if (onBarTap != null) {
        modifier.pointerInput(entries.size) {
            detectTapGestures { offset ->
                val barCount = entries.size
                val gap = 4.dp.toPx()
                val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)
                val tappedIndex = (offset.x / (barWidth + gap)).toInt().coerceIn(0, barCount - 1)
                onBarTap(tappedIndex)
            }
        }
    } else modifier

    Canvas(modifier = tapModifier) {
        val barCount = entries.size
        val gap = 4.dp.toPx()
        val totalGap = gap * (barCount - 1)
        val barWidth = ((size.width - totalGap) / barCount).coerceAtLeast(2f)

        entries.forEachIndexed { i, entry ->
            val x = i * (barWidth + gap)
            val barHeight = (entry.value / maxVal) * size.height * 0.9f
            val isSelected = i == selectedIndex

            // Track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
            )

            // Bar
            if (entry.value > 0) {
                drawRoundRect(
                    color = if (isSelected) selectedColor else barColor.copy(alpha = if (selectedIndex >= 0) 0.4f else 1f),
                    topLeft = Offset(x, size.height - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
                )
            }
        }
    }
}

// ── Stacked Bar Chart ─────────────────────────────────────────

data class StackedBarSegment(
    val value: Float,
    val color: Color,
)

@Composable
fun FaarighStackedBar(
    segments: List<StackedBarSegment>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(12.dp),
) {
    if (segments.isEmpty()) return
    val total = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        var x = 0f
        val cornerR = size.height / 2
        val gap = 2.dp.toPx()

        segments.forEachIndexed { i, seg ->
            val segWidth = (seg.value / total) * (size.width - gap * (segments.size - 1))
            if (segWidth > 0) {
                drawRoundRect(
                    color = seg.color,
                    topLeft = Offset(x, 0f),
                    size = androidx.compose.ui.geometry.Size(segWidth, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR),
                )
                x += segWidth + gap
            }
        }
    }
}
