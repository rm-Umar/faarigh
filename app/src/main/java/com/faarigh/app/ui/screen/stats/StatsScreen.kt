package com.faarigh.app.ui.screen.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.faarigh.app.ui.component.BarChartEntry
import com.faarigh.app.ui.component.FaarighBarChart
import com.faarigh.app.ui.component.FaarighDonutChart
import com.faarigh.app.ui.component.FaarighStackedBar
import com.faarigh.app.ui.component.FaarighStatCard
import com.faarigh.app.ui.component.RetroCard
import com.faarigh.app.ui.component.RetroHeading
import com.faarigh.app.ui.component.StackedBarSegment
import com.faarigh.app.ui.component.gridPaper
import com.faarigh.app.ui.theme.CardboardColors
import com.faarigh.app.ui.theme.ModuleColors
import com.faarigh.app.ui.theme.MonospaceFamily
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.runtime.remember

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val appLabelCache = remember { mutableMapOf<String, String>() }
    fun resolveAppName(packageName: String): String {
        return appLabelCache.getOrPut(packageName) {
            try {
                val pm = context.packageManager
                pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
            } catch (_: Exception) {
                packageName.substringAfterLast('.')
                    .replaceFirstChar { it.uppercase() }
            }
        }
    }

    val selectedRange by viewModel.selectedRange.collectAsStateWithLifecycle()
    val screenTime by viewModel.screenTimeForDate.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val weeklyScreenTime by viewModel.weeklyScreenTime.collectAsStateWithLifecycle()
    val topApps by viewModel.topApps.collectAsStateWithLifecycle()
    val unlockCount by viewModel.unlockCount.collectAsStateWithLifecycle()
    val intentionalUseRatio by viewModel.intentionalUseRatio.collectAsStateWithLifecycle()
    val consciousChoicesToday by viewModel.consciousChoicesToday.collectAsStateWithLifecycle()
    val dailyIntentionalTrend by viewModel.dailyIntentionalTrend.collectAsStateWithLifecycle()
    val categoryBreakdown by viewModel.categoryBreakdownUsage.collectAsStateWithLifecycle()
    val interventionSuccessRate by viewModel.interventionSuccessRate.collectAsStateWithLifecycle()
    val appPauseStats by viewModel.appPauseStats.collectAsStateWithLifecycle()
    val nsfwStats by viewModel.nsfwStats.collectAsStateWithLifecycle()
    val shortsStats by viewModel.shortsStats.collectAsStateWithLifecycle()
    val interventionCount by viewModel.interventionCount.collectAsStateWithLifecycle()
    val dnsBlocked by viewModel.dnsBlocked.collectAsStateWithLifecycle()
    val dnsTotal by viewModel.dnsTotal.collectAsStateWithLifecycle()
    val mostPausedApps by viewModel.mostPausedApps.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val expandedSections by viewModel.expandedSections.collectAsStateWithLifecycle()

    // Derive social media apps and time
    val socialMediaApps = topApps.filter { it.category == "Social Media" }
    val socialMediaTime = socialMediaApps.fold(Duration.ZERO) { acc, app -> acc.plus(app.usageTime) }

    // Calculate total screen time for percentage
    val totalScreenTimeMinutes = screenTime.toMinutes().coerceAtLeast(1)

    // Compute yesterday's values for trend comparison
    val dashboardOverview by viewModel.dashboardOverview.collectAsStateWithLifecycle()
    val yesterdayScreenTime = dashboardOverview?.screenTimeYesterday ?: Duration.ZERO

    // Screen time trend
    val screenTimeDelta = screenTime.toMinutes() - yesterdayScreenTime.toMinutes()
    val screenTimeTrendUp = screenTimeDelta > 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .gridPaper()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header ───────────────────────────────────────
        item(key = "header") {
            Column {
                Text(
                    "YOUR PROGRESS",
                    fontFamily = MonospaceFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 3.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "How you're doing",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Your screen time trends, stats, and insights",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (selectedDate != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Showing: ${selectedDate!!.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${selectedDate!!.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${selectedDate!!.dayOfMonth}",
                        fontFamily = MonospaceFamily,
                        fontSize = 12.sp,
                        color = CardboardColors.accentGreen,
                    )
                }
            }
        }

        // ── Time Range Chips ─────────────────────────────
        item(key = "time_range_chips") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeRange.entries.forEach { range ->
                    val isSelected = range == selectedRange
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(4.dp),
                            )
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { viewModel.setTimeRange(range) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            range.label,
                            fontFamily = MonospaceFamily,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════
        // SECTION 1: USAGE
        // ══════════════════════════════════════════════════
        item(key = "section_usage") {
            CollapsibleSection(
                title = "USAGE",
                isExpanded = "usage" in expandedSections,
                onToggle = { viewModel.toggleSection("usage") },
            ) {
                // ── Overview Cards Row ───────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FaarighStatCard(
                        modifier = Modifier.weight(1f),
                        value = formatDuration(screenTime),
                        label = if (screenTimeDelta != 0L) {
                            "${if (screenTimeTrendUp) "\u2191" else "\u2193"} ${kotlin.math.abs(screenTimeDelta)}m vs yesterday"
                        } else "Screen time",
                        accent = CardboardColors.accentGreen,
                    )
                    FaarighStatCard(
                        modifier = Modifier.weight(1f),
                        value = "$unlockCount",
                        label = "Unlocks",
                        accent = CardboardColors.accentAmber,
                    )
                    FaarighStatCard(
                        modifier = Modifier.weight(1f),
                        value = "$consciousChoicesToday",
                        label = "Conscious",
                        accent = CardboardColors.accentPurple,
                    )
                }

                // ── Screen Time Trend (Weekly Bar Chart) ─────────
                if (weeklyScreenTime.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    RetroHeading("SCREEN TIME TREND")
                    Spacer(Modifier.height(8.dp))

                    val selectedDayIndex = if (selectedDate != null) {
                        weeklyScreenTime.indexOfFirst { it.first == selectedDate }
                    } else -1
                    val selectedEntry = if (selectedDayIndex in weeklyScreenTime.indices) weeklyScreenTime[selectedDayIndex] else null
                    val avgMinutes = weeklyScreenTime.map { it.second.toMinutes() }.average().toLong()

                    RetroCard {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Weekly screen time",
                                    fontFamily = MonospaceFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (selectedEntry != null) {
                                    val dayName = selectedEntry.first.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                    Text(
                                        "$dayName: ${formatDuration(selectedEntry.second)}",
                                        fontFamily = MonospaceFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = CardboardColors.accentGreen,
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Avg: ${formatDuration(Duration.ofMinutes(avgMinutes))}",
                                fontFamily = MonospaceFamily,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                            ) {
                                weeklyScreenTime.forEach { (date, _) ->
                                    Text(
                                        date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            FaarighBarChart(
                                entries = weeklyScreenTime.map {
                                    BarChartEntry(it.second.toMinutes().toFloat(), it.first.dayOfWeek.name.take(3))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                barColor = CardboardColors.accentGreen,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f),
                                cornerRadius = 0f,
                                selectedIndex = selectedDayIndex,
                                selectedColor = CardboardColors.accentGreen,
                                onBarTap = { index ->
                                    if (index in weeklyScreenTime.indices) {
                                        val tappedDate = weeklyScreenTime[index].first
                                        viewModel.setSelectedDate(
                                            if (selectedDate == tappedDate) null else tappedDate,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }

                // ── App Usage Ranking ────────────────────────────
                if (topApps.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    RetroHeading("TOP APPS")
                    Spacer(Modifier.height(8.dp))
                    val categoryColors = mapOf(
                        "Social Media" to CardboardColors.accentCoral,
                        "Entertainment" to CardboardColors.accentAmber,
                        "Productivity" to CardboardColors.accentGreen,
                        "Communication" to CardboardColors.accentPurple,
                    )
                    val topMaxMs = (topApps.firstOrNull()?.usageTime?.toMillis() ?: 1L).coerceAtLeast(1L)
                    RetroCard {
                        Column {
                            topApps.take(5).forEachIndexed { index, app ->
                                if (index > 0) Spacer(Modifier.height(10.dp))
                                val accent = categoryColors[app.category] ?: MaterialTheme.colorScheme.outline
                                val fraction = (app.usageTime.toMillis().toFloat() / topMaxMs).coerceIn(0f, 1f)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        app.appName.take(14),
                                        fontFamily = MonospaceFamily,
                                        fontSize = 11.sp,
                                        color = accent,
                                        modifier = Modifier.width(100.dp),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(7.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction)
                                                .height(7.dp)
                                                .background(accent),
                                        )
                                    }
                                    Text(
                                        formatDuration(app.usageTime),
                                        fontFamily = MonospaceFamily,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(44.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Social Media Breakdown ───────────────────────
                if (socialMediaApps.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    RetroHeading("SOCIAL MEDIA")
                    Spacer(Modifier.height(8.dp))
                    val socialMaxMs = (socialMediaApps.firstOrNull()?.usageTime?.toMillis() ?: 1L).coerceAtLeast(1L)
                    RetroCard {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "Total social",
                                    fontFamily = MonospaceFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    formatDuration(socialMediaTime),
                                    fontFamily = MonospaceFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = CardboardColors.accentCoral,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            socialMediaApps.forEachIndexed { index, app ->
                                if (index > 0) Spacer(Modifier.height(8.dp))
                                val fraction = (app.usageTime.toMillis().toFloat() / socialMaxMs).coerceIn(0f, 1f)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        app.appName.take(12),
                                        fontFamily = MonospaceFamily,
                                        fontSize = 11.sp,
                                        color = CardboardColors.accentCoral,
                                        modifier = Modifier.width(90.dp),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(7.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction)
                                                .height(7.dp)
                                                .background(CardboardColors.accentCoral),
                                        )
                                    }
                                    Text(
                                        formatDuration(app.usageTime),
                                        fontFamily = MonospaceFamily,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(44.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Unlock Pattern ───────────────────────────────
                Spacer(Modifier.height(16.dp))
                RetroHeading("UNLOCK PATTERN")
                Spacer(Modifier.height(8.dp))
                RetroCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                "Unlocks today",
                                fontFamily = MonospaceFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Phone unlock count",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "$unlockCount",
                            fontFamily = MonospaceFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = CardboardColors.accentAmber,
                        )
                    }
                }

                // ── Category Breakdown ─────────────────────────────
                if (categoryBreakdown.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    RetroHeading("TIME BY CATEGORY")
                    Spacer(Modifier.height(8.dp))
                    val categoryColors = mapOf(
                        "Social Media" to CardboardColors.accentCoral,
                        "Entertainment" to CardboardColors.accentAmber,
                        "Productivity" to CardboardColors.accentGreen,
                        "Communication" to CardboardColors.accentPurple,
                        "Other" to MaterialTheme.colorScheme.outline,
                    )
                    RetroCard {
                        Column {
                            FaarighStackedBar(
                                segments = categoryBreakdown.map { (cat, dur) ->
                                    StackedBarSegment(
                                        value = dur.toMinutes().toFloat().coerceAtLeast(0.1f),
                                        color = categoryColors[cat] ?: MaterialTheme.colorScheme.outline,
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            categoryBreakdown.forEach { (cat, dur) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(categoryColors[cat] ?: MaterialTheme.colorScheme.outline),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        cat,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        formatDuration(dur),
                                        fontFamily = MonospaceFamily,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Category Usage Circles ─────────────────────────
                if (categoryBreakdown.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    RetroHeading("CATEGORY USAGE")
                    Spacer(Modifier.height(8.dp))
                    val categoryColors2 = mapOf(
                        "Social Media" to CardboardColors.accentCoral,
                        "Entertainment" to CardboardColors.accentAmber,
                        "Productivity" to CardboardColors.accentGreen,
                        "Communication" to CardboardColors.accentPurple,
                    )
                    RetroCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            categoryBreakdown.entries.take(4).forEach { (cat, dur) ->
                                val color = categoryColors2[cat] ?: MaterialTheme.colorScheme.outline
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    FaarighDonutChart(
                                        percent = (dur.toMinutes().toFloat() / totalScreenTimeMinutes * 100f).coerceIn(0f, 100f),
                                        modifier = Modifier.size(64.dp),
                                        strokeWidth = 8.dp,
                                        accentColor = color,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        formatDuration(dur),
                                        fontFamily = MonospaceFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = color,
                                    )
                                    Text(
                                        cat.take(8),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════
        // SECTION 2: CONSCIOUS CHOICES
        // ══════════════════════════════════════════════════
        item(key = "section_conscious") {
            CollapsibleSection(
                title = "CONSCIOUS CHOICES",
                isExpanded = "conscious" in expandedSections,
                onToggle = { viewModel.toggleSection("conscious") },
            ) {
                // ── Intentional-Use Ratio ───────────────────────
                if (intentionalUseRatio != null) {
                    val ratio = intentionalUseRatio!!
                    val total = ratio.blocked + ratio.allowed
                    val percent = if (total > 0) (ratio.blocked.toFloat() / total * 100f) else 0f

                    RetroHeading("INTENTIONAL USE")
                    Spacer(Modifier.height(8.dp))
                    RetroCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FaarighDonutChart(
                                percent = percent,
                                modifier = Modifier.size(100.dp),
                                strokeWidth = 12.dp,
                                accentColor = CardboardColors.accentGreen,
                            )
                            Spacer(Modifier.width(20.dp))
                            Column {
                                Text(
                                    "${"%.0f".format(percent)}% intentional",
                                    fontFamily = MonospaceFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = CardboardColors.accentGreen,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${ratio.blocked} went back / ${ratio.allowed} continued",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (dailyIntentionalTrend.size >= 2) {
                                    Spacer(Modifier.height(4.dp))
                                    val lastWeek = dailyIntentionalTrend.dropLast(1)
                                    val thisWeekBlocked = dailyIntentionalTrend.last().blocked
                                    val lastWeekBlocked = if (lastWeek.isNotEmpty()) lastWeek.last().blocked else 0
                                    val delta = thisWeekBlocked - lastWeekBlocked
                                    if (delta != 0) {
                                        Text(
                                            "${if (delta > 0) "\u2191" else "\u2193"} ${kotlin.math.abs(delta)} from previous day",
                                            fontFamily = MonospaceFamily,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Weekly Comparison ────────────────────────────
                if (dailyIntentionalTrend.size >= 7) {
                    Spacer(Modifier.height(16.dp))
                    RetroHeading("WEEKLY COMPARISON")
                    Spacer(Modifier.height(8.dp))

                    val thisWeek = dailyIntentionalTrend.takeLast(7)
                    val lastWeek = if (dailyIntentionalTrend.size >= 14) {
                        dailyIntentionalTrend.dropLast(7).takeLast(7)
                    } else emptyList()

                    val thisWeekBlocked = thisWeek.sumOf { it.blocked }
                    val lastWeekBlocked = lastWeek.sumOfOrNull { it.blocked } ?: 0
                    val thisWeekAllowed = thisWeek.sumOf { it.allowed }
                    val lastWeekAllowed = lastWeek.sumOfOrNull { it.allowed } ?: 0

                    RetroCard {
                        Column {
                            Text(
                                "This week vs last week",
                                fontFamily = MonospaceFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(12.dp))

                            if (weeklyScreenTime.size >= 7) {
                                val thisWeekScreenMin = weeklyScreenTime.takeLast(7).sumOf { it.second.toMinutes() }
                                val avgThisWeek = thisWeekScreenMin / 7
                                ComparisonRow(
                                    label = "Avg screen time",
                                    value = formatDuration(Duration.ofMinutes(avgThisWeek)),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            if (lastWeek.isNotEmpty()) {
                                val delta = thisWeekBlocked - lastWeekBlocked
                                val arrow = if (delta > 0) "\u2191" else if (delta < 0) "\u2193" else ""
                                ComparisonRow(
                                    label = "Conscious choices",
                                    value = "$thisWeekBlocked ${if (delta != 0) "($arrow ${kotlin.math.abs(delta)})" else ""}",
                                    color = if (delta > 0) CardboardColors.accentGreen else if (delta < 0) CardboardColors.accentAmber else MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(8.dp))

                                val deltaAllowed = thisWeekAllowed - lastWeekAllowed
                                val arrowAllowed = if (deltaAllowed > 0) "\u2191" else if (deltaAllowed < 0) "\u2193" else ""
                                ComparisonRow(
                                    label = "App continuations",
                                    value = "$thisWeekAllowed ${if (deltaAllowed != 0) "($arrowAllowed ${kotlin.math.abs(deltaAllowed)})" else ""}",
                                    color = if (deltaAllowed < 0) CardboardColors.accentGreen else if (deltaAllowed > 0) CardboardColors.accentAmber else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════
        // SECTION 3: CONTENT MODULES
        // ══════════════════════════════════════════════════
        item(key = "section_modules") {
            CollapsibleSection(
                title = "CONTENT MODULES",
                isExpanded = "modules" in expandedSections,
                onToggle = { viewModel.toggleSection("modules") },
            ) {
                // ── Intervention Effectiveness ─────────────────────
                RetroHeading("INTERVENTION EFFECTIVENESS")
                Spacer(Modifier.height(8.dp))
                RetroCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FaarighDonutChart(
                                percent = interventionSuccessRate * 100f,
                                modifier = Modifier.size(80.dp),
                                strokeWidth = 10.dp,
                                accentColor = CardboardColors.accentGreen,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${"%.0f".format(interventionSuccessRate * 100f)}%",
                                fontFamily = MonospaceFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = CardboardColors.accentGreen,
                            )
                            Text(
                                "Overall success",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FaarighDonutChart(
                                percent = appPauseStats.successRate * 100f,
                                modifier = Modifier.size(80.dp),
                                strokeWidth = 10.dp,
                                accentColor = ModuleColors.AppPause,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${"%.0f".format(appPauseStats.successRate * 100f)}%",
                                fontFamily = MonospaceFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ModuleColors.AppPause,
                            )
                            Text(
                                "App Pause success",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // ── Module Overview ────────────────────────────────
                Spacer(Modifier.height(16.dp))
                RetroHeading("MODULE OVERVIEW")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FaarighStatCard(
                        modifier = Modifier.weight(1f),
                        value = "${appPauseStats.shown}",
                        label = "App Pauses\n${"%.0f".format(appPauseStats.successRate * 100f)}% success",
                        accent = ModuleColors.AppPause,
                    )
                    FaarighStatCard(
                        modifier = Modifier.weight(1f),
                        value = "${shortsStats.blocked}",
                        label = "Shorts\ndetected",
                        accent = ModuleColors.ShortsBlocker,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FaarighStatCard(
                        modifier = Modifier.weight(1f),
                        value = "$dnsBlocked",
                        label = "DNS queries\nfiltered",
                        accent = ModuleColors.DnsFilter,
                    )
                    FaarighStatCard(
                        modifier = Modifier.weight(1f),
                        value = "${nsfwStats.blocked}",
                        label = "Content\ndetections",
                        accent = ModuleColors.NsfwShield,
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════
        // SECTION 4: CONTENT INSIGHTS
        // ══════════════════════════════════════════════════
        item(key = "section_content_insights") {
            CollapsibleSection(
                title = "CONTENT INSIGHTS",
                isExpanded = "content_insights" in expandedSections,
                onToggle = { viewModel.toggleSection("content_insights") },
            ) {
                // ── Social Media Time ─────────────────────────────
                RetroHeading("TIME BREAKDOWN")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FaarighStatCard(
                        modifier = Modifier.weight(1f),
                        value = formatDuration(socialMediaTime),
                        label = "Social\nmedia",
                        accent = CardboardColors.accentCoral,
                    )
                    val entertainmentTime = categoryBreakdown["Entertainment"] ?: Duration.ZERO
                    FaarighStatCard(
                        modifier = Modifier.weight(1f),
                        value = formatDuration(entertainmentTime),
                        label = "Entertainment\n& video",
                        accent = CardboardColors.accentAmber,
                    )
                }

                // ── Shorts & Reels ─────────────────────────────────
                if (shortsStats.shown > 0) {
                    Spacer(Modifier.height(16.dp))
                    RetroHeading("REELS & SHORTS")
                    Spacer(Modifier.height(8.dp))
                    RetroCard {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        "${shortsStats.shown} detected",
                                        fontFamily = MonospaceFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = ModuleColors.ShortsBlocker,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${shortsStats.blocked} redirected  •  ${shortsStats.shown - shortsStats.blocked} continued",
                                        fontFamily = MonospaceFamily,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                // Estimated time saved: redirected × avg 3min
                                val timeSavedMin = shortsStats.blocked * 3
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "~${timeSavedMin}m",
                                        fontFamily = MonospaceFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = CardboardColors.accentGreen,
                                    )
                                    Text(
                                        "est. saved",
                                        fontFamily = MonospaceFamily,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (shortsStats.shown > 0) {
                                Spacer(Modifier.height(8.dp))
                                val redirectedFraction = (shortsStats.blocked.toFloat() / shortsStats.shown).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(redirectedFraction)
                                            .height(6.dp)
                                            .background(CardboardColors.accentGreen),
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${"%.0f".format(redirectedFraction * 100f)}% redirect rate",
                                    fontFamily = MonospaceFamily,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // ── NSFW Detection ─────────────────────────────────
                if (nsfwStats.shown > 0) {
                    Spacer(Modifier.height(16.dp))
                    RetroHeading("CONTENT AWARENESS")
                    Spacer(Modifier.height(8.dp))
                    RetroCard {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        "${nsfwStats.shown} detections",
                                        fontFamily = MonospaceFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = ModuleColors.NsfwShield,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${nsfwStats.blocked} went back  •  ${nsfwStats.shown - nsfwStats.blocked} stayed",
                                        fontFamily = MonospaceFamily,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${"%.0f".format(nsfwStats.successRate * 100f)}%",
                                        fontFamily = MonospaceFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = CardboardColors.accentGreen,
                                    )
                                    Text(
                                        "back rate",
                                        fontFamily = MonospaceFamily,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                // Show placeholder if no content module data yet
                if (shortsStats.shown == 0 && nsfwStats.shown == 0) {
                    Spacer(Modifier.height(8.dp))
                    RetroCard {
                        Text(
                            "No content module activity yet for this period.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════
        // SECTION 5: NETWORK
        // ══════════════════════════════════════════════════
        item(key = "section_network") {
            CollapsibleSection(
                title = "NETWORK",
                isExpanded = "network" in expandedSections,
                onToggle = { viewModel.toggleSection("network") },
            ) {
                val filterRate = if (dnsTotal > 0) (dnsBlocked.toFloat() / dnsTotal * 100f) else 0f

                RetroHeading("DNS FILTERING")
                Spacer(Modifier.height(8.dp))
                RetroCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$dnsTotal",
                                fontFamily = MonospaceFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = ModuleColors.DnsFilter,
                            )
                            Text(
                                "Total queries",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$dnsBlocked",
                                fontFamily = MonospaceFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = CardboardColors.accentCoral,
                            )
                            Text(
                                "Filtered",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${"%.1f".format(filterRate)}%",
                                fontFamily = MonospaceFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = CardboardColors.accentPurple,
                            )
                            Text(
                                "Filter rate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════
        // SECTION 5: RECENT ACTIVITY
        // ══════════════════════════════════════════════════
        item(key = "section_recent") {
            CollapsibleSection(
                title = "RECENT ACTIVITY",
                isExpanded = "recent" in expandedSections,
                onToggle = { viewModel.toggleSection("recent") },
            ) {
                // ── Most Intervened Apps ───────────────────────────
                if (mostPausedApps.isNotEmpty()) {
                    RetroHeading("MOST INTERVENED APPS")
                    Spacer(Modifier.height(8.dp))
                    RetroCard {
                        Column {
                            mostPausedApps.forEachIndexed { index, app ->
                                if (index > 0) Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        resolveAppName(app.packageName),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "${app.interventionCount} pauses",
                                            fontFamily = MonospaceFamily,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                "${app.blockedCount} \u2190",
                                                fontFamily = MonospaceFamily,
                                                fontSize = 10.sp,
                                                color = CardboardColors.accentGreen,
                                            )
                                            Text(
                                                "${app.allowedCount} \u2192",
                                                fontFamily = MonospaceFamily,
                                                fontSize = 10.sp,
                                                color = CardboardColors.accentCoral,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                val total = (app.blockedCount + app.allowedCount).coerceAtLeast(1)
                                val blockedFraction = app.blockedCount.toFloat() / total
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape),
                                ) {
                                    if (blockedFraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(blockedFraction.coerceAtLeast(0.01f))
                                                .height(4.dp)
                                                .background(CardboardColors.accentGreen, CircleShape),
                                        )
                                    }
                                    if (blockedFraction < 1f) {
                                        Box(
                                            modifier = Modifier
                                                .weight((1f - blockedFraction).coerceAtLeast(0.01f))
                                                .height(4.dp)
                                                .background(CardboardColors.accentCoral.copy(alpha = 0.5f), CircleShape),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Recent Activity Event Feed ────────────────────
                if (events.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    RetroHeading("RECENT EVENTS")
                    Spacer(Modifier.height(8.dp))
                    RetroCard {
                        Column {
                            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
                            events.take(10).forEachIndexed { index, event ->
                                if (index > 0) Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (event.action == "turned_back") CardboardColors.accentGreen
                                                else CardboardColors.accentCoral,
                                            ),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        resolveAppName(event.packageName),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (event.action == "turned_back") "went back" else "continued",
                                        fontFamily = MonospaceFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (event.action == "turned_back") CardboardColors.accentGreen else CardboardColors.accentCoral,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        Instant.ofEpochMilli(event.timestamp)
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalTime()
                                            .format(timeFormatter),
                                        fontFamily = MonospaceFamily,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Collapsible Section ──────────────────────────────────────
@Composable
private fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (isExpanded) "\u25BE" else "\u25B8",
                fontFamily = MonospaceFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            RetroHeading(title)
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    value: String,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            value,
            fontFamily = MonospaceFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

private fun formatDuration(d: Duration): String {
    val hours = d.toHours()
    val minutes = d.toMinutes() % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun <T> List<T>.sumOfOrNull(selector: (T) -> Int): Int? {
    if (isEmpty()) return null
    return sumOf(selector)
}
