package com.gadget.root

import dev.ranzlappen.gadget.core.root.*
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbDebuggingController
import dev.ranzlappen.gadget.feature.audio.control.AudioRoutingController
import dev.ranzlappen.gadget.feature.automation.control.AutomationController
import dev.ranzlappen.gadget.feature.battery.control.BatteryController
import dev.ranzlappen.gadget.feature.radios.bt.control.BluetoothController
import dev.ranzlappen.gadget.feature.camera.control.CameraController
import dev.ranzlappen.gadget.feature.radios.cell.control.CellController
import dev.ranzlappen.gadget.feature.diagnostics.control.DiagnosticsController
import dev.ranzlappen.gadget.feature.display.control.DisplayController
import dev.ranzlappen.gadget.feature.gps.control.GpsController
import dev.ranzlappen.gadget.feature.gps.spoof.GpsSpoofController
import dev.ranzlappen.gadget.feature.radios.ir.control.IrController
import com.gadget.keepalive.KeepAliveController
import dev.ranzlappen.gadget.feature.microphone.control.MicrophoneController
import dev.ranzlappen.gadget.feature.radios.nfc.control.NfcController
import dev.ranzlappen.gadget.feature.notification.control.NotificationController
import dev.ranzlappen.gadget.core.root.emergency.EmergencyResetCoordinator
import dev.ranzlappen.gadget.feature.storage.control.StorageController
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbDebuggingController
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
 * reason: it depends on the one remaining **legacy non-modular feature
 * controller** — `KeepAliveController`, still in
 * `app/src/main/java/com/gadget/keepalive/` because it depends on the app-shell
 * `PersistentKeepAliveService` foreground service, which must be relocated
 * first. Every other feature controller (radios/GPS/diagnostics/storage/camera/
 * battery/display/adbdebug/usbdebug/automation/notification/microphone/audio)
 * has already migrated out to its own feature module. Pulling
 * the entry-point into `:core:root` would force `:core:root` to depend
 * on that legacy controller, defeating the purpose of the extraction.
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
