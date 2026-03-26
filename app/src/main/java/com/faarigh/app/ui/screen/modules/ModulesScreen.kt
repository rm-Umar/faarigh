package com.faarigh.app.ui.screen.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.faarigh.app.ui.component.ModuleEducationSheet
import com.faarigh.app.ui.component.RetroCard
import com.faarigh.app.ui.component.RetroToggle
import com.faarigh.app.ui.component.gridPaper
import com.faarigh.app.ui.theme.MonospaceFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulesScreen(
    onModuleClick: (String) -> Unit = {},
    viewModel: ModulesViewModel = hiltViewModel(),
) {
    val modules by viewModel.modules.collectAsStateWithLifecycle()
    val pendingEducation by viewModel.pendingEducation.collectAsStateWithLifecycle()

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
                Text(
                    "MODULES",
                    fontFamily = MonospaceFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 3.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your tools",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${modules.count { it.isEnabled }} of ${modules.size} active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // ── Module Cards (2-column grid) ─────────────────
        // Chunk modules into pairs for 2x2 grid
        val rows = modules.chunked(2)
        rows.forEach { rowModules ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowModules.forEach { module ->
                        ModuleCard(
                            module = module,
                            modifier = Modifier.weight(1f),
                            onClick = { onModuleClick(module.id) },
                            onToggle = { viewModel.toggleModule(module.id) },
                        )
                    }
                    // Fill remaining space if odd count in last row
                    if (rowModules.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(
    module: ModuleInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val accent = module.accentColor

    RetroCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Column {
            // Icon box + toggle row (matching Home screen layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                        painter = painterResource(id = module.iconRes),
                        contentDescription = module.name,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp),
                    )
                }
                RetroToggle(
                    checked = module.isEnabled,
                    onCheckedChange = { onToggle() },
                    checkedColor = accent,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                module.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (module.isEnabled) "Active" else "Inactive",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
        }
    }
}
