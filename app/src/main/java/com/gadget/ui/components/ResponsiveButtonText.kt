package com.gadget.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * Drop-in replacement for `Text(...)` inside Button content lambdas where the
 * label may be a long localized string and would otherwise wrap onto two
 * lines on narrow devices.
 *
 * Renders as a single line, shrinking the font down to 11.sp as needed before
 * ellipsizing. Pairs with leading icons placed as siblings inside the same
 * Button content lambda.
 */
@Composable
fun ResponsiveButtonText(
    text: String,
    modifier: Modifier = Modifier,
) {
    AutoShrinkText(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center,
    )
}
