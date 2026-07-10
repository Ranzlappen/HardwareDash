package dev.ranzlappen.gadget.feature.logbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogbookViewModel @Inject constructor(
    private val repository: LogbookRepository,
) : ViewModel() {

    private val composer = MutableStateFlow(EntryComposerState())
    private val processBuilder = MutableStateFlow<ProcessBuilderState?>(null)
    private val entriesExpanded = MutableStateFlow(true)
    private val processesExpanded = MutableStateFlow(true)

    private data class UiOnlyState(
        val composer: EntryComposerState,
        val processBuilder: ProcessBuilderState?,
        val entriesExpanded: Boolean,
        val processesExpanded: Boolean,
    )

    private val uiOnly = combine(composer, processBuilder, entriesExpanded, processesExpanded, ::UiOnlyState)

    val state: StateFlow<LogbookScreenState> = combine(
        repository.entries,
        repository.processes,
        uiOnly,
    ) { entries, processes, ui ->
        LogbookScreenState(
            entries = entries,
            processes = processes,
            composer = ui.composer,
            processBuilder = ui.processBuilder,
            entriesExpanded = ui.entriesExpanded,
            processesExpanded = ui.processesExpanded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SubscriptionTimeoutMillis),
        initialValue = LogbookScreenState(),
    )

    fun onEvent(event: LogbookUiEvent) {
        when (event) {
            is LogbookUiEvent.ComposerTextChange -> composer.update { it.copy(text = event.text) }
            is LogbookUiEvent.ComposerTagChange -> composer.update { it.copy(tag = event.tag) }
            LogbookUiEvent.SubmitEntry -> submitEntry()
            is LogbookUiEvent.DeleteEntry -> viewModelScope.launch { repository.deleteEntry(event.id) }
            LogbookUiEvent.ToggleEntriesExpanded -> entriesExpanded.update { !it }

            LogbookUiEvent.ToggleProcessesExpanded -> processesExpanded.update { !it }
            is LogbookUiEvent.DeleteProcess -> viewModelScope.launch { repository.deleteProcess(event.id) }
            is LogbookUiEvent.SetCheckpointCompleted -> viewModelScope.launch {
                repository.setCheckpointCompleted(event.processId, event.checkpointIndex, event.completed)
            }

            LogbookUiEvent.OpenProcessBuilder -> processBuilder.value = ProcessBuilderState()
            LogbookUiEvent.DismissProcessBuilder -> processBuilder.value = null
            is LogbookUiEvent.ProcessNameChange -> processBuilder.update { it?.copy(name = event.name) }
            is LogbookUiEvent.CheckpointNameChange -> updateCheckpointDraft(event.index) { it.copy(name = event.name) }
            is LogbookUiEvent.CheckpointDueDateChange -> updateCheckpointDraft(event.index) {
                it.copy(dueAtMillis = event.dueAtMillis)
            }
            is LogbookUiEvent.CheckpointReminderChange -> updateCheckpointDraft(event.index) {
                it.copy(reminderAtMillis = event.reminderAtMillis)
            }
            LogbookUiEvent.AddCheckpointRow -> processBuilder.update { builder ->
                builder?.copy(checkpoints = builder.checkpoints + LogbookCheckpointDraft(name = ""))
            }
            is LogbookUiEvent.RemoveCheckpointRow -> processBuilder.update { builder ->
                val checkpoints = builder?.checkpoints?.toMutableList() ?: return@update builder
                if (checkpoints.size <= 1 || event.index !in checkpoints.indices) return@update builder
                checkpoints.removeAt(event.index)
                builder.copy(checkpoints = checkpoints)
            }
            LogbookUiEvent.SubmitProcess -> submitProcess()
        }
    }

    private fun submitEntry() {
        val draft = composer.value
        if (draft.text.isBlank()) return
        viewModelScope.launch { repository.addEntry(draft.text, draft.tag) }
        composer.value = EntryComposerState()
    }

    private fun submitProcess() {
        val draft = processBuilder.value ?: return
        if (draft.name.isBlank() || draft.checkpoints.none { it.name.isNotBlank() }) return
        viewModelScope.launch { repository.addProcess(draft.name, draft.checkpoints) }
        processBuilder.value = null
    }

    private fun updateCheckpointDraft(index: Int, transform: (LogbookCheckpointDraft) -> LogbookCheckpointDraft) {
        processBuilder.update { builder ->
            val checkpoints = builder?.checkpoints ?: return@update builder
            if (index !in checkpoints.indices) return@update builder
            builder.copy(checkpoints = checkpoints.mapIndexed { i, cp -> if (i == index) transform(cp) else cp })
        }
    }

    private companion object {
        const val SubscriptionTimeoutMillis: Long = 5_000L
    }
}
