package dev.ranzlappen.gadget.feature.logbook

import dev.ranzlappen.gadget.core.data.logbook.LogbookEntryEntity
import dev.ranzlappen.gadget.core.data.logbook.LogbookTagColor

/** Immutable render state for [LogbookScreenContent]. */
data class LogbookScreenState(
    val entries: List<LogbookEntryEntity> = emptyList(),
    val processes: List<LogbookProcessWithCheckpoints> = emptyList(),
    val composer: EntryComposerState = EntryComposerState(),
    val processBuilder: ProcessBuilderState? = null,
    val entriesExpanded: Boolean = true,
    val processesExpanded: Boolean = true,
)

/** The "add entry" composer's in-progress draft. */
data class EntryComposerState(
    val text: String = "",
    val tag: LogbookTagColor = LogbookTagColor.None,
)

/** The "new process" bottom sheet's in-progress draft. `null` on
 *  [LogbookScreenState.processBuilder] means the sheet is hidden. */
data class ProcessBuilderState(
    val name: String = "",
    val checkpoints: List<LogbookCheckpointDraft> = listOf(LogbookCheckpointDraft(name = "")),
)
