package dev.ranzlappen.gadget.feature.torch.sysfs

/**
 * Result returned by every [TorchSysfsController] extreme-tier method.
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
sealed class TorchSysfsControllerResult {
    data object Ok : TorchSysfsControllerResult()
    data object Unsupported : TorchSysfsControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : TorchSysfsControllerResult()
    data object OptedOut : TorchSysfsControllerResult()
    data class HardwareError(val message: String) : TorchSysfsControllerResult()
}
