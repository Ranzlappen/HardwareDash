package com.gadget.ui.apps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gadget.apps.WebLinkRepository
import com.gadget.apps.pin.PinFolderHelper
import com.gadget.data.db.apps.AppRecord
import com.gadget.data.db.apps.AppsDao
import com.gadget.data.db.apps.Folder
import com.gadget.data.db.apps.FolderApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val folderId: Long = savedStateHandle.get<Long>("folderId") ?: 0L

    val folder: StateFlow<Folder?> = dao.observeFolders()
        .map { folders -> folders.firstOrNull { it.id == folderId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val allApps: StateFlow<List<AppRecord>> = dao.observeAppRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val membership: StateFlow<Set<String>> = dao.observeMembership(folderId)
        .map { rows -> rows.mapTo(HashSet(rows.size)) { it.appKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

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
}
