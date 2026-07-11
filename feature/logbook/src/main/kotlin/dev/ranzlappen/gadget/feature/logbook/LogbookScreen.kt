package dev.ranzlappen.gadget.feature.logbook

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ranzlappen.gadget.core.data.logbook.LogbookEntryEntity
import dev.ranzlappen.gadget.core.data.logbook.LogbookProcessEntity
import dev.ranzlappen.gadget.core.data.logbook.LogbookTagColor
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.ModulePermission
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.logbook.components.EntriesSection
import dev.ranzlappen.gadget.feature.logbook.components.EntryComposerCard
import dev.ranzlappen.gadget.feature.logbook.components.ProcessBuilderSheet
import dev.ranzlappen.gadget.feature.logbook.components.ProcessesSection

@Composable
fun LogbookScreen(
    modifier: Modifier = Modifier,
    viewModel: LogbookViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LogbookScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        moduleInfo = logbookModuleInfo(),
        modifier = modifier,
    )
}

@Composable
private fun logbookModuleInfo(): ModuleInfo {
    val notificationsSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val permissions = if (notificationsSupported) {
        listOf(
            ModulePermission(
                permission = "android.permission.POST_NOTIFICATIONS",
                label = stringResource(R.string.logbook_perm_notifications_label),
                rationale = stringResource(R.string.logbook_perm_notifications_rationale),
                optional = true,
            ),
        )
    } else {
        emptyList()
    }
    return ModuleInfo(
        permissions = permissions,
        compatibility = OsCompatibility(minSdk = 24),
        capabilities = listOf(
            ModuleCapability(
                name = stringResource(R.string.logbook_cap_reminders_name),
                detail = stringResource(R.string.logbook_cap_reminders_detail),
                status = {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.logbook_cap_reminders_ready),
                    )
                },
            ),
        ),
    )
}

@Composable
internal fun LogbookScreenContent(
    state: LogbookScreenState,
    onEvent: (LogbookUiEvent) -> Unit,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.logbook_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            EntryComposerCard(
                state = state.composer,
                onTextChange = { onEvent(LogbookUiEvent.ComposerTextChange(it)) },
                onTagChange = { onEvent(LogbookUiEvent.ComposerTagChange(it)) },
                onSubmit = { onEvent(LogbookUiEvent.SubmitEntry) },
            )
            EntriesSection(
                entries = state.entries,
                expanded = state.entriesExpanded,
                onExpandedChange = { onEvent(LogbookUiEvent.ToggleEntriesExpanded) },
                onDelete = { onEvent(LogbookUiEvent.DeleteEntry(it)) },
            )
            ProcessesSection(
                processes = state.processes,
                expanded = state.processesExpanded,
                onExpandedChange = { onEvent(LogbookUiEvent.ToggleProcessesExpanded) },
                onAddProcess = { onEvent(LogbookUiEvent.OpenProcessBuilder) },
                onDeleteProcess = { onEvent(LogbookUiEvent.DeleteProcess(it)) },
                onSetCheckpointCompleted = { processId, index, completed ->
                    onEvent(LogbookUiEvent.SetCheckpointCompleted(processId, index, completed))
                },
            )
            monitors()
        },
    )

    val builder = state.processBuilder
    if (builder != null) {
        ProcessBuilderSheet(
            state = builder,
            onNameChange = { onEvent(LogbookUiEvent.ProcessNameChange(it)) },
            onCheckpointNameChange = { index, name ->
                onEvent(LogbookUiEvent.CheckpointNameChange(index, name))
            },
            onCheckpointDueChange = { index, due ->
                onEvent(LogbookUiEvent.CheckpointDueDateChange(index, due))
            },
            onCheckpointReminderChange = { index, reminder ->
                onEvent(LogbookUiEvent.CheckpointReminderChange(index, reminder))
            },
            onAddCheckpoint = { onEvent(LogbookUiEvent.AddCheckpointRow) },
            onRemoveCheckpoint = { onEvent(LogbookUiEvent.RemoveCheckpointRow(it)) },
            onSubmit = { onEvent(LogbookUiEvent.SubmitProcess) },
            onDismiss = { onEvent(LogbookUiEvent.DismissProcessBuilder) },
        )
    }
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun LogbookScreenPreview() = GadgetThemedPreview {
    LogbookScreenContent(
        state = LogbookScreenState(
            entries = listOf(
                LogbookEntryEntity(
                    id = 1,
                    timestampMillis = 1_700_000_000_000L,
                    text = "Reflashed the bootloader and verified the checksum.",
                    tag = LogbookTagColor.Teal,
                ),
                LogbookEntryEntity(
                    id = 2,
                    timestampMillis = 1_700_003_600_000L,
                    text = "Noted intermittent USB disconnects under load.",
                    tag = LogbookTagColor.Amber,
                ),
            ),
            processes = listOf(
                LogbookProcessWithCheckpoints(
                    process = LogbookProcessEntity(
                        id = 1,
                        name = "Device bring-up",
                        createdAtMillis = 1_700_000_000_000L,
                    ),
                    checkpoints = emptyList(),
                ),
            ),
        ),
        onEvent = {},
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun LogbookScreenEmptyPreview() = GadgetThemedPreview {
    LogbookScreenContent(
        state = LogbookScreenState(),
        onEvent = {},
        moduleInfo = null,
    )
}
