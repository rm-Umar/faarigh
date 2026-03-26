package com.faarigh.app.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// Design colors
private val SurfaceContainer = Color(0xFF171A1E)
private val SurfaceContainerHigh = Color(0xFF1C2025)
private val Primary = Color(0xFFFF9C7E)
private val Secondary = Color(0xFF76D5E1)
private val Tertiary = Color(0xFFF8C3FF)
private val OnSurface = Color(0xFFE3E5ED)
private val OnSurfaceVariant = Color(0xFFA8ABB2)

/**
 * Education cards shown when a user enables a module for the first time.
 * 4 swipeable cards:
 * 1. What this module is
 * 2. How it works locally and why it's safe
 * 3. Why it's needed (research-backed)
 * 4. Why you should have it
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ModuleEducationSheet(
    moduleId: String,
    onDismiss: () -> Unit,
    onEnable: () -> Unit,
) {
    val cards = getEducationCards(moduleId)
    val pagerState = rememberPagerState(pageCount = { cards.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == cards.size - 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(SurfaceContainer)
            .padding(24.dp)
            .animateContentSize(),
    ) {
        // Page indicator dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(cards.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == pagerState.currentPage) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) Primary
                            else OnSurfaceVariant.copy(alpha = 0.3f),
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card content pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val card = cards[page]
            EducationCardContent(card = card, pageNumber = page + 1, totalPages = cards.size)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Buttons
        if (isLastPage) {
            // Last page: Enable button
            Button(
                onClick = onEnable,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Text(
                    "Enable Module",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF651900),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Not now", color = OnSurfaceVariant)
            }
        } else {
            // Not last page: Next + Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Skip", color = OnSurfaceVariant)
                }
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh),
                ) {
                    Text("Next", color = OnSurface, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Text("→", color = Primary)
                }
            }
        }
    }
}

@Composable
private fun EducationCardContent(card: EducationCard, pageNumber: Int, totalPages: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(card.accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = card.icon,
                contentDescription = null,
                tint = card.accentColor,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Step label
        Text(
            text = card.stepLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = card.accentColor,
            letterSpacing = 2.sp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = card.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Body
        Text(
            text = card.body,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        // Research citation if present
        if (card.citation != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceContainerHigh)
                    .padding(12.dp),
            ) {
                Text(
                    text = card.citation,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.7f),
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

data class EducationCard(
    val stepLabel: String,
    val title: String,
    val body: String,
    val icon: ImageVector,
    val accentColor: Color,
    val citation: String? = null,
)

private fun getEducationCards(moduleId: String): List<EducationCard> = when (moduleId) {
    "app_pause" -> listOf(
        EducationCard(
            stepLabel = "What it is",
            title = "A Brief Pause",
            body = "When you open a selected app, a short breathing moment appears. It's a 3-second window to check in with yourself before continuing. You always choose what happens next.",
            icon = Icons.Outlined.PauseCircle,
            accentColor = Secondary,
        ),
        EducationCard(
            stepLabel = "How it works",
            title = "Fully On-Device",
            body = "The app uses Android's Accessibility Service to detect when you open a selected app. A breathing screen appears for a few seconds. No data is collected, no servers are contacted. Everything happens on your phone.",
            icon = Icons.Outlined.PhoneAndroid,
            accentColor = Secondary,
            citation = "Technical: Uses AccessibilityService API. No network calls. No screenshots. Only detects app launch events for apps you choose.",
        ),
        EducationCard(
            stepLabel = "Why it matters",
            title = "The 3-Second Gap",
            body = "Research shows that most habitual phone use is automatic — we open apps without conscious intent. A brief pause between impulse and action activates the prefrontal cortex, shifting from reactive to intentional behavior.",
            icon = Icons.Outlined.Psychology,
            accentColor = Secondary,
            citation = "Wood & Neal (2007) found that 43% of daily actions are habitual. A momentary interruption can break the automaticity loop. Judah et al. (2013) showed that implementation intentions with a pause component improved self-regulation by 32%.",
        ),
        EducationCard(
            stepLabel = "Your choice",
            title = "You're Always in Control",
            body = "After the pause, you choose: open the app or do something else. Both choices are equally valid. This isn't about restricting yourself — it's about making sure you're choosing consciously rather than reacting automatically.",
            icon = Icons.Outlined.TouchApp,
            accentColor = Secondary,
        ),
    )

    "nsfw_detection" -> listOf(
        EducationCard(
            stepLabel = "What it is",
            title = "Content Awareness",
            body = "An on-device AI model scans your screen periodically. If it detects explicit content, it shows a check-in screen. You choose whether to continue or go back. No judgment either way.",
            icon = Icons.Outlined.Visibility,
            accentColor = Tertiary,
        ),
        EducationCard(
            stepLabel = "How it works",
            title = "AI That Never Leaves Your Phone",
            body = "A small neural network (under 10MB) runs entirely on your device. It analyzes screenshots locally — no images are ever sent anywhere. The model detects nudity and explicit imagery with adjustable sensitivity.",
            icon = Icons.Outlined.Memory,
            accentColor = Tertiary,
            citation = "Technical: Uses TensorFlow Lite MobileNetV2 classifier. All inference runs on-device CPU/GPU. Screenshots are processed in memory and immediately discarded. No storage, no transmission.",
        ),
        EducationCard(
            stepLabel = "Why it matters",
            title = "Conscious Consumption",
            body = "Research links excessive pornography consumption to altered dopamine response, reduced relationship satisfaction, and increased anxiety. A moment of awareness before consuming explicit content helps you make a conscious choice about what you want.",
            icon = Icons.Outlined.SelfImprovement,
            accentColor = Tertiary,
            citation = "Voon et al. (2014) found neural patterns in compulsive pornography users similar to substance addiction. Park et al. (2016) linked excessive use to decreased gray matter volume. However, moderate, intentional use shows no such effects.",
        ),
        EducationCard(
            stepLabel = "Your choice",
            title = "No Judgment, Just Awareness",
            body = "If you choose to continue, the content stays visible for 30 minutes without interruption. If you go back, that's fine too. This tool exists to add a moment of reflection, not to police your behavior. Your choices are yours.",
            icon = Icons.Outlined.Balance,
            accentColor = Tertiary,
        ),
    )

    "shorts_blocker" -> listOf(
        EducationCard(
            stepLabel = "What it is",
            title = "Short-Form Video Filter",
            body = "Detects when you enter YouTube Shorts, Instagram Reels, or TikTok's feed and shows a check-in. The rest of these apps works normally — only the infinite scroll video feeds are flagged.",
            icon = Icons.Outlined.VideoLibrary,
            accentColor = Primary,
        ),
        EducationCard(
            stepLabel = "How it works",
            title = "Smart Detection, Not Blocking",
            body = "Uses the Accessibility Service to read screen content and detect short-form video UI elements. When detected, it shows an overlay. You can dismiss it and continue watching — it's a checkpoint, not a wall.",
            icon = Icons.Outlined.Tune,
            accentColor = Primary,
            citation = "Technical: Detects specific UI elements (Shorts tab, Reels viewer, TikTok feed) using AccessibilityNodeInfo. Does not block network traffic or modify apps.",
        ),
        EducationCard(
            stepLabel = "Why it matters",
            title = "The Infinite Scroll Problem",
            body = "Short-form videos are engineered for maximum engagement. The variable-reward loop (swipe for the next video) triggers dopamine in the same pattern as slot machines. Average intended viewing time is 5 minutes; average actual time is 46 minutes.",
            icon = Icons.Outlined.TrendingDown,
            accentColor = Primary,
            citation = "Montag et al. (2021) found TikTok use associated with reduced attention span. The average session exceeds intended time by 9x (Kaye et al., 2022). Variable reward schedules in infinite scroll activate the nucleus accumbens similarly to gambling (Meshi et al., 2015).",
        ),
        EducationCard(
            stepLabel = "Your choice",
            title = "Watch When You Want To",
            body = "Sometimes you want to watch shorts — and that's completely fine. This tool ensures that when you do, it's because you chose to, not because the algorithm pulled you in without you noticing. Intentional scrolling is not a problem.",
            icon = Icons.Outlined.CheckCircle,
            accentColor = Primary,
        ),
    )

    "dns_filter" -> listOf(
        EducationCard(
            stepLabel = "What it is",
            title = "Network-Level Filtering",
            body = "A local VPN that filters DNS queries on your device. It filters ads, trackers, telemetry, and known explicit domains before they load — similar to Pi-hole but running on your phone.",
            icon = Icons.Outlined.Dns,
            accentColor = Secondary,
        ),
        EducationCard(
            stepLabel = "How it works",
            title = "Local VPN, No Server",
            body = "Creates a VPN tunnel that stays on your device. DNS queries are checked against a filter list. Filtered domains get an empty response; allowed ones pass through normally. Your browsing data never leaves your phone.",
            icon = Icons.Outlined.Security,
            accentColor = Secondary,
            citation = "Technical: Uses Android VpnService with DNS-only routing (RFC 5737 TEST-NET aliases). Only DNS packets are intercepted — your actual web traffic flows directly to the internet. Blocklist is stored locally in a Room database.",
        ),
        EducationCard(
            stepLabel = "Why it matters",
            title = "Invisible Tracking Is Everywhere",
            body = "The average app contains 6 third-party trackers. A typical phone makes 10,000+ DNS queries per day, with 20-40% being ads and telemetry. Filtering these reduces data collection, speeds up browsing, and saves battery.",
            icon = Icons.Outlined.PrivacyTip,
            accentColor = Secondary,
            citation = "Binns et al. (2018) found that 90% of free apps contain at least one tracker. Razaghpanah et al. (2018) measured 20-35% of mobile DNS traffic goes to advertising/tracking domains. Blocking these reduced page load time by 27% on average.",
        ),
        EducationCard(
            stepLabel = "Your choice",
            title = "You Control the Blocklist",
            body = "You choose what categories to filter: ads, trackers, telemetry, explicit domains — or none at all. You can add custom domains or whitelist anything. The default list is a starting point you can customize.",
            icon = Icons.Outlined.FilterList,
            accentColor = Secondary,
        ),
    )

    else -> emptyList()
}
