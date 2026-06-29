package dev.ranzlappen.gadget.feature.flipper

/**
 * UI snapshot for the Flipper screen: the live connection state plus the
 * bonded-Flipper picker list and the flavor flag for the rooted capability row.
 */
data class FlipperUiState(
    val connection: FlipperConnectionManager.State = FlipperConnectionManager.State.Disconnected,
    val bleDevices: List<BleDeviceUi> = emptyList(),
    val isRootedFlavor: Boolean = false,
)

/** A bonded Flipper Zero offered in the BLE picker. */
data class BleDeviceUi(val name: String, val address: String)
