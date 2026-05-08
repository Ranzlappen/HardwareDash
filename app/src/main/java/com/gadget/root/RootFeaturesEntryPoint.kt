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
import com.gadget.ir.IrController
import com.gadget.keepalive.KeepAliveController
import com.gadget.microphone.MicrophoneController
import com.gadget.nfc.NfcController
import com.gadget.notification.NotificationController
import com.gadget.root.emergency.EmergencyResetCoordinator
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
    fun torchController(): TorchController
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
