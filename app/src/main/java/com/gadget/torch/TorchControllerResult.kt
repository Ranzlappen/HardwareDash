package com.gadget.torch

/**
 * Result returned by every [TorchController] extreme-tier method.
 *
 * `Ok` — the operation completed successfully.
 * `Unsupported` — running on standard flavor, missing root, or no
 *   sysfs path matched on this device. Caller should hide the control.
 * `RateLimited` — the soft limiter throttled the call; surface
 *   [retryAfterMillis] in UI ("try again in X s").
 * `OptedOut` — the user disabled this feature in Settings.
 * `HardwareError` — sysfs write or shell exec failed for a device-
 *   specific reason; [message] is human-readable.
 */
sealed class TorchControllerResult {
    data object Ok : TorchControllerResult()
    data object Unsupported : TorchControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : TorchControllerResult()
    data object OptedOut : TorchControllerResult()
    data class HardwareError(val message: String) : TorchControllerResult()
}
