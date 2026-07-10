package dev.ranzlappen.gadget.feature.radios.cell

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.feature.radios.cell.control.CellController
import dev.ranzlappen.gadget.feature.radios.cell.control.CellControllerResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CellViewModel @Inject constructor(
    private val tracker: CellTelephonyTracker,
    private val cellController: CellController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor

    val state: StateFlow<CellState> = tracker.state

    private val _rawModemDump = MutableStateFlow<CellDumpUiState>(CellDumpUiState.Idle)
    val rawModemDump: StateFlow<CellDumpUiState> = _rawModemDump.asStateFlow()

    private val _signalDeepDump = MutableStateFlow<CellDumpUiState>(CellDumpUiState.Idle)
    val signalDeepDump: StateFlow<CellDumpUiState> = _signalDeepDump.asStateFlow()

    fun onPermissionGranted() = tracker.startTracking()

    fun onPermissionRevoked() = tracker.stopTracking()

    override fun onCleared() {
        tracker.stopTracking()
    }

    /** Fetches [CellController.rawModemDump] on demand — this is a
     * heavyweight rooted shell walk, so it's user-triggered from the
     * expandable panel rather than run automatically on screen entry. */
    fun loadRawModemDump() {
        if (_rawModemDump.value is CellDumpUiState.Loading) return
        _rawModemDump.value = CellDumpUiState.Loading
        viewModelScope.launch {
            _rawModemDump.value = cellController.rawModemDump().toDumpUiState()
        }
    }

    /** Fetches [CellController.signalDeepDump] on demand; same rationale as
     * [loadRawModemDump]. */
    fun loadSignalDeepDump() {
        if (_signalDeepDump.value is CellDumpUiState.Loading) return
        _signalDeepDump.value = CellDumpUiState.Loading
        viewModelScope.launch {
            _signalDeepDump.value = cellController.signalDeepDump().toDumpUiState()
        }
    }
}

/**
 * UI-facing projection of [CellControllerResult] for the two on-demand
 * rooted dump panels ([CellController.rawModemDump] /
 * [CellController.signalDeepDump]). Those two methods only ever return
 * [CellControllerResult.ModemDump] / [CellControllerResult.SignalDeepDump],
 * [CellControllerResult.Unsupported], [CellControllerResult.OptedOut], or
 * [CellControllerResult.RateLimited] / [CellControllerResult.HardwareError]
 * — never [CellControllerResult.Ok] or [CellControllerResult.ResetCompleted]
 * (those only come back from [CellController.resetAllCellMutations]) — but
 * [toDumpUiState] still covers them defensively.
 */
@Immutable
sealed interface CellDumpUiState {
    data object Idle : CellDumpUiState
    data object Loading : CellDumpUiState
    data class Loaded(val nodes: Map<String, String>) : CellDumpUiState
    data object Unsupported : CellDumpUiState
    data class Error(val message: String) : CellDumpUiState
}

internal fun CellControllerResult.toDumpUiState(): CellDumpUiState = when (this) {
    is CellControllerResult.ModemDump -> CellDumpUiState.Loaded(nodes)
    is CellControllerResult.SignalDeepDump -> CellDumpUiState.Loaded(perBand)
    CellControllerResult.Unsupported -> CellDumpUiState.Unsupported
    CellControllerResult.OptedOut -> CellDumpUiState.Error("Rooted diagnostics are opted out in Settings")
    is CellControllerResult.RateLimited ->
        CellDumpUiState.Error("Rate limited — retry in ${retryAfterMillis / 1000}s")
    is CellControllerResult.HardwareError -> CellDumpUiState.Error(message)
    is CellControllerResult.Ok, is CellControllerResult.ResetCompleted ->
        CellDumpUiState.Error("Unexpected result")
}
