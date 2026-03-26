package com.faarigh.app.ui.screen.protection

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.faarigh.app.R
import com.faarigh.app.ui.component.ModuleEducationSheet
import com.faarigh.app.ui.component.RetroCard
import com.faarigh.app.ui.component.RetroHeading
import com.faarigh.app.ui.component.RetroToggle
import com.faarigh.app.ui.component.gridPaper
import com.faarigh.app.ui.screen.modules.ModulesViewModel
import com.faarigh.app.ui.theme.CardboardColors
import com.faarigh.app.ui.theme.ModuleColors
import com.faarigh.app.ui.theme.MonospaceFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionScreen(
    onNavigateToModuleDetail: (String) -> Unit = {},
    onNavigateToContentFilter: () -> Unit = {},
    onRequestVpnConsent: () -> Unit = {},
    viewModel: ModulesViewModel = hiltViewModel(),
    dnsViewModel: ProtectionDnsViewModel = hiltViewModel(),
) {
    val modules by viewModel.modules.collectAsStateWithLifecycle()
    val pendingEducation by viewModel.pendingEducation.collectAsStateWithLifecycle()

    // DNS stats
    val dnsTotal by dnsViewModel.dnsTotal.collectAsStateWithLifecycle()
    val dnsBlocked by dnsViewModel.dnsBlocked.collectAsStateWithLifecycle()
    val vpnRunning by dnsViewModel.vpnRunning.collectAsStateWithLifecycle()

    val filterRate = if (dnsTotal > 0) (dnsBlocked.toFloat() / dnsTotal * 100) else 0f

    // Education bottom sheet
    pendingEducation?.let { moduleId ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissEducation() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ModuleEducationSheet(
                moduleId = moduleId,
                onDismiss = { viewModel.dismissEducation() },
                onEnable = { viewModel.confirmEnableAfterEducation(moduleId) },
            )
        }
    }

    // Helper to find module info by ID
    fun findModule(id: String) = modules.find { it.id == id }

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
                    "TOOLKIT",
                    fontFamily = MonospaceFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 3.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your toolkit",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                val activeCount = modules.count { it.isEnabled }
                Text(
                    "$activeCount of ${modules.size} modules active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // ═══════════════════════════════════════════════════
        // Section 1: TIME CONTROL
        // ═══════════════════════════════════════════════════
        item(key = "section_time") {
            RetroHeading("TIME CONTROL")
        }

        // App Pause
        item(key = "module_app_pause") {
            val appPause = findModule("app_pause")
            ProtectionModuleCard(
                title = "App Pause",
                subtitle = "Adds a pause before opening selected apps",
                iconRes = R.drawable.ic_module_app_pause,
                accent = ModuleColors.AppPause,
                isEnabled = appPause?.isEnabled ?: false,
                onToggle = { viewModel.toggleModule("app_pause") },
                onTap = { onNavigateToModuleDetail("app_pause") },
            )
        }

        // ═══════════════════════════════════════════════════
        // Section 2: CONTENT
        // ═══════════════════════════════════════════════════
        item(key = "section_content") {
            RetroHeading("CONTENT")
        }

        // Shorts Blocker
        item(key = "module_shorts_blocker") {
            val shorts = findModule("shorts_blocker")
            ProtectionModuleCard(
                title = "Shorts Blocker",
                subtitle = "Detects short-form video feeds",
                iconRes = R.drawable.ic_module_shorts_blocker,
                accent = ModuleColors.ShortsBlocker,
                isEnabled = shorts?.isEnabled ?: false,
                onToggle = { viewModel.toggleModule("shorts_blocker") },
                onTap = { onNavigateToModuleDetail("shorts_blocker") },
            )
        }

        // Content Awareness (NSFW Detection)
        item(key = "module_nsfw") {
            val nsfw = findModule("nsfw_detection")
            ProtectionModuleCard(
                title = "Content Awareness",
                subtitle = "On-device detection of explicit content",
                iconRes = R.drawable.ic_module_content_awareness,
                accent = ModuleColors.NsfwShield,
                isEnabled = nsfw?.isEnabled ?: false,
                onToggle = { viewModel.toggleModule("nsfw_detection") },
                onTap = { onNavigateToModuleDetail("nsfw_detection") },
            )
        }

        // ═══════════════════════════════════════════════════
        // Section 3: NETWORK
        // ═══════════════════════════════════════════════════
        item(key = "section_network") {
            RetroHeading("NETWORK")
        }

        // DNS Filter
        item(key = "module_dns_filter") {
            val dns = findModule("dns_filter")
            // DNS module is truly active only when both preference is on AND VPN is running
            val dnsActive = (dns?.isEnabled ?: false) && vpnRunning
            RetroCard(
                onClick = { onNavigateToModuleDetail("dns_filter") },
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, ModuleColors.DnsFilter.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .background(ModuleColors.DnsFilter.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_module_dns_filter),
                                contentDescription = "DNS Filter",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "DNS Filter",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                if (dnsActive) "Active — filtering traffic"
                                else if (dns?.isEnabled == true) "Enabled but VPN not running"
                                else "Filters ads, trackers, and harmful domains",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (dnsActive) CardboardColors.accentGreen
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        RetroToggle(
                            checked = dns?.isEnabled ?: false,
                            onCheckedChange = { enabled ->
                                viewModel.toggleModule("dns_filter")
                                // Sync VPN state with DNS toggle
                                if (enabled) {
                                    // Enabling DNS → request VPN consent to start VPN
                                    onRequestVpnConsent()
                                } else {
                                    // Disabling DNS → stop VPN
                                    dnsViewModel.stopVpn()
                                }
                            },
                            checkedColor = ModuleColors.DnsFilter,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // VPN status toggle
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val shieldShape = RoundedCornerShape(4.dp)
                        val activeColor = CardboardColors.accentGreen
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(shieldShape)
                                .border(
                                    3.dp,
                                    if (vpnRunning) activeColor else MaterialTheme.colorScheme.outlineVariant,
                                    shieldShape,
                                )
                                .background(Color.Transparent)
                                .clickable {
                                    if (vpnRunning) dnsViewModel.stopVpn() else onRequestVpnConsent()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Shield,
                                contentDescription = "VPN",
                                tint = if (vpnRunning) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (vpnRunning) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(CardboardColors.accentGreen),
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = if (vpnRunning) "VPN Active" else "VPN Inactive",
                            fontFamily = MonospaceFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (vpnRunning) CardboardColors.accentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (vpnRunning) "Tap to stop filtering" else "Tap to start filtering",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(16.dp))

                    // Quick DNS stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        DnsQuickStat(
                            value = "$dnsTotal",
                            label = "Queries today",
                            accent = ModuleColors.DnsFilter,
                        )
                        DnsQuickStat(
                            value = "$dnsBlocked",
                            label = "Filtered today",
                            accent = CardboardColors.accentCoral,
                        )
                        DnsQuickStat(
                            value = "${"%.1f".format(filterRate)}%",
                            label = "Filter rate",
                            accent = CardboardColors.accentPurple,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Manage filters link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                            .clickable { onNavigateToContentFilter() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Manage filters",
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
            }
        }
    }
}

// ── Module Card ────────────────────────────────────────────────

@Composable
private fun ProtectionModuleCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    accent: Color,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    onTap: () -> Unit,
) {
    RetroCard(onClick = onTap) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .background(accent.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RetroToggle(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                checkedColor = accent,
            )
        }
    }
}

@Composable
private fun DnsQuickStat(
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
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
