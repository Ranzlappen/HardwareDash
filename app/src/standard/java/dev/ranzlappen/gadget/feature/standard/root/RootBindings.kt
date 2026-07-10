package dev.ranzlappen.gadget.feature.standard.root

import dev.ranzlappen.gadget.core.root.*
import dev.ranzlappen.gadget.core.root.companion.CompanionModuleDetector
import dev.ranzlappen.gadget.core.root.core.RootDetector
import dev.ranzlappen.gadget.core.root.core.RootService
import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.launch.LaunchGate
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbDebuggingController
import dev.ranzlappen.gadget.feature.adbdebug.control.StandardAdbDebuggingController
import dev.ranzlappen.gadget.feature.audio.control.AudioRoutingController
import dev.ranzlappen.gadget.feature.audio.control.StandardAudioRoutingController
import dev.ranzlappen.gadget.feature.automation.control.AutomationController
import dev.ranzlappen.gadget.feature.automation.control.StandardAutomationController
import dev.ranzlappen.gadget.feature.battery.control.BatteryController
import dev.ranzlappen.gadget.feature.battery.control.StandardBatteryController
import dev.ranzlappen.gadget.feature.radios.bt.control.BluetoothController
import dev.ranzlappen.gadget.feature.radios.bt.control.StandardBluetoothController
import dev.ranzlappen.gadget.feature.camera.control.CameraController
import dev.ranzlappen.gadget.feature.camera.control.StandardCameraController
import dev.ranzlappen.gadget.feature.radios.cell.control.CellController
import dev.ranzlappen.gadget.feature.radios.cell.control.StandardCellController
import dev.ranzlappen.gadget.feature.diagnostics.control.DiagnosticsController
import dev.ranzlappen.gadget.feature.diagnostics.control.StandardDiagnosticsController
import dev.ranzlappen.gadget.feature.display.control.DisplayController
import dev.ranzlappen.gadget.feature.display.control.StandardDisplayController
import dev.ranzlappen.gadget.feature.gps.control.GpsController
import dev.ranzlappen.gadget.feature.gps.control.StandardGpsController
import dev.ranzlappen.gadget.feature.gps.spoof.GpsSpoofController
import dev.ranzlappen.gadget.feature.gps.spoof.StandardGpsSpoofController
import dev.ranzlappen.gadget.feature.radios.ir.control.IrController
import dev.ranzlappen.gadget.feature.radios.ir.control.StandardIrController
import dev.ranzlappen.gadget.feature.keepalive.control.KeepAliveController
import dev.ranzlappen.gadget.feature.keepalive.control.StandardKeepAliveController
import dev.ranzlappen.gadget.feature.microphone.control.MicrophoneController
import dev.ranzlappen.gadget.feature.microphone.control.StandardMicrophoneController
import dev.ranzlappen.gadget.feature.radios.nfc.control.NfcController
import dev.ranzlappen.gadget.feature.radios.nfc.control.StandardNfcController
import dev.ranzlappen.gadget.feature.notification.control.NotificationController
import dev.ranzlappen.gadget.feature.notification.control.StandardNotificationController
import dev.ranzlappen.gadget.core.root.emergency.EmergencyResetCoordinator
import dev.ranzlappen.gadget.feature.standard.root.emergency.StandardEmergencyResetCoordinator
import dev.ranzlappen.gadget.feature.standard.root.sysfs.StandardSysfsMutationLog
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dev.ranzlappen.gadget.feature.storage.control.StandardStorageController
import dev.ranzlappen.gadget.feature.storage.control.StorageController
import dev.ranzlappen.gadget.feature.apps.root.StandardAppsRootController
import dev.ranzlappen.gadget.feature.apps.root.AppsRootController
import dev.ranzlappen.gadget.feature.radios.wifi.control.StandardWifiController
import dev.ranzlappen.gadget.feature.radios.wifi.control.WifiController
import dev.ranzlappen.gadget.feature.usbdebug.control.StandardUsbDebuggingController
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbDebuggingController
import dev.ranzlappen.gadget.feature.radios.bt.BtEnhancedInfoProvider
import dev.ranzlappen.gadget.feature.radios.bt.StandardBtEnhancedInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Standard-flavor Hilt bindings for the rooted-features safety framework and
 * root core layer. Every binding resolves to a no-op so shared code can
 * inject the same interfaces in both flavors without branching on
 * `BuildConfig.IS_ROOTED`.
 *
 * The fully-qualified class name MUST match the rooted flavor's binding file
 * (`dev.ranzlappen.gadget.feature.standard.root.RootBindings`) — Gradle source-set merging picks one of
 * the two based on the active product flavor, so a name mismatch would let
 * both compile into the rooted APK and cause a duplicate-binding crash.
 */
@Module
@InstallIn(SingletonComponent::class)
object RootBindings {

    @Provides
    @Singleton
    fun provideRootCapabilityRegistry(): RootCapabilityRegistry =
        NoOpRootCapabilityRegistry()

    @Provides
    @Singleton
    fun provideRootSafetyGate(): RootSafetyGate = NoOpRootSafetyGate()

    @Provides
    @Singleton
    fun provideRootSoftLimiter(): RootSoftLimiter = NoOpRootSoftLimiter()

    @Provides
    @Singleton
    fun provideRootFeatureToggles(): RootFeatureToggles = NoOpRootFeatureToggles()

    // ──── Batch 2: root core layer ────

    @Provides
    @Singleton
    fun provideRootDetector(): RootDetector = NoOpRootDetector()

    @Provides
    @Singleton
    fun provideRootShell(): RootShell = NoOpRootShell()

    @Provides
    @Singleton
    fun provideRootService(): RootService = NoOpRootService()

    @Provides
    @Singleton
    fun provideLaunchGate(): LaunchGate = NoOpLaunchGate()

    @Provides
    @Singleton
    fun provideCompanionModuleDetector(): CompanionModuleDetector =
        NoOpCompanionModuleDetector()

    // ──── Batch 3: feature controllers ────
    //
    // Torch bindings (TorchSysfsController + TorchRootCapabilities) moved to
    // :feature:torch-standard's StandardTorchModule — the standard-flavor
    // sibling of :feature:torch-rooted's RootedTorchModule — pulled in via
    // standardImplementation. The remaining cross-feature no-ops stay here
    // until each feature follows the same migration.

    @Provides
    @Singleton
    fun provideCameraController(impl: StandardCameraController): CameraController = impl

    @Provides
    @Singleton
    fun provideMicrophoneController(impl: StandardMicrophoneController): MicrophoneController = impl

    // ──── Batch 5: sensors + battery + sysfs mutation log ────

    @Provides
    @Singleton
    fun provideSysfsMutationLog(impl: StandardSysfsMutationLog): SysfsMutationLog = impl

    @Provides
    @Singleton
    fun provideBatteryController(impl: StandardBatteryController): BatteryController = impl

    // ──── Batch 6: radios + connectivity controllers ────

    @Provides
    @Singleton
    fun provideBtEnhancedInfoProvider(impl: StandardBtEnhancedInfoProvider): BtEnhancedInfoProvider = impl

    @Provides
    @Singleton
    fun provideWifiController(impl: StandardWifiController): WifiController = impl

    @Provides
    @Singleton
    fun provideBluetoothController(impl: StandardBluetoothController): BluetoothController = impl

    @Provides
    @Singleton
    fun provideNfcController(impl: StandardNfcController): NfcController = impl

    @Provides
    @Singleton
    fun provideIrController(impl: StandardIrController): IrController = impl

    @Provides
    @Singleton
    fun provideCellController(impl: StandardCellController): CellController = impl

    @Provides
    @Singleton
    fun provideGpsController(impl: StandardGpsController): GpsController = impl

    // ──── Batch 13: GPS spoofing ────

    @Provides
    @Singleton
    fun provideGpsSpoofController(impl: StandardGpsSpoofController): GpsSpoofController = impl

    // ──── Batch 7: automation + notifications + keep-alive controllers ────

    @Provides
    @Singleton
    fun provideAutomationController(impl: StandardAutomationController): AutomationController = impl

    @Provides
    @Singleton
    fun provideNotificationController(impl: StandardNotificationController): NotificationController = impl

    @Provides
    @Singleton
    fun provideKeepAliveController(impl: StandardKeepAliveController): KeepAliveController = impl

    // ──── Batch 8: storage + display + audio routing controllers ────

    @Provides
    @Singleton
    fun provideStorageController(impl: StandardStorageController): StorageController = impl

    @Provides
    @Singleton
    fun provideDisplayController(impl: StandardDisplayController): DisplayController = impl

    @Provides
    @Singleton
    fun provideAudioRoutingController(
        impl: StandardAudioRoutingController,
    ): AudioRoutingController = impl

    // ──── Batch 9: ADB + USB Debugging controllers ────

    @Provides
    @Singleton
    fun provideAdbDebuggingController(
        impl: StandardAdbDebuggingController,
    ): AdbDebuggingController = impl

    @Provides
    @Singleton
    fun provideUsbDebuggingController(
        impl: StandardUsbDebuggingController,
    ): UsbDebuggingController = impl

    // ──── Batch 10: Diagnostics + Emergency Reset ────

    @Provides
    @Singleton
    fun provideDiagnosticsController(
        impl: StandardDiagnosticsController,
    ): DiagnosticsController = impl

    @Provides
    @Singleton
    fun provideEmergencyResetCoordinator(
        impl: StandardEmergencyResetCoordinator,
    ): EmergencyResetCoordinator = impl

    // ──── Batch 17: App-Organizer (apps-rooted) controller ────

    @Provides
    @Singleton
    fun provideAppsRootController(impl: StandardAppsRootController): AppsRootController = impl
}
