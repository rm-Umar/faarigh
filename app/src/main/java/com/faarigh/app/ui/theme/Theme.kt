package com.faarigh.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// ═══════════════════════════════════════════════════════════════
// Faarigh — Cardboard Retro Theme
// Single theme: warm cream light / warm brown dark
// ═══════════════════════════════════════════════════════════════

@Composable
fun FaarighTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CardboardRetroDark else CardboardRetroLight

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FaarighTypography,
        shapes = FaarighShapes,
        content = content,
    )
}

// Backward-compat alias
@Composable
fun FaarighTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    FaarighTheme(darkTheme = darkTheme, content = content)
}
