package dev.ranzlappen.gadget.feature.apps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.data.apps.AppsDao
import dev.ranzlappen.gadget.core.data.apps.Folder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State + mutators for the top-level Apps screen. Owns folder lifecycle
 * (create / delete). The membership-and-rule editor uses a separate
 * ViewModel scoped to a single folder.
 */
@HiltViewModel
class AppsViewModel @Inject constructor(
    private val dao: AppsDao,
) : ViewModel() {

    val folders: StateFlow<List<Folder>> = dao.observeFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun createFolder(name: String, baseColorArgb: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            dao.insertFolder(
                Folder(
                    name = name.trim(),
                    baseColorArgb = baseColorArgb,
                    coverIcon = COVER_AUTO,
                    sortOrder = (folders.value.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                    locked = false,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            // No foreign-key constraints on the schema, so clean the satellite
            // tables explicitly to avoid orphaned rows.
            dao.clearFolderMembership(id)
            dao.deleteRule(id)
            dao.deleteFolder(id)
        }
    }

    private companion object {
        const val COVER_AUTO = "auto"
    }
}
