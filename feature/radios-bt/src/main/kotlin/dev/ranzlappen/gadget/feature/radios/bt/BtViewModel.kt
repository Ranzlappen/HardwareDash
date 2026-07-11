package dev.ranzlappen.gadget.feature.radios.bt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.feature.radios.bt.control.BluetoothController
import dev.ranzlappen.gadget.feature.radios.bt.control.BluetoothControllerResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The rooted-tools panel state for the Bluetooth screen (W6 in-screen surface). */
data class BtRootToolsState(
    val hciSnoop: RootActionState = RootActionState(),
)

@HiltViewModel
class BtViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adapter: BluetoothAdapterWrapper,
    private val enhancedInfo: BtEnhancedInfoProvider,
    private val bluetoothController: BluetoothController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow(BtState(isRootedFlavor = rootCapabilityRegistry.isRootedFlavor))
    val state: StateFlow<BtState> = _state

    private val _rootTools = MutableStateFlow(BtRootToolsState())

    /** Live status of the rooted read-only HCI snoop-log dump. */
    val rootTools: StateFlow<BtRootToolsState> = _rootTools.asStateFlow()

    fun onDumpHciSnoop() {
        viewModelScope.launch {
            _rootTools.update { it.copy(hciSnoop = it.hciSnoop.copy(running = true)) }
            val result = bluetoothController.hciSnoopDump()
            _rootTools.update { it.copy(hciSnoop = result.toActionState()) }
        }
    }

    private fun BluetoothControllerResult.toActionState(): RootActionState = when (this) {
        is BluetoothControllerResult.Ok ->
            RootActionState(message = statusNote ?: "Done")
        BluetoothControllerResult.Unsupported ->
            RootActionState(message = "Requires the rooted app version", isError = true)
        is BluetoothControllerResult.RateLimited ->
            RootActionState(message = "Rate limited — retry in ${retryAfterMillis}ms", isError = true)
        BluetoothControllerResult.OptedOut ->
            RootActionState(message = "Blocked by your root-safety opt-out", isError = true)
        is BluetoothControllerResult.HardwareError ->
            RootActionState(message = message, isError = true)
        is BluetoothControllerResult.ResetCompleted ->
            RootActionState(message = "Reset $restored restored, $failed failed")
        is BluetoothControllerResult.HciSnoopExcerpt ->
            RootActionState(message = "Captured ${tailLines.size} HCI snoop lines")
    }

    init { refresh() }

    fun refresh() {
        val hasPermission = checkPermission()
        val connected = if (hasPermission) {
            runCatching { enhancedInfo.connectedAddresses() }.getOrDefault(emptySet())
        } else emptySet()

        val devices = if (hasPermission) {
            adapter.bondedDevices().map { it.copy(isConnected = it.address in connected) }
        } else emptyList()

        _state.update {
            BtState(
                adapterAvailable = adapter.isAvailable(),
                adapterEnabled = adapter.isEnabled(),
                adapterName = if (hasPermission) adapter.name() else null,
                bondedDevices = devices,
                permissionGranted = hasPermission,
            )
        }

        if (hasPermission && connected.isNotEmpty()) {
            viewModelScope.launch { enrichConnectedDevices() }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted) }
        if (granted) refresh()
    }

    /**
     * For each connected device: try the hidden-API battery level first
     * (rooted only, covers all types); fall back to GATT BAS + RSSI for
     * BLE and Dual devices on the standard flavor.
     */
    private suspend fun enrichConnectedDevices() {
        val enriched = _state.value.bondedDevices.map { device ->
            if (!device.isConnected) return@map device

            val rawDevice = adapter.remoteDevice(device.address) ?: return@map device

            // Rooted path: hidden API covers BLE + Classic
            val hiddenBattery = runCatching { enhancedInfo.hiddenBatteryLevel(rawDevice) }.getOrNull()
            if (hiddenBattery != null) {
                return@map device.copy(
                    batteryPercent = hiddenBattery,
                    codecName = runCatching { enhancedInfo.a2dpCodecName(rawDevice) }.getOrNull(),
                )
            }

            // Standard path: GATT BAS + RSSI for BLE / Dual only
            if (device.typeName == "BLE" || device.typeName == "Dual") {
                val (battery, rssi) = runCatching {
                    enhancedInfo.readGattBatteryAndRssi(rawDevice)
                }.getOrDefault(Pair(null, null))
                device.copy(batteryPercent = battery, rssiDbm = rssi)
            } else {
                device
            }
        }
        _state.update { it.copy(bondedDevices = enriched) }
    }

    private fun checkPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}
