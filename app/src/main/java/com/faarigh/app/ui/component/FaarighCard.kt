package com.faarigh.app.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.faarigh.app.ui.theme.CardboardColors
import com.faarigh.app.ui.theme.MonospaceFamily

/**
 * Faarigh Cardboard Retro — Card Components
 *
 * FaarighCard: Outlined card with thin border (retro paper aesthetic)
 * FaarighDashedCard: Dashed border for empty states
 * FaarighHeroCard: Outlined with accent-tinted background
 * FaarighStatCard: Compact stat with number + label + sparkline
 * FaarighDonutChart: Animated donut/ring chart
 * FaarighSparkLine: Mini inline line chart
 */

@Composable
fun FaarighCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable () -> Unit,
) {
    OutlinedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun FaarighDashedCard(
    modifier: Modifier = Modifier,
    dashColor: Color = CardboardColors.dashedBorder,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val cornerRadius = 24.dp

    Box(
        modifier = modifier
            .drawBehind {
                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(8.dp.toPx(), 4.dp.toPx()),
                        0f,
                    ),
                )
                drawRoundRect(
                    color = dashColor,
                    style = stroke,
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                )
            }
            .padding(20.dp),
    ) {
        content()
    }
}

@Composable
fun FaarighHeroCard(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primaryContainer,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable () -> Unit,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = accentColor),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.padding(24.dp)) {
            content()
        }
    }
}

@Composable
fun FaarighStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    sparklineData: List<Float>? = null,
) {
    RetroCard(modifier = modifier) {
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = accent,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (sparklineData != null && sparklineData.size >= 2) {
                Spacer(modifier = Modifier.height(8.dp))
                FaarighSparkLine(
                    data = sparklineData,
                    color = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                )
            }
        }
    }
}

@Composable
fun FaarighDonutChart(
    percent: Float,
    modifier: Modifier = Modifier.size(100.dp),
    strokeWidth: Dp = 12.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percent,
        animationSpec = tween(1000),
        label = "donut",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (animatedPercent > 0) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = animatedPercent * 3.6f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${"%.0f".format(animatedPercent)}%",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun FaarighSparkLine(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    fillAlpha: Float = 0.12f,
) {
    if (data.size < 2) return

    val maxVal = data.max().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val stepX = size.width / (data.size - 1)
        val points = data.mapIndexed { i, v ->
            Offset(i * stepX, size.height - (v / maxVal * size.height * 0.85f))
        }

        val fillPath = Path().apply {
            moveTo(0f, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(color.copy(alpha = fillAlpha), Color.Transparent),
            ),
        )

        for (i in 0 until points.size - 1) {
            drawLine(
                color = color,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        val last = points.last()
        drawCircle(color = color, radius = 3.dp.toPx(), center = last)
    }
}

@Composable
fun FaarighSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        action?.invoke()
    }
}

@Composable
fun FaarighModuleCard(
    modifier: Modifier = Modifier,
    title: String,
    stat: String,
    accentColor: Color,
    icon: @Composable () -> Unit = {},
    illustration: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    OutlinedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (illustration != null) {
                illustration()
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                icon()
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stat,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
            )
        }
    }
}

@Composable
fun StatusDot(
    isActive: Boolean,
    modifier: Modifier = Modifier.size(8.dp),
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (isActive) CardboardColors.accentGreen
                else MaterialTheme.colorScheme.outline,
            ),
    )
}
