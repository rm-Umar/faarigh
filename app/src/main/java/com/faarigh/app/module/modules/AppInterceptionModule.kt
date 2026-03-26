package com.faarigh.app.module.modules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TouchApp
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
class AppInterceptionModule @Inject constructor(
    private val prefs: ModulePreferences,
    private val interventionDao: InterventionEventDao,
) : WellbeingModule {

    override val id = "app_pause"
    override val name = "App Pause"
    override val description = "Adds a mindful pause before opening selected apps"
    override val iconRes = R.drawable.ic_module_app_pause
    override val accentColor = 0xFFA8D5A2L

    override val isEnabled: Flow<Boolean> = prefs.appPauseEnabled
    override suspend fun setEnabled(enabled: Boolean) = prefs.setAppPauseEnabled(enabled)

    override val configItems: List<ModuleConfigItem> = listOf(
        ModuleConfigItem.Slider(
            key = "breathing_duration",
            label = "Breathing duration",
            range = 1f..10f,
            steps = 8,
            value = prefs.breathingDurationSec.map { it.toFloat() },
            valueLabel = { "${it.toInt()}s" },
            onValueChange = { prefs.setBreathingDurationSec(it.toInt()) },
        ),
        ModuleConfigItem.Slider(
            key = "cooldown",
            label = "Cooldown period",
            range = 5f..120f,
            steps = 22,
            value = prefs.appPauseCooldownMin.map { it.toFloat() },
            valueLabel = { "${it.toInt()} min" },
            onValueChange = { prefs.setAppPauseCooldownMin(it.toInt()) },
        ),
        ModuleConfigItem.Choice(
            key = "breathing_pattern",
            label = "Breathing pattern",
            options = listOf(
                "simple" to "Simple Pause",
                "sigh" to "Physiological Sigh",
                "box" to "Box Breathing",
            ),
            value = prefs.preferredBreathingPattern,
            onSelect = { prefs.setPreferredBreathingPattern(it) },
        ),
        ModuleConfigItem.Slider(
            key = "medium_threshold",
            label = "Medium escalation threshold",
            range = 5f..60f,
            steps = 10,
            value = prefs.mediumThresholdMin.map { it.toFloat() },
            valueLabel = { "${it.toInt()} min" },
            onValueChange = { prefs.setMediumThresholdMin(it.toInt()) },
        ),
        ModuleConfigItem.Slider(
            key = "deep_threshold",
            label = "Deep escalation threshold",
            range = 15f..120f,
            steps = 20,
            value = prefs.deepThresholdMin.map { it.toFloat() },
            valueLabel = { "${it.toInt()} min" },
            onValueChange = { prefs.setDeepThresholdMin(it.toInt()) },
        ),
        ModuleConfigItem.Info(
            key = "escalation_info",
            label = "Mid-session check-in: the app will periodically check in during long sessions to help you stay aware of your usage and make conscious choices about continuing.",
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

        val totalFlow = interventionDao.getEventCountByModule("app_pause", todayStart, todayEnd)
        val allowedBlockedFlow = interventionDao.getAllowedVsBlockedByModule("app_pause", todayStart, todayEnd)

        combine(totalFlow, allowedBlockedFlow) { total, counts ->
            val blocked = counts.find { it.action == "blocked" }?.count ?: 0
            val allowed = counts.find { it.action == "allowed" }?.count ?: 0
            val decisions = blocked + allowed
            val successRate = if (decisions > 0) (blocked * 100 / decisions) else 0

            listOf(
                ModuleStat(label = "Pauses today", value = "$total"),
                ModuleStat(label = "Went back", value = "$blocked"),
                ModuleStat(label = "Continued", value = "$allowed"),
                ModuleStat(label = "Success rate", value = "$successRate%"),
            )
        }
    }

    override val educationCards = listOf(
        OnboardingCard(
            title = "A Pause Before You Scroll",
            body = "App Interception creates a brief moment of awareness when you open " +
                "apps you've selected. Instead of mindlessly tapping and scrolling, " +
                "you get a chance to ask yourself: \"Do I actually want to do this right now?\"",
            icon = Icons.Default.TouchApp,
            category = CardCategory.WHAT_IS_IT,
        ),
        OnboardingCard(
            title = "Runs Entirely On Your Device",
            body = "This feature uses Android's Accessibility Service to detect when " +
                "you open a selected app. It then shows an overlay — a brief pause — " +
                "before you proceed. Everything happens locally. No data about which " +
                "apps you use ever leaves your phone. No servers, no analytics, no tracking.",
            icon = Icons.Default.Lock,
            category = CardCategory.HOW_IT_WORKS,
        ),
        OnboardingCard(
            title = "Your Brain on Auto-Pilot",
            body = "Research on implementation intentions shows that inserting a " +
                "decision point between an urge and an action significantly improves " +
                "self-regulation. A 2023 study on the \"One Sec\" intervention found " +
                "that a simple friction delay reduced unwanted social media use by 57%. " +
                "We pick up our phones an average of 96 times per day — most of those " +
                "times without any conscious intent.",
            icon = Icons.Default.Psychology,
            category = CardCategory.WHY_NEEDED,
            citations = listOf(
                Citation(
                    label = "Gollwitzer, 1999",
                    title = "Implementation Intentions: Strong Effects of Simple Plans",
                    url = "https://doi.org/10.1037/0003-066X.54.7.493",
                ),
                Citation(
                    label = "Lyngs et al., 2020",
                    title = "\"I Just Want to Hack Myself to Not Get Distracted\": Evaluating Design Interventions for Self-Control on Facebook",
                    url = "https://doi.org/10.1145/3313831.3376672",
                ),
                Citation(
                    label = "Lukas & Elhai, 2023",
                    title = "A Friction Intervention Reduces Problematic Smartphone Use",
                    url = "https://doi.org/10.1016/j.chbr.2023.100319",
                ),
            ),
            stats = listOf(
                "57% reduction in unwanted app opens with friction delay (Lukas & Elhai, 2023)",
                "Habit accounts for ~43% of daily behavior (Wood et al., 2002 — Journal of Personality and Social Psychology)",
                "Implementation intentions double the likelihood of follow-through (Gollwitzer & Sheeran, 2006 meta-analysis)",
            ),
        ),
        OnboardingCard(
            title = "Take Back Your Attention",
            body = "This isn't about blocking or punishing yourself. It's about " +
                "turning unconscious scrolling into a conscious choice. You can always " +
                "proceed — the app respects your decision either way. But research " +
                "shows that even a brief pause activates your prefrontal cortex — " +
                "the brain region responsible for deliberate decision-making — " +
                "overriding the habit loop in the basal ganglia.",
            icon = Icons.Default.AccessTime,
            category = CardCategory.WHY_HAVE_IT,
            citations = listOf(
                Citation(
                    label = "Duhigg, 2012",
                    title = "The Power of Habit: Why We Do What We Do in Life and Business",
                    url = "https://charlesduhigg.com/the-power-of-habit/",
                ),
                Citation(
                    label = "Wood & Neal, 2007",
                    title = "A New Look at Habits and the Habit-Goal Interface",
                    url = "https://doi.org/10.1037/0033-295X.114.4.843",
                ),
            ),
        ),
    )

    override fun onStart() { /* Started via AccessibilityService */ }
    override fun onStop() { /* Stopped via AccessibilityService */ }
}
