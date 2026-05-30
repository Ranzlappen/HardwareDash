package com.gadget.usbdebug

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

internal const val USB_FUNCTION_PSEUDO_PATH = "cmd-usb://function"

private const val GET_FUNCTIONS_CMD = "cmd usb get-functions"

/**
 * Switches the device's USB function role via `cmd usb set-functions`.
 *
 * The syntax for this command changed between API 28 and API 30: the
 * older form is a single positional argument (`cmd usb set-functions
 * mtp`) while the newer form takes a trailing `1` to keep the function
 * persistent across reboot (`cmd usb set-functions mtp 1`). We probe by
 * parsing the output of `cmd usb get-functions` first — devices that
 * respond with `Persistent: false` (or similar) signal the older syntax;
 * anything else uses the newer two-arg form. This way callers don't need
 * to care which stack the device runs.
 */
@Singleton
class UsbCommandHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun setFunction(function: UsbFunctionType): UsbDebuggingControllerResult {
        val priorRaw = readCurrent()
            ?: return UsbDebuggingControllerResult.Unsupported
        val useTwoArgForm = !priorRaw.contains("Persistent:")
        mutationLog.register(USB_FUNCTION_PSEUDO_PATH, priorRaw)
        val command = if (useTwoArgForm) {
            "cmd usb set-functions ${function.wireName} 1"
        } else {
            "cmd usb set-functions ${function.wireName}"
        }
        val result = shell.exec(command)
        if (!result.isSuccess) {
            mutationLog.unregister(USB_FUNCTION_PSEUDO_PATH)
            return UsbDebuggingControllerResult.HardwareError(
                "$command failed (exit=${result.exitCode})",
            )
        }
        return UsbDebuggingControllerResult.UsbFunctionSnapshot(
            appliedFunction = function,
            priorFunction = priorRaw.trim(),
        )
    }

    private suspend fun readCurrent(): String? {
        val result = shell.exec(GET_FUNCTIONS_CMD)
        if (!result.isSuccess) return null
        val joined = result.stdout.joinToString("\n").trim()
        return joined.ifEmpty { null }
    }
}
