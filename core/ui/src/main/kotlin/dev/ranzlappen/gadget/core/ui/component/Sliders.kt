package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import kotlin.math.roundToInt

/**
 * Themed slider with a tap-to-edit numeric value affordance.
 *
 * The design-system primitive that every future slider in the app
 * consumes. Wraps Material 3 [Slider] and fixes two long-standing
 * pain points with raw M3 sliders inside reactive state pipelines:
 *
 * 1. **Drag now works.** Naively binding `value =
 *    state.somethingFromAFlow` makes the slider snap back to the
 *    flow's previous value mid-gesture, because the flow hasn't
 *    re-emitted yet. This composable hoists a *local* drag value,
 *    syncs to external changes via [LaunchedEffect] only while the
 *    user isn't dragging, and commits to the caller on release via
 *    [onValueChangeFinished].
 *
 * 2. **The value is manually editable.** Tap the trailing value
 *    label; it swaps for a compact text field. Type a number → on
 *    IME-done / focus loss, the parsed value (clamped to
 *    [valueRange]) commits. Invalid input reverts silently.
 *
 * @param value the canonical value (the caller's hoisted state).
 * @param onValueChange fires on every drag frame with the local
 *        intermediate value. Most callers should NOT persist on this
 *        — wait for [onValueChangeFinished] to avoid hammering
 *        DataStore at 60 Hz.
 * @param valueRange inclusive range. Values are clamped.
 * @param modifier outer modifier.
 * @param label optional leading label (e.g. "Rate").
 * @param suffix optional trailing unit (e.g. "Hz"). Renders next to
 *        the numeric value inside the same row.
 * @param valueFormatter rendering function for the trailing label.
 *        Default rounds to int.
 * @param valueParser inverse of [valueFormatter] for the editable
 *        text-input path. Default parses a plain Float.
 * @param steps M3 step count (0 = continuous).
 * @param onValueChangeFinished fires once on touch release with the
 *        final value. Persist here.
 * @param enabled standard enabled flag.
 */
@Composable
fun GadgetSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    label: String? = null,
    suffix: String? = null,
    valueFormatter: (Float) -> String = { it.roundToInt().toString() },
    valueParser: (String) -> Float? = { it.trim().toFloatOrNull() },
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val spacing = LocalGadgetTheme.current.spacing

    // Local drag value — eagerly updated on every onValueChange so the
    // visual thumb tracks the finger. Synced FROM the canonical value
    // only when the user isn't actively dragging.
    var dragValue by remember { mutableStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }
    var isEditingText by remember { mutableStateOf(false) }
    var editingText by remember { mutableStateOf("") }

    LaunchedEffect(value) {
        if (!isDragging && !isEditingText) dragValue = value
    }

    // Build the a11y label as one merged string so screen readers
    // announce the slider as a single semantic unit instead of a
    // slider + an editable text + a unit suffix.
    val a11y = buildString {
        if (label != null) append("$label, ")
        append(valueFormatter(dragValue))
        if (suffix != null) append(" $suffix")
        append(", double tap to edit")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = a11y },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Slider(
            value = dragValue.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = { newRaw ->
                val new = newRaw.coerceIn(valueRange.start, valueRange.endInclusive)
                isDragging = true
                dragValue = new
                onValueChange(new)
            },
            onValueChangeFinished = {
                isDragging = false
                onValueChangeFinished?.invoke()
            },
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.widthIn(min = ValueChipMinWidth),
            contentAlignment = Alignment.Center,
        ) {
            if (isEditingText) {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            commitText(
                                input = editingText,
                                parser = valueParser,
                                range = valueRange,
                                onValueChange = onValueChange,
                                onValueChangeFinished = onValueChangeFinished,
                                onDragValueUpdate = { dragValue = it },
                            )
                            isEditingText = false
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EditFieldHeight),
                )
            } else {
                val display = buildString {
                    append(valueFormatter(dragValue))
                    if (suffix != null) append(" $suffix")
                }
                Text(
                    text = display,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = spacing.tiny)
                        .semantics { contentDescription = a11y }
                        .clickableLabel(enabled = enabled) {
                            editingText = valueFormatter(dragValue)
                            isEditingText = true
                        },
                )
            }
        }
    }
}

/**
 * Click-to-edit wrapper for the value label.
 *
 * Non-`@Composable` extension so it composes inside the slider's
 * modifier chain without forcing a recompose-key. Uses
 * `Modifier.clickable` with the default M3 indication (ripple).
 */
private fun Modifier.clickableLabel(
    enabled: Boolean,
    onClick: () -> Unit,
): Modifier = if (enabled) this.clickable(onClick = onClick) else this

/**
 * Commit the typed value: parse, clamp, fire onValueChange +
 * onValueChangeFinished, update the local drag value to match. If
 * parsing fails the commit is silently skipped (caller stays at the
 * previous value).
 */
private inline fun commitText(
    input: String,
    parser: (String) -> Float?,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)?,
    onDragValueUpdate: (Float) -> Unit,
) {
    val parsed = parser(input) ?: return
    val clamped = parsed.coerceIn(range.start, range.endInclusive)
    onDragValueUpdate(clamped)
    onValueChange(clamped)
    onValueChangeFinished?.invoke()
}

/** Minimum width for the trailing value chip so the slider thumb
 *  doesn't jitter as the rendered digit count changes. */
private val ValueChipMinWidth: Dp = 56.dp

/** Compact height for the inline edit text field. */
private val EditFieldHeight: Dp = 48.dp

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun GadgetSliderPreview() = GadgetThemedPreview {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        var rate by remember { mutableStateOf(5f) }
        GadgetSlider(
            value = rate,
            onValueChange = { rate = it },
            valueRange = 1f..20f,
            label = "Rate",
            suffix = "Hz",
            steps = 18,
        )
        GadgetSlider(
            value = 0.6f,
            onValueChange = {},
            valueRange = 0f..1f,
            label = "Intensity",
            valueFormatter = { "${(it * 100).roundToInt()}%" },
        )
    }
}
