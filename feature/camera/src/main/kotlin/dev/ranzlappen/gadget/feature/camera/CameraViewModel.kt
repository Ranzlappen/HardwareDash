package dev.ranzlappen.gadget.feature.camera

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.camera.core.CameraControl
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: ScanHistoryRepository,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor


    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

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
