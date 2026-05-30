package dev.ranzlappen.gadget.feature.torch.legacy

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
sealed class LegacyTorchControllerResult {
    data object Ok : LegacyTorchControllerResult()
    data object Unsupported : LegacyTorchControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : LegacyTorchControllerResult()
    data object OptedOut : LegacyTorchControllerResult()
    data class HardwareError(val message: String) : LegacyTorchControllerResult()
}
