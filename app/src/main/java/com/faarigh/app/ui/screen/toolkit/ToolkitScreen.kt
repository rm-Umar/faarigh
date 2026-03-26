package com.faarigh.app.ui.screen.toolkit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.faarigh.app.ui.component.ModuleOnboardingSheet
import com.faarigh.app.ui.component.RetroCard
import com.faarigh.app.ui.component.RetroToggle
import com.faarigh.app.ui.component.gridPaper
import com.faarigh.app.ui.theme.CardboardColors
import com.faarigh.app.ui.theme.ModuleColors
import com.faarigh.app.ui.theme.MonospaceFamily

/** Tooltip explanations for each module — plain English, no jargon. */
private val moduleTooltips = mapOf(
    "app_pause" to "When you open an app you're monitoring, you'll get a moment to check in with yourself. It's a pause, not a block — you always have the final choice.",
    "shorts_blocker" to "Detects short-form video feeds (YouTube Shorts, Instagram Reels) and navigates you back. You can always return if you choose.",
    "nsfw_detection" to "Uses an on-device AI model to flag explicit visual content. Everything stays on your phone. You decide what to do.",
    "dns_filter" to "Filters DNS requests locally to block ads, trackers, and telemetry. Works through a local VPN — your traffic stays on your device.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolkitScreen(
    onNavigateToModuleDetail: (String) -> Unit = {},
    onNavigateToApps: () -> Unit = {},
    onNavigateToContentFilter: () -> Unit = {},
    onNavigateToCategoryLimits: () -> Unit = {},
    onNavigateToLearn: () -> Unit = {},
    onNavigateToQuarantine: () -> Unit = {},
    onRequestVpnConsent: () -> Unit = {},
    viewModel: ToolkitViewModel = hiltViewModel(),
) {
    val modules by viewModel.modules.collectAsStateWithLifecycle()
    val pendingEducation by viewModel.pendingEducation.collectAsStateWithLifecycle()
    val monitoredApps by viewModel.monitoredApps.collectAsStateWithLifecycle()
    val expandedTooltips by viewModel.expandedTooltips.collectAsStateWithLifecycle()
    val vpnRunning by viewModel.vpnRunning.collectAsStateWithLifecycle()
    val dnsTotal by viewModel.dnsTotal.collectAsStateWithLifecycle()
    val dnsBlocked by viewModel.dnsBlocked.collectAsStateWithLifecycle()

    // Education bottom sheet — uses same cards as ModuleDetailScreen
    pendingEducation?.let { moduleId ->
        ModuleOnboardingSheet(
            moduleName = viewModel.getModuleName(moduleId),
            cards = viewModel.getModuleCards(moduleId),
            onDismiss = { viewModel.dismissEducation() },
            onEnable = { viewModel.confirmEnableAfterEducation(moduleId) },
        )
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
            Column {
                Text(
                    "YOUR TOOLKIT",
                    fontFamily = MonospaceFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 3.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "All your tools in one place",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                val activeCount = modules.count { it.isEnabled }
                Text(
                    "$activeCount of ${modules.size} modules active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // ── Module Cards ─────────────────────────────────────
        modules.forEach { module ->
            item(key = "module_${module.id}") {
                ToolkitModuleCard(
                    module = module,
                    isTooltipExpanded = module.id in expandedTooltips,
                    tooltipText = moduleTooltips[module.id] ?: "",
                    vpnRunning = vpnRunning,
                    dnsTotal = dnsTotal,
                    dnsBlocked = dnsBlocked,
                    onToggle = {
                        viewModel.toggleModule(module.id)
                        // If enabling DNS filter, also request VPN consent
                        if (!module.isEnabled && module.id == "dns_filter") {
                            onRequestVpnConsent()
                        }
                    },
                    onDisableDns = {
                        viewModel.toggleModule(module.id)
                        viewModel.stopVpn()
                    },
                    onToggleTooltip = { viewModel.toggleTooltip(module.id) },
                    onConfigure = { onNavigateToModuleDetail(module.id) },
                    onManageFilters = onNavigateToContentFilter,
                )
            }
        }

        // ── Monitored Apps ───────────────────────────────────
        item(key = "monitored_header") {
            Column {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Apps you're being mindful about",
                    fontFamily = MonospaceFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 1.sp,
                )
                Text(
                    "${monitoredApps.size} apps monitored",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "monitored_apps") {
            if (monitoredApps.isEmpty()) {
                RetroCard(onClick = onNavigateToApps) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Choose apps to monitor",
                            fontFamily = MonospaceFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(monitoredApps, key = { it.packageName }) { app ->
                        OutlinedCard(
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.outlineVariant,
                            ),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon,
                                        contentDescription = app.label,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    item(key = "add_app") {
                        OutlinedCard(
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.outlineVariant,
                            ),
                            onClick = onNavigateToApps,
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = "Add app",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Quick Links (conditional) ────────────────────────
        val dnsEnabled = modules.find { it.id == "dns_filter" }?.isEnabled == true
        val hasApps = monitoredApps.isNotEmpty()

        if (dnsEnabled) {
            item(key = "link_filters") {
                QuickLink(
                    text = "Manage content filters",
                    onClick = onNavigateToContentFilter,
                )
            }
        }

        if (hasApps) {
            item(key = "link_category_limits") {
                QuickLink(
                    text = "Category time limits",
                    onClick = onNavigateToCategoryLimits,
                )
            }
        }

        item(key = "link_quarantine") {
            QuickLink(
                text = "App schedules & quarantine",
                onClick = onNavigateToQuarantine,
            )
        }

        item(key = "link_learn") {
            QuickLink(
                text = "Learn about digital wellbeing",
                onClick = onNavigateToLearn,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Module Card with expandable tooltip
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ToolkitModuleCard(
    module: ToolkitModuleInfo,
    isTooltipExpanded: Boolean,
    tooltipText: String,
    vpnRunning: Boolean,
    dnsTotal: Int,
    dnsBlocked: Int,
    onToggle: () -> Unit,
    onDisableDns: () -> Unit,
    onToggleTooltip: () -> Unit,
    onConfigure: () -> Unit,
    onManageFilters: () -> Unit,
) {
    val isDns = module.id == "dns_filter"
    val dnsActive = isDns && module.isEnabled && vpnRunning

    RetroCard(onClick = onConfigure) {
        Column {
            // Top row: icon + title + toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, module.accentColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .background(module.accentColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = module.iconRes),
                        contentDescription = module.name,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        module.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        module.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RetroToggle(
                    checked = module.isEnabled,
                    onCheckedChange = {
                        if (isDns && module.isEnabled) onDisableDns() else onToggle()
                    },
                    checkedColor = module.accentColor,
                )
            }

            // DNS-specific: quick stats when active
            if (isDns && dnsActive) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    DnsQuickStat("$dnsTotal", "Queries", ModuleColors.DnsFilter)
                    DnsQuickStat("$dnsBlocked", "Filtered", CardboardColors.accentCoral)
                    val rate = if (dnsTotal > 0) "%.0f%%".format(dnsBlocked.toFloat() / dnsTotal * 100) else "0%"
                    DnsQuickStat(rate, "Filter rate", CardboardColors.accentPurple)
                }
            }

            // DNS-specific: manage filters link
            if (isDns && module.isEnabled) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                        .clickable { onManageFilters() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Manage filters",
                        fontFamily = MonospaceFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "\u203A",
                        fontFamily = MonospaceFamily,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // "What is this?" expandable tooltip
            if (tooltipText.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.clickable { onToggleTooltip() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isTooltipExpanded) "Hide details" else "What is this?",
                        fontFamily = MonospaceFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                AnimatedVisibility(
                    visible = isTooltipExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Text(
                        tooltipText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                        lineHeight = 18.sp,
                    )
                }
            }

            // Configure link
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.clickable { onConfigure() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Configure",
                    fontFamily = MonospaceFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "\u203A",
                    fontFamily = MonospaceFamily,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DnsQuickStat(value: String, label: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontFamily = MonospaceFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = accent,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuickLink(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text,
            fontFamily = MonospaceFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "\u203A",
            fontFamily = MonospaceFamily,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
