package com.gadget.usbdebug

import android.os.Build
import com.gadget.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

internal const val SERIAL_DUMP_TAIL_CAP_BYTES = 8 * 1024

/**
 * Read-only `dumpsys SerialService` snapshot, tail-capped to 8 KB. Falls
 * back to `dumpsys serial` on devices that don't expose the AOSP service
 * name. Returns [UsbDebuggingControllerResult.Unsupported] on Android < R
 * since SerialService is API-30+ only.
 */
@Singleton
class SerialServiceDumpHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun snapshot(): UsbDebuggingControllerResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return UsbDebuggingControllerResult.Unsupported
        }
        val firstAttempt = shell.exec(
            "dumpsys SerialService 2>/dev/null | tail -c $SERIAL_DUMP_TAIL_CAP_BYTES",
        )
        val (excerptRaw, source) = if (firstAttempt.isSuccess && firstAttempt.stdout.isNotEmpty()) {
            firstAttempt.stdout.joinToString("\n") to "dumpsys SerialService"
        } else {
            val fallback = shell.exec(
                "dumpsys serial 2>/dev/null | tail -c $SERIAL_DUMP_TAIL_CAP_BYTES",
            )
            if (!fallback.isSuccess) {
                return UsbDebuggingControllerResult.HardwareError(
                    "dumpsys SerialService and dumpsys serial both failed",
                )
            }
            fallback.stdout.joinToString("\n") to "dumpsys serial"
        }
        return UsbDebuggingControllerResult.UsbDumpExcerpt(
            excerpt = excerptRaw.take(SERIAL_DUMP_TAIL_CAP_BYTES),
            source = source,
        )
    }
}
