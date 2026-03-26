package com.faarigh.app.data.learn

data class LearnArticle(
    val id: String,
    val title: String,
    val subtitle: String,
    val accentColor: Long, // Color as Long (e.g., 0xFF5A8A54)
    val sections: List<LearnSection>,
)

data class LearnSection(
    val heading: String? = null,
    val body: String,
    val citation: String? = null,
)

object LearnContentProvider {
    val articles: List<LearnArticle> = listOf(
        // Article 1: How Your Brain Works
        LearnArticle(
            id = "dopamine",
            title = "How Your Brain Works",
            subtitle = "Dopamine, habits, and why you keep checking",
            accentColor = 0xFF5A8A54,
            sections = listOf(
                LearnSection(heading = "The Dopamine Loop", body = "Every time you check your phone and find something interesting \u2014 a like, a message, a funny video \u2014 your brain releases dopamine. This feels good, so your brain wants to do it again. The trick is: your brain releases MORE dopamine when the reward is unpredictable, just like a slot machine."),
                LearnSection(heading = "Variable Rewards", body = "Social media apps are designed around variable reward schedules. Sometimes you scroll and find something amazing. Sometimes nothing. This unpredictability is what makes it so hard to stop \u2014 your brain is always hoping the next scroll will be the jackpot.", citation = "Sharpe & Spooner, 2025, SAGE Journals"),
                LearnSection(heading = "Habit vs. Choice", body = "Most phone checking is habitual \u2014 you do it without thinking. Research shows that even when notifications are disabled, people check their phones just as often because the behavior has become automatic.", citation = "Tandfonline, 2024"),
                LearnSection(heading = "The Good News", body = "Habits can be interrupted. By adding a brief pause between the urge and the action, you activate your brain\u2019s deliberate thinking system. This is exactly what Faarigh does \u2014 it creates that pause so you can choose consciously."),
            ),
        ),
        // Article 2: The Science of Scrolling
        LearnArticle(
            id = "scrolling",
            title = "The Science of Scrolling",
            subtitle = "Why short-form video is designed to hook you",
            accentColor = 0xFFE57373,
            sections = listOf(
                LearnSection(heading = "Infinite Scroll", body = "The infinite scroll was designed to remove stopping cues. In a book, you see the end of a chapter. On a feed, there is no end. Your brain never gets a natural signal to stop."),
                LearnSection(heading = "Short-Form Video", body = "Short videos (TikTok, Reels, Shorts) are especially potent because each video is a complete reward cycle in 15\u201360 seconds. Your brain gets rapid-fire dopamine hits that are hard to walk away from."),
                LearnSection(heading = "The Numbers", body = "Research shows daily screen time of 4+ hours is associated with 45% higher anxiety risk. Evening smartphone use delays your circadian rhythm by 1.7 minutes per hour of pre-sleep use.", citation = "CDC, 2025; Oxford Academic, 2024"),
                LearnSection(heading = "It\u2019s Not About Willpower", body = "These apps employ thousands of engineers optimizing for engagement. Struggling to put your phone down isn\u2019t a personal failure \u2014 it\u2019s by design. Tools like Faarigh level the playing field."),
            ),
        ),
        // Article 3: Breathing & Your Nervous System
        LearnArticle(
            id = "breathing",
            title = "Breathing & Your Nervous System",
            subtitle = "Why a simple breath can change everything",
            accentColor = 0xFF9C7EDB,
            sections = listOf(
                LearnSection(heading = "The Physiological Sigh", body = "Stanford researchers found that a specific breathing pattern \u2014 a double inhale through the nose followed by a long exhale through the mouth \u2014 is the fastest known way to calm your nervous system in real time. One cycle takes about 8\u201310 seconds.", citation = "Stanford Cell Reports Medicine, 2023"),
                LearnSection(heading = "How It Works", body = "The double inhale fully inflates the tiny air sacs in your lungs, maximizing the surface area for gas exchange. The extended exhale activates your parasympathetic nervous system \u2014 the \u2018rest and digest\u2019 system \u2014 slowing your heart rate and reducing stress."),
                LearnSection(heading = "Pattern Interrupt", body = "Even a brief 3\u20135 second pause breaks the automatic open-scroll-scroll loop. It forces your brain to switch from automatic (System 1) to deliberate (System 2) thinking.", citation = "One Sec PNAS Study, Gamba et al., 2023"),
                LearnSection(heading = "In Faarigh", body = "Faarigh uses the physiological sigh as your default breathing exercise. When you first open an app, you get a light pause. As your usage increases, the exercises become longer and more calming \u2014 helping you make better decisions when you need it most."),
            ),
        ),
        // Article 4: Building Better Habits
        LearnArticle(
            id = "habits",
            title = "Building Better Habits",
            subtitle = "Practical tips from behavioral science",
            accentColor = 0xFFFFB74D,
            sections = listOf(
                LearnSection(heading = "The Power of Choice", body = "The most effective component of friction-based apps is the dismiss option \u2014 simply having the choice to say \u2018not right now\u2019 reduced app usage by 57% in a rigorous study. You don\u2019t need to be locked out. You just need to be asked.", citation = "PNAS, Gamba et al., 2023"),
                LearnSection(heading = "Commitment Devices", body = "Faarigh is what behavioral scientists call a \u2018commitment device\u2019 \u2014 a tool you set up in a clear-headed moment to help your future self make better decisions. You\u2019re both the architect and the beneficiary."),
                LearnSection(heading = "Break Days Are OK", body = "Research shows that users who take periodic breaks from their digital wellbeing tools quickly rebound when they return. Taking a day off doesn\u2019t undo your progress \u2014 it might even prevent burnout.", citation = "CHI 2024 Longitudinal Study"),
                LearnSection(heading = "Small Wins Matter", body = "Every time you choose to close an app instead of scrolling, that\u2019s a conscious choice. Track these wins \u2014 they compound over time. Your \u2018intentional-use ratio\u2019 is more important than raw screen time."),
            ),
        ),
        // Article 5: Understanding Your Data
        LearnArticle(
            id = "data",
            title = "Understanding Your Data",
            subtitle = "How to read your stats meaningfully",
            accentColor = 0xFF5A8A54,
            sections = listOf(
                LearnSection(heading = "Beyond Screen Time", body = "Raw screen time isn\u2019t the full picture. Two hours of intentional video calls with family is very different from two hours of mindless scrolling. What matters is whether your phone use aligns with your values."),
                LearnSection(heading = "Intentional-Use Ratio", body = "This is your most important metric. It measures how often you choose to go back vs. continue when Faarigh pauses you. A ratio of 60% means 6 out of 10 times, you made a conscious choice to close the app."),
                LearnSection(heading = "Trends Over Counts", body = "Don\u2019t focus on today\u2019s numbers \u2014 look at weekly and monthly trends. Are you making more conscious choices this week than last? Is your social media time trending down? Progress, not perfection."),
                LearnSection(heading = "No Shame, Just Data", body = "Your stats are tools for self-reflection, not judgment. Faarigh never frames your data negatively. \u2018You made 5 conscious choices today\u2019 is more useful than \u2018You spent 3 hours on Instagram.\u2019"),
            ),
        ),
    )
}
