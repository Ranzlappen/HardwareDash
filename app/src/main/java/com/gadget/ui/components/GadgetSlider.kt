package com.gadget.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Standardized slider for the app. Thin wrapper around [SliderWithInput]
 * exposing the same parameters; serves as the canonical name for the
 * design-system slider so callers don't reach for raw [androidx.compose.material3.Slider].
 *
 * @param compact When true, reserved for future tighter-spacing variant.
 *                Currently a no-op; kept in the signature so callers can
 *                opt in early without churn later.
 */
@Composable
fun GadgetSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChangeFinished: (() -> Unit)? = null,
    formatValue: (Float) -> String = { "%.0f".format(it) },
    suffix: String = "",
    label: String? = null,
    @Suppress("UNUSED_PARAMETER") compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    SliderWithInput(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        formatValue = formatValue,
        suffix = suffix,
        label = label,
        modifier = modifier,
    )
}
