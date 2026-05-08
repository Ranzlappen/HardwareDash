package com.gadget.usbdebug

/**
 * Result returned by every [UsbDebuggingController] privileged method.
 * Same shape as the Batch-7 / Batch-8 controller result types.
 */
sealed class UsbDebuggingControllerResult {
    data class Ok(val statusNote: String? = null) : UsbDebuggingControllerResult()
    data object Unsupported : UsbDebuggingControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : UsbDebuggingControllerResult()
    data object OptedOut : UsbDebuggingControllerResult()
    data class HardwareError(val message: String) : UsbDebuggingControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : UsbDebuggingControllerResult()

    /**
     * Snapshot returned by [UsbDebuggingController.switchUsbFunction].
     * [appliedFunction] echoes the function actually written;
     * [priorFunction] is the pre-mutation state.
     */
    data class UsbFunctionSnapshot(
        val appliedFunction: UsbFunctionType,
        val priorFunction: String?,
    ) : UsbDebuggingControllerResult()

    /**
     * Read-only USB diagnostic excerpt. [source] identifies which probe
     * actually produced the bytes (e.g. `dumpsys usb`, `debugfs`,
     * `dumpsys SerialService`).
     */
    data class UsbDumpExcerpt(
        val excerpt: String,
        val source: String,
    ) : UsbDebuggingControllerResult()
}
