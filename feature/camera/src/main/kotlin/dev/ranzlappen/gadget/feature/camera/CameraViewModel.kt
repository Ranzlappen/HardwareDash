package dev.ranzlappen.gadget.feature.camera

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.camera.core.CameraControl
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.feature.camera.control.CameraController
import dev.ranzlappen.gadget.feature.camera.control.CameraControllerResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The rooted-tools panel state for the camera screen (W6 in-screen write-tier surface). */
data class CameraRootToolsState(
    val halBypass: RootActionState = RootActionState(),
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: ScanHistoryRepository,
    private val cameraController: CameraController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor


    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private val _rootTools = MutableStateFlow(CameraRootToolsState())

    /** Live status of the confirm-gated rooted HAL-bypass frame capture. */
    val rootTools: StateFlow<CameraRootToolsState> = _rootTools.asStateFlow()

    fun onHalBypassFrame() {
        viewModelScope.launch {
            _rootTools.update { it.copy(halBypass = it.halBypass.copy(running = true)) }
            val result = cameraController.halBypassFrame()
            _rootTools.update { it.copy(halBypass = result.toActionState()) }
        }
    }

    private fun CameraControllerResult.toActionState(): RootActionState = when (this) {
        CameraControllerResult.Ok ->
            RootActionState(message = "Captured HAL-bypass frame")
        CameraControllerResult.Unsupported ->
            RootActionState(message = "Requires the rooted app version", isError = true)
        is CameraControllerResult.RateLimited ->
            RootActionState(message = "Rate limited — retry in ${retryAfterMillis}ms", isError = true)
        CameraControllerResult.OptedOut ->
            RootActionState(message = "Blocked by your root-safety opt-out", isError = true)
        is CameraControllerResult.HardwareError ->
            RootActionState(message = message, isError = true)
    }

    val history: StateFlow<List<BarcodeResult>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun onPermissionResult(granted: Boolean) = _state.update { it.copy(permissionGranted = granted) }

    fun onScanDetected(result: BarcodeResult) {
        _state.update { it.copy(latestScan = result, error = null) }
        viewModelScope.launch { repository.add(result) }
    }

    fun toggleTorch(cameraControl: CameraControl) {
        val next = !_state.value.isTorchOn
        _state.update { it.copy(isTorchOn = next) }
        cameraControl.enableTorch(next)
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clear() }
    }

    fun copyToClipboard(context: Context, result: BarcodeResult) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("barcode", result.rawValue))
    }
}
