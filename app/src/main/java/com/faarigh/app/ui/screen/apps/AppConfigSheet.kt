package com.faarigh.app.ui.screen.apps

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faarigh.app.ui.component.RetroCard
import com.faarigh.app.ui.component.RetroToggle
import com.faarigh.app.ui.theme.CardboardColors
import com.faarigh.app.ui.theme.MonospaceFamily

/**
 * Bottom sheet content for per-app configuration.
 * Shows monitor toggle, daily limit slider, and schedule block controls.
 */
@Composable
fun AppConfigSheet(
    state: ConfigSheetState,
    onMonitorToggle: (Boolean) -> Unit,
    onDailyLimitChange: (Int) -> Unit,
    onScheduleToggle: (Boolean) -> Unit,
    onScheduleTimeChange: (startHour: Int, startMin: Int, endHour: Int, endMin: Int) -> Unit,
) {
    val accent = CardboardColors.accentGreen
    val context = LocalContext.current

    var localDailyLimit by remember(state.dailyLimitMin) {
        mutableFloatStateOf(state.dailyLimitMin.toFloat())
    }
    var scheduleEnabled by remember(state.scheduleEnabled) { mutableStateOf(state.scheduleEnabled) }
    var startHour by remember(state.scheduleStartHour) { mutableIntStateOf(state.scheduleStartHour) }
    var startMin by remember(state.scheduleStartMin) { mutableIntStateOf(state.scheduleStartMin) }
    var endHour by remember(state.scheduleEndHour) { mutableIntStateOf(state.scheduleEndHour) }
    var endMin by remember(state.scheduleEndMin) { mutableIntStateOf(state.scheduleEndMin) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // -- Header --
        Text(
            text = state.appLabel,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = state.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(4.dp))

        // -- 1. Monitor toggle --
        RetroCard(
            surfaceColor = if (state.isMonitored) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Monitor this app", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Enable check-ins when opening", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RetroToggle(
                    checked = state.isMonitored,
                    onCheckedChange = onMonitorToggle,
                    checkedColor = accent,
                )
            }
        }

        // -- 2. Daily limit slider --
        RetroCard {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily limit", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Set a maximum usage time per day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = formatLimit(localDailyLimit.toInt()),
                        fontFamily = MonospaceFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = accent,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = localDailyLimit,
                    onValueChange = { localDailyLimit = it },
                    onValueChangeFinished = { onDailyLimitChange(localDailyLimit.toInt()) },
                    valueRange = 0f..240f,
                    steps = 15,  // 15-min increments: 0, 15, 30 ... 240
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }
        }

        // -- 3. Schedule block --
        RetroCard(
            surfaceColor = if (scheduleEnabled) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Schedule block", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Block this app during set hours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RetroToggle(
                        checked = scheduleEnabled,
                        onCheckedChange = {
                            scheduleEnabled = it
                            if (it) {
                                onScheduleTimeChange(startHour, startMin, endHour, endMin)
                            } else {
                                onScheduleToggle(false)
                            }
                        },
                        checkedColor = accent,
                    )
                }

                if (scheduleEnabled) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        // Start time picker button
                        TextButton(onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    startHour = h
                                    startMin = m
                                    onScheduleTimeChange(startHour, startMin, endHour, endMin)
                                },
                                startHour, startMin, false,
                            ).show()
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("From", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatTime12h(startHour, startMin),
                                    fontFamily = MonospaceFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = accent,
                                )
                            }
                        }

                        // End time picker button
                        TextButton(onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    endHour = h
                                    endMin = m
                                    onScheduleTimeChange(startHour, startMin, endHour, endMin)
                                },
                                endHour, endMin, false,
                            ).show()
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("To", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatTime12h(endHour, endMin),
                                    fontFamily = MonospaceFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = accent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatLimit(minutes: Int): String {
    if (minutes == 0) return "Unlimited"
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

private fun formatTime12h(hour: Int, min: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%d:%02d %s".format(h12, min, amPm)
}
