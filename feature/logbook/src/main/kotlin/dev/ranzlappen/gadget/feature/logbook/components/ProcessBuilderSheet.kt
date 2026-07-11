package dev.ranzlappen.gadget.feature.logbook.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetBottomSheet
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetIconButton
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.feature.logbook.LogbookCheckpointDraft
import dev.ranzlappen.gadget.feature.logbook.LogbookFormatting
import dev.ranzlappen.gadget.feature.logbook.ProcessBuilderState
import dev.ranzlappen.gadget.feature.logbook.R

/**
 * The bottom-sheet builder for a new checkpoint process: name the process,
 * add named checkpoints, and optionally attach a due date + a reminder
 * (delivered by [dev.ranzlappen.gadget.feature.logbook.worker.LogbookReminderWorker])
 * per checkpoint.
 */
@Composable
fun ProcessBuilderSheet(
    state: ProcessBuilderState,
    onNameChange: (String) -> Unit,
    onCheckpointNameChange: (Int, String) -> Unit,
    onCheckpointDueChange: (Int, Long?) -> Unit,
    onCheckpointReminderChange: (Int, Long?) -> Unit,
    onAddCheckpoint: () -> Unit,
    onRemoveCheckpoint: (Int) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = stringResource(R.string.logbook_builder_title),
    ) {
        GadgetTextField(
            value = state.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.logbook_builder_name_label),
        )
        Text(
            text = stringResource(R.string.logbook_builder_checkpoints_header),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        state.checkpoints.forEachIndexed { index, draft ->
            key(index) {
                CheckpointDraftRow(
                    draft = draft,
                    removable = state.checkpoints.size > 1,
                    onNameChange = { onCheckpointNameChange(index, it) },
                    onDueChange = { onCheckpointDueChange(index, it) },
                    onReminderChange = { onCheckpointReminderChange(index, it) },
                    onRemove = { onRemoveCheckpoint(index) },
                )
            }
        }
        GadgetTertiaryButton(
            onClick = onAddCheckpoint,
            text = stringResource(R.string.logbook_builder_add_checkpoint),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            GadgetTertiaryButton(
                onClick = onDismiss,
                text = stringResource(R.string.logbook_builder_cancel),
                modifier = Modifier.weight(1f),
            )
            GadgetPrimaryButton(
                onClick = onSubmit,
                text = stringResource(R.string.logbook_builder_save),
                enabled = state.name.isNotBlank() &&
                    state.checkpoints.any { it.name.isNotBlank() },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CheckpointDraftRow(
    draft: LogbookCheckpointDraft,
    removable: Boolean,
    onNameChange: (String) -> Unit,
    onDueChange: (Long?) -> Unit,
    onReminderChange: (Long?) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val launchDuePicker = rememberDueDatePickerLauncher(draft.dueAtMillis) { onDueChange(it) }
    val launchReminderPicker =
        rememberReminderPickerLauncher(draft.reminderAtMillis) { onReminderChange(it) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
        ) {
            GadgetTextField(
                value = draft.name,
                onValueChange = onNameChange,
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.logbook_builder_checkpoint_name),
            )
            if (removable) {
                GadgetIconButton(
                    onClick = onRemove,
                    icon = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.logbook_builder_remove_checkpoint),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            val dueAt = draft.dueAtMillis
            GadgetChip(
                selected = dueAt != null,
                onClick = launchDuePicker,
                label = if (dueAt != null) {
                    stringResource(
                        R.string.logbook_builder_due_set,
                        LogbookFormatting.formatDate(dueAt),
                    )
                } else {
                    stringResource(R.string.logbook_builder_set_due)
                },
            )
            val reminderAt = draft.reminderAtMillis
            GadgetChip(
                selected = reminderAt != null,
                onClick = launchReminderPicker,
                label = if (reminderAt != null) {
                    stringResource(
                        R.string.logbook_builder_reminder_set,
                        LogbookFormatting.formatDateTime(reminderAt),
                    )
                } else {
                    stringResource(R.string.logbook_builder_set_reminder)
                },
            )
        }
    }
}
