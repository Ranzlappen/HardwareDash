package dev.ranzlappen.gadget.feature.usbdebug.control


/**
 * Rooted-only USB Debugging surface. Standard flavor returns
 * [UsbDebuggingControllerResult.Unsupported] for every method.
 *
 * Privileged paths: `cmd usb get-functions` + `cmd usb set-functions <fn>`
 * (syntax flavor differs between API 28 and 30 — the helper detects which
 * form the device accepts), `dumpsys usb`, `dumpsys SerialService` (API
 * 30+ only — the helper falls back to `dumpsys serial` on older devices),
 * and `cat /sys/kernel/debug/usb/devices` with a `dumpsys usb` fallback
 * if debugfs is not mounted (we never auto-mount).
 */
interface UsbDebuggingController {

    /**
     * Switches the device's USB function role. Snapshot+restore via
     * `cmd-usb://function`. The helper detects the API-28 vs API-30
     * `cmd usb set-functions` syntax flavor by parsing the output of
     * `cmd usb get-functions` first, so callers don't need to care which
     * stack the device runs.
     */
    suspend fun switchUsbFunction(function: UsbFunctionType): UsbDebuggingControllerResult

    /** Read-only `dumpsys usb` snapshot, tail-capped to 8 KB. */
    suspend fun dumpUsb(): UsbDebuggingControllerResult

    /**
     * Read-only `dumpsys SerialService` snapshot, tail-capped to 8 KB.
     * Falls back to `dumpsys serial` on devices that don't expose the
     * AOSP service name. Returns
     * [UsbDebuggingControllerResult.Unsupported] on Android < R since
     * SerialService is API-30+ only.
     */
    suspend fun dumpSerialService(): UsbDebuggingControllerResult

    /**
     * Read-only `/sys/kernel/debug/usb/devices` snapshot, tail-capped to
     * 8 KB. Falls back to `dumpsys usb` if debugfs is not mounted (we
     * NEVER auto-mount debugfs).
     */
    suspend fun dumpUsbDevicesDebug(): UsbDebuggingControllerResult

    /** Reverts every USB-surface mutation registered with the log. */
    suspend fun resetAllUsbMutations(): UsbDebuggingControllerResult

    /**
     * Auto-revert path called on screen dispose. Filters by
     * `cmd-usb://` prefix.
     */
    suspend fun revertOnScreenExit(): UsbDebuggingControllerResult
}
