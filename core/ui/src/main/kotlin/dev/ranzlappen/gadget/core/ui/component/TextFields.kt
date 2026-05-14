package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetSpacing
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Outlined text field with Gadget styling.
 *
 * Wraps Material 3 [OutlinedTextField] with theme-derived colours and
 * the design-system's small shape token. State is hoisted — callers
 * own the [value] / [onValueChange] pair as usual for stateful inputs.
 *
 * Long-text handling:
 *  - When [singleLine] is true (the default), text truncates with
 *    [TextOverflow.Ellipsis] and the field scrolls horizontally.
 *  - When [singleLine] is false, the field grows vertically up to
 *    [maxLines] and falls back to [TextOverflow.Ellipsis] beyond that.
 *
 * Pair with a [GadgetButton] for form-style "submit on tap" flows;
 * use [keyboardActions] for "submit on IME Done" flows. The
 * [trailingIcon] + [onTrailingIconClick] pair is a convenience for
 * the common "show/hide password" or "clear" affordance.
 */
@Composable
fun GadgetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else MaxLinesUnbounded,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyLarge,
        label = if (label != null) {
            { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } else null,
        placeholder = if (placeholder != null) {
            { Text(text = placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } else null,
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(LeadingTrailingIconSize),
                )
            }
        } else null,
        trailingIcon = if (trailingIcon != null) {
            {
                if (onTrailingIconClick != null) {
                    IconButton(onClick = onTrailingIconClick, enabled = enabled) {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(LeadingTrailingIconSize),
                        )
                    }
                } else {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(LeadingTrailingIconSize),
                    )
                }
            }
        } else null,
        supportingText = if (supportingText != null) {
            { Text(text = supportingText, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        } else null,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = gadgetTextFieldColors(),
    )
}

/**
 * Search-optimised text field — leading magnifier icon, optional
 * trailing clear-button when [value] is non-empty, and an IME
 * "search" action that triggers [onSearch].
 *
 * The clear button auto-appears when there's content; consumers don't
 * need to manage trailing-icon state. For other input fields with
 * different trailing behaviour, use [GadgetTextField] directly.
 */
@Composable
fun GadgetSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    enabled: Boolean = true,
    onSearch: (() -> Unit)? = null,
) {
    GadgetTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = Icons.Outlined.Search,
        trailingIcon = if (value.isNotEmpty()) Icons.Outlined.Cancel else null,
        onTrailingIconClick = if (value.isNotEmpty()) {
            { onValueChange("") }
        } else null,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
    )
}

// ─── Internals ──────────────────────────────────────────────────────

/** Sentinel for "no upper bound on visible lines" in multi-line mode. */
private const val MaxLinesUnbounded: Int = Int.MAX_VALUE

/** Fixed-size design token: leading / trailing icon graphic. */
private val LeadingTrailingIconSize: Dp = 20.dp

/**
 * Shared colour scheme for Gadget text fields. Wraps
 * [OutlinedTextFieldDefaults.colors] with theme-pulled values; the
 * design-system spec prohibits raw colour literals here.
 *
 * Disabled-state colours are left to M3's built-in defaults inside
 * [OutlinedTextFieldDefaults.colors] — they already apply the
 * appropriate `colorScheme.onSurface` × disabled-alpha calculation
 * and respect dynamic colour.
 */
@Composable
private fun gadgetTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorBorderColor = MaterialTheme.colorScheme.error,
    errorLabelColor = MaterialTheme.colorScheme.error,
)

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@Composable
private fun TextFieldsPreview() = GadgetThemedPreview {
    Column(verticalArrangement = Arrangement.spacedBy(GadgetSpacing.Medium)) {
        var name by remember { mutableStateOf("") }
        var search by remember { mutableStateOf("Rover-2") }
        GadgetTextField(
            value = name,
            onValueChange = { name = it },
            label = "Sensor name",
            placeholder = "e.g. Battery probe",
        )
        GadgetTextField(
            value = "ABC-123",
            onValueChange = {},
            label = "ID",
            isError = true,
            supportingText = "ID is already in use.",
        )
        GadgetSearchField(value = search, onValueChange = { search = it })
        GadgetSearchField(value = "", onValueChange = {})
    }
}
