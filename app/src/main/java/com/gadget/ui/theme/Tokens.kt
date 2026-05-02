package com.gadget.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared visual tokens used by Dash-prefixed components (DashCard, DashScaffold, etc.)
 * and by screens that want consistent spacing, sizing, and corner radii.
 *
 * Theme.kt remains the source of truth for colors and typography; this file only
 * holds size-class tokens that aren't part of MaterialTheme.
 */

object DashShapes {
    /** Default corner radius for content cards (DashCard). */
    val card: Shape = RoundedCornerShape(20.dp)
    /** Pill shape for chips, status indicators, segmented controls. */
    val pill: Shape = RoundedCornerShape(percent = 50)
    /** Bottom sheet / modal corner radius. */
    val sheet: Shape = RoundedCornerShape(28.dp)
}

object DashElevation {
    /** Resting state for content cards. */
    val card: Dp = 1.dp
    /** Hover/selected state for content cards. */
    val cardRaised: Dp = 3.dp
    /** Floating sheets and dialogs. */
    val sheet: Dp = 6.dp
}

object DashSpacing {
    /** Outer padding for screen content under DashScaffold. */
    val screenPadding: Dp = 16.dp
    /** Vertical gap between SectionHeader-bounded groups. */
    val sectionGap: Dp = 24.dp
    /** Vertical gap between cards inside a section. */
    val itemGap: Dp = 12.dp
}

object DashIcon {
    /** Icon size for ListItem leadingContent. */
    val list: Dp = 24.dp
    /** Icon size inside Buttons (paired with ResponsiveButtonText). */
    val button: Dp = 18.dp
    /** Hero icon at the top of detail screens. */
    val hero: Dp = 32.dp
}
