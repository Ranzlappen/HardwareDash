package com.gadget.root

import com.gadget.root.companion.CompanionModuleDetector
import com.gadget.root.companion.RootedCompanionModuleDetector
import com.gadget.root.core.RootDetector
import com.gadget.root.core.RootService
import com.gadget.root.core.RootShell
import com.gadget.root.core.RootedRootDetector
import com.gadget.root.core.RootedRootService
import com.gadget.root.core.RootedRootShell
import com.gadget.root.launch.LaunchGate
import com.gadget.root.launch.RootedLaunchGate
import com.gadget.adbdebug.AdbDebuggingController
import com.gadget.adbdebug.RootedAdbDebuggingController
import com.gadget.audio.AudioRoutingController
import com.gadget.audio.RootedAudioRoutingController
import com.gadget.automation.AutomationController
import com.gadget.automation.RootedAutomationController
import com.gadget.battery.BatteryController
import com.gadget.battery.RootedBatteryController
import com.gadget.bluetooth.BluetoothController
import com.gadget.bluetooth.RootedBluetoothController
import com.gadget.camera.CameraController
import com.gadget.camera.RootedCameraController
import com.gadget.cell.CellController
import com.gadget.cell.RootedCellController
import com.gadget.display.DisplayController
import com.gadget.display.RootedDisplayController
import com.gadget.gps.GpsController
import com.gadget.gps.RootedGpsController
import com.gadget.ir.IrController
import com.gadget.ir.RootedIrController
import com.gadget.keepalive.KeepAliveController
import com.gadget.keepalive.RootedKeepAliveController
import com.gadget.microphone.MicrophoneController
import com.gadget.microphone.RootedMicrophoneController
import com.gadget.nfc.NfcController
import com.gadget.nfc.RootedNfcController
import com.gadget.notification.NotificationController
import com.gadget.notification.RootedNotificationController
import com.gadget.root.sysfs.RootedSysfsMutationLog
import com.gadget.root.sysfs.SysfsMutationLog
import com.gadget.sensors.RootedSensorsController
import com.gadget.sensors.SensorsController
import com.gadget.storage.RootedStorageController
import com.gadget.storage.StorageController
import com.gadget.wifi.RootedWifiController
import com.gadget.wifi.WifiController
import com.gadget.torch.RootedTorchController
import com.gadget.torch.TorchController
import com.gadget.usbdebug.RootedUsbDebuggingController
import com.gadget.usbdebug.UsbDebuggingController
import com.gadget.vibration.RootedVibrationController
import com.gadget.vibration.VibrationController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Rooted-flavor Hilt bindings. Same fully-qualified name as the standard
 * flavor's binding file; Gradle picks one based on the active product flavor
 * so the two never collide in a single APK.
 *
 * Uses `@Binds` instead of `@Provides` because each implementation is itself
 * `@Singleton` and constructor-injected — Hilt builds them, this module
 * just wires the interface → impl mapping.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RootBindings {

    @Binds @Singleton
    abstract fun bindRootCapabilityRegistry(impl: RootedRootCapabilityRegistry): RootCapabilityRegistry

    @Binds @Singleton
    abstract fun bindRootSafetyGate(impl: RootedRootSafetyGate): RootSafetyGate

    @Binds @Singleton
    abstract fun bindRootSoftLimiter(impl: RootedRootSoftLimiter): RootSoftLimiter

    @Binds @Singleton
    abstract fun bindRootFeatureToggles(impl: RootedRootFeatureToggles): RootFeatureToggles

    // ──── Batch 2: root core layer ────

    @Binds @Singleton
    abstract fun bindRootDetector(impl: RootedRootDetector): RootDetector

    @Binds @Singleton
    abstract fun bindRootShell(impl: RootedRootShell): RootShell

    @Binds @Singleton
    abstract fun bindRootService(impl: RootedRootService): RootService

    @Binds @Singleton
    abstract fun bindLaunchGate(impl: RootedLaunchGate): LaunchGate

    @Binds @Singleton
    abstract fun bindCompanionModuleDetector(impl: RootedCompanionModuleDetector): CompanionModuleDetector

    // ──── Batch 3: feature controllers ────

    @Binds @Singleton
    abstract fun bindTorchController(impl: RootedTorchController): TorchController

    @Binds @Singleton
    abstract fun bindVibrationController(impl: RootedVibrationController): VibrationController

    @Binds @Singleton
    abstract fun bindCameraController(impl: RootedCameraController): CameraController

    @Binds @Singleton
    abstract fun bindMicrophoneController(impl: RootedMicrophoneController): MicrophoneController

    // ──── Batch 5: sensors + battery + sysfs mutation log ────

    @Binds @Singleton
    abstract fun bindSysfsMutationLog(impl: RootedSysfsMutationLog): SysfsMutationLog

    @Binds @Singleton
    abstract fun bindSensorsController(impl: RootedSensorsController): SensorsController

    @Binds @Singleton
    abstract fun bindBatteryController(impl: RootedBatteryController): BatteryController

    // ──── Batch 6: radios + connectivity controllers ────

    @Binds @Singleton
    abstract fun bindWifiController(impl: RootedWifiController): WifiController

    @Binds @Singleton
    abstract fun bindBluetoothController(impl: RootedBluetoothController): BluetoothController

    @Binds @Singleton
    abstract fun bindNfcController(impl: RootedNfcController): NfcController

    @Binds @Singleton
    abstract fun bindIrController(impl: RootedIrController): IrController

    @Binds @Singleton
    abstract fun bindCellController(impl: RootedCellController): CellController

    @Binds @Singleton
    abstract fun bindGpsController(impl: RootedGpsController): GpsController

    // ──── Batch 7: automation + notifications + keep-alive controllers ────

    @Binds @Singleton
    abstract fun bindAutomationController(impl: RootedAutomationController): AutomationController

    @Binds @Singleton
    abstract fun bindNotificationController(impl: RootedNotificationController): NotificationController

    @Binds @Singleton
    abstract fun bindKeepAliveController(impl: RootedKeepAliveController): KeepAliveController

    // ──── Batch 8: storage + display + audio routing controllers ────

    @Binds @Singleton
    abstract fun bindStorageController(impl: RootedStorageController): StorageController

    @Binds @Singleton
    abstract fun bindDisplayController(impl: RootedDisplayController): DisplayController

    @Binds @Singleton
    abstract fun bindAudioRoutingController(
        impl: RootedAudioRoutingController,
    ): AudioRoutingController

    // ──── Batch 9: ADB + USB Debugging controllers ────

    @Binds @Singleton
    abstract fun bindAdbDebuggingController(
        impl: RootedAdbDebuggingController,
    ): AdbDebuggingController

    @Binds @Singleton
    abstract fun bindUsbDebuggingController(
        impl: RootedUsbDebuggingController,
    ): UsbDebuggingController
}
