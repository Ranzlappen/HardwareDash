package dev.ranzlappen.gadget.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Gadget M3 shape scale.
 *
 * Corners are more pronounced than M3 defaults — glassy surfaces feel
 * softer with 18-24dp radii instead of the 12-16dp default scale.
 * Component sizing chart (radius → typical consumer):
 *
 *   extraSmall (6dp)  — chips, small badges
 *   small      (10dp) — buttons, text fields, inline filter pills
 *   medium     (18dp) — DashCard, dialog surfaces
 *   large      (24dp) — modal sheets, hero surfaces
 *   extraLarge (36dp) — onboarding hero panels, splash plates
 */
val GadgetShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(36.dp),
)
