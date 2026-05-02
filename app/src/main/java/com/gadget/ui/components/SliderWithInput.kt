package com.gadget.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A standardized slider with a synchronized editable number input field.
 * The text field displays the current value and accepts manual input.
 * No discrete steps — always continuous.
 *
 * @param value Current slider value
 * @param onValueChange Called on every drag/input change
 * @param valueRange Min..Max range
 * @param onValueChangeFinished Called when drag ends or text is committed (optional)
 * @param formatValue Formats the float for display (e.g., "%.0f", "%.1f")
 * @param suffix Unit suffix shown after the input (e.g., "Hz", "%", "ms")
 * @param label Optional label shown above the slider row
 * @param modifier Modifier for the outer container
 */
@Composable
fun SliderWithInput(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChangeFinished: (() -> Unit)? = null,
    formatValue: (Float) -> String = { "%.0f".format(it) },
    suffix: String = "",
    label: String? = null,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var textFieldValue by remember { mutableStateOf(formatValue(value)) }
    var isFocused by remember { mutableStateOf(false) }

    // Sync the field to external value changes only while it's not being edited.
    // Keying remember on `value` would reset the field mid-typing because the
    // field's own onValueChange propagates back through `value`.
    LaunchedEffect(value, isFocused) {
        if (!isFocused) {
            val formatted = formatValue(value)
            if (formatted != textFieldValue) textFieldValue = formatted
        }
    }

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(4.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Slider(
                value = value,
                onValueChange = {
                    onValueChange(it)
                    textFieldValue = formatValue(it)
                },
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                modifier = Modifier
                    .weight(1f)
                    .semantics { stateDescription = "${formatValue(value)} $suffix".trim() },
            )

            Spacer(Modifier.width(8.dp))

            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { input ->
                    textFieldValue = input
                    val parsed = input.replace(",", ".").toFloatOrNull()
                    if (parsed != null) {
                        val clamped = parsed.coerceIn(valueRange)
                        onValueChange(clamped)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val parsed = textFieldValue.replace(",", ".").toFloatOrNull()
                        if (parsed != null) {
                            val clamped = parsed.coerceIn(valueRange)
                            onValueChange(clamped)
                            textFieldValue = formatValue(clamped)
                        } else {
                            textFieldValue = formatValue(value)
                        }
                        onValueChangeFinished?.invoke()
                        focusManager.clearFocus()
                    },
                ),
                label = if (label != null) {
                    { Text(label, style = MaterialTheme.typography.labelSmall) }
                } else null,
                suffix = if (suffix.isNotEmpty()) {
                    { Text(suffix, style = MaterialTheme.typography.bodySmall) }
                } else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
                modifier = Modifier
                    .width(90.dp)
                    .onFocusChanged { state ->
                        val gainedFocus = state.isFocused
                        if (!gainedFocus && isFocused) {
                            // Lost focus: snap displayed text back to the canonical value.
                            textFieldValue = formatValue(value)
                            onValueChangeFinished?.invoke()
                        }
                        isFocused = gainedFocus
                    },
            )
        }
    }
}
