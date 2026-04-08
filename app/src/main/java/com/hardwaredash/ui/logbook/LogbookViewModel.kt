package com.hardwaredash.ui.logbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class LogbookViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = LogbookRepository(application)

    // ── Persisted state (from DataStore) ─────────────────────────────
    private val _store = MutableStateFlow(LogbookStore())
    val store: StateFlow<LogbookStore> = _store.asStateFlow()

    // ── UI-only state ────────────────────────────────────────────────
    private val _activeTab = MutableStateFlow(ActiveTab.LOG)
    val activeTab: StateFlow<ActiveTab> = _activeTab.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    // Log tab filter / sort
    private val _entryTypeFilter = MutableStateFlow(EntryTypeFilter.ALL)
    val entryTypeFilter: StateFlow<EntryTypeFilter> = _entryTypeFilter.asStateFlow()
    private val _entrySearch = MutableStateFlow("")
    val entrySearch: StateFlow<String> = _entrySearch.asStateFlow()
    private val _entryDateFilter = MutableStateFlow("")
    val entryDateFilter: StateFlow<String> = _entryDateFilter.asStateFlow()
    private val _entrySortField = MutableStateFlow(SortField.TIME)
    val entrySortField: StateFlow<SortField> = _entrySortField.asStateFlow()
    private val _entrySortDir = MutableStateFlow(SortDirection.DESC)
    val entrySortDir: StateFlow<SortDirection> = _entrySortDir.asStateFlow()

    // Processes tab filter / sort
    private val _procTypeFilter = MutableStateFlow(ProcessTypeFilter.ALL)
    val procTypeFilter: StateFlow<ProcessTypeFilter> = _procTypeFilter.asStateFlow()
    private val _procSearch = MutableStateFlow("")
    val procSearch: StateFlow<String> = _procSearch.asStateFlow()
    private val _procDateFilter = MutableStateFlow("")
    val procDateFilter: StateFlow<String> = _procDateFilter.asStateFlow()
    private val _procSortField = MutableStateFlow(SortField.TIME)
    val procSortField: StateFlow<SortField> = _procSortField.asStateFlow()
    private val _procSortDir = MutableStateFlow(SortDirection.DESC)
    val procSortDir: StateFlow<SortDirection> = _procSortDir.asStateFlow()

    // ── Derived state ────────────────────────────────────────────────

    val filteredEntries: StateFlow<List<LogbookEntry>> = combine(
        _store,
        _entryTypeFilter,
        _entrySearch,
        _entryDateFilter,
        combine(_entrySortField, _entrySortDir) { f, d -> f to d }
    ) { store, typeFilter, search, dateFilter, sortPair ->
        val (sortField, sortDir) = sortPair
        var list = store.entries

        // Type filter
        list = when (typeFilter) {
            EntryTypeFilter.ALL -> list
            EntryTypeFilter.AUTO -> list.filter { !it.custom }
            EntryTypeFilter.CUSTOM -> list.filter { it.custom }
            EntryTypeFilter.EDITED -> list.filter { "edited" in it.tags }
        }

        // Date filter
        if (dateFilter.isNotBlank()) {
            list = list.filter { isoToDateStr(it.isoDate) == dateFilter }
        }

        // Search
        if (search.isNotBlank()) {
            val q = search.lowercase()
            list = list.filter { e ->
                e.text.lowercase().contains(q) ||
                    isoToDisplayDate(e.isoDate).lowercase().contains(q) ||
                    e.tags.joinToString(" ").lowercase().contains(q)
            }
        }

        // Sort
        sortList(list, sortField, sortDir)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredProcesses: StateFlow<List<LogbookProcess>> = combine(
        _store,
        _procTypeFilter,
        _procSearch,
        _procDateFilter,
        combine(_procSortField, _procSortDir) { f, d -> f to d }
    ) { store, typeFilter, search, dateFilter, sortPair ->
        val (sortField, sortDir) = sortPair
        var list = store.processes

        // Type filter
        list = when (typeFilter) {
            ProcessTypeFilter.ALL -> list
            ProcessTypeFilter.EDITED -> list.filter { "edited" in it.tags }
            ProcessTypeFilter.OVERDUE -> list.filter { it.isOverdue() }
        }

        // Date filter
        if (dateFilter.isNotBlank()) {
            list = list.filter { isoToDateStr(it.isoDate) == dateFilter }
        }

        // Search
        if (search.isNotBlank()) {
            val q = search.lowercase()
            list = list.filter { p ->
                p.text.lowercase().contains(q) ||
                    isoToDisplayDate(p.isoDate).lowercase().contains(q) ||
                    p.tags.joinToString(" ").lowercase().contains(q) ||
                    p.checkpoints.any { cp ->
                        cp.name.lowercase().contains(q) ||
                            cp.comment.lowercase().contains(q)
                    }
            }
        }

        // Sort
        sortList(list, sortField, sortDir)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val overdueCount: StateFlow<Int> = _store.map { countOverdue(it.processes) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ── Init ─────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            repo.storeFlow.collect { loaded ->
                _store.value = loaded
                scheduleAllPendingReminders()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Entry operations
    // ═══════════════════════════════════════════════════════════════════

    fun addEntry(text: String) {
        val entry = LogbookEntry(
            id = UUID.randomUUID().toString(),
            isoDate = Instant.now().toString(),
            text = text.trim(),
            custom = false,
            tags = emptyList(),
        )
        updateStore { copy(entries = listOf(entry) + entries) }
    }

    fun addCustomEntry(text: String, date: LocalDate, time: LocalTime) {
        val ldt = LocalDateTime.of(date, time)
        val iso = ldt.atZone(ZoneId.systemDefault()).toInstant().toString()
        val entry = LogbookEntry(
            id = UUID.randomUUID().toString(),
            isoDate = iso,
            text = text.trim(),
            custom = true,
            tags = listOf("custom"),
        )
        val sorted = (entries() + entry).sortedByDescending { it.isoDate }
        updateStore { copy(entries = sorted) }
    }

    fun deleteEntry(id: String) {
        updateStore { copy(entries = entries.filter { it.id != id }) }
    }

    fun updateEntryText(id: String, newText: String) {
        updateStore {
            copy(entries = entries.map { e ->
                if (e.id != id) e
                else e.copy(
                    text = newText.trim(),
                    tags = e.tags.addTagIfAbsent("edited"),
                )
            })
        }
    }

    fun updateEntryTime(id: String, date: LocalDate, time: LocalTime) {
        val ldt = LocalDateTime.of(date, time)
        val iso = ldt.atZone(ZoneId.systemDefault()).toInstant().toString()
        updateStore {
            copy(entries = entries.map { e ->
                if (e.id != id) e
                else e.copy(
                    isoDate = iso,
                    tags = e.tags.addTagIfAbsent("edited"),
                )
            })
        }
    }

    fun applyEntryColor(id: String, mode: String, color: String) {
        updateStore {
            copy(entries = entries.map { e ->
                if (e.id != id) e
                else if (mode == "bg") e.copy(bgColor = color)
                else e.copy(borderColor = color)
            })
        }
    }

    fun clearAllEntries() {
        updateStore { copy(entries = emptyList()) }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Process operations
    // ═══════════════════════════════════════════════════════════════════

    fun addProcess(text: String) {
        if (text.isBlank()) return
        val now = Instant.now().toString()
        val proc = LogbookProcess(
            id = UUID.randomUUID().toString(),
            isoDate = now,
            text = text.trim(),
            checkpoints = listOf(
                Checkpoint(
                    id = UUID.randomUUID().toString(),
                    name = "Start",
                    timestamp = now,
                )
            ),
        )
        updateStore { copy(processes = listOf(proc) + processes) }
    }

    fun addProcessFromTemplate(template: ProcessTemplate) {
        val count = processes().count { it.text.startsWith(template.baseName) } + 1
        val now = Instant.now().toString()
        val cps = template.checkpointNames.mapIndexed { idx, name ->
            Checkpoint(
                id = UUID.randomUUID().toString(),
                name = name,
                timestamp = if (idx == 0) now else "",
            )
        }
        val proc = LogbookProcess(
            id = UUID.randomUUID().toString(),
            isoDate = now,
            text = "${template.baseName} #$count",
            checkpoints = cps,
        )
        updateStore { copy(processes = listOf(proc) + processes) }
    }

    fun deleteProcess(id: String) {
        // Cancel any pending reminders for this process
        val proc = processes().find { it.id == id }
        proc?.checkpoints?.forEachIndexed { idx, _ ->
            LogbookReminderWorker.cancel(getApplication(), id, idx)
        }
        updateStore { copy(processes = processes.filter { it.id != id }) }
    }

    fun updateProcessText(id: String, newText: String) {
        updateStore {
            copy(processes = processes.map { p ->
                if (p.id != id) p
                else p.copy(
                    text = newText.trim(),
                    tags = p.tags.addTagIfAbsent("edited"),
                )
            })
        }
    }

    fun updateProcessTime(id: String, date: LocalDate, time: LocalTime) {
        val ldt = LocalDateTime.of(date, time)
        val iso = ldt.atZone(ZoneId.systemDefault()).toInstant().toString()
        updateStore {
            copy(processes = processes.map { p ->
                if (p.id != id) p
                else p.copy(
                    isoDate = iso,
                    tags = p.tags.addTagIfAbsent("edited"),
                )
            })
        }
    }

    fun applyProcessColor(id: String, mode: String, color: String) {
        updateStore {
            copy(processes = processes.map { p ->
                if (p.id != id) p
                else if (mode == "bg") p.copy(bgColor = color)
                else p.copy(borderColor = color)
            })
        }
    }

    fun clearAllProcesses() {
        // Cancel all reminders
        processes().forEach { proc ->
            proc.checkpoints.forEachIndexed { idx, _ ->
                LogbookReminderWorker.cancel(getApplication(), proc.id, idx)
            }
        }
        updateStore { copy(processes = emptyList()) }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Checkpoint operations
    // ═══════════════════════════════════════════════════════════════════

    fun jumpToCheckpoint(procId: String, cpIdx: Int) {
        updateStore {
            copy(processes = processes.map { p ->
                if (p.id != procId) p
                else p.copy(currentCheckpoint = cpIdx)
            })
        }
    }

    fun addCheckpoint(procId: String) {
        updateStore {
            copy(processes = processes.map { p ->
                if (p.id != procId) p
                else {
                    val newCp = Checkpoint(
                        id = UUID.randomUUID().toString(),
                        name = "Checkpoint ${p.checkpoints.size + 1}",
                        timestamp = Instant.now().toString(),
                    )
                    p.copy(checkpoints = p.checkpoints + newCp)
                }
            })
        }
    }

    fun updateCheckpoint(
        procId: String,
        cpIdx: Int,
        name: String,
        comment: String,
        dueDate: String,
        remindAt: String,
        notify: Boolean,
    ) {
        updateStore {
            copy(processes = processes.map { p ->
                if (p.id != procId) p
                else {
                    val cps = p.checkpoints.toMutableList()
                    cps[cpIdx] = cps[cpIdx].copy(
                        name = name.ifBlank { "Checkpoint ${cpIdx + 1}" },
                        comment = comment,
                        dueDate = dueDate,
                        remindAt = remindAt,
                        notify = notify,
                    )
                    p.copy(checkpoints = cps)
                }
            })
        }

        // Schedule or cancel reminder
        val app = getApplication<Application>()
        if (notify && remindAt.isNotBlank()) {
            val proc = _store.value.processes.find { it.id == procId } ?: return
            val cp = proc.checkpoints.getOrNull(cpIdx) ?: return
            LogbookReminderWorker.schedule(app, procId, cpIdx, proc.text, cp.name, remindAt)
        } else {
            LogbookReminderWorker.cancel(app, procId, cpIdx)
        }
    }

    fun deleteCheckpoint(procId: String, cpIdx: Int) {
        LogbookReminderWorker.cancel(getApplication(), procId, cpIdx)
        updateStore {
            copy(processes = processes.map { p ->
                if (p.id != procId) p
                else {
                    val cps = p.checkpoints.filterIndexed { i, _ -> i != cpIdx }
                    var newCur = p.currentCheckpoint
                    if (cpIdx < newCur) newCur--
                    if (newCur >= cps.size) newCur = cps.size - 1
                    p.copy(
                        checkpoints = cps,
                        currentCheckpoint = newCur.coerceAtLeast(0),
                    )
                }
            })
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Palette
    // ═══════════════════════════════════════════════════════════════════

    fun updatePaletteColor(index: Int, color: String) {
        updateStore {
            val newPalette = palette.toMutableList()
            if (index in newPalette.indices) newPalette[index] = color
            copy(palette = newPalette)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Import / Export
    // ═══════════════════════════════════════════════════════════════════

    fun importJson(jsonString: String): Pair<Int, Int> {
        val current = _store.value
        val merged = repo.parseImport(jsonString, current)
        val newEntries = merged.entries.size - current.entries.size
        val newProcs = merged.processes.size - current.processes.size
        updateStore { merged }
        return newEntries to newProcs
    }

    fun buildExportJson(): String = repo.buildExportJson(_store.value)

    // ═══════════════════════════════════════════════════════════════════
    //  UI state setters
    // ═══════════════════════════════════════════════════════════════════

    fun setActiveTab(tab: ActiveTab) { _activeTab.value = tab }
    fun setViewMode(mode: ViewMode) { _viewMode.value = mode }

    // Log filters
    fun setEntryTypeFilter(f: EntryTypeFilter) { _entryTypeFilter.value = f }
    fun setEntrySearch(q: String) { _entrySearch.value = q }
    fun setEntryDateFilter(d: String) { _entryDateFilter.value = d }
    fun toggleEntrySort(field: SortField) {
        if (_entrySortField.value == field) {
            _entrySortDir.value = if (_entrySortDir.value == SortDirection.DESC) SortDirection.ASC else SortDirection.DESC
        } else {
            _entrySortField.value = field
            _entrySortDir.value = SortDirection.DESC
        }
    }
    fun clearEntryFilters() {
        _entryTypeFilter.value = EntryTypeFilter.ALL
        _entrySearch.value = ""
        _entryDateFilter.value = ""
    }

    // Process filters
    fun setProcTypeFilter(f: ProcessTypeFilter) { _procTypeFilter.value = f }
    fun setProcSearch(q: String) { _procSearch.value = q }
    fun setProcDateFilter(d: String) { _procDateFilter.value = d }
    fun toggleProcSort(field: SortField) {
        if (_procSortField.value == field) {
            _procSortDir.value = if (_procSortDir.value == SortDirection.DESC) SortDirection.ASC else SortDirection.DESC
        } else {
            _procSortField.value = field
            _procSortDir.value = SortDirection.DESC
        }
    }
    fun clearProcFilters() {
        _procTypeFilter.value = ProcessTypeFilter.ALL
        _procSearch.value = ""
        _procDateFilter.value = ""
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Private helpers
    // ═══════════════════════════════════════════════════════════════════

    private fun entries() = _store.value.entries
    private fun processes() = _store.value.processes

    /** Apply a transform to the store and persist. */
    private fun updateStore(transform: LogbookStore.() -> LogbookStore) {
        val updated = _store.value.transform()
        _store.value = updated
        viewModelScope.launch { repo.save(updated) }
    }

    /** Convenience overload for direct replacement. */
    private fun updateStore(direct: LogbookStore) {
        _store.value = direct
        viewModelScope.launch { repo.save(direct) }
    }

    private fun scheduleAllPendingReminders() {
        val app = getApplication<Application>()
        _store.value.processes.forEach { proc ->
            proc.checkpoints.forEachIndexed { idx, cp ->
                if (cp.notify && cp.remindAt.isNotBlank()) {
                    LogbookReminderWorker.schedule(app, proc.id, idx, proc.text, cp.name, cp.remindAt)
                }
            }
        }
    }

    private fun List<String>.addTagIfAbsent(tag: String): List<String> =
        if (tag in this) this else this + tag

    private fun <T> sortList(
        list: List<T>,
        sortField: SortField,
        sortDir: SortDirection,
    ): List<T> where T : Any {
        @Suppress("UNCHECKED_CAST")
        return when (sortField) {
            SortField.TIME -> {
                val selector: (T) -> String = { item ->
                    when (item) {
                        is LogbookEntry -> item.isoDate
                        is LogbookProcess -> item.isoDate
                        else -> ""
                    }
                }
                if (sortDir == SortDirection.DESC) list.sortedByDescending(selector)
                else list.sortedBy(selector)
            }
            SortField.TEXT -> {
                val selector: (T) -> String = { item ->
                    when (item) {
                        is LogbookEntry -> item.text.lowercase()
                        is LogbookProcess -> item.text.lowercase()
                        else -> ""
                    }
                }
                if (sortDir == SortDirection.DESC) list.sortedByDescending(selector)
                else list.sortedBy(selector)
            }
        }
    }

    companion object {
        /** Format ISO instant to "YYYY-MM-DD" for date filtering. */
        fun isoToDateStr(iso: String): String = try {
            val instant = Instant.parse(iso)
            val ld = instant.atZone(ZoneId.systemDefault()).toLocalDate()
            ld.toString() // YYYY-MM-DD
        } catch (_: Exception) { "" }

        /** Format ISO instant to a human-readable display string. */
        fun isoToDisplayDate(iso: String): String = try {
            val instant = Instant.parse(iso)
            val zdt = instant.atZone(ZoneId.systemDefault())
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy, hh:mm:ss a")
            zdt.format(formatter)
        } catch (_: Exception) { iso }
    }
}
