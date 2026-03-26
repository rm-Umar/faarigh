package com.faarigh.app.ui.screen.home

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.faarigh.app.service.accessibility.FaarighAccessibilityService
import com.faarigh.app.ui.component.RetroCard
import com.faarigh.app.ui.component.PixelStar
import com.faarigh.app.ui.component.gridPaper
import com.faarigh.app.ui.theme.CardboardColors
import com.faarigh.app.ui.theme.ModuleColors
import com.faarigh.app.ui.theme.MonospaceFamily
import java.time.Duration

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToModuleDetail: (String) -> Unit = {},
    onNavigateToApps: () -> Unit = {},
    onNavigateToToolkit: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
) {
    val consciousChoices by viewModel.consciousChoicesToday.collectAsStateWithLifecycle()
    val screenTime by viewModel.screenTimeToday.collectAsStateWithLifecycle()
    val screenTimeYesterday by viewModel.screenTimeYesterday.collectAsStateWithLifecycle()
    val unlocks by viewModel.unlockCount.collectAsStateWithLifecycle()
    val interventions by viewModel.interventionCountToday.collectAsStateWithLifecycle()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsStateWithLifecycle()
    val monitoredApps by viewModel.monitoredApps.collectAsStateWithLifecycle()
    val appPauseEnabled by viewModel.appPauseEnabled.collectAsStateWithLifecycle()
    val shortsEnabled by viewModel.shortsBlockerEnabled.collectAsStateWithLifecycle()
    val nsfwEnabled by viewModel.nsfwEnabled.collectAsStateWithLifecycle()
    val dnsEnabled by viewModel.dnsFilterEnabled.collectAsStateWithLifecycle()
    val weeklyScreenTime by viewModel.weeklyScreenTime.collectAsStateWithLifecycle()
    val topApps by viewModel.topAppsToday.collectAsStateWithLifecycle()

    val anyModuleEnabled = appPauseEnabled || shortsEnabled || nsfwEnabled || dnsEnabled
    val hasApps = monitoredApps.isNotEmpty()
    val hasData = screenTime > Duration.ZERO || consciousChoices > 0 || unlocks > 0

    // Determine setup state
    val isSetupComplete = anyModuleEnabled && hasApps
    val context = LocalContext.current
    val setupDismissed = remember {
        context.getSharedPreferences("faarigh_prefs", Context.MODE_PRIVATE)
            .getBoolean("setup_dismissed", false)
    }
    val showSetup = !isSetupComplete && !setupDismissed

    val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .gridPaper()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header ───────────────────────────────────────────
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "FAARIGH",
                            fontFamily = MonospaceFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 3.sp,
                        )
                        Spacer(Modifier.width(6.dp))
                        PixelStar(size = 14.dp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (showSetup) "$greeting!" else "Your day so far",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                // App icon shown in header
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardboardColors.accentGreen.copy(alpha = 0.1f))
                        .border(1.dp, CardboardColors.accentGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = com.faarigh.app.R.drawable.ic_launcher_foreground),
                        contentDescription = "Faarigh",
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }

        // ── Permissions Banner ────────────────────────────────
        item(key = "permissions") {
            PermissionsBanner(viewModel)
        }

        // ── Setup Checklist (fresh install) ──────────────────
        if (showSetup) {
            item(key = "setup") {
                RetroCard {
                    Column {
                        Text(
                            "Get started",
                            fontFamily = MonospaceFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Set up Faarigh in a few steps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))

                        SetupStep(
                            text = "Choose apps to be mindful about",
                            isDone = hasApps,
                            onClick = onNavigateToApps,
                        )
                        Spacer(Modifier.height(10.dp))
                        SetupStep(
                            text = "Enable your first module",
                            isDone = anyModuleEnabled,
                            onClick = onNavigateToToolkit,
                        )
                        Spacer(Modifier.height(10.dp))
                        SetupStep(
                            text = "Use your phone for a day",
                            isDone = hasData,
                            onClick = null,
                            subtitle = "Stats need a bit of data to show up",
                        )
                    }
                }
            }
        }

        // ── Screen Time Hero Card with Weekly Chart ──────────
        item(key = "screen_time_card") {
            RetroCard(onClick = onNavigateToProgress) {
                Column {
                    // Top row: today's time + vs yesterday
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                "TODAY",
                                fontFamily = MonospaceFamily,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 2.sp,
                            )
                            Text(
                                formatDuration(screenTime),
                                fontFamily = MonospaceFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = CardboardColors.accentGreen,
                            )
                        }
                        // Vs yesterday delta
                        if (screenTimeYesterday > Duration.ZERO) {
                            val diff = screenTime.minus(screenTimeYesterday)
                            val isLess = diff.isNegative
                            val diffStr = formatDuration(if (isLess) diff.negated() else diff)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "vs yesterday",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "${if (isLess) "-" else "+"}$diffStr",
                                    fontFamily = MonospaceFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLess) CardboardColors.accentGreen else CardboardColors.accentCoral,
                                )
                            }
                        }
                    }

                    if (weeklyScreenTime.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        WeeklyBarChart(data = weeklyScreenTime)
                    }

                    // Mini stat row below chart
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        MiniStat("$unlocks", "unlocks", CardboardColors.accentAmber)
                        MiniStat("$consciousChoices", "choices", CardboardColors.accentPurple)
                        MiniStat("$interventions", "check-ins", CardboardColors.accentCoral)
                    }
                }
            }
        }

        // ── Top Apps (when data available) ───────────────────
        if (topApps.isNotEmpty()) {
            item(key = "top_apps") {
                RetroCard(onClick = onNavigateToProgress) {
                    Column {
                        Text(
                            "TOP APPS TODAY",
                            fontFamily = MonospaceFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        val maxTime: Long = (topApps.maxOfOrNull { it.usageTime.toMillis() } ?: 0L).coerceAtLeast(1L)
                        topApps.take(4).forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    app.appName.take(14),
                                    fontFamily = MonospaceFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(100.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                // Retro bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(
                                                fraction = (app.usageTime.toMillis().toFloat() / maxTime).coerceIn(0f, 1f),
                                            )
                                            .background(CardboardColors.accentGreen.copy(alpha = 0.7f)),
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    formatDuration(app.usageTime),
                                    fontFamily = MonospaceFamily,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(40.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Time by Category ─────────────────────────────────
        if (categoryBreakdown.isNotEmpty()) {
            item(key = "category_breakdown") {
                RetroCard(onClick = onNavigateToProgress) {
                    Column {
                        Text(
                            "TIME BY CATEGORY",
                            fontFamily = MonospaceFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        val categoryColors = mapOf(
                            "Social Media" to CardboardColors.accentCoral,
                            "Entertainment" to CardboardColors.accentAmber,
                            "Productivity" to CardboardColors.accentGreen,
                            "Communication" to CardboardColors.accentPurple,
                        )
                        val catMaxMs = categoryBreakdown.values.maxOfOrNull { it.toMillis() }?.coerceAtLeast(1L) ?: 1L
                        categoryBreakdown.entries.sortedByDescending { it.value }.take(4).forEach { (cat, dur) ->
                            val accent = categoryColors[cat] ?: MaterialTheme.colorScheme.outline
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    cat.take(12),
                                    fontFamily = MonospaceFamily,
                                    fontSize = 10.sp,
                                    color = accent,
                                    modifier = Modifier.width(90.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((dur.toMillis().toFloat() / catMaxMs).coerceIn(0f, 1f))
                                            .background(accent.copy(alpha = 0.7f)),
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    formatDuration(dur),
                                    fontFamily = MonospaceFamily,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(40.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Module Status Grid ───────────────────────────────
        item(key = "module_grid") {
            Column {
                Text(
                    "YOUR MODULES",
                    fontFamily = MonospaceFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(8.dp))
                // 2x2 grid — always show all 4, active ones highlighted
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ModuleTile(
                        modifier = Modifier.weight(1f),
                        iconRes = com.faarigh.app.R.drawable.ic_module_app_pause,
                        name = "App Pause",
                        accent = ModuleColors.AppPause,
                        active = appPauseEnabled,
                        onClick = onNavigateToToolkit,
                    )
                    ModuleTile(
                        modifier = Modifier.weight(1f),
                        iconRes = com.faarigh.app.R.drawable.ic_module_shorts_blocker,
                        name = "Shorts",
                        accent = ModuleColors.ShortsBlocker,
                        active = shortsEnabled,
                        onClick = onNavigateToToolkit,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ModuleTile(
                        modifier = Modifier.weight(1f),
                        iconRes = com.faarigh.app.R.drawable.ic_module_content_awareness,
                        name = "Content",
                        accent = ModuleColors.NsfwShield,
                        active = nsfwEnabled,
                        onClick = onNavigateToToolkit,
                    )
                    ModuleTile(
                        modifier = Modifier.weight(1f),
                        iconRes = com.faarigh.app.R.drawable.ic_module_dns_filter,
                        name = "DNS Filter",
                        accent = ModuleColors.DnsFilter,
                        active = dnsEnabled,
                        onClick = onNavigateToToolkit,
                    )
                }
            }
        }

        // ── View Progress (only after 3+ days worth of data) ─
        if (hasData) {
            item(key = "progress_link") {
                RetroCard(onClick = onNavigateToProgress) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                "See your progress",
                                fontFamily = MonospaceFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Screen time, trends, and insights",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "\u203A",
                            fontFamily = MonospaceFamily,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Local components
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PermissionsBanner(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var needsAccessibility by remember { mutableStateOf(FaarighAccessibilityService.instance == null) }
    var needsUsageStats by remember { mutableStateOf(!viewModel.hasUsagePermission) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                needsAccessibility = FaarighAccessibilityService.instance == null
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
                }
                needsUsageStats = mode != AppOpsManager.MODE_ALLOWED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (needsAccessibility || needsUsageStats) {
        RetroCard(
            surfaceColor = CardboardColors.accentAmber.copy(alpha = 0.06f),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Warning, contentDescription = null, tint = CardboardColors.accentAmber, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Missing permissions", fontFamily = MonospaceFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap to enable required permissions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                if (needsAccessibility) {
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, CardboardColors.accentAmber.copy(alpha = 0.4f)),
                    ) {
                        Text("Enable Accessibility Service", fontFamily = MonospaceFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (needsUsageStats) {
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, CardboardColors.accentAmber.copy(alpha = 0.4f)),
                    ) {
                        Text("Enable Usage Stats", fontFamily = MonospaceFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStep(
    text: String,
    isDone: Boolean,
    onClick: (() -> Unit)?,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null && !isDone) Modifier.clickable { onClick() } else Modifier)
            .clip(RoundedCornerShape(4.dp))
            .border(
                1.dp,
                if (isDone) CardboardColors.accentGreen.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(4.dp),
            )
            .background(if (isDone) CardboardColors.accentGreen.copy(alpha = 0.04f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(
                    1.5.dp,
                    if (isDone) CardboardColors.accentGreen else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(4.dp),
                )
                .background(if (isDone) CardboardColors.accentGreen.copy(alpha = 0.15f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = "Done",
                    tint = CardboardColors.accentGreen,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text,
                fontFamily = MonospaceFamily,
                fontSize = 12.sp,
                fontWeight = if (isDone) FontWeight.Normal else FontWeight.Bold,
                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onClick != null && !isDone) {
            Spacer(Modifier.weight(1f))
            Text(
                "\u203A",
                fontFamily = MonospaceFamily,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModuleTile(
    modifier: Modifier = Modifier,
    iconRes: Int,
    name: String,
    accent: Color,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(
                1.dp,
                if (active) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(4.dp),
            )
            .background(if (active) accent.copy(alpha = 0.08f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (active) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                    if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                ),
            )
        }
        Text(
            name,
            fontFamily = MonospaceFamily,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun RetroStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    accent: Color,
) {
    RetroCard(modifier = modifier) {
        Column {
            Text(
                text = value,
                fontFamily = MonospaceFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = accent,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDuration(d: Duration): String {
    val hours = d.toHours()
    val minutes = d.toMinutes() % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
private fun WeeklyBarChart(
    data: List<Pair<java.time.LocalDate, Duration>>,
    modifier: Modifier = Modifier,
) {
    val barColor = CardboardColors.accentGreen
    val todayColor = CardboardColors.accentGreen
    val mutedColor = CardboardColors.accentGreen.copy(alpha = 0.35f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val today = java.time.LocalDate.now()
    val maxMs: Long = (data.maxOfOrNull { it.second.toMillis() } ?: 0L).coerceAtLeast(1L)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            val count = data.size.coerceAtLeast(1)
            val gap = 4.dp.toPx()
            val barWidth = (size.width - gap * (count - 1)) / count
            data.forEachIndexed { index, (date, dur) ->
                val fraction = (dur.toMillis().toFloat() / maxMs).coerceIn(0f, 1f)
                val barHeight = fraction * size.height
                val x = index * (barWidth + gap)
                val y = size.height - barHeight
                val color = if (date == today) todayColor else mutedColor
                drawRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // Day labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            val dayFmt = java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.getDefault())
            data.forEach { (date, _) ->
                Text(
                    text = date.format(dayFmt).take(1).uppercase(),
                    fontFamily = MonospaceFamily,
                    fontSize = 9.sp,
                    color = if (date == today) CardboardColors.accentGreen else labelColor.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun MiniStat(
    value: String,
    label: String,
    accent: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontFamily = MonospaceFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = accent,
        )
        Text(
            label,
            fontFamily = MonospaceFamily,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
