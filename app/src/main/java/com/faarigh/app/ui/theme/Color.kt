package com.faarigh.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// Faarigh — Cardboard Retro Design System
// Warm cream paper backgrounds, monospace headings, outlined cards
// ═══════════════════════════════════════════════════════════════

// ── Light Mode (primary) ─────────────────────────────────────

val CardboardRetroLight = lightColorScheme(
    // Cream paper backgrounds
    background = Color(0xFFF5F0E8),
    surface = Color(0xFFF5F0E8),
    surfaceContainerLowest = Color(0xFFFFFDF8),
    surfaceContainerLow = Color(0xFFF0EADE),
    surfaceContainer = Color(0xFFEBE4D8),
    surfaceContainerHigh = Color(0xFFE5DED2),
    surfaceContainerHighest = Color(0xFFDFD8CC),
    surfaceVariant = Color(0xFFE6E0D4),
    surfaceBright = Color(0xFFFAF6EE),

    // Green accent
    primary = Color(0xFF5A8A54),
    primaryContainer = Color(0xFFC8E6C2),
    onPrimary = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFF1A3518),

    // Purple accent
    secondary = Color(0xFF7B5E99),
    secondaryContainer = Color(0xFFE8D8F4),
    onSecondary = Color(0xFFFFFFFF),
    onSecondaryContainer = Color(0xFF2D1A40),

    // Amber accent
    tertiary = Color(0xFF9A7B2F),
    tertiaryContainer = Color(0xFFF5E6B0),
    onTertiary = Color(0xFFFFFFFF),
    onTertiaryContainer = Color(0xFF352A0C),

    // Coral for errors/warnings
    error = Color(0xFFBF4D3A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD2),
    onErrorContainer = Color(0xFF410E04),

    // Text on cream
    onBackground = Color(0xFF2C2416),
    onSurface = Color(0xFF2C2416),
    onSurfaceVariant = Color(0xFF6B5E4F),

    // Borders
    outline = Color(0xFF8C7E6E),
    outlineVariant = Color(0xFFC4B8A8),

    inverseSurface = Color(0xFF342D22),
    inverseOnSurface = Color(0xFFF0E8DC),
    inversePrimary = Color(0xFFA8D5A2),
    surfaceTint = Color(0xFF5A8A54),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFFE0D8CC),
)

// ── Dark Mode ────────────────────────────────────────────────

val CardboardRetroDark = darkColorScheme(
    // Dark mode — cards subtle, borders bright
    background = Color(0xFF141210),
    surface = Color(0xFF1E1B17),
    surfaceContainerLowest = Color(0xFF0F0D0B),
    surfaceContainerLow = Color(0xFF1A1714),
    surfaceContainer = Color(0xFF211E19),
    surfaceContainerHigh = Color(0xFF28241E),
    surfaceContainerHighest = Color(0xFF302B24),
    surfaceVariant = Color(0xFF28241E),
    surfaceBright = Color(0xFF352F27),

    // Green accent (brightened for dark bg)
    primary = Color(0xFFA8D5A2),
    primaryContainer = Color(0xFF3D6B38),
    onPrimary = Color(0xFF1A3518),
    onPrimaryContainer = Color(0xFFC8E6C2),

    // Purple accent
    secondary = Color(0xFFC4A8E0),
    secondaryContainer = Color(0xFF5C4278),
    onSecondary = Color(0xFF2D1A40),
    onSecondaryContainer = Color(0xFFE8D8F4),

    // Amber accent
    tertiary = Color(0xFFF5D76E),
    tertiaryContainer = Color(0xFF6B5A1E),
    onTertiary = Color(0xFF352A0C),
    onTertiaryContainer = Color(0xFFF5E6B0),

    // Coral for errors
    error = Color(0xFFFF9C7E),
    onError = Color(0xFF410E04),
    errorContainer = Color(0xFF8C2F1E),
    onErrorContainer = Color(0xFFFFDAD2),

    // Bright text for readability on dark
    onBackground = Color(0xFFF5F0E8),
    onSurface = Color(0xFFF5F0E8),
    onSurfaceVariant = Color(0xFFCCC0B2),

    // Borders — brighter in dark mode for retro pop
    outline = Color(0xFF9A8E80),
    outlineVariant = Color(0xFF5A5040),

    inverseSurface = Color(0xFFF0E8DC),
    inverseOnSurface = Color(0xFF342D22),
    inversePrimary = Color(0xFF5A8A54),
    surfaceTint = Color(0xFFA8D5A2),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFF1A1612),
)

// ── Semantic colors for retro aesthetic ───────────────────────

object CardboardColors {
    val paperLight = Color(0xFFF5F0E8)
    val paperDark = Color(0xFF1A1612)
    val cardBorder = Color(0xFFC4B8A8)
    val dashedBorder = Color(0xFFB0A494)
    val gridLine = Color(0xFFE0D8CC)
    val accentGreen = Color(0xFFA8D5A2)
    val accentPurple = Color(0xFFC4A8E0)
    val accentAmber = Color(0xFFF5D76E)
    val accentCoral = Color(0xFFFF9C7E)
}

// ── Module accent colors ─────────────────────────────────────

object ModuleColors {
    val AppPause = Color(0xFFA8D5A2)        // green
    val NsfwShield = Color(0xFFC4A8E0)      // purple
    val ScreenTime = Color(0xFFFF9C7E)      // coral
    val DnsFilter = Color(0xFFF5D76E)       // amber
    val ShortsBlocker = Color(0xFFFF9C7E)   // coral
    val FocusMode = Color(0xFFA8D5A2)
    val SleepMode = Color(0xFFC4A8E0)
    val DoomScroll = Color(0xFFF5D76E)

    val appInterception get() = AppPause
    val nsfwDetection get() = NsfwShield
}

// ── Chart colors ─────────────────────────────────────────────

object ChartColors {
    val area1 = Color(0xFFA8D5A2)     // Green
    val area2 = Color(0xFFC4A8E0)     // Purple
    val area3 = Color(0xFFF5D76E)     // Amber
    val blocked = Color(0xFFFF9C7E)   // Coral
    val allowed = Color(0xFFA8D5A2)   // Green
    val sparkline = Color(0xFF5A8A54) // Dark green
}

// ── Backward-compat aliases ──────────────────────────────────

val FaarighDarkColors = CardboardRetroDark
val FaarighLightColors = CardboardRetroLight
