package dev.ranzlappen.gadget.feature.radios.cell

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.feature.radios.cell.control.CellController
import dev.ranzlappen.gadget.feature.radios.cell.control.CellControllerResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The rooted-tools panel state for the cell screen (W6 in-screen surface). */
data class CellRootToolsState(
    val modem: RootActionState = RootActionState(),
    val signal: RootActionState = RootActionState(),
)

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

    private val _rootTools = MutableStateFlow(CellRootToolsState())

    /** Live status of the two rooted read-only cell dumps (W6 surface). */
    val rootTools: StateFlow<CellRootToolsState> = _rootTools.asStateFlow()

    fun onDumpModem() {
        viewModelScope.launch {
            _rootTools.update { it.copy(modem = it.modem.copy(running = true)) }
            val result = cellController.rawModemDump()
            _rootTools.update { it.copy(modem = result.toActionState()) }
        }
    }

    fun onDumpSignal() {
        viewModelScope.launch {
            _rootTools.update { it.copy(signal = it.signal.copy(running = true)) }
            val result = cellController.signalDeepDump()
            _rootTools.update { it.copy(signal = result.toActionState()) }
        }
    }

    private fun CellControllerResult.toActionState(): RootActionState = when (this) {
        is CellControllerResult.Ok ->
            RootActionState(message = statusNote ?: "Done")
        CellControllerResult.Unsupported ->
            RootActionState(message = "Requires the rooted app version", isError = true)
        is CellControllerResult.RateLimited ->
            RootActionState(message = "Rate limited — retry in ${retryAfterMillis}ms", isError = true)
        CellControllerResult.OptedOut ->
            RootActionState(message = "Blocked by your root-safety opt-out", isError = true)
        is CellControllerResult.HardwareError ->
            RootActionState(message = message, isError = true)
        is CellControllerResult.ResetCompleted ->
            RootActionState(message = "Reset $restored restored, $failed failed")
        is CellControllerResult.ModemDump ->
            RootActionState(message = "Read ${nodes.size} modem nodes")
        is CellControllerResult.SignalDeepDump ->
            RootActionState(message = "Read ${perBand.size} bands")
    }

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
