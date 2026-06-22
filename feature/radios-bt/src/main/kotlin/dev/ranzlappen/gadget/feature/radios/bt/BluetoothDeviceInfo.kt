package dev.ranzlappen.gadget.feature.radios.bt

data class BluetoothDeviceInfo(
    val name: String?,
    val address: String,
    val typeName: String,
    val isConnected: Boolean = false,
    /** Battery percentage 0–100 from GATT BAS or hidden API; null = unknown. */
    val batteryPercent: Int? = null,
    /** Signal strength in dBm from GATT readRemoteRssi; null = N/A. */
    val rssiDbm: Int? = null,
    /** Negotiated A2DP codec name (rooted only); null on standard flavor. */
    val codecName: String? = null,
)
