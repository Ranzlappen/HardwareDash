package com.gadget.usbdebug

import com.gadget.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

internal const val USB_DEVICES_TAIL_CAP_BYTES = 8 * 1024

private const val DEBUGFS_USB_DEVICES = "/sys/kernel/debug/usb/devices"

/**
 * Read-only `/sys/kernel/debug/usb/devices` snapshot, tail-capped to 8 KB.
 *
 * Modern Android does NOT mount debugfs by default in user builds, so
 * this helper falls back to `dumpsys usb` if the read fails. We never
 * auto-mount debugfs — that would require a mount-namespace write, which
 * is its own privileged surface and not in scope for this seam.
 */
@Singleton
class UsbDevicesDebugHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun snapshot(): UsbDebuggingControllerResult {
        val debugfs = shell.exec(
            "cat $DEBUGFS_USB_DEVICES 2>/dev/null | tail -c $USB_DEVICES_TAIL_CAP_BYTES",
        )
        if (debugfs.isSuccess && debugfs.stdout.isNotEmpty()) {
            val excerpt = debugfs.stdout.joinToString("\n").take(USB_DEVICES_TAIL_CAP_BYTES)
            if (excerpt.isNotBlank()) {
                return UsbDebuggingControllerResult.UsbDumpExcerpt(
                    excerpt = excerpt,
                    source = "debugfs",
                )
            }
        }
        val fallback = shell.exec(
            "dumpsys usb 2>/dev/null | tail -c $USB_DEVICES_TAIL_CAP_BYTES",
        )
        if (!fallback.isSuccess) {
            return UsbDebuggingControllerResult.HardwareError(
                "debugfs read failed and dumpsys usb fallback also failed",
            )
        }
        val excerpt = fallback.stdout.joinToString("\n").take(USB_DEVICES_TAIL_CAP_BYTES)
        return UsbDebuggingControllerResult.UsbDumpExcerpt(
            excerpt = excerpt,
            source = "dumpsys usb (debugfs unavailable)",
        )
    }
}
