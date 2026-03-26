package com.faarigh.app.ui.screen.content

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.faarigh.app.ui.component.FaarighStatCard
import com.faarigh.app.ui.component.FaarighStackedBar
import com.faarigh.app.ui.component.RetroCard
import com.faarigh.app.ui.component.RetroHeading
import com.faarigh.app.ui.component.RetroToggle
import com.faarigh.app.ui.component.StackedBarSegment
import com.faarigh.app.ui.component.gridPaper
import com.faarigh.app.ui.theme.CardboardColors
import com.faarigh.app.ui.theme.ChartColors
import com.faarigh.app.ui.theme.ModuleColors
import com.faarigh.app.ui.theme.MonospaceFamily

@Composable
fun ContentFilterScreen(
    onRequestVpnConsent: () -> Unit = {},
    viewModel: ContentFilterViewModel = hiltViewModel(),
) {
    val vpnRunning by viewModel.vpnRunning.collectAsStateWithLifecycle()
    val allDomains by viewModel.allDomains.collectAsStateWithLifecycle()
    val enabledCount by viewModel.enabledCount.collectAsStateWithLifecycle()

    // DNS stats
    val dnsTotal by viewModel.dnsTotal.collectAsStateWithLifecycle()
    val dnsBlocked by viewModel.dnsBlocked.collectAsStateWithLifecycle()
    val topBlockedDomains by viewModel.topBlockedDomains.collectAsStateWithLifecycle()
    val dnsCategoryBreakdown by viewModel.dnsCategoryBreakdown.collectAsStateWithLifecycle()
    val uniqueDomains by viewModel.uniqueDomains.collectAsStateWithLifecycle()

    val categories by remember(allDomains) {
        derivedStateOf {
            allDomains.filter { it.category != "custom" }
                .groupBy { it.category }
                .mapValues { (_, domains) -> domains.all { it.isEnabled } }
        }
    }
    val customDomains by remember(allDomains) {
        derivedStateOf { allDomains.filter { it.category == "custom" } }
    }

    var customDomainInput by remember { mutableStateOf("") }

    val filterRate = if (dnsTotal > 0) (dnsBlocked.toFloat() / dnsTotal * 100) else 0f

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
        item {
            Column {
                Text("DNS FILTER", fontFamily = MonospaceFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 3.sp)
                Spacer(Modifier.height(4.dp))
                Text("DNS filtering", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            }
        }

        // ── VPN Hero Toggle ──────────────────────────────
        item {
            RetroCard(
                surfaceColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Filter icon — transparent bg, bright green border + icon
                    val shieldShape = RoundedCornerShape(4.dp)
                    val activeColor = CardboardColors.accentGreen  // green instead of amber
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(shieldShape)
                            .border(
                                3.dp,
                                if (vpnRunning) activeColor else MaterialTheme.colorScheme.outlineVariant,
                                shieldShape,
                            )
                            .background(Color.Transparent)
                            .clickable {
                                if (vpnRunning) viewModel.stopVpn() else onRequestVpnConsent()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = "VPN",
                            tint = if (vpnRunning) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (vpnRunning) "Filtering Active" else "Filtering Off",
                        fontFamily = MonospaceFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (vpnRunning) CardboardColors.accentGreen else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (vpnRunning) "Tap to stop filtering" else "Tap to start filtering",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))

                    // Stats row
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$enabledCount", fontFamily = MonospaceFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CardboardColors.accentGreen)
                            Text("rules active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${allDomains.size}", fontFamily = MonospaceFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CardboardColors.accentAmber)
                            Text("total rules", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // ── DNS Quick Stats ─────────────────────────────
        item {
            RetroHeading("DNS STATS")
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FaarighStatCard(modifier = Modifier.weight(1f), value = "$dnsTotal", label = "Total queries", accent = ModuleColors.DnsFilter)
                FaarighStatCard(modifier = Modifier.weight(1f), value = "$dnsBlocked", label = "Filtered", accent = CardboardColors.accentCoral)
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FaarighStatCard(modifier = Modifier.weight(1f), value = "${"%.1f".format(filterRate)}%", label = "Filter rate", accent = CardboardColors.accentPurple)
                FaarighStatCard(modifier = Modifier.weight(1f), value = "$uniqueDomains", label = "Unique domains", accent = CardboardColors.accentGreen)
            }
        }

        // ── Top Filtered Domains ──────────────────────────
        if (topBlockedDomains.isNotEmpty()) {
            item {
                RetroHeading("TOP FILTERED DOMAINS")
            }
            item {
                RetroCard {
                    Column {
                        topBlockedDomains.forEachIndexed { index, domain ->
                            if (index > 0) Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${index + 1}.",
                                    fontFamily = MonospaceFamily,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(domain.domain, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1)
                                Text("${domain.count}", fontFamily = MonospaceFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CardboardColors.accentCoral)
                            }
                        }
                    }
                }
            }
        }

        // ── DNS Category Breakdown ───────────────────────
        if (dnsCategoryBreakdown.isNotEmpty()) {
            item {
                RetroCard {
                    Column {
                        Text("DNS categories", fontFamily = MonospaceFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(12.dp))
                        val catColors = listOf(ChartColors.area1, ChartColors.area2, ChartColors.area3, ChartColors.blocked)
                        FaarighStackedBar(
                            segments = dnsCategoryBreakdown.mapIndexed { i, cat -> StackedBarSegment(cat.count.toFloat(), catColors[i % catColors.size]) },
                            modifier = Modifier.fillMaxWidth().height(12.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        dnsCategoryBreakdown.forEachIndexed { i, cat ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(catColors[i % catColors.size]))
                                Spacer(Modifier.width(8.dp))
                                Text(cat.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                Text("${cat.count}", fontFamily = MonospaceFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (i < dnsCategoryBreakdown.lastIndex) Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        // ── Categories ───────────────────────────────────
        item {
            RetroHeading("CATEGORIES")
        }

        val categoryList = categories.toList()
        items(categoryList, key = { it.first }) { (category, enabled) ->
            RetroCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val count = allDomains.count { it.category == category }
                        Text("$count domains", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RetroToggle(
                        checked = enabled,
                        onCheckedChange = { viewModel.setCategoryEnabled(category, it) },
                        checkedColor = CardboardColors.accentGreen,
                    )
                }
            }
        }

        // ── Custom Domains ───────────────────────────────
        item {
            RetroHeading("CUSTOM DOMAINS")
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = customDomainInput,
                    onValueChange = { customDomainInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("example.com", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (customDomainInput.isNotBlank()) {
                            viewModel.addCustomDomain(customDomainInput.trim())
                            customDomainInput = ""
                        }
                    }),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (customDomainInput.isNotBlank()) {
                            viewModel.addCustomDomain(customDomainInput.trim())
                            customDomainInput = ""
                        }
                    },
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (customDomains.isNotEmpty()) {
            items(customDomains, key = { it.domain }) { domain ->
                RetroCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(domain.domain, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { viewModel.removeDomain(domain.domain) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // ── Load Defaults ────────────────────────────────
        item {
            TextButton(
                onClick = { viewModel.loadDefaults() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Load default filter lists", fontFamily = MonospaceFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
