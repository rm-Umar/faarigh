package com.faarigh.app.ui.screen.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faarigh.app.data.preferences.ModulePreferences
import com.faarigh.app.ui.component.RetroCard
import com.faarigh.app.ui.component.RetroHeading
import com.faarigh.app.ui.component.gridPaper
import com.faarigh.app.ui.theme.MonospaceFamily
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface SettingsEntryPoint {
    fun modulePreferences(): ModulePreferences
}

@Composable
fun SettingsScreen(onNavigateToLearn: () -> Unit = {}) {
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, SettingsEntryPoint::class.java)
    val modulePrefs = entryPoint.modulePreferences()
    val scope = rememberCoroutineScope()

    val themeMode by modulePrefs.themeMode.collectAsState(initial = "auto")

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
                Text("SETTINGS", fontFamily = MonospaceFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 3.sp)
                Spacer(Modifier.height(4.dp))
                Text("Preferences", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(2.dp))
                Text("Customize your experience", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ── Appearance ───────────────────────────────────
        item(key = "appearance_heading") {
            RetroHeading("APPEARANCE")
        }
        item(key = "theme_card") {
            RetroCard {
                Column {
                    Text("Theme", fontFamily = MonospaceFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text("Choose your preferred color scheme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                            val isSelected = themeMode == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(4.dp),
                                    )
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { scope.launch { modulePrefs.setThemeMode(key) } }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    label,
                                    fontFamily = MonospaceFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Learn ────────────────────────────────────────
        item(key = "learn_heading") {
            RetroHeading("RESOURCES")
        }
        item(key = "learn_card") {
            RetroCard(onClick = onNavigateToLearn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Learn", fontFamily = MonospaceFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(2.dp))
                        Text("Understand the science behind your habits", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        // ── Privacy ─────────────────────────────────────
        item(key = "privacy_heading") {
            RetroHeading("PRIVACY")
        }
        item(key = "privacy_card") {
            RetroCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PrivacyItem(
                        icon = Icons.Outlined.WifiOff,
                        title = "100% Offline",
                        subtitle = "All processing happens on your device",
                    )
                    PrivacyItem(
                        icon = Icons.Outlined.DeleteForever,
                        title = "Zero Data Collection",
                        subtitle = "We never collect, store, or sell your data",
                    )
                    PrivacyItem(
                        icon = Icons.Outlined.PhoneAndroid,
                        title = "Your Data, Your Device",
                        subtitle = "Everything stays on your phone",
                    )
                    PrivacyItem(
                        icon = Icons.Outlined.PersonOff,
                        title = "No Accounts Required",
                        subtitle = "No sign-up, no cloud sync",
                    )
                }
            }
        }

        // ── About ────────────────────────────────────────
        item(key = "about_heading") {
            RetroHeading("ABOUT")
        }
        item(key = "about_card") {
            RetroCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("\u0641\u0627\u0631\u063A", fontFamily = MonospaceFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(4.dp))
                    Text("FAARIGH", fontFamily = MonospaceFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 3.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Digital wellbeing through\nconscious choice, not restriction.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("v0.1.0", fontFamily = MonospaceFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Local composables
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PrivacyItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontFamily = MonospaceFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
