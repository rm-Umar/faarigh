package com.faarigh.app.module.modules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
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
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NsfwDetectionModule @Inject constructor(
    private val prefs: ModulePreferences,
    private val interventionDao: InterventionEventDao,
) : WellbeingModule {

    override val id = "nsfw_detection"
    override val name = "Content Awareness"
    override val description = "Detects explicit visual content on-screen using on-device AI"
    override val iconRes = R.drawable.ic_module_content_awareness
    override val accentColor = 0xFFC4A8E0L

    override val isEnabled: Flow<Boolean> = prefs.nsfwEnabled
    override suspend fun setEnabled(enabled: Boolean) = prefs.setNsfwEnabled(enabled)

    override val configItems: List<ModuleConfigItem> = listOf(
        ModuleConfigItem.Slider(
            key = "nsfw_threshold",
            label = "Detection sensitivity",
            range = 0.3f..0.95f,
            steps = 12,
            value = prefs.nsfwThreshold,
            valueLabel = { "${"%.0f".format(it * 100)}%" },
            onValueChange = { prefs.setNsfwThreshold(it) },
        ),
        ModuleConfigItem.Slider(
            key = "nsfw_interval",
            label = "Scan interval",
            range = 500f..5000f,
            steps = 8,
            value = prefs.nsfwScanIntervalMs.map { it.toFloat() },
            valueLabel = { "${(it / 1000).toInt()}s" },
            onValueChange = { prefs.setNsfwScanIntervalMs(it.toLong()) },
        ),
        ModuleConfigItem.Slider(
            key = "nsfw_cooldown",
            label = "Cooldown after allowing",
            range = 5f..120f,
            steps = 22,
            value = prefs.nsfwAllowCooldownMin.map { it.toFloat() },
            valueLabel = { "${it.toInt()} min" },
            onValueChange = { prefs.setNsfwAllowCooldownMin(it.toInt()) },
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

        val totalFlow = interventionDao.getNsfwEventCount(todayStart, todayEnd)
        val allowedBlockedFlow = interventionDao.getNsfwAllowedVsBlocked(todayStart, todayEnd)
        val falsePositiveFlow = interventionDao.getEventCountByAction("false_positive", todayStart, todayEnd)

        combine(totalFlow, allowedBlockedFlow, falsePositiveFlow) { total, counts, falsePositives ->
            val blocked = counts.find { it.action == "blocked" }?.count ?: 0

            listOf(
                ModuleStat(label = "Detections today", value = "$total"),
                ModuleStat(label = "Went back", value = "$blocked"),
                ModuleStat(label = "False positives", value = "$falsePositives"),
            )
        }
    }

    override val educationCards = listOf(
        OnboardingCard(
            title = "On-Device Content Awareness",
            body = "Content Detection uses a small AI model running entirely on your " +
                "phone to analyze what's on screen. When it detects explicit visual " +
                "content, it triggers a mindful intervention — giving you a moment " +
                "to consciously decide if this is what you want to be doing right now.",
            icon = Icons.Default.RemoveRedEye,
            category = CardCategory.WHAT_IS_IT,
        ),
        OnboardingCard(
            title = "AI That Never Phones Home",
            body = "The detection model (TensorFlow Lite) is a tiny neural network " +
                "bundled inside the app — about 3MB. It processes screen captures " +
                "in memory and immediately discards them. No images are ever saved " +
                "to storage. No data is sent anywhere. The model runs in milliseconds " +
                "and only activates when you're in a non-trusted app with new content " +
                "on screen. Your phone, your processing, your privacy.",
            icon = Icons.Default.Memory,
            category = CardCategory.HOW_IT_WORKS,
        ),
        OnboardingCard(
            title = "The Compulsive Consumption Loop",
            body = "A Cambridge University study using fMRI brain scans showed that " +
                "compulsive pornography users show the same neural activation patterns " +
                "as substance addicts — particularly in the ventral striatum (the brain's " +
                "reward center). A separate 2019 meta-analysis of 22 studies found a " +
                "significant link between problematic consumption and reduced psychological " +
                "wellbeing, relationship satisfaction, and self-esteem. The issue isn't " +
                "the content — it's the unconscious, compulsive loop.",
            icon = Icons.Default.SelfImprovement,
            category = CardCategory.WHY_NEEDED,
            citations = listOf(
                Citation(
                    label = "Voon et al., 2014",
                    title = "Neural Correlates of Sexual Cue Reactivity in Individuals with and without Compulsive Sexual Behaviours",
                    url = "https://doi.org/10.1371/journal.pone.0102419",
                ),
                Citation(
                    label = "Grubbs et al., 2019",
                    title = "Internet Pornography Use: Perceived Addiction, Psychological Distress, and the Validation of a Brief Measure",
                    url = "https://doi.org/10.1556/2006.7.2018.87",
                ),
                Citation(
                    label = "Wright et al., 2017",
                    title = "A Meta-Analysis of Pornography Consumption and Actual Acts of Sexual Aggression",
                    url = "https://doi.org/10.1177/0093650215614281",
                ),
            ),
            stats = listOf(
                "Compulsive users show identical ventral striatum activation as substance addicts (Voon et al., 2014 — PLOS ONE)",
                "Significant association between problematic use and reduced wellbeing across 22 studies (Grubbs et al., 2019 meta-review)",
                "ACT-based interventions show 85% reduction in viewing at 3-month follow-up (Twohig & Crosby, 2010)",
            ),
        ),
        OnboardingCard(
            title = "Awareness Without Judgment",
            body = "This module doesn't block, shame, or punish. It applies the same " +
                "philosophy as the rest of Mindful: bring conscious awareness to " +
                "automatic behavior. Acceptance and Commitment Therapy (ACT) research " +
                "shows that non-judgmental awareness of urges — rather than suppression — " +
                "is the most effective approach to behavior change. You always choose " +
                "what happens next.",
            icon = Icons.Default.Security,
            category = CardCategory.WHY_HAVE_IT,
            citations = listOf(
                Citation(
                    label = "Twohig & Crosby, 2010",
                    title = "Acceptance and Commitment Therapy as a Treatment for Problematic Internet Pornography Viewing",
                    url = "https://doi.org/10.1016/j.beth.2009.06.002",
                ),
                Citation(
                    label = "Hayes et al., 2006",
                    title = "Acceptance and Commitment Therapy: Model, Processes and Outcomes",
                    url = "https://doi.org/10.1016/j.brat.2005.06.006",
                ),
            ),
        ),
    )

    override fun onStart() { /* Started via AccessibilityService screenshot scanning */ }
    override fun onStop() { /* Stopped via AccessibilityService */ }
}
