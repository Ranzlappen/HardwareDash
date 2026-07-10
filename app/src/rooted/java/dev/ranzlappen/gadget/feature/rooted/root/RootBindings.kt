package dev.ranzlappen.gadget.feature.rooted.root

import dev.ranzlappen.gadget.core.root.*
import dev.ranzlappen.gadget.core.root.companion.CompanionModuleDetector
import dev.ranzlappen.gadget.feature.rooted.root.companion.RootedCompanionModuleDetector
import dev.ranzlappen.gadget.core.root.core.RootDetector
import dev.ranzlappen.gadget.core.root.core.RootService
import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.feature.rooted.root.core.RootedRootDetector
import dev.ranzlappen.gadget.feature.rooted.root.core.RootedRootService
import dev.ranzlappen.gadget.feature.rooted.root.core.RootedRootShell
import dev.ranzlappen.gadget.core.root.launch.LaunchGate
import dev.ranzlappen.gadget.feature.rooted.root.launch.RootedLaunchGate
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbDebuggingController
import dev.ranzlappen.gadget.feature.adbdebug.rooted.control.RootedAdbDebuggingController
import dev.ranzlappen.gadget.feature.audio.control.AudioRoutingController
import dev.ranzlappen.gadget.feature.audio.rooted.control.RootedAudioRoutingController
import dev.ranzlappen.gadget.feature.automation.control.AutomationController
import dev.ranzlappen.gadget.feature.automation.rooted.control.RootedAutomationController
import dev.ranzlappen.gadget.feature.battery.control.BatteryController
import dev.ranzlappen.gadget.feature.battery.rooted.control.RootedBatteryController
import dev.ranzlappen.gadget.feature.radios.bt.control.BluetoothController
import dev.ranzlappen.gadget.feature.radios.bt.rooted.control.RootedBluetoothController
import dev.ranzlappen.gadget.feature.camera.control.CameraController
import dev.ranzlappen.gadget.feature.camera.rooted.control.RootedCameraController
import dev.ranzlappen.gadget.feature.radios.cell.control.CellController
import dev.ranzlappen.gadget.feature.radios.cell.rooted.control.RootedCellController
import dev.ranzlappen.gadget.feature.diagnostics.control.DiagnosticsController
import dev.ranzlappen.gadget.feature.diagnostics.rooted.control.RootedDiagnosticsController
import dev.ranzlappen.gadget.feature.display.control.DisplayController
import dev.ranzlappen.gadget.feature.display.rooted.control.RootedDisplayController
import dev.ranzlappen.gadget.feature.gps.control.GpsController
import dev.ranzlappen.gadget.feature.gps.rooted.control.RootedGpsController
import dev.ranzlappen.gadget.feature.gps.spoof.GpsSpoofController
import dev.ranzlappen.gadget.feature.gps.rooted.spoof.RootedGpsSpoofController
import dev.ranzlappen.gadget.feature.radios.ir.control.IrController
import dev.ranzlappen.gadget.feature.radios.ir.rooted.control.RootedIrController
import dev.ranzlappen.gadget.feature.keepalive.control.KeepAliveController
import dev.ranzlappen.gadget.feature.keepalive.rooted.control.RootedKeepAliveController
import dev.ranzlappen.gadget.feature.microphone.control.MicrophoneController
import dev.ranzlappen.gadget.feature.microphone.rooted.control.RootedMicrophoneController
import dev.ranzlappen.gadget.feature.radios.nfc.control.NfcController
import dev.ranzlappen.gadget.feature.radios.nfc.rooted.control.RootedNfcController
import dev.ranzlappen.gadget.feature.notification.control.NotificationController
import dev.ranzlappen.gadget.feature.notification.rooted.control.RootedNotificationController
import dev.ranzlappen.gadget.core.root.emergency.EmergencyResetCoordinator
import dev.ranzlappen.gadget.feature.rooted.root.emergency.RootedEmergencyResetCoordinator
import dev.ranzlappen.gadget.feature.rooted.root.sysfs.RootedSysfsMutationLog
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dev.ranzlappen.gadget.feature.storage.rooted.control.RootedStorageController
import dev.ranzlappen.gadget.feature.storage.control.StorageController
import dev.ranzlappen.gadget.feature.apps.rooted.control.RootedAppsRootController
import dev.ranzlappen.gadget.feature.apps.root.AppsRootController
import dev.ranzlappen.gadget.feature.radios.wifi.rooted.control.RootedWifiController
import dev.ranzlappen.gadget.feature.radios.wifi.control.WifiController
import dev.ranzlappen.gadget.feature.usbdebug.rooted.control.RootedUsbDebuggingController
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbDebuggingController
import dev.ranzlappen.gadget.feature.radios.bt.BtEnhancedInfoProvider
import dev.ranzlappen.gadget.feature.radios.bt.RootedBtEnhancedInfoProvider
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
    //
    // Torch bindings (TorchSysfsController + TorchRootCapabilities)
    // moved to :feature:torch-rooted's RootedTorchModule in
    // refactor-2026 Phase 2 / E2. The remaining cross-feature
    // controllers stay here until each feature follows the same
    // migration (tracked at
    // https://github.com/Ranzlappen/HardwareDash/issues/94).

    @Binds @Singleton
    abstract fun bindCameraController(impl: RootedCameraController): CameraController

    @Binds @Singleton
    abstract fun bindMicrophoneController(impl: RootedMicrophoneController): MicrophoneController

    // ──── Batch 5: sensors + battery + sysfs mutation log ────

    @Binds @Singleton
    abstract fun bindSysfsMutationLog(impl: RootedSysfsMutationLog): SysfsMutationLog

    @Binds @Singleton
    abstract fun bindBatteryController(impl: RootedBatteryController): BatteryController

    // ──── Batch 6: radios + connectivity controllers ────

    @Binds @Singleton
    abstract fun bindBtEnhancedInfoProvider(impl: RootedBtEnhancedInfoProvider): BtEnhancedInfoProvider

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

    // ──── Batch 13: GPS spoofing ────

    @Binds @Singleton
    abstract fun bindGpsSpoofController(impl: RootedGpsSpoofController): GpsSpoofController

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

    // ──── Batch 10: Diagnostics + Emergency Reset ────

    @Binds @Singleton
    abstract fun bindDiagnosticsController(
        impl: RootedDiagnosticsController,
    ): DiagnosticsController

    @Binds @Singleton
    abstract fun bindEmergencyResetCoordinator(
        impl: RootedEmergencyResetCoordinator,
    ): EmergencyResetCoordinator

    // ──── Batch 17: App-Organizer (apps-rooted) controller ────

    @Binds @Singleton
    abstract fun bindAppsRootController(impl: RootedAppsRootController): AppsRootController
}
