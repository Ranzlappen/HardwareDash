package dev.ranzlappen.gadget.feature.radios.bt

data class BtState(
    val adapterAvailable: Boolean = false,
    val adapterEnabled: Boolean = false,
    val adapterName: String? = null,
    val bondedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val permissionGranted: Boolean = false,
)
