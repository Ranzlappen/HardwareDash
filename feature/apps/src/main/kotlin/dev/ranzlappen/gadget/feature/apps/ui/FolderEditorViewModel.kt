package dev.ranzlappen.gadget.feature.apps.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.data.apps.AppRecord
import dev.ranzlappen.gadget.core.data.apps.AppsDao
import dev.ranzlappen.gadget.core.data.apps.Folder
import dev.ranzlappen.gadget.core.data.apps.FolderApp
import dev.ranzlappen.gadget.core.data.apps.FolderRuleEntity
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.apps.R
import dev.ranzlappen.gadget.feature.apps.WebLinkRepository
import dev.ranzlappen.gadget.feature.apps.icons.CoverImageStore
import dev.ranzlappen.gadget.feature.apps.root.AppsRootController
import dev.ranzlappen.gadget.feature.apps.root.AppsRootControllerResult
import dev.ranzlappen.gadget.feature.apps.rules.FolderRule
import dev.ranzlappen.gadget.feature.apps.rules.FolderRuleSet
import dev.ranzlappen.gadget.feature.apps.rules.RuleCodec
import dev.ranzlappen.gadget.feature.apps.widget.FolderWidgetConfig
import dev.ranzlappen.gadget.feature.apps.widget.PinFolderHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State + mutators for the folder editor. Reads `folderId` from
 * `SavedStateHandle` so it works with `hiltViewModel()` + nav arguments.
 * Persists every interaction immediately — no save button — matching the
 * settings-screen ergonomics, so a back-press never loses work.
 */
@HiltViewModel
class FolderEditorViewModel @Inject constructor(
    private val dao: AppsDao,
    private val webLinkRepository: WebLinkRepository,
    private val pinFolderHelper: PinFolderHelper,
    private val coverImageStore: CoverImageStore,
    private val widgetStore: WidgetConfigStore<FolderWidgetConfig>,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val folderId: Long = savedStateHandle.get<Long>(ARG_FOLDER_ID) ?: 0L

    val folder: StateFlow<Folder?> = dao.observeFolders()
        .map { folders -> folders.firstOrNull { it.id == folderId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val allApps: StateFlow<List<AppRecord>> = dao.observeAppRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchQuery: MutableStateFlow<String> = MutableStateFlow("")

    /** Catalog filtered by [searchQuery] (case-insensitive label OR package match). */
    val filteredApps: StateFlow<List<AppRecord>> = combine(allApps, searchQuery) { records, query ->
        if (query.isBlank()) {
            records
        } else {
            val needle = query.trim().lowercase()
            records.filter {
                it.label.lowercase().contains(needle) ||
                    it.packageName.lowercase().contains(needle)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val membership: StateFlow<Set<String>> = dao.observeMembership(folderId)
        .map { rows -> rows.mapTo(HashSet(rows.size)) { it.appKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /**
     * Map appKey → names of *other* folders that already contain it. Powers
     * the editor's "in: <folder>" subtitle. The current folder is excluded.
     */
    val otherFolderMembership: StateFlow<Map<String, List<String>>> = combine(
        dao.observeFolders(),
        dao.observeAllMembership(),
    ) { folders, allMembership ->
        val nameById = folders.associate { it.id to it.name }
        allMembership
            .asSequence()
            .filter { it.folderId != folderId }
            .groupBy({ it.appKey }, { nameById[it.folderId].orEmpty() })
            .mapValues { (_, names) -> names.filter { it.isNotEmpty() } }
            .filterValues { it.isNotEmpty() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val ruleSet: StateFlow<FolderRuleSet> = dao.observeRules()
        .map { rows ->
            rows.firstOrNull { it.folderId == folderId }
                ?.let { RuleCodec.decode(it.ruleJson) }
                ?: FolderRuleSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FolderRuleSet())

    /** All placed widgets bound to this folder, excluding soft-deleted ones. */
    val placedWidgets: StateFlow<List<PlacedFolderWidget>> = widgetStore.all
        .map { all ->
            all.entries
                .filter { (_, cfg) -> cfg.folderId == folderId && !cfg.removed }
                .map { (id, cfg) -> PlacedFolderWidget(appWidgetId = id, displayName = cfg.displayName) }
                .sortedBy { it.appWidgetId }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteWidget(appWidgetId: Int) {
        viewModelScope.launch {
            val cfg = widgetStore.get(appWidgetId) ?: return@launch
            widgetStore.save(appWidgetId, cfg.copy(removed = true))
        }
    }

    fun rename(newName: String) {
        val f = folder.value ?: return
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == f.name) return
        viewModelScope.launch { dao.updateFolder(f.copy(name = trimmed)) }
    }

    fun setBaseColor(argb: Int) {
        val f = folder.value ?: return
        if (f.baseColorArgb == argb) return
        viewModelScope.launch { dao.updateFolder(f.copy(baseColorArgb = argb)) }
    }

    fun setLocked(locked: Boolean) {
        val f = folder.value ?: return
        if (f.locked == locked) return
        viewModelScope.launch { dao.updateFolder(f.copy(locked = locked)) }
    }

    /** Returns true if the launcher accepted the pin request. */
    /**
     * Returns `true` synchronously when the launcher supports programmatic
     * pinning (so the caller can show the "unsupported" snackbar on `false`),
     * then drives the suspending pin request — which enqueues the pending
     * config before calling `requestPinAppWidget` — on [viewModelScope].
     */
    fun pinToHome(): Boolean {
        val f = folder.value ?: return false
        if (!pinFolderHelper.isSupported()) return false
        viewModelScope.launch { pinFolderHelper.requestPin(f) }
        return true
    }

    fun toggleMember(appKey: String) {
        val current = membership.value
        viewModelScope.launch {
            if (appKey in current) {
                dao.removeFolderApp(folderId, appKey)
            } else {
                val nextOrder = current.size
                dao.insertFolderApp(FolderApp(folderId = folderId, appKey = appKey, sortOrder = nextOrder))
            }
        }
    }

    fun addWebLink(url: String, label: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            val newId = webLinkRepository.add(url.trim(), label.trim())
            val nextOrder = membership.value.size
            dao.insertFolderApp(
                FolderApp(folderId = folderId, appKey = "weblink:$newId", sortOrder = nextOrder),
            )
        }
    }

    fun setCoverSymbol(symbolId: String) {
        val f = folder.value ?: return
        val newCover = "symbol:$symbolId"
        if (f.coverIcon == newCover) return
        viewModelScope.launch {
            coverImageStore.delete(f.id)
            dao.updateFolder(f.copy(coverIcon = newCover))
        }
    }

    fun clearCover() {
        val f = folder.value ?: return
        if (f.coverIcon.isEmpty() || f.coverIcon == "auto") return
        viewModelScope.launch {
            coverImageStore.delete(f.id)
            dao.updateFolder(f.copy(coverIcon = "auto"))
        }
    }

    fun setCoverImageFromUri(uri: Uri) {
        val f = folder.value ?: return
        viewModelScope.launch {
            val path = coverImageStore.saveFromUri(f.id, uri) ?: return@launch
            dao.updateFolder(f.copy(coverIcon = "image:$path"))
        }
    }

    /** Replaces the entire rule set. Empty list deletes the row so the
     *  apps_folder_rule table stays compact for manual-only folders. */
    private fun persistRuleSet(set: FolderRuleSet) {
        viewModelScope.launch {
            if (set.rules.isEmpty()) {
                dao.deleteRule(folderId)
            } else {
                dao.upsertRule(FolderRuleEntity(folderId = folderId, ruleJson = RuleCodec.encode(set)))
            }
        }
    }

    /** Adds [rule]; if a rule of the same kind exists it's replaced. */
    fun addOrReplaceRule(rule: FolderRule) {
        val current = ruleSet.value.rules.filterNot { sameKind(it, rule) }
        persistRuleSet(FolderRuleSet(current + rule))
    }

    /** Removes any rule whose kind matches [matcher]. */
    fun removeRuleOfKind(matcher: (FolderRule) -> Boolean) {
        val next = ruleSet.value.rules.filterNot(matcher)
        if (next.size == ruleSet.value.rules.size) return
        persistRuleSet(FolderRuleSet(next))
    }

    private fun sameKind(a: FolderRule, b: FolderRule): Boolean = a::class == b::class

    companion object {
        const val ARG_FOLDER_ID = "folderId"
    }
}
