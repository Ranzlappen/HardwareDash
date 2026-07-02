package dev.ranzlappen.gadget.feature.standard.root

import dev.ranzlappen.gadget.core.root.*
import dev.ranzlappen.gadget.core.root.companion.CompanionModuleDetector
import dev.ranzlappen.gadget.core.root.core.RootDetector
import dev.ranzlappen.gadget.core.root.core.RootService
import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.launch.LaunchGate
import com.gadget.adbdebug.AdbDebuggingController
import com.gadget.adbdebug.StandardAdbDebuggingController
import com.gadget.audio.AudioRoutingController
import com.gadget.audio.StandardAudioRoutingController
import com.gadget.automation.AutomationController
import com.gadget.automation.StandardAutomationController
import com.gadget.battery.BatteryController
import com.gadget.battery.StandardBatteryController
import dev.ranzlappen.gadget.feature.radios.bt.control.BluetoothController
import dev.ranzlappen.gadget.feature.radios.bt.control.StandardBluetoothController
import com.gadget.camera.CameraController
import com.gadget.camera.StandardCameraController
import com.gadget.cell.CellController
import com.gadget.cell.StandardCellController
import com.gadget.diagnostics.DiagnosticsController
import com.gadget.diagnostics.StandardDiagnosticsController
import com.gadget.display.DisplayController
import com.gadget.display.StandardDisplayController
import com.gadget.gps.GpsController
import com.gadget.gps.StandardGpsController
import com.gadget.gps.spoof.GpsSpoofController
import com.gadget.gps.spoof.StandardGpsSpoofController
import com.gadget.ir.IrController
import com.gadget.ir.StandardIrController
import com.gadget.keepalive.KeepAliveController
import com.gadget.keepalive.StandardKeepAliveController
import com.gadget.microphone.MicrophoneController
import com.gadget.microphone.StandardMicrophoneController
import com.gadget.nfc.NfcController
import com.gadget.nfc.StandardNfcController
import com.gadget.notification.NotificationController
import com.gadget.notification.StandardNotificationController
import dev.ranzlappen.gadget.core.root.emergency.EmergencyResetCoordinator
import dev.ranzlappen.gadget.feature.standard.root.emergency.StandardEmergencyResetCoordinator
import dev.ranzlappen.gadget.feature.standard.root.sysfs.StandardSysfsMutationLog
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import com.gadget.storage.StandardStorageController
import com.gadget.storage.StorageController
import dev.ranzlappen.gadget.feature.radios.wifi.control.StandardWifiController
import dev.ranzlappen.gadget.feature.radios.wifi.control.WifiController
import com.gadget.usbdebug.StandardUsbDebuggingController
import com.gadget.usbdebug.UsbDebuggingController
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
}
