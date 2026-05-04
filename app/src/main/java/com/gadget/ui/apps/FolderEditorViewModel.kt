package com.gadget.ui.apps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.gadget.apps.WebLinkRepository
import com.gadget.apps.icons.CoverImageStore
import com.gadget.apps.pin.PinFolderHelper
import com.gadget.apps.rules.FolderRule
import com.gadget.apps.rules.FolderRuleSet
import com.gadget.apps.rules.RuleCodec
import com.gadget.data.db.apps.FolderRuleEntity
import com.gadget.data.db.apps.AppRecord
import com.gadget.data.db.apps.AppsDao
import com.gadget.data.db.apps.Folder
import com.gadget.data.db.apps.FolderApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State + mutators for [com.gadget.ui.apps.FolderEditorScreen]. Reads `folderId`
 * from `SavedStateHandle` so it works with `hiltViewModel()` + nav arguments.
 *
 * Persists every interaction immediately — no explicit save button — which
 * matches the rest of the app's settings-screen ergonomics. The "name" field
 * could in principle debounce, but Room writes are fast enough that on-change
 * is fine and saves us from losing state on accidental back-presses.
 */
@HiltViewModel
class FolderEditorViewModel @Inject constructor(
    private val dao: AppsDao,
    private val webLinkRepository: WebLinkRepository,
    private val pinFolderHelper: PinFolderHelper,
    private val coverImageStore: CoverImageStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val folderId: Long = savedStateHandle.get<Long>("folderId") ?: 0L

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
     * the editor's "in: <folder>" subtitle. The current folder is excluded so
     * checking an app off in this editor doesn't immediately make the row
     * look like it's also in itself.
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
    fun pinToHome(): Boolean {
        val f = folder.value ?: return false
        return pinFolderHelper.requestPin(f.id)
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
            // Auto-include the new web-link in the folder being edited.
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
            // Old image (if any) is no longer referenced — drop the file.
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

    /** Replaces the entire rule set. Empty list deletes the row entirely so
     *  the apps_folder_rule table stays compact for "manual-only" folders. */
    private fun persistRuleSet(set: FolderRuleSet) {
        viewModelScope.launch {
            if (set.rules.isEmpty()) {
                dao.deleteRule(folderId)
            } else {
                dao.upsertRule(
                    FolderRuleEntity(
                        folderId = folderId,
                        ruleJson = RuleCodec.encode(set),
                    ),
                )
            }
        }
    }

    /** Adds [rule]; if a rule of the same kind already exists it's replaced. */
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
}
