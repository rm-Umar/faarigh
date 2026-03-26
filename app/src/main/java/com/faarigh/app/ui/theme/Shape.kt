package com.faarigh.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Faarigh Shape System — Widget-card aesthetic
 *
 * Large: 24dp — Hero cards, module cards
 * Medium: 16dp — Standard cards, containers
 * Small: 12dp — Chips, badges, buttons
 * ExtraSmall: 8dp — Compact elements
 */
val FaarighShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp),
)
