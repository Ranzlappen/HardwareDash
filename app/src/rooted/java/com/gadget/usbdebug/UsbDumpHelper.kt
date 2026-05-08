package com.gadget.usbdebug

import com.gadget.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

internal const val USB_DUMP_TAIL_CAP_BYTES = 8 * 1024

/**
 * Read-only `dumpsys usb` snapshot, tail-capped to 8 KB.
 */
@Singleton
class UsbDumpHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun snapshot(): UsbDebuggingControllerResult {
        val result = shell.exec(
            "dumpsys usb 2>/dev/null | tail -c $USB_DUMP_TAIL_CAP_BYTES",
        )
        if (!result.isSuccess) {
            return UsbDebuggingControllerResult.HardwareError(
                "dumpsys usb failed (exit=${result.exitCode})",
            )
        }
        val excerpt = result.stdout.joinToString("\n").take(USB_DUMP_TAIL_CAP_BYTES)
        return UsbDebuggingControllerResult.UsbDumpExcerpt(
            excerpt = excerpt,
            source = "dumpsys usb",
        )
    }
}
