package com.gadget.root

import com.gadget.adbdebug.AdbDebuggingController
import com.gadget.audio.AudioRoutingController
import com.gadget.automation.AutomationController
import com.gadget.battery.BatteryController
import com.gadget.bluetooth.BluetoothController
import com.gadget.camera.CameraController
import com.gadget.cell.CellController
import com.gadget.diagnostics.DiagnosticsController
import com.gadget.display.DisplayController
import com.gadget.gps.GpsController
import com.gadget.gps.spoof.GpsSpoofController
import com.gadget.ir.IrController
import com.gadget.keepalive.KeepAliveController
import com.gadget.microphone.MicrophoneController
import com.gadget.nfc.NfcController
import com.gadget.notification.NotificationController
import dev.ranzlappen.gadget.core.root.emergency.EmergencyResetCoordinator
import com.gadget.sensors.SensorsController
import com.gadget.storage.StorageController
import com.gadget.torch.TorchController
import com.gadget.usbdebug.UsbDebuggingController
import com.gadget.vibration.VibrationController
import com.gadget.wifi.WifiController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point for the rooted-features module. Composable code in
 * `src/main` reaches the controllers + capability/toggle services via
 * `EntryPointAccessors.fromApplication(...)` rather than `@Inject`, since
 * `@Composable` functions can't take constructor parameters.
 *
 * Mirrors the `AppsEntryPoint` shape.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RootFeaturesEntryPoint {
    fun capabilityRegistry(): RootCapabilityRegistry
    fun featureRegistry(): RootFeatureRegistry
    fun featureToggles(): RootFeatureToggles
    /**
     * Returns the **legacy** `com.gadget.torch.TorchController` —
     * the one still wired into the rooted-extras card. Phase 2
     * migrated standard-tier torch control to
     * `dev.ranzlappen.gadget.feature.torch.TorchController`, but
     * the rooted extras (DutyCycleStrobe / MultiLed / Thermal) live
     * on the legacy controller until they're ported under issue
     * https://github.com/Ranzlappen/HardwareDash/issues/94. The
     * method is intentionally `legacy*`-prefixed so it doesn't
     * collide with the new modular feature's entry points (which
     * publish a `torchController()` returning the modular type and
     * would otherwise generate two methods with the same name and
     * different return types on the singleton component).
     */
    fun legacyTorchController(): TorchController
    fun vibrationController(): VibrationController
    fun cameraController(): CameraController
    fun microphoneController(): MicrophoneController
    fun sensorsController(): SensorsController
    fun batteryController(): BatteryController
    fun wifiController(): WifiController
    fun bluetoothController(): BluetoothController
    fun nfcController(): NfcController
    fun irController(): IrController
    fun cellController(): CellController
    fun gpsController(): GpsController
    fun gpsSpoofController(): GpsSpoofController
    fun automationController(): AutomationController
    fun notificationController(): NotificationController
    fun keepAliveController(): KeepAliveController
    fun storageController(): StorageController
    fun displayController(): DisplayController
    fun audioRoutingController(): AudioRoutingController
    fun adbDebuggingController(): AdbDebuggingController
    fun usbDebuggingController(): UsbDebuggingController
    fun diagnosticsController(): DiagnosticsController
    fun emergencyResetCoordinator(): EmergencyResetCoordinator
}
