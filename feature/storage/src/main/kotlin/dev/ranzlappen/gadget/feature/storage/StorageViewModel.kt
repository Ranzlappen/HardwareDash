package dev.ranzlappen.gadget.feature.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.feature.storage.control.StorageController
import dev.ranzlappen.gadget.feature.storage.control.StorageControllerResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The rooted-tools panel state for the storage screen. */
data class StorageRootToolsState(
    val diskstats: RootActionState = RootActionState(),
    val mounts: RootActionState = RootActionState(),
)

@HiltViewModel
class StorageViewModel @Inject constructor(
    monitor: StorageMonitor,
    private val storageController: StorageController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor

    val volumes: StateFlow<List<StorageVolumeInfo>> = monitor.volumes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    private val _rootTools = MutableStateFlow(StorageRootToolsState())

    /** Live status of the rooted read-only diskstats / mounts actions. */
    val rootTools: StateFlow<StorageRootToolsState> = _rootTools.asStateFlow()

    fun onDumpDiskstats() {
        viewModelScope.launch {
            _rootTools.update { it.copy(diskstats = it.diskstats.copy(running = true)) }
            val result = storageController.dumpDiskstats()
            _rootTools.update { it.copy(diskstats = result.toActionState()) }
        }
    }

    fun onEnumerateMounts() {
        viewModelScope.launch {
            _rootTools.update { it.copy(mounts = it.mounts.copy(running = true)) }
            val result = storageController.enumerateMounts()
            _rootTools.update { it.copy(mounts = result.toActionState()) }
        }
    }

    private fun StorageControllerResult.toActionState(): RootActionState = when (this) {
        is StorageControllerResult.DiskstatsExcerpt ->
            RootActionState(message = "Captured ${excerpt.length} chars of diskstats")
        is StorageControllerResult.MountList ->
            RootActionState(message = "Enumerated ${mounts.size} mounts")
        is StorageControllerResult.Ok ->
            RootActionState(message = statusNote ?: "Done")
        StorageControllerResult.Unsupported ->
            RootActionState(message = "Requires the rooted app version", isError = true)
        StorageControllerResult.OptedOut ->
            RootActionState(message = "Blocked by your root-safety opt-out", isError = true)
        is StorageControllerResult.RateLimited ->
            RootActionState(message = "Rate limited — retry in ${retryAfterMillis}ms", isError = true)
        is StorageControllerResult.HardwareError ->
            RootActionState(message = message, isError = true)
        is StorageControllerResult.ResetCompleted ->
            RootActionState(message = "Reset $restored restored, $failed failed")
    }
}
