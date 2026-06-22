package dev.ranzlappen.gadget.feature.radios.wifi

data class WifiState(
    val enabled: Boolean = false,
    val connected: Boolean = false,
    val ssid: String? = null,
    val rssiDbm: Int? = null,
    val linkSpeedMbps: Int? = null,
    val frequencyMhz: Int? = null,
    val bssid: String? = null,
    val isRootedFlavor: Boolean = false,
)
