package com.gadget.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

/**
 * Modifier extension that applies heading semantics for TalkBack heading navigation.
 * Use on section title Text() composables so users can jump between sections.
 */
fun Modifier.sectionHeading(): Modifier = this.then(
    Modifier.semantics { heading() }
)

/**
 * Modifier extension that applies live region semantics for dynamic content.
 * TalkBack will announce changes to elements with this modifier.
 *
 * @param description The current content description to announce
 */
fun Modifier.liveUpdate(description: String): Modifier = this.then(
    Modifier.semantics {
        liveRegion = LiveRegionMode.Polite
        contentDescription = description
    }
)

/**
 * Modifier extension that ensures minimum 48dp touch target for interactive elements.
 * Apply to IconButtons or clickable elements that are smaller than 48dp.
 */
fun Modifier.minimumTouchTarget(): Modifier = this.then(
    Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
)

/**
 * Modifier extension for clickable cards that act as buttons.
 * Adds Role.Button semantics, content description, and a visible focus indicator.
 *
 * @param label Accessible label describing the card's action
 * @param onClick Action to perform when clicked
 */
@Composable
fun Modifier.accessibleCard(
    label: String,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusColor = MaterialTheme.colorScheme.primary

    return this
        .semantics(mergeDescendants = true) {
            role = Role.Button
            contentDescription = label
        }
        .then(
            if (isFocused) Modifier.border(2.dp, focusColor, MaterialTheme.shapes.medium)
            else Modifier
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
        .focusable(interactionSource = interactionSource)
}

/**
 * Canvas wrapper that provides accessibility content description.
 * Use for all chart/graph Canvas elements so screen readers can describe the data.
 *
 * @param modifier Modifier for the canvas
 * @param contentDescription Description of the chart content for screen readers
 * @param onDraw Drawing lambda
 */
@Composable
fun AccessibleCanvas(
    modifier: Modifier = Modifier,
    contentDescription: String,
    onDraw: DrawScope.() -> Unit,
) {
    Canvas(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        },
        onDraw = onDraw,
    )
}

/**
 * Announces the screen name via TalkBack when the composable enters composition.
 * Place at the top of each screen composable.
 *
 * @param screenName The name of the screen to announce
 */
@Composable
fun ScreenAnnouncement(screenName: String) {
    val view = LocalView.current
    LaunchedEffect(screenName) {
        view.announceForAccessibility(screenName)
    }
}
