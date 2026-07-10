package dev.ranzlappen.gadget.feature.logbook.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.data.logbook.LogbookEntryEntity
import dev.ranzlappen.gadget.core.data.logbook.LogbookTagColor
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.CompactCard
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetIconButton
import dev.ranzlappen.gadget.feature.logbook.LogbookFormatting
import dev.ranzlappen.gadget.feature.logbook.LogbookTagSwatch
import dev.ranzlappen.gadget.feature.logbook.R
import dev.ranzlappen.gadget.feature.logbook.label

/**
 * Collapsible feed of [LogbookEntryEntity] rows, newest first. Mirrors
 * `SavedPatternsCard`'s "GadgetExpandableCard + plain Column of
 * CompactCard rows + GadgetEmptyState" shape rather than a nested
 * `LazyColumn` (the whole screen already scrolls via `ModuleScreenScaffold`).
 */
@Composable
fun EntriesSection(
    entries: List<LogbookEntryEntity>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        title = stringResource(R.string.logbook_entries_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth(),
        icon = Icons.Outlined.Notes,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            if (entries.isEmpty()) {
                GadgetEmptyState(
                    title = stringResource(R.string.logbook_entries_empty_title),
                    subtitle = stringResource(R.string.logbook_entries_empty_subtitle),
                    icon = Icons.Outlined.Notes,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                entries.forEach { entry ->
                    EntryRow(entry = entry, onDelete = { onDelete(entry.id) })
                }
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: LogbookEntryEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    CompactCard(
        modifier = modifier.fillMaxWidth(),
        title = entry.text,
        subtitle = LogbookFormatting.formatDateTime(entry.timestampMillis),
        singleLineTitle = false,
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
            ) {
                if (entry.tag != LogbookTagColor.None) {
                    LogbookTagSwatch(
                        tag = entry.tag,
                        selected = false,
                        onClick = {},
                        contentDescription = entry.tag.label(),
                        modifier = Modifier
                            .padding(end = spacing.tiny)
                            .size(EntryRowDefaults.TagSwatchSize),
                    )
                }
                GadgetIconButton(
                    onClick = onDelete,
                    icon = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.logbook_entry_delete),
                )
            }
        },
    )
}

private object EntryRowDefaults {
    val TagSwatchSize: Dp = 16.dp
}
