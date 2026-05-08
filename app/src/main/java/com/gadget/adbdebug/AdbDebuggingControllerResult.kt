package com.gadget.adbdebug

/**
 * Result returned by every [AdbDebuggingController] privileged method.
 * Same shape as the Batch-7 / Batch-8 controller result types.
 */
sealed class AdbDebuggingControllerResult {
    data class Ok(val statusNote: String? = null) : AdbDebuggingControllerResult()
    data object Unsupported : AdbDebuggingControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : AdbDebuggingControllerResult()
    data object OptedOut : AdbDebuggingControllerResult()
    data class HardwareError(val message: String) : AdbDebuggingControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : AdbDebuggingControllerResult()

    /**
     * Snapshot returned by [AdbDebuggingController.toggleAdbEnabled].
     * [appliedEnabled] reflects the value actually written;
     * [priorEnabled] is the pre-mutation state (null if it could not be read).
     */
    data class AdbToggleSnapshot(
        val appliedEnabled: Boolean,
        val priorEnabled: Boolean?,
    ) : AdbDebuggingControllerResult()

    /**
     * Snapshot returned by [AdbDebuggingController.toggleAdbOverNetwork].
     * [appliedPort] is null when the network listener is being disabled.
     */
    data class AdbNetworkSnapshot(
        val appliedPort: Int?,
        val priorPort: Int?,
    ) : AdbDebuggingControllerResult()

    /**
     * `getprop` excerpt. [persistedFile] is the absolute path of the JSON
     * snapshot if the caller passed `persist = true` and the Logbook write
     * succeeded; null otherwise.
     */
    data class PropertyDump(
        val excerpt: String,
        val persistedFile: String? = null,
    ) : AdbDebuggingControllerResult()

    /**
     * Snapshot returned by [AdbDebuggingController.overrideSystemProperty].
     * [priorValue] is null if the property was previously unset.
     * [appliedValue] echoes the value actually written after allow-list
     * filtering.
     */
    data class SetpropSnapshot(
        val key: String,
        val appliedValue: String,
        val priorValue: String?,
    ) : AdbDebuggingControllerResult()
}
