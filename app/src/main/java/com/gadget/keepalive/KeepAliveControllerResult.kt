package com.gadget.keepalive

/**
 * Result returned by every [KeepAliveController] method. Same shape
 * as the other Batch-7 controller result types.
 */
sealed class KeepAliveControllerResult {
    data class Ok(val statusNote: String? = null) : KeepAliveControllerResult()
    data object Unsupported : KeepAliveControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : KeepAliveControllerResult()
    data object OptedOut : KeepAliveControllerResult()
    data class HardwareError(val message: String) : KeepAliveControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : KeepAliveControllerResult()

    /**
     * Standard-flavor `enable()` returns this so the caller (Settings
     * screen toggle) knows it should also fire the
     * `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` system intent. The
     * rooted-flavor `enable()` never returns this — it issues
     * `cmd deviceidle whitelist` directly.
     */
    data object UserBatteryOptExemptionRequested : KeepAliveControllerResult()
}
