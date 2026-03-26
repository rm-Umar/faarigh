package com.faarigh.app.ui.screen.categories

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.faarigh.app.ui.component.RetroButton
import com.faarigh.app.ui.component.RetroCard
import com.faarigh.app.ui.component.RetroHeading
import com.faarigh.app.ui.component.RetroToggle
import com.faarigh.app.ui.component.gridPaper
import com.faarigh.app.ui.theme.CardboardColors
import com.faarigh.app.ui.theme.MonospaceFamily
import java.time.Duration

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryLimitsScreen(
    onBack: () -> Unit = {},
    viewModel: CategoryLimitsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val categoryLimits by viewModel.categoryLimits.collectAsStateWithLifecycle()
    val categoryUsageToday by viewModel.categoryUsageToday.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()

    val builtInCategories = CategoryLimitsViewModel.BUILT_IN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .gridPaper()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // -- Header --
        Text(
            text = "USAGE LIMITS",
            fontFamily = MonospaceFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 3.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Set daily limits by category",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Built-in categories ──
            items(builtInCategories, key = { "builtin_${it.name}" }) { category ->
                val limitMin = categoryLimits[category.name] ?: 0
                val usedToday = categoryUsageToday[category.name]

                CategoryCard(
                    categoryName = category.name,
                    limitMin = limitMin,
                    usedToday = usedToday,
                    appPackages = category.packages,
                    onLimitChange = { viewModel.updateCategoryLimit(category.name, it) },
                )
            }

            // ── Custom categories ──
            val customCatNames = customCategories.keys.toList()
            if (customCatNames.isNotEmpty()) {
                item(key = "custom_header") {
                    Spacer(Modifier.height(4.dp))
                    RetroHeading("CUSTOM CATEGORIES")
                }
            }
            items(customCatNames, key = { "custom_$it" }) { catName ->
                val limitMin = categoryLimits[catName] ?: 0
                val usedToday = categoryUsageToday[catName]
                val packages = customCategories[catName] ?: emptySet()

                CategoryCard(
                    categoryName = catName,
                    limitMin = limitMin,
                    usedToday = usedToday,
                    appPackages = packages,
                    onLimitChange = { viewModel.updateCategoryLimit(catName, it) },
                )
            }

            // ── Create Custom Category ──
            item(key = "add_custom") {
                Spacer(Modifier.height(4.dp))
                CreateCustomCategoryCard(
                    installedApps = installedApps,
                    onCreateCategory = { name, packages, limit ->
                        viewModel.addCustomCategory(name, packages, limit)
                    },
                )
            }
        }
    }
}

// ── Category Card with circular progress ──────────────────

@Composable
private fun CategoryCard(
    categoryName: String,
    limitMin: Int,
    usedToday: Duration?,
    appPackages: Set<String>,
    onLimitChange: (Int) -> Unit,
) {
    val accent = when (categoryName) {
        "Social Media" -> CardboardColors.accentPurple
        "Entertainment" -> CardboardColors.accentCoral
        "Communication" -> CardboardColors.accentGreen
        "Games" -> Color(0xFFE8A838)
        else -> MaterialTheme.colorScheme.primary
    }
    var localLimit by remember(limitMin) { mutableFloatStateOf(limitMin.toFloat()) }
    val usedMin = usedToday?.toMinutes()?.toInt() ?: 0
    var expanded by remember { mutableStateOf(false) }

    RetroCard(onClick = { expanded = !expanded }) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Circular progress
                CircularUsageIndicator(
                    usedMin = usedMin,
                    limitMin = limitMin,
                    accent = accent,
                    modifier = Modifier.size(56.dp),
                )

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        categoryName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "${formatMinutes(usedMin)} / ${if (limitMin > 0) formatMinutes(limitMin) else "No limit"}",
                        fontFamily = MonospaceFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (limitMin > 0 && usedMin >= limitMin) CardboardColors.accentCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = formatMinutes(localLimit.toInt()),
                    fontFamily = MonospaceFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = accent,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Slider
            Slider(
                value = localLimit,
                onValueChange = { localLimit = it },
                onValueChangeFinished = { onLimitChange(localLimit.toInt()) },
                valueRange = 0f..240f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )

            // Expanded: show apps in category
            if (expanded && appPackages.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Apps in this category:",
                    fontFamily = MonospaceFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                val context = LocalContext.current
                appPackages.forEach { pkg ->
                    val label = remember(pkg) {
                        try {
                            val pm = context.packageManager
                            pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
                        } catch (_: Exception) {
                            pkg.substringAfterLast('.')
                        }
                    }
                    Text(
                        "  - $label",
                        fontFamily = MonospaceFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Circular Usage Indicator ──────────────────────────────

@Composable
private fun CircularUsageIndicator(
    usedMin: Int,
    limitMin: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val fraction = if (limitMin > 0) (usedMin.toFloat() / limitMin).coerceIn(0f, 1f) else 0f
    val overLimit = limitMin > 0 && usedMin >= limitMin
    val arcColor = if (overLimit) CardboardColors.accentCoral else accent
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = modifier
            .drawBehind {
                val strokeWidth = 5.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val topLeft = Offset(
                    (size.width - radius * 2) / 2f,
                    (size.height - radius * 2) / 2f,
                )
                val arcSize = Size(radius * 2, radius * 2)

                // Track
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                // Progress
                if (fraction > 0f) {
                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${(fraction * 100).toInt()}%",
            fontFamily = MonospaceFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = arcColor,
        )
    }
}

// ── Create Custom Category Card ──────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateCustomCategoryCard(
    installedApps: List<com.faarigh.app.util.InstalledApp>,
    onCreateCategory: (name: String, packages: Set<String>, limitMin: Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var categoryName by remember { mutableStateOf("") }
    var limitValue by remember { mutableFloatStateOf(60f) }
    var selectedPackages by remember { mutableStateOf(setOf<String>()) }
    var appSearchQuery by remember { mutableStateOf("") }

    RetroCard(onClick = { expanded = !expanded }) {
        Column {
            Text(
                text = if (expanded) "New Custom Category" else "+ Create Custom Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = CardboardColors.accentPurple,
            )

            if (expanded) {
                Spacer(Modifier.height(12.dp))

                // Category name
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Category name",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Spacer(Modifier.height(8.dp))

                // Limit slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Daily limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatMinutes(limitValue.toInt()),
                        fontFamily = MonospaceFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CardboardColors.accentPurple,
                    )
                }
                Slider(
                    value = limitValue,
                    onValueChange = { limitValue = it },
                    valueRange = 0f..240f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = CardboardColors.accentPurple,
                        activeTrackColor = CardboardColors.accentPurple,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )

                Spacer(Modifier.height(8.dp))

                // App picker search
                Text(
                    "Select apps (${selectedPackages.size} selected):",
                    fontFamily = MonospaceFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = appSearchQuery,
                    onValueChange = { appSearchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search apps...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Spacer(Modifier.height(4.dp))

                // Show selected chips
                if (selectedPackages.isNotEmpty()) {
                    val context = LocalContext.current
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        selectedPackages.forEach { pkg ->
                            val label = remember(pkg) {
                                try {
                                    val pm = context.packageManager
                                    pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
                                } catch (_: Exception) {
                                    pkg.substringAfterLast('.')
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CardboardColors.accentPurple.copy(alpha = 0.15f))
                                    .clickable {
                                        selectedPackages = selectedPackages - pkg
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "$label x",
                                    fontFamily = MonospaceFamily,
                                    fontSize = 10.sp,
                                    color = CardboardColors.accentPurple,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // App list (filtered, max 10)
                val filtered = remember(appSearchQuery, installedApps) {
                    if (appSearchQuery.isBlank()) installedApps.take(10)
                    else installedApps.filter {
                        it.label.contains(appSearchQuery, ignoreCase = true) ||
                            it.packageName.contains(appSearchQuery, ignoreCase = true)
                    }.take(10)
                }

                filtered.forEach { app ->
                    val isSelected = app.packageName in selectedPackages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPackages = if (isSelected) {
                                    selectedPackages - app.packageName
                                } else {
                                    selectedPackages + app.packageName
                                }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            app.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        RetroToggle(
                            checked = isSelected,
                            onCheckedChange = {
                                selectedPackages = if (isSelected) {
                                    selectedPackages - app.packageName
                                } else {
                                    selectedPackages + app.packageName
                                }
                            },
                            checkedColor = CardboardColors.accentPurple,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Save button
                RetroButton(
                    text = "Save Category",
                    onClick = {
                        val name = categoryName.trim()
                        if (name.isNotEmpty() && selectedPackages.isNotEmpty()) {
                            onCreateCategory(name, selectedPackages, limitValue.toInt())
                            categoryName = ""
                            limitValue = 60f
                            selectedPackages = emptySet()
                            appSearchQuery = ""
                            expanded = false
                        }
                    },
                    color = CardboardColors.accentPurple,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    if (minutes == 0) return "Unlimited"
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}
