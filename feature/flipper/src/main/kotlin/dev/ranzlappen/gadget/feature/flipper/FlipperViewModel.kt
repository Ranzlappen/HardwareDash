package dev.ranzlappen.gadget.feature.flipper

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FlipperViewModel @Inject constructor(
    private val manager: FlipperConnectionManager,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val isRootedFlavor = rootCapabilityRegistry.isRootedFlavor
    private val bonded = MutableStateFlow<List<BleDeviceUi>>(emptyList())

    val state: StateFlow<FlipperUiState> =
        combine(manager.state, bonded) { connection, devices ->
            FlipperUiState(connection = connection, bleDevices = devices, isRootedFlavor = isRootedFlavor)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FlipperUiState(isRootedFlavor = isRootedFlavor),
        )

    fun connectUsb() = viewModelScope.launch { manager.connectUsb() }

    fun disconnect() = viewModelScope.launch { manager.disconnect() }

    fun ping() = viewModelScope.launch { manager.system?.ping() }

    /** Re-scan bonded Bluetooth devices for Flippers (call after BT permission). */
    @SuppressLint("MissingPermission")
    fun refreshBondedDevices() {
        bonded.value = manager.bondedFlippers().map { device ->
            BleDeviceUi(
                name = runCatching { device.name }.getOrNull() ?: device.address,
                address = device.address,
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun connectBle(address: String) = viewModelScope.launch {
        val device = manager.bondedFlippers().firstOrNull { it.address == address } ?: return@launch
        manager.connectBle(device)
    }
}
