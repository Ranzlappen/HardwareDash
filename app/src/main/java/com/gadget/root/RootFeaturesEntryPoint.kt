package com.gadget.root

import dev.ranzlappen.gadget.core.root.*
import com.gadget.adbdebug.AdbDebuggingController
import com.gadget.audio.AudioRoutingController
import com.gadget.automation.AutomationController
import com.gadget.battery.BatteryController
import dev.ranzlappen.gadget.feature.radios.bt.control.BluetoothController
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
import com.gadget.storage.StorageController
import com.gadget.usbdebug.UsbDebuggingController
import dev.ranzlappen.gadget.feature.radios.wifi.control.WifiController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point for the rooted-features module. Composable code in
 * `src/main` reaches the controllers + capability/toggle services via
 * `EntryPointAccessors.fromApplication(...)` rather than `@Inject`, since
 * `@Composable` functions can't take constructor parameters.
 *
 * **Why this file (and its `ui/` siblings) stay in `:app/src/main/`
 * (refactor-2026 Phase 2 / D4 policy).** The safety + capability
 * framework (`RootSafetyGate`, `RootCapabilityRegistry`,
 * `RootFeatureToggles`, `RootSafetyEvent`, `EmergencyResetCoordinator`,
 * …) moved to `:core:root` in D1. The flavor impls under both
 * `app/src/{standard,rooted}/java/com/gadget/root/` re-packaged to
 * `dev.ranzlappen.gadget.feature.{standard,rooted}.root.*` in D2 + D3.
 *
 * This file (and the 13 `ui/Rooted*` Compose composables that reach it)
 * stays at its legacy `com.gadget.root.*` location for one specific
 * reason: it depends on **20 legacy non-modular feature controllers**
 * (`CameraController`, `MicrophoneController`, …, each still in
 * `app/src/main/java/com/gadget/<feature>/`). Pulling
 * the entry-point into `:core:root` would force `:core:root` to depend
 * on every one of those legacy controllers, defeating the purpose of
 * the extraction.
 *
 * **Replacement plan.** Once each feature controller migrates to its
 * own `:feature:<name>` module (the modular torch / vibration / etc.
 * controllers already exist as the standard tier), this entry point
 * becomes obsolete: the UI sites would consume the modular controllers
 * directly via Hilt `@Inject` (since they'd live in feature modules
 * with their own composables), and the few cross-feature aggregations
 * the entry-point still provides could migrate to a kit-style
 * `Map<FeatureId, ?>` multibinding (the pattern `:core:automation`'s
 * `ModuleActionRegistry` already established). Tracked at
 * https://github.com/Ranzlappen/HardwareDash/issues/94.
 *
 * Mirrors the `AppsEntryPoint` shape.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RootFeaturesEntryPoint {
    fun capabilityRegistry(): RootCapabilityRegistry
    fun featureRegistry(): RootFeatureRegistry
    fun featureToggles(): RootFeatureToggles
    fun cameraController(): CameraController
    fun microphoneController(): MicrophoneController
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
