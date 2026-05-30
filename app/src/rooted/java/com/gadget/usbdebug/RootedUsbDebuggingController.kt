package com.gadget.usbdebug

import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

private val USB_RESET_PREFIXES = listOf("cmd-usb://")
private val USB_SCREEN_EXIT_PREFIXES = listOf("cmd-usb://")

/**
 * Rooted-flavor USB Debugging controller. Wires the safety gate to the
 * four USB helpers. Auto-revert on screen exit filters `cmd-usb://` so
 * navigating away while a USB function override is active puts the
 * device back into the prior state.
 */
@Singleton
class RootedUsbDebuggingController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val usbCommandHelper: UsbCommandHelper,
    private val usbDumpHelper: UsbDumpHelper,
    private val serialServiceDumpHelper: SerialServiceDumpHelper,
    private val usbDevicesDebugHelper: UsbDevicesDebugHelper,
    private val mutationLog: SysfsMutationLog,
) : UsbDebuggingController {

    override suspend fun switchUsbFunction(
        function: UsbFunctionType,
    ): UsbDebuggingControllerResult =
        runGated(RootFeatureKey.UsbSwitchFunction) { usbCommandHelper.setFunction(function) }

    override suspend fun dumpUsb(): UsbDebuggingControllerResult =
        runGated(RootFeatureKey.UsbDumpUsb) { usbDumpHelper.snapshot() }

    override suspend fun dumpSerialService(): UsbDebuggingControllerResult =
        runGated(RootFeatureKey.UsbDumpSerialService) { serialServiceDumpHelper.snapshot() }

    override suspend fun dumpUsbDevicesDebug(): UsbDebuggingControllerResult =
        runGated(RootFeatureKey.UsbDumpUsbDevicesDebug) { usbDevicesDebugHelper.snapshot() }

    override suspend fun resetAllUsbMutations(): UsbDebuggingControllerResult {
        val outcome = mutationLog.revertAll(USB_RESET_PREFIXES)
        return UsbDebuggingControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    override suspend fun revertOnScreenExit(): UsbDebuggingControllerResult {
        val outcome = mutationLog.revertAll(USB_SCREEN_EXIT_PREFIXES)
        return UsbDebuggingControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> UsbDebuggingControllerResult,
    ): UsbDebuggingControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it !is UsbDebuggingControllerResult.OptedOut &&
                it !is UsbDebuggingControllerResult.Unsupported &&
                it !is UsbDebuggingControllerResult.RateLimited &&
                it !is UsbDebuggingControllerResult.HardwareError
            ) {
                safetyGate.recordInvocation(feature)
            }
        }
        RootGateDecision.BlockedByUser -> UsbDebuggingControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            UsbDebuggingControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> UsbDebuggingControllerResult.Unsupported
    }
}
