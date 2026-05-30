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
import com.gadget.bluetooth.BluetoothController
import com.gadget.bluetooth.StandardBluetoothController
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
import com.gadget.sensors.SensorsController
import com.gadget.sensors.StandardSensorsController
import com.gadget.storage.StandardStorageController
import com.gadget.storage.StorageController
import com.gadget.wifi.StandardWifiController
import com.gadget.wifi.WifiController
import dev.ranzlappen.gadget.feature.standard.torch.StandardTorchSysfsController
import dev.ranzlappen.gadget.feature.torch.sysfs.TorchSysfsController
import com.gadget.usbdebug.StandardUsbDebuggingController
import com.gadget.usbdebug.UsbDebuggingController
import com.gadget.vibration.StandardVibrationController
import com.gadget.vibration.VibrationController
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

    @Provides
    @Singleton
    fun provideTorchSysfsController(impl: StandardTorchSysfsController): TorchSysfsController = impl

    @Provides
    @Singleton
    fun provideTorchRootCapabilities(
        impl: dev.ranzlappen.gadget.feature.torch.standard.StandardTorchRootCapabilities,
    ): dev.ranzlappen.gadget.feature.torch.TorchRootCapabilities = impl

    @Provides
    @Singleton
    fun provideVibrationController(impl: StandardVibrationController): VibrationController = impl

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
    fun provideSensorsController(impl: StandardSensorsController): SensorsController = impl

    @Provides
    @Singleton
    fun provideBatteryController(impl: StandardBatteryController): BatteryController = impl

    // ──── Batch 6: radios + connectivity controllers ────

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
