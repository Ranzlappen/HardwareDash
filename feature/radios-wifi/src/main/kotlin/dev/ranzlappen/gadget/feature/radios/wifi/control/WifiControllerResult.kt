package dev.ranzlappen.gadget.feature.radios.wifi.control

/**
 * Result returned by every [WifiController] extreme-tier method. Same
 * shape as `BatteryControllerResult` / `SensorsControllerResult`.
 *
 * The injection-probe result is intentionally named [InjectionCapabilityProbe]
 * (not just `InjectionCapability`) to make the read-only intent unambiguous
 * in the type system: the method only inspects what `iw phy ... info`
 * reports; **actual packet injection requires a custom kernel module
 * (e.g. nexmon) the app does not ship**.
 */
sealed class WifiControllerResult {
    data class Ok(val statusNote: String? = null) : WifiControllerResult()
    data object Unsupported : WifiControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : WifiControllerResult()
    data object OptedOut : WifiControllerResult()
    data class HardwareError(val message: String) : WifiControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : WifiControllerResult()
    data class RfkillState(val blocked: Boolean) : WifiControllerResult()
    data class InjectionCapabilityProbe(
        val supportsMonitor: Boolean,
        val supportsIbss: Boolean,
        val rawPhyInfoExcerpt: String,
    ) : WifiControllerResult()
}
