package com.faarigh.app.module

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a research citation linked from an onboarding card.
 */
data class Citation(
    val label: String,   // e.g., "Gollwitzer, 1999"
    val title: String,   // Full paper title
    val url: String,     // DOI or URL
)

/**
 * Represents a single education card shown during module onboarding.
 * Each module provides a list of these cards to educate the user
 * before they enable the feature.
 */
data class OnboardingCard(
    val title: String,
    val body: String,
    val icon: ImageVector,
    val category: CardCategory,
    val citations: List<Citation> = emptyList(),
    val stats: List<String> = emptyList(), // Key statistics to highlight
)

/**
 * Categories that structure the education flow.
 * Every module should have at least one card per category.
 */
enum class CardCategory(val label: String) {
    WHAT_IS_IT("What is this?"),
    HOW_IT_WORKS("How it works"),
    WHY_NEEDED("Why it's needed"),
    WHY_HAVE_IT("Why you should enable it"),
}

// Note: education cards are now part of the base WellbeingModule interface.
// The EducationalModule sub-interface has been removed — all modules
// provide educationCards directly via WellbeingModule.
