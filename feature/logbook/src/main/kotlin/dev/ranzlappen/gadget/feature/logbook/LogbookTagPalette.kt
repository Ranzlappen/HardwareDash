package dev.ranzlappen.gadget.feature.logbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.data.logbook.LogbookTagColor
import dev.ranzlappen.gadget.feature.logbook.R

/**
 * Resolves a [LogbookTagColor] to a design-system token color — never a raw
 * hex literal. This is deliberately the entirety of the module's "color tag"
 * system: the legacy `com.gadget.ui.logbook` tool let users paint an
 * arbitrary ARGB background/border pair via a full picker plus an editable
 * palette; this module drops that customization surface and instead reuses
 * the theme's existing semantic tones, so a tag is always legible and
 * theme-consistent (light/dark, dynamic color, custom themes) with zero
 * extra state to persist per tag.
 */
@Composable
fun LogbookTagColor.paint(): Color = when (this) {
    LogbookTagColor.None -> MaterialTheme.colorScheme.surfaceVariant
    LogbookTagColor.Teal -> MaterialTheme.colorScheme.primary
    LogbookTagColor.Amber -> MaterialTheme.colorScheme.tertiary
    LogbookTagColor.Rose -> MaterialTheme.colorScheme.error
    LogbookTagColor.Violet -> MaterialTheme.colorScheme.secondary
    LogbookTagColor.Slate -> MaterialTheme.colorScheme.outline
}

/**
 * One selectable swatch in the tag picker row. Pure presentation — callers
 * supply [selected] + [onClick]; [contentDescription] carries the tag's
 * name for screen readers since the swatch itself is a wordless dot.
 */
@Composable
fun LogbookTagSwatch(
    tag: LogbookTagColor,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val ringColor = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
    Box(
        modifier = modifier
            .size(LogbookTagSwatchDefaults.Size)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics {
                this.contentDescription = contentDescription
                this.selected = selected
                this.role = Role.RadioButton
            }
            .border(width = LogbookTagSwatchDefaults.RingWidth, color = ringColor, shape = CircleShape)
            .padding(LogbookTagSwatchDefaults.RingWidth)
            .clip(CircleShape)
            .background(tag.paint()),
    )
}

/** Human-readable label for a [LogbookTagColor], used both as the tag
 *  swatch's a11y [contentDescription] and as an inline text label on
 *  tagged entry rows. */
@Composable
fun LogbookTagColor.label(): String = when (this) {
    LogbookTagColor.None -> stringResource(R.string.logbook_tag_none)
    LogbookTagColor.Teal -> stringResource(R.string.logbook_tag_teal)
    LogbookTagColor.Amber -> stringResource(R.string.logbook_tag_amber)
    LogbookTagColor.Rose -> stringResource(R.string.logbook_tag_rose)
    LogbookTagColor.Violet -> stringResource(R.string.logbook_tag_violet)
    LogbookTagColor.Slate -> stringResource(R.string.logbook_tag_slate)
}

private object LogbookTagSwatchDefaults {
    val Size: Dp = 28.dp
    val RingWidth: Dp = 2.dp
}
