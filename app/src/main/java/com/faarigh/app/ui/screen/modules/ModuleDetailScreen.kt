package com.faarigh.app.ui.screen.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.faarigh.app.module.ModuleConfigItem
import com.faarigh.app.module.ModuleStat
import com.faarigh.app.ui.component.ModuleOnboardingSheet
import com.faarigh.app.ui.component.RetroCard
import com.faarigh.app.ui.component.RetroToggle
import com.faarigh.app.ui.component.gridPaper
import com.faarigh.app.ui.theme.MonospaceFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ModuleDetailScreen(
    moduleId: String,
    onBack: () -> Unit = {},
    onNavigateToContentFilter: () -> Unit = {},
    viewModel: ModuleDetailViewModel = hiltViewModel(),
) {
    val module = viewModel.module

    if (module == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Module not found", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    // Safe color conversion (Long to Color without overflow)
    val accent = Color(
        red = ((module.accentColor shr 16) and 0xFF).toInt(),
        green = ((module.accentColor shr 8) and 0xFF).toInt(),
        blue = (module.accentColor and 0xFF).toInt(),
        alpha = ((module.accentColor shr 24) and 0xFF).toInt(),
    )
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val isEnabled by module.isEnabled.collectAsStateWithLifecycle(initialValue = false)
    val stats by module.statsItems.collectAsStateWithLifecycle(initialValue = emptyList())

    // Education sheet state — shows when user first enables a module
    var showEducation by remember { mutableStateOf(false) }

    if (showEducation && module.educationCards.isNotEmpty()) {
        ModuleOnboardingSheet(
            moduleName = module.name,
            cards = module.educationCards,
            onEnable = {
                markEducationSeen(context, module.id)
                scope.launch { module.setEnabled(true) }
                showEducation = false
            },
            onDismiss = {
                markEducationSeen(context, module.id)
                scope.launch { module.setEnabled(true) }
                showEducation = false
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .gridPaper()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Header ──────────────────────────────────────
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(module.name.uppercase(), fontFamily = MonospaceFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 3.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(module.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                }
                // Info button to show education cards anytime
                if (module.educationCards.isNotEmpty()) {
                    IconButton(onClick = { showEducation = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "Learn more", tint = accent)
                    }
                }
            }
        }

        // ── Enable toggle ───────────────────────────────
        item(key = "enable") {
            EnableCard(title = module.name, enabled = isEnabled, accent = accent) { newValue ->
                if (newValue && module.educationCards.isNotEmpty() && !hasSeenEducation(context, module.id)) {
                    // Enabling for first time → show education sheet, onEnable callback will set enabled=true
                    showEducation = true
                } else {
                    scope.launch { module.setEnabled(newValue) }
                }
            }
        }

        // ── ABOUT section (first education card as inline summary) ──
        if (module.educationCards.isNotEmpty()) {
            item(key = "about") {
                val firstCard = module.educationCards.first()
                RetroCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = module.iconRes),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "What it does",
                                fontFamily = MonospaceFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = accent,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            firstCard.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ── STATS section ───────────────────────────────
        if (stats.isNotEmpty()) {
            item(key = "stats_label") {
                SectionLabel("STATS")
            }
            items(stats, key = { it.label }) { stat ->
                StatCard(stat = stat, defaultAccent = accent)
            }
        }

        // ── SETTINGS section ────────────────────────────
        val configItems = module.configItems.filter { it.key != "breathing_pattern" } // auto-escalate, don't show picker
        if (configItems.isNotEmpty()) {
            item(key = "settings_label") {
                SectionLabel("SETTINGS")
            }
            items(configItems, key = { it.key }) { configItem ->
                ConfigItemRenderer(configItem = configItem, accent = accent, scope = scope)
            }
        }

        // ── DNS Filter special: link to Content Filters ─
        if (moduleId == "dns_filter") {
            item(key = "dns_link") {
                RetroCard(onClick = onNavigateToContentFilter) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Content Filters", fontFamily = MonospaceFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Manage filter lists, categories, and custom domains", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("\u203A", fontFamily = MonospaceFamily, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Generic config item renderer
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ConfigItemRenderer(
    configItem: ModuleConfigItem,
    accent: Color,
    scope: CoroutineScope,
) {
    when (configItem) {
        is ModuleConfigItem.Toggle -> {
            val value by configItem.value.collectAsStateWithLifecycle(initialValue = false)
            RetroCard(
                surfaceColor = if (value) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(configItem.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        if (configItem.description.isNotBlank()) {
                            Text(configItem.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    RetroToggle(
                        checked = value,
                        onCheckedChange = { scope.launch { configItem.onToggle(it) } },
                        checkedColor = accent,
                    )
                }
            }
        }

        is ModuleConfigItem.Slider -> {
            val value by configItem.value.collectAsStateWithLifecycle(initialValue = configItem.range.start)
            SliderCard(
                title = configItem.label,
                value = value,
                valueLabel = configItem.valueLabel(value),
                range = configItem.range,
                steps = configItem.steps,
                accent = accent,
                onValueChange = { scope.launch { configItem.onValueChange(it) } },
            )
        }

        is ModuleConfigItem.Choice -> {
            val selected by configItem.value.collectAsStateWithLifecycle(initialValue = configItem.options.firstOrNull()?.first ?: "")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(configItem.label.uppercase())
                ChoiceSelector(
                    options = configItem.options,
                    selected = selected,
                    accent = accent,
                    onSelect = { scope.launch { configItem.onSelect(it) } },
                )
            }
        }

        is ModuleConfigItem.ToggleGroup -> {
            val selectedItems by configItem.selectedItems.collectAsStateWithLifecycle(initialValue = emptySet())
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(configItem.label.uppercase())
                configItem.items.forEach { (itemId, itemLabel) ->
                    val isSelected = itemId in selectedItems
                    RetroCard(
                        surfaceColor = if (isSelected) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(itemLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(itemId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RetroToggle(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    scope.launch { configItem.onToggleItem(itemId, checked) }
                                },
                                checkedColor = accent,
                            )
                        }
                    }
                }
            }
        }

        is ModuleConfigItem.Info -> {
            InfoCard(configItem.label)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Reusable composables
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EnableCard(title: String, enabled: Boolean, accent: Color, onToggle: (Boolean) -> Unit) {
    RetroCard(
        surfaceColor = if (enabled) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (enabled) "Active" else "Inactive",
                    fontFamily = MonospaceFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = if (enabled) accent else MaterialTheme.colorScheme.outline,
                )
            }
            RetroToggle(
                checked = enabled,
                onCheckedChange = onToggle,
                checkedColor = accent,
            )
        }
    }
}

@Composable
private fun SliderCard(
    title: String,
    value: Float,
    valueLabel: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    accent: Color,
    onValueChange: (Float) -> Unit,
) {
    RetroCard {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(valueLabel, fontFamily = MonospaceFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accent)
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        }
    }
}

@Composable
private fun ChoiceSelector(
    options: List<Pair<String, String>>,
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit,
) {
    RetroCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (value, label) ->
                val isSelected = selected == value
                val shape = RoundedCornerShape(4.dp)
                Text(
                    text = label,
                    modifier = Modifier
                        .weight(1f)
                        .clip(shape)
                        .border(
                            BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) accent else MaterialTheme.colorScheme.outline,
                            ),
                            shape,
                        )
                        .background(
                            if (isSelected) accent.copy(alpha = 0.15f) else Color.Transparent,
                            shape,
                        )
                        .clickable { onSelect(value) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun StatCard(stat: ModuleStat, defaultAccent: Color) {
    val color = if (stat.accentColor != null) {
        Color(
            red = ((stat.accentColor shr 16) and 0xFF).toInt(),
            green = ((stat.accentColor shr 8) and 0xFF).toInt(),
            blue = (stat.accentColor and 0xFF).toInt(),
            alpha = ((stat.accentColor shr 24) and 0xFF).toInt(),
        )
    } else defaultAccent
    RetroCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stat.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Column(horizontalAlignment = Alignment.End) {
                Text(stat.value, fontFamily = MonospaceFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
                if (stat.trend != null) {
                    Text(stat.trend, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontFamily = MonospaceFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 3.sp,
    )
}

@Composable
private fun InfoCard(text: String) {
    RetroCard {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun hasSeenEducation(context: android.content.Context, moduleId: String): Boolean {
    return context.getSharedPreferences("faarigh_education", android.content.Context.MODE_PRIVATE)
        .getBoolean("seen_$moduleId", false)
}

private fun markEducationSeen(context: android.content.Context, moduleId: String) {
    context.getSharedPreferences("faarigh_education", android.content.Context.MODE_PRIVATE)
        .edit().putBoolean("seen_$moduleId", true).apply()
}
