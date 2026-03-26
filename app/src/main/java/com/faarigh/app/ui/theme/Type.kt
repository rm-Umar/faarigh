package com.faarigh.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.faarigh.app.R

// ═══════════════════════════════════════════════════════════════
// Faarigh — Cardboard Retro Typography
// Press Start 2P for display/headlines (pixel 8-bit font)
// VT323 for body/labels/titles (retro terminal font)
// JetBrains Mono kept as JetBrainsFamily for fallback
// ═══════════════════════════════════════════════════════════════

/** Pixel 8-bit font — for display/headline sizes */
val PixelFamily = FontFamily(
    Font(R.font.press_start_2p, FontWeight.Normal),
)

/** Retro terminal font — for body/labels/titles */
val RetroFamily = FontFamily(
    Font(R.font.vt323_regular, FontWeight.Normal),
)

/** JetBrains Mono — kept available for specific uses */
val JetBrainsFamily = FontFamily(
    Font(R.font.jetbrainsmono_regular, FontWeight.Normal),
    Font(R.font.jetbrainsmono_bold, FontWeight.Bold),
)

/**
 * MonospaceFamily — used across all screens for labels, stats, headings.
 * Points to JetBrains Mono for readability with retro feel.
 * PixelFamily used for big display/headline text only.
 */
val MonospaceFamily = JetBrainsFamily

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

val FaarighTypography = Typography(
    // ── Display: Press Start 2P (smaller sizes — pixel font is chunky)
    displayLarge = TextStyle(
        fontFamily = PixelFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = PixelFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = PixelFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),

    // ── Headlines: Press Start 2P ─────────────────────────────
    headlineLarge = TextStyle(
        fontFamily = PixelFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = PixelFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = PixelFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),

    // ── Titles: JetBrains Mono Bold ────────────────────────────
    titleLarge = TextStyle(
        fontFamily = JetBrainsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = JetBrainsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = JetBrainsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // ── Body: JetBrains Mono ─────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily = JetBrainsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = JetBrainsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = JetBrainsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),

    // ── Labels: JetBrains Mono ───────────────────────────────
    labelLarge = TextStyle(
        fontFamily = JetBrainsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)
