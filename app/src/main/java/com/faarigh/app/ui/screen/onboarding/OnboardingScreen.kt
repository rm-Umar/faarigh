package com.faarigh.app.ui.screen.onboarding

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.faarigh.app.R
import com.faarigh.app.data.repository.AppInterceptionRepository
import com.faarigh.app.service.accessibility.FaarighAccessibilityService
import com.faarigh.app.service.vpn.FaarighVpnService
import com.faarigh.app.ui.component.PixelStar
import com.faarigh.app.ui.component.RetroButton
import com.faarigh.app.ui.component.RetroCard
import com.faarigh.app.ui.component.RetroToggle
import com.faarigh.app.ui.component.gridPaper
import com.faarigh.app.ui.theme.CardboardColors
import com.faarigh.app.ui.theme.ModuleColors
import com.faarigh.app.ui.theme.MonospaceFamily
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface OnboardingEntryPoint {
    fun appInterceptionRepository(): AppInterceptionRepository
}

@Composable
fun OnboardingScreen(
    onRequestVpnConsent: () -> Unit = {},
    onComplete: () -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .gridPaper(),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
        ) { page ->
            when (page) {
                0 -> WelcomePage { scope.launch { pagerState.animateScrollToPage(1) } }
                1 -> WhatItDoesPage { scope.launch { pagerState.animateScrollToPage(2) } }
                2 -> HowItWorksPage(
                    onRequestVpnConsent = onRequestVpnConsent,
                    onContinue = { scope.launch { pagerState.animateScrollToPage(3) } },
                )
                3 -> WhyItsDifferentPage { scope.launch { pagerState.animateScrollToPage(4) } }
                4 -> YourFirstStepsPage(onComplete = onComplete)
            }
        }

        // Page indicators — small retro squares
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(5) { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == pagerState.currentPage) 10.dp else 6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (i == pagerState.currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
        }
    }
}

// ── Page 1: Welcome ─────────────────────────────────────────

@Composable
private fun WelcomePage(onBegin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PixelStar(size = 56.dp, color = MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.height(20.dp))
        Text(
            text = "FAARIGH",
            fontFamily = MonospaceFamily,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 6.sp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Free yourself from the noise",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Faarigh is a toolkit that helps you use your phone the way you want to.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No judgment. No scores. Just awareness and choice.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(48.dp))
        RetroButton(
            text = "Get Started",
            onClick = onBegin,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Page 2: What It Does ────────────────────────────────────

@Composable
private fun WhatItDoesPage(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 64.dp),
    ) {
        Text(
            text = "WHAT IT DOES",
            fontFamily = MonospaceFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Your toolkit",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(24.dp))

        ModuleInfoCard(
            iconRes = R.drawable.ic_module_app_pause,
            title = "App Pause",
            description = "A moment to check in before opening apps",
            accent = ModuleColors.AppPause,
        )
        Spacer(Modifier.height(12.dp))
        ModuleInfoCard(
            iconRes = R.drawable.ic_module_shorts_blocker,
            title = "Shorts Blocker",
            description = "Catch yourself before the scroll",
            accent = ModuleColors.ShortsBlocker,
        )
        Spacer(Modifier.height(12.dp))
        ModuleInfoCard(
            iconRes = R.drawable.ic_module_content_awareness,
            title = "Content Awareness",
            description = "On-device, private content detection",
            accent = ModuleColors.NsfwShield,
        )
        Spacer(Modifier.height(12.dp))
        ModuleInfoCard(
            iconRes = R.drawable.ic_module_dns_filter,
            title = "DNS Filter",
            description = "Filter ads and trackers at the network level",
            accent = ModuleColors.DnsFilter,
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = "You can enable any of these later. No pressure.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))
        RetroButton(
            text = "Next",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun ModuleInfoCard(iconRes: Int, title: String, description: String, accent: Color) {
    RetroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .background(accent.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Page 3: How It Works ────────────────────────────────────

private fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

@Composable
private fun HowItWorksPage(onRequestVpnConsent: () -> Unit, onContinue: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var accessibilityGranted by remember { mutableStateOf(FaarighAccessibilityService.instance != null) }
    var usageStatsGranted by remember { mutableStateOf(checkUsageStatsPermission(context)) }
    val vpnRunning by FaarighVpnService.isRunningFlow.collectAsStateWithLifecycle()

    // Re-check when resumed (user returns from settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityGranted = FaarighAccessibilityService.instance != null
                usageStatsGranted = checkUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 64.dp),
    ) {
        Text(
            text = "HOW IT WORKS",
            fontFamily = MonospaceFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Everything stays on your phone",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No accounts, no servers, no data leaves your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        PermissionCard(
            title = "Accessibility Service",
            description = "Lets the app notice when you open apps",
            isGranted = accessibilityGranted,
        ) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        Spacer(Modifier.height(12.dp))
        PermissionCard(
            title = "Usage Stats",
            description = "Shows you how much time you spend in apps",
            isGranted = usageStatsGranted,
        ) {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        Spacer(Modifier.height(12.dp))
        PermissionCard(
            title = "VPN Service",
            description = "Filters DNS requests locally on your device",
            isGranted = vpnRunning,
            onRequest = onRequestVpnConsent,
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))
        RetroButton(
            text = "Next",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun PermissionCard(title: String, description: String, isGranted: Boolean, onRequest: () -> Unit) {
    OutlinedCard(
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(
            1.dp,
            if (isGranted) CardboardColors.accentGreen.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isGranted) CardboardColors.accentGreen.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            if (isGranted) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CardboardColors.accentGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Granted",
                        tint = CardboardColors.accentGreen,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onRequest,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Text("Grant", fontFamily = MonospaceFamily, fontSize = 12.sp)
                }
            }
        }
    }
}

// ── Page 4: Why It's Different ──────────────────────────────

@Composable
private fun WhyItsDifferentPage(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 64.dp),
    ) {
        Text(
            text = "WHY IT'S DIFFERENT",
            fontFamily = MonospaceFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Built different, on purpose",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(24.dp))

        val statements = listOf(
            "Every check-in is a choice, not a punishment.",
            "Your stats are observations, not report cards.",
            "Even choosing to keep scrolling is valid.",
            "Built on research from Stanford, PNAS, and ACT therapy.",
        )

        statements.forEachIndexed { index, statement ->
            RetroCard {
                Text(
                    text = statement,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (index < statements.lastIndex) {
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))
        RetroButton(
            text = "Next",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(60.dp))
    }
}

// ── Page 5: Your First Steps ────────────────────────────────

private data class SocialApp(
    val packageName: String,
    val label: String,
)

private val socialApps = listOf(
    SocialApp("com.instagram.android", "Instagram"),
    SocialApp("com.twitter.android", "X (Twitter)"),
    SocialApp("com.snapchat.android", "Snapchat"),
    SocialApp("com.zhiliaoapp.musically", "TikTok"),
    SocialApp("com.google.android.youtube", "YouTube"),
    SocialApp("com.facebook.katana", "Facebook"),
    SocialApp("com.reddit.frontpage", "Reddit"),
)

private fun getInstalledSocialApps(context: Context): List<SocialApp> {
    val pm = context.packageManager
    return socialApps.filter { app ->
        try {
            pm.getPackageInfo(app.packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }.take(6)
}

@Composable
private fun YourFirstStepsPage(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            OnboardingEntryPoint::class.java,
        )
    }
    val repo = remember { entryPoint.appInterceptionRepository() }

    val installedApps = remember { getInstalledSocialApps(context) }
    val selectedApps = remember { mutableStateMapOf<String, Boolean>() }

    // Initialize all as unselected
    LaunchedEffect(installedApps) {
        installedApps.forEach { app ->
            if (!selectedApps.containsKey(app.packageName)) {
                selectedApps[app.packageName] = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 64.dp),
    ) {
        Text(
            text = "YOUR FIRST STEPS",
            fontFamily = MonospaceFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Pick a few apps to be mindful about",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "You can always change this later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        if (installedApps.isEmpty()) {
            RetroCard {
                Text(
                    text = "No common social apps found. You can add apps later from the toolkit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            installedApps.forEach { app ->
                val isSelected = selectedApps[app.packageName] ?: false
                RetroCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        RetroToggle(
                            checked = isSelected,
                            onCheckedChange = { selectedApps[app.packageName] = it },
                            checkedColor = ModuleColors.AppPause,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))
        RetroButton(
            text = "Start Using Faarigh",
            onClick = {
                scope.launch(Dispatchers.IO) {
                    selectedApps.forEach { (pkg, selected) ->
                        if (selected) {
                            val label = installedApps.find { it.packageName == pkg }?.label ?: pkg
                            repo.addApp(pkg, label)
                        }
                    }
                }
                onComplete()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(60.dp))
    }
}
