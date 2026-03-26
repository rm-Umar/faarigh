package com.faarigh.app.ui.screen.apps

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.faarigh.app.ui.component.RetroCard
import com.faarigh.app.ui.component.RetroToggle
import com.faarigh.app.ui.component.gridPaper
import com.faarigh.app.ui.theme.CardboardColors
import com.faarigh.app.ui.theme.MonospaceFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    viewModel: AppSelectionViewModel = hiltViewModel(),
    onNavigateToQuarantine: () -> Unit = {},
    onNavigateToCategoryLimits: () -> Unit = {},
) {
    val context = LocalContext.current
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val interceptedApps by viewModel.interceptedApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val configSheet by viewModel.configSheetApp.collectAsStateWithLifecycle()

    val interceptedPackages by remember(interceptedApps) {
        derivedStateOf { interceptedApps.map { it.packageName }.toSet() }
    }

    // Icon cache
    val iconCache = remember { mutableMapOf<String, ImageBitmap?>() }

    // -- Per-app config bottom sheet --
    configSheet?.let { state ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeConfigSheet() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            AppConfigSheet(
                state = state,
                onMonitorToggle = { viewModel.updateMonitored(state.packageName, state.appLabel, it) },
                onDailyLimitChange = { viewModel.updateDailyLimit(state.packageName, state.appLabel, it) },
                onScheduleToggle = { viewModel.updateScheduleBlock(state.packageName, state.appLabel, it) },
                onScheduleTimeChange = { sh, sm, eh, em ->
                    viewModel.updateScheduleBlock(state.packageName, state.appLabel, true, sh, sm, eh, em)
                },
            )
        }
    }

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
            text = "APP MANAGEMENT",
            fontFamily = MonospaceFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 3.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Configure your apps",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${interceptedApps.size} apps selected",
            style = MaterialTheme.typography.bodySmall,
            color = CardboardColors.accentGreen,
            fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // -- Search --
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Search apps...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

        Spacer(modifier = Modifier.height(16.dp))

        // -- Loading / App List --
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = CardboardColors.accentGreen,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Loading apps...",
                        fontFamily = MonospaceFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // ── Usage Limits link ──────────────────────
                item(key = "usage_limits_link") {
                    RetroCard(onClick = onNavigateToCategoryLimits) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    "Usage Limits",
                                    fontFamily = MonospaceFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CardboardColors.accentPurple,
                                )
                                Text(
                                    "Set daily limits by app category",
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

                // ── Quarantine link ──────────────────────
                item(key = "quarantine_link") {
                    Spacer(Modifier.height(4.dp))
                    RetroCard(onClick = onNavigateToQuarantine) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    "Quarantine",
                                    fontFamily = MonospaceFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "Schedule app pauses for specific times",
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

                item(key = "apps_header") {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "ALL APPS",
                        fontFamily = MonospaceFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 3.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Tap an app to configure limits and schedules",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // ── App List ────────────────────────────
                items(filteredApps, key = { it.packageName }) { app ->
                    val isSelected = app.packageName in interceptedPackages

                    val bitmap = remember(app.packageName) {
                        iconCache.getOrPut(app.packageName) {
                            try {
                                val drawable = context.packageManager.getApplicationIcon(app.packageName)
                                drawableToBitmap(drawable)
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }

                    OutlinedCard(
                        onClick = { viewModel.openConfigSheet(app.packageName, app.label) },
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) CardboardColors.accentGreen.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected)
                                CardboardColors.accentGreen.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // App icon
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = app.label,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }

                            RetroToggle(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    viewModel.toggleApp(app.packageName, app.label, checked)
                                },
                                checkedColor = CardboardColors.accentGreen,
                            )

                            Spacer(Modifier.width(8.dp))

                            // Chevron indicating tappable
                            Text(
                                text = "\u203A",
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
}


private fun drawableToBitmap(drawable: Drawable): ImageBitmap {
    val bmp = if (drawable is BitmapDrawable) {
        drawable.bitmap
    } else {
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp
    }
    return bmp.asImageBitmap()
}
