package com.faarigh.app.module.modules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrowseGallery
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import com.faarigh.app.R
import com.faarigh.app.data.db.dao.InterventionEventDao
import com.faarigh.app.data.preferences.ModulePreferences
import com.faarigh.app.module.CardCategory
import com.faarigh.app.module.Citation
import com.faarigh.app.module.ModuleConfigItem
import com.faarigh.app.module.ModuleStat
import com.faarigh.app.module.OnboardingCard
import com.faarigh.app.module.WellbeingModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortsBlockerModule @Inject constructor(
    private val prefs: ModulePreferences,
    private val interventionDao: InterventionEventDao,
) : WellbeingModule {

    override val id = "shorts_blocker"
    override val name = "Shorts Blocker"
    override val description = "Detects and blocks short-form video feeds in YouTube, Instagram, and TikTok"
    override val iconRes = R.drawable.ic_module_shorts_blocker
    override val accentColor = 0xFFFF9C7EL

    override val isEnabled: Flow<Boolean> = prefs.shortsBlockerEnabled
    override suspend fun setEnabled(enabled: Boolean) = prefs.setShortsBlockerEnabled(enabled)

    override val configItems: List<ModuleConfigItem> = listOf(
        ModuleConfigItem.Info(
            key = "shorts_info",
            label = "Detects and filters short-form video feeds. Uses accessibility service to identify shorts/reels and navigate away.",
        ),
        ModuleConfigItem.ToggleGroup(
            key = "shorts_apps",
            label = "Blocked apps",
            items = listOf(
                "com.google.android.youtube" to "YouTube",
                "com.instagram.android" to "Instagram",
                "com.snapchat.android" to "Snapchat",
                "com.zhiliaoapp.musically" to "TikTok",
            ),
            selectedItems = prefs.shortsBlockerApps,
            onToggleItem = { pkg, checked ->
                val current = prefs.getShortsBlockerAppsSnapshot()
                val newSet = if (checked) current + pkg else current - pkg
                prefs.setShortsBlockerApps(newSet)
            },
        ),
    )

    override val statsItems: Flow<List<ModuleStat>> = run {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + 24 * 60 * 60 * 1000L

        val todayFlow = interventionDao.getEventCountByModule("shorts_blocker", todayStart, todayEnd)
        val allTimeFlow = interventionDao.getEventCountByModule("shorts_blocker", 0L, Long.MAX_VALUE)

        combine(todayFlow, allTimeFlow) { today, allTime ->
            listOf(
                ModuleStat(label = "Blocked today", value = "$today"),
                ModuleStat(label = "Total blocks", value = "$allTime"),
            )
        }
    }

    override val educationCards = listOf(
        OnboardingCard(
            title = "Short Videos, Long Consequences",
            body = "Shorts & Reels Blocker detects when you enter the short-form " +
                "video sections of YouTube, Instagram, or TikTok. When detected, " +
                "it can either navigate you back or show a mindful intervention. " +
                "You stay in control of the app — it just removes the infinite " +
                "scroll trap.",
            icon = Icons.Default.BrowseGallery,
            category = CardCategory.WHAT_IS_IT,
        ),
        OnboardingCard(
            title = "Reading the Screen, Locally",
            body = "This uses Android's Accessibility Service to read the structure " +
                "of what's on screen — similar to how screen readers work for " +
                "visually impaired users. It looks for specific UI patterns (like " +
                "the \"Shorts\" tab in YouTube or \"Reels\" in Instagram) to detect " +
                "when you've entered a short-form feed. No screenshots are taken. " +
                "No content is analyzed. It only checks UI element labels — all on-device.",
            icon = Icons.Default.Search,
            category = CardCategory.HOW_IT_WORKS,
        ),
        OnboardingCard(
            title = "Engineered to Be Addictive",
            body = "A 2023 study published in Nature found that short-form video " +
                "platforms use variable ratio reinforcement — the same mechanism that " +
                "makes slot machines addictive. A large-scale study of 30,000+ TikTok " +
                "users found that usage beyond 30 min/day significantly predicted " +
                "anxiety, depression, and sleep disturbance. The infinite scroll and " +
                "auto-play features are specifically designed to override your natural " +
                "stopping cues.",
            icon = Icons.Default.Psychology,
            category = CardCategory.WHY_NEEDED,
            citations = listOf(
                Citation(
                    label = "Montag et al., 2021",
                    title = "On the Psychology of TikTok Use: A First Glimpse from Empirical Findings",
                    url = "https://doi.org/10.3389/fpubh.2021.641673",
                ),
                Citation(
                    label = "Bai et al., 2024",
                    title = "Association Between Short Video Addiction and Mental Health",
                    url = "https://doi.org/10.1038/s44271-024-00068-7",
                ),
                Citation(
                    label = "Tran, 2022",
                    title = "Doom Scrolling, TikTok and Attentional Capture",
                    url = "https://doi.org/10.1016/j.paid.2022.111539",
                ),
                Citation(
                    label = "Zhao, 2021",
                    title = "How Short-Form Video Features Influence Addiction",
                    url = "https://doi.org/10.1108/INTR-11-2020-0623",
                ),
            ),
            stats = listOf(
                "Average TikTok session: 10.85 minutes (Data.ai, 2023)",
                "Users who watch >30 min/day: 2.3x more likely to report anxiety symptoms",
                "Short-form video uses same reward mechanism as slot machines",
                "Average user opens TikTok 19 times per day",
            ),
        ),
        OnboardingCard(
            title = "Reclaim Hours of Your Life",
            body = "The average person spends 54 minutes per day on short-form video " +
                "(eMarketer, 2023). That's 328 hours per year — over 8 work weeks. " +
                "This module doesn't block the entire app. You can still use YouTube " +
                "to search and watch specific videos, use Instagram to message friends. " +
                "You just remove the one feature specifically designed to steal your " +
                "time through an infinite, algorithmically-optimized feed.",
            icon = Icons.Default.Timer,
            category = CardCategory.WHY_HAVE_IT,
            citations = listOf(
                Citation(
                    label = "eMarketer, 2023",
                    title = "Time Spent with Short-Form Video per Day",
                    url = "https://www.insiderintelligence.com/content/us-time-spent-with-media-2023",
                ),
                Citation(
                    label = "Allcott et al., 2022",
                    title = "Digital Addiction",
                    url = "https://doi.org/10.1257/aer.20210867",
                ),
            ),
            stats = listOf(
                "54 min/day average short-form video consumption (eMarketer, 2023)",
                "328 hours/year = 8+ full work weeks",
                "Deactivation experiments show increased life satisfaction within 1 week",
            ),
        ),
    )

    override fun onStart() { /* Started via AccessibilityService */ }
    override fun onStop() { /* Stopped via AccessibilityService */ }
}
