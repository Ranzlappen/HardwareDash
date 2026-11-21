package dev.ranzlappen.gadget.feature.files

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FilesState(
    val uri: Uri? = null,
    val loading: Boolean = false,
    val metadata: FileMetadata? = null,
    /** Current text of each editable EXIF field. */
    val edits: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val message: FilesMessage? = null,
) {
    val changes: Map<String, String?>
        get() = metadata?.exif?.let { FileMetadataFormat.exifChanges(it, edits) }.orEmpty()
}

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val store: FileMetadataStore,
) : ViewModel() {

    private val _state = MutableStateFlow(FilesState())
    val state: StateFlow<FilesState> = _state.asStateFlow()

    fun open(uri: Uri) {
        _state.value = FilesState(uri = uri, loading = true)
        load(uri, message = null)
    }

    fun edit(tag: String, value: String) = _state.update { it.copy(edits = it.edits + (tag to value), message = null) }

    fun revert() = _state.update { it.copy(edits = editsOf(it.metadata), message = null) }

    fun save() {
        val s = _state.value
        val uri = s.uri ?: return
        val changes = s.changes
        if (changes.isEmpty() || s.saving) return
        _state.update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            val ok = store.writeExif(uri, changes)
            if (ok) load(uri, FilesMessage.Saved) else _state.update { it.copy(saving = false, message = FilesMessage.SaveFailed) }
        }
    }

    fun removeLocation() {
        val uri = _state.value.uri ?: return
        _state.update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            val ok = store.removeLocation(uri)
            if (ok) load(uri, FilesMessage.LocationRemoved) else _state.update { it.copy(saving = false, message = FilesMessage.SaveFailed) }
        }
    }

    private fun load(uri: Uri, message: FilesMessage?) {
        viewModelScope.launch {
            val metadata = store.read(uri)
            _state.update {
                it.copy(
                    loading = false,
                    saving = false,
                    metadata = metadata,
                    edits = editsOf(metadata),
                    message = if (metadata == null) FilesMessage.ReadFailed else message,
                )
            }
        }
    }

    private fun editsOf(metadata: FileMetadata?): Map<String, String> =
        FileMetadataFormat.EDITABLE_TAGS.associateWith { metadata?.exif?.get(it).orEmpty() }
}
