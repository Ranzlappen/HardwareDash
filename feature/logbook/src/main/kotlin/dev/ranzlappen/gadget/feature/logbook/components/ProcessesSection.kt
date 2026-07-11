package dev.ranzlappen.gadget.feature.logbook.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.data.logbook.LogbookCheckpointEntity
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetIconButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.feature.logbook.LogbookFormatting
import dev.ranzlappen.gadget.feature.logbook.LogbookProcessWithCheckpoints
import dev.ranzlappen.gadget.feature.logbook.R

/**
 * The checkpoint/process half of the logbook screen: every tracked process
 * with its independently-completable checkpoints, plus the entry point into
 * the process-builder sheet.
 */
@Composable
fun ProcessesSection(
    processes: List<LogbookProcessWithCheckpoints>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddProcess: () -> Unit,
    onDeleteProcess: (Long) -> Unit,
    onSetCheckpointCompleted: (processId: Long, checkpointIndex: Int, completed: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        title = stringResource(R.string.logbook_processes_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth(),
        icon = Icons.Outlined.Checklist,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            if (processes.isEmpty()) {
                GadgetEmptyState(
                    title = stringResource(R.string.logbook_processes_empty_title),
                    subtitle = stringResource(R.string.logbook_processes_empty_subtitle),
                    icon = Icons.Outlined.Checklist,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                processes.forEach { process ->
                    ProcessBlock(
                        process = process,
                        onDelete = { onDeleteProcess(process.process.id) },
                        onSetCheckpointCompleted = { index, completed ->
                            onSetCheckpointCompleted(process.process.id, index, completed)
                        },
                    )
                }
            }
            GadgetSecondaryButton(
                onClick = onAddProcess,
                text = stringResource(R.string.logbook_process_add),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProcessBlock(
    process: LogbookProcessWithCheckpoints,
    onDelete: () -> Unit,
    onSetCheckpointCompleted: (checkpointIndex: Int, completed: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = process.process.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (process.openCount > 0) {
                    stringResource(R.string.logbook_process_open_count, process.openCount)
                } else {
                    stringResource(R.string.logbook_process_done)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (process.openCount > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = spacing.small),
            )
            GadgetIconButton(
                onClick = onDelete,
                icon = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.logbook_process_delete),
            )
        }
        process.checkpoints.forEachIndexed { index, checkpoint ->
            CheckpointRow(
                checkpoint = checkpoint,
                onCompletedChange = { completed -> onSetCheckpointCompleted(index, completed) },
            )
        }
    }
}

@Composable
private fun CheckpointRow(
    checkpoint: LogbookCheckpointEntity,
    onCompletedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checkpoint.completed,
            onCheckedChange = onCompletedChange,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = checkpoint.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = if (checkpoint.completed) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val dueAt = checkpoint.dueAtMillis
            if (dueAt != null) {
                val overdue = !checkpoint.completed && LogbookFormatting.isOverdue(dueAt)
                Text(
                    text = if (overdue) {
                        stringResource(
                            R.string.logbook_checkpoint_overdue,
                            LogbookFormatting.formatDate(dueAt),
                        )
                    } else {
                        stringResource(
                            R.string.logbook_checkpoint_due,
                            LogbookFormatting.formatDate(dueAt),
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (overdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
