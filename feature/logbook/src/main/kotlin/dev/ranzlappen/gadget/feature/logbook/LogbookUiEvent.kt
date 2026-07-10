package dev.ranzlappen.gadget.feature.logbook

import dev.ranzlappen.gadget.core.data.logbook.LogbookTagColor

/** Every user intent [LogbookScreenContent] can raise, dispatched through
 *  [LogbookViewModel.onEvent]. */
sealed interface LogbookUiEvent {

    // ── Entries ──────────────────────────────────────────────────────────
    data class ComposerTextChange(val text: String) : LogbookUiEvent
    data class ComposerTagChange(val tag: LogbookTagColor) : LogbookUiEvent
    data object SubmitEntry : LogbookUiEvent
    data class DeleteEntry(val id: Long) : LogbookUiEvent
    data object ToggleEntriesExpanded : LogbookUiEvent

    // ── Processes / checkpoints ──────────────────────────────────────────
    data object ToggleProcessesExpanded : LogbookUiEvent
    data class DeleteProcess(val id: Long) : LogbookUiEvent
    data class SetCheckpointCompleted(
        val processId: Long,
        val checkpointIndex: Int,
        val completed: Boolean,
    ) : LogbookUiEvent

    // ── Process builder sheet ────────────────────────────────────────────
    data object OpenProcessBuilder : LogbookUiEvent
    data object DismissProcessBuilder : LogbookUiEvent
    data class ProcessNameChange(val name: String) : LogbookUiEvent
    data class CheckpointNameChange(val index: Int, val name: String) : LogbookUiEvent
    data class CheckpointDueDateChange(val index: Int, val dueAtMillis: Long?) : LogbookUiEvent
    data class CheckpointReminderChange(val index: Int, val reminderAtMillis: Long?) : LogbookUiEvent
    data object AddCheckpointRow : LogbookUiEvent
    data class RemoveCheckpointRow(val index: Int) : LogbookUiEvent
    data object SubmitProcess : LogbookUiEvent
}
