package com.gadget.usbdebug

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor USB Debugging controller. Every method returns
 * [UsbDebuggingControllerResult.Unsupported] — the standard APK has no
 * privileged shell so direct `cmd usb set-functions` writes and
 * `dumpsys usb` snapshots are impossible regardless of permissions.
 */
@Singleton
class StandardUsbDebuggingController @Inject constructor() : UsbDebuggingController {

    override suspend fun switchUsbFunction(function: UsbFunctionType): UsbDebuggingControllerResult =
        UsbDebuggingControllerResult.Unsupported

    override suspend fun dumpUsb(): UsbDebuggingControllerResult =
        UsbDebuggingControllerResult.Unsupported

    override suspend fun dumpSerialService(): UsbDebuggingControllerResult =
        UsbDebuggingControllerResult.Unsupported

    override suspend fun dumpUsbDevicesDebug(): UsbDebuggingControllerResult =
        UsbDebuggingControllerResult.Unsupported

    override suspend fun resetAllUsbMutations(): UsbDebuggingControllerResult =
        UsbDebuggingControllerResult.ResetCompleted(restored = 0, failed = 0)

    override suspend fun revertOnScreenExit(): UsbDebuggingControllerResult =
        UsbDebuggingControllerResult.ResetCompleted(restored = 0, failed = 0)
}
