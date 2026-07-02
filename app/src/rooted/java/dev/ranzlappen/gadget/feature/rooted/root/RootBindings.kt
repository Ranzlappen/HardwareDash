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
import com.gadget.adbdebug.AdbDebuggingController
import com.gadget.adbdebug.RootedAdbDebuggingController
import com.gadget.audio.AudioRoutingController
import com.gadget.audio.RootedAudioRoutingController
import com.gadget.automation.AutomationController
import com.gadget.automation.RootedAutomationController
import com.gadget.battery.BatteryController
import com.gadget.battery.RootedBatteryController
import dev.ranzlappen.gadget.feature.radios.bt.control.BluetoothController
import dev.ranzlappen.gadget.feature.radios.bt.rooted.control.RootedBluetoothController
import com.gadget.camera.CameraController
import com.gadget.camera.RootedCameraController
import com.gadget.cell.CellController
import com.gadget.cell.RootedCellController
import com.gadget.diagnostics.DiagnosticsController
import com.gadget.diagnostics.RootedDiagnosticsController
import com.gadget.display.DisplayController
import com.gadget.display.RootedDisplayController
import com.gadget.gps.GpsController
import com.gadget.gps.RootedGpsController
import com.gadget.gps.spoof.GpsSpoofController
import com.gadget.gps.spoof.RootedGpsSpoofController
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
import dev.ranzlappen.gadget.core.root.emergency.EmergencyResetCoordinator
import dev.ranzlappen.gadget.feature.rooted.root.emergency.RootedEmergencyResetCoordinator
import dev.ranzlappen.gadget.feature.rooted.root.sysfs.RootedSysfsMutationLog
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import com.gadget.storage.RootedStorageController
import com.gadget.storage.StorageController
import dev.ranzlappen.gadget.feature.radios.wifi.rooted.control.RootedWifiController
import dev.ranzlappen.gadget.feature.radios.wifi.control.WifiController
import com.gadget.usbdebug.RootedUsbDebuggingController
import com.gadget.usbdebug.UsbDebuggingController
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
}
