package com.gadget.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gadget.data.db.apps.AppRecord
import com.gadget.data.db.apps.AppsDao
import com.gadget.data.db.apps.Folder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Backs [FolderPopupContent]. Resolves the folder + its current member apps
 * (manual rule only for v1; smart rules will plug into the same flow once the
 * rule editor in 6d lands).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FolderPopupViewModel @Inject constructor(
    private val dao: AppsDao,
) : ViewModel() {

    private val folderIdFlow = MutableStateFlow(-1L)

    val folder: StateFlow<Folder?> = folderIdFlow
        .flatMapLatest { id ->
            if (id < 0L) flowOf(null)
            else dao.observeFolders().map { folders -> folders.firstOrNull { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val appsInFolder: StateFlow<List<AppRecord>> = folderIdFlow
        .flatMapLatest { id ->
            if (id < 0L) {
                flowOf(emptyList())
            } else {
                combine(
                    dao.observeMembership(id),
                    dao.observeAppRecords(),
                ) { members, allRecords ->
                    val orderByKey = members
                        .sortedBy { it.sortOrder }
                        .withIndex()
                        .associate { (idx, m) -> m.appKey to idx }
                    allRecords
                        .filter { it.appKey in orderByKey }
                        .sortedBy { orderByKey[it.appKey] ?: Int.MAX_VALUE }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(folderId: Long) {
        folderIdFlow.value = folderId
    }
}
