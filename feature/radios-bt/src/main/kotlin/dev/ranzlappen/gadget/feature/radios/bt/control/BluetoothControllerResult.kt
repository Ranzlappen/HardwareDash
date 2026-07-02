package dev.ranzlappen.gadget.feature.radios.bt.control

/**
 * Result returned by every [BluetoothController] extreme-tier method.
 * Same shape as `WifiControllerResult` / `BatteryControllerResult`.
 */
sealed class BluetoothControllerResult {
    data class Ok(val statusNote: String? = null) : BluetoothControllerResult()
    data object Unsupported : BluetoothControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : BluetoothControllerResult()
    data object OptedOut : BluetoothControllerResult()
    data class HardwareError(val message: String) : BluetoothControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : BluetoothControllerResult()
    data class HciSnoopExcerpt(val tailLines: List<String>) : BluetoothControllerResult()
}
