package dev.ranzlappen.gadget.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level routes in the Gadget app.
 *
 * String-based routes for now — Navigation Compose 2.7.6 (the version
 * pinned in the catalog) predates the typesafe-route API that landed
 * in 2.8. Encapsulated behind a sealed interface so:
 *
 *   * The literal route string lives in exactly one place per route.
 *   * Per-destination metadata (label, filled/outlined icons) travels
 *     with the route — no parallel string-to-icon map to keep in sync.
 *   * Migrating to typesafe routes will be a cosmetic refactor when
 *     the catalog bumps Navigation Compose to 2.8+.
 *
 * Top-level destinations only. Sub-routes (e.g. "/sensors/{id}") will
 * be modelled as nested sealed entries on each top-level destination
 * as they're added in later batches.
 */
@Immutable
sealed interface GadgetDestination {
    /** The route string registered with NavHost. */
    val route: String

    /** Human-readable label, shown in the nav bar / rail. */
    val label: String

    /** Icon when the destination is selected (filled M3 variant). */
    val iconFilled: ImageVector

    /** Icon when the destination is unselected (outlined M3 variant). */
    val iconOutlined: ImageVector

    data object Dashboard : GadgetDestination {
        override val route = "dashboard"
        override val label = "Dashboard"
        override val iconFilled = Icons.Filled.Dashboard
        override val iconOutlined = Icons.Outlined.Dashboard
    }

    data object Sensors : GadgetDestination {
        override val route = "sensors"
        override val label = "Sensors"
        override val iconFilled = Icons.Filled.Sensors
        override val iconOutlined = Icons.Outlined.Sensors
    }

    /**
     * Motion feature module — gyroscope, step counter, and motion-detect
     * sensors. A first-class entry in the scrollable [modules] region.
     * Ships monitoring ([RotationRateMetricSource], [StepCounterMetricSource],
     * [MotionDetectedMetricSource]) ready; read-only sensors, no ActionHandler
     * needed.
     */
    data object Motion : GadgetDestination {
        override val route = "motion"
        override val label = "Motion"
        override val iconFilled = Icons.Filled.DirectionsRun
        override val iconOutlined = Icons.Outlined.DirectionsRun
    }

    /**
     * Audio feature module — live microphone dB meter and WAV voice
     * recording. A first-class entry in the scrollable [modules] region.
     * Ships monitoring ([DbMeterMetricSource]) and automation
     * ([AudioActionHandler]) ready.
     */
    data object Audio : GadgetDestination {
        override val route = "audio"
        override val label = "Audio"
        override val iconFilled = Icons.Filled.Mic
        override val iconOutlined = Icons.Outlined.Mic
    }

    data object Actuators : GadgetDestination {
        override val route = "actuators"
        override val label = "Actuators"
        override val iconFilled = Icons.Filled.Tune
        override val iconOutlined = Icons.Outlined.Tune
    }

    data object Automation : GadgetDestination {
        override val route = "automation"
        override val label = "Automation"
        override val iconFilled = Icons.Filled.Bolt
        override val iconOutlined = Icons.Outlined.Bolt
    }

    data object Settings : GadgetDestination {
        override val route = "settings"
        override val label = "Settings"
        override val iconFilled = Icons.Filled.Settings
        override val iconOutlined = Icons.Outlined.Settings
    }

    /**
     * Torch / Flashlight feature module. A first-class rail entry in
     * the scrollable [modules] region. Also reachable from QS tile /
     * home-screen widget interactions (those drive the controller
     * directly, not this route) and from a deep-link / notification
     * action.
     *
     * Torch is the reference implementation of the module blueprint —
     * it ships a `ModuleInfo` (permissions / OS-compatibility /
     * firmware) consumed by `ModuleScreenScaffold`.
     */
    data object Torch : GadgetDestination {
        override val route = "torch"
        override val label = "Torch"
        override val iconFilled = Icons.Filled.FlashlightOn
        override val iconOutlined = Icons.Outlined.FlashlightOn
    }

    /**
     * Vibration / haptics feature module — the second real module after
     * [Torch], a first-class entry in the scrollable [modules] region. Ships
     * the full module blueprint (ModuleInfo + monitoring + automation + a
     * 4-widget surface + a rooted extreme-tier).
     */
    data object Vibration : GadgetDestination {
        override val route = "vibration"
        override val label = "Vibration"
        override val iconFilled = Icons.Filled.Vibration
        override val iconOutlined = Icons.Outlined.Vibration
    }

    /**
     * App-Organizer feature module — folders of installed apps / PWAs / web
     * links that double as designable home-screen folder widgets. Migrated out
     * of the legacy monolith; a first-class entry in the scrollable [modules]
     * region. Owns a top-level folder grid + a per-folder editor sub-route.
     */
    data object Apps : GadgetDestination {
        override val route = "apps"
        override val label = "Apps"
        override val iconFilled = Icons.Filled.Apps
        override val iconOutlined = Icons.Outlined.Apps
    }

    /**
     * Battery feature module — level, charging state, temperature, voltage,
     * and health readouts from [android.os.BatteryManager] broadcasts. Live
     * and history monitoring via the shared monitoring framework. The rooted
     * extreme-tier (fuel-gauge, cell monitor, charging-profile override) ships
     * separately as `:feature:battery-rooted`.
     */
    data object Battery : GadgetDestination {
        override val route = "battery"
        override val label = "Battery"
        override val iconFilled = Icons.Filled.BatteryFull
        override val iconOutlined = Icons.Outlined.BatteryFull
    }

    /**
     * GPS / Location feature module — live position on an OSMDroid map,
     * coordinates readout (lat/lon/altitude/speed/bearing/accuracy), and
     * speed + altitude monitoring. Requires [ACCESS_FINE_LOCATION]; the
     * rooted extreme-tier (NMEA tap, constellation dump, spoofing) ships
     * separately as `:feature:gps-rooted`.
     */
    data object Gps : GadgetDestination {
        override val route = "gps"
        override val label = "GPS"
        override val iconFilled = Icons.Filled.LocationOn
        override val iconOutlined = Icons.Outlined.LocationOn
    }

    /**
     * Storage feature module — used / free / total per mounted volume
     * ([StatFs] + [StorageManager.getStorageVolumes]). No runtime
     * permissions required. The rooted extreme-tier (fstrim, drop_caches,
     * dumpsys diskstats) ships separately as `:feature:storage-rooted`.
     */
    data object Storage : GadgetDestination {
        override val route = "storage"
        override val label = "Storage"
        override val iconFilled = Icons.Filled.Storage
        override val iconOutlined = Icons.Outlined.Storage
    }

    /**
     * IR Blaster feature module — transmit NEC / Pronto / RAW IR codes via
     * [android.hardware.ConsumerIrManager]. Saved-signal library + automation
     * action binding. No runtime permissions required. The rooted extreme-tier
     * (LIRC sysfs + raw GPIO) ships separately as `:feature:radios-ir-rooted`.
     */
    data object RadiosIr : GadgetDestination {
        override val route = "radios_ir"
        override val label = "IR Blaster"
        override val iconFilled = Icons.Filled.SettingsRemote
        override val iconOutlined = Icons.Outlined.SettingsRemote
    }

    /**
     * Camera / Barcode Scanner feature module — CameraX preview + MLKit
     * barcode scanning (all formats), scan history, WiFi/URL extraction.
     * Requires [android.Manifest.permission.CAMERA].
     */
    data object Camera : GadgetDestination {
        override val route = "camera"
        override val label = "Scanner"
        override val iconFilled = Icons.Filled.QrCodeScanner
        override val iconOutlined = Icons.Outlined.QrCodeScanner
    }

    /**
     * NFC feature module — reads NFC/NDEF tags and emulates them via
     * Host Card Emulation (HCE). A Phase-2 migration from the legacy
     * monolith; a first-class entry in the scrollable [modules] region.
     */
    data object RadiosNfc : GadgetDestination {
        override val route = "radios_nfc"
        override val label = "NFC"
        override val iconFilled = Icons.Filled.Nfc
        override val iconOutlined = Icons.Outlined.Nfc
    }

    /**
     * Bluetooth feature module — adapter status, bonded device list,
     * BT-enabled metric source for automation, and read-only BLE info.
     */
    data object RadiosBt : GadgetDestination {
        override val route = "radios_bt"
        override val label = "Bluetooth"
        override val iconFilled = Icons.Filled.Bluetooth
        override val iconOutlined = Icons.Outlined.Bluetooth
    }

    /**
     * WiFi feature module — adapter status, live signal strength,
     * network details (SSID / BSSID / frequency / link speed), WiFi-signal
     * and WiFi-enabled metric sources for monitoring and automation.
     */
    data object RadiosWifi : GadgetDestination {
        override val route = "radios_wifi"
        override val label = "WiFi"
        override val iconFilled = Icons.Filled.Wifi
        override val iconOutlined = Icons.Outlined.Wifi
    }

    /**
     * Sub-GHz feature module — detects an attached SDR / Sub-GHz USB
     * transceiver (RTL-SDR, HackRF, YARD Stick One, …) on the host bus,
     * exposes a bridge-connected metric source for monitoring/automation,
     * and lists the rooted raw-radio one-ups. Android has no first-party
     * Sub-GHz radio API, so the standard flavor is detection-only.
     */
    data object RadiosSubghz : GadgetDestination {
        override val route = "radios_subghz"
        override val label = "Sub-GHz"
        override val iconFilled = Icons.Filled.SettingsInputAntenna
        override val iconOutlined = Icons.Outlined.SettingsInputAntenna
    }

    /**
     * Ambient light feature module — live lux reading from the device's
     * light sensor, with level descriptors and monitoring support.
     */
    data object Ambient : GadgetDestination {
        override val route = "ambient"
        override val label = "Ambient"
        override val iconFilled = Icons.Filled.LightMode
        override val iconOutlined = Icons.Outlined.LightMode
    }

    /**
     * Lock / Security feature module — keyguard lock state, screen-lock
     * security level, biometric enrollment, and lock-state monitoring.
     */
    data object Lock : GadgetDestination {
        override val route = "lock"
        override val label = "Lock"
        override val iconFilled = Icons.Filled.Lock
        override val iconOutlined = Icons.Outlined.Lock
    }

    /**
     * Diagnostics feature module — capability overview for rooted diagnostic
     * shell dumps (logcat, meminfo, cpuinfo, procstats) via the automation engine.
     */
    data object Diagnostics : GadgetDestination {
        override val route = "diagnostics"
        override val label = "Diagnostics"
        override val iconFilled = Icons.Filled.Analytics
        override val iconOutlined = Icons.Outlined.Analytics
    }

    /**
     * Health / BugReport feature module — scans all runtime permission grant
     * states and surfaces missing permissions. No runtime requests are triggered.
     */
    data object BugReport : GadgetDestination {
        override val route = "bugreport"
        override val label = "Health"
        override val iconFilled = Icons.Filled.HealthAndSafety
        override val iconOutlined = Icons.Outlined.HealthAndSafety
    }

    /**
     * Manual / Help feature module — static documentation for all modules,
     * capabilities, and how to use the automation engine.
     */
    data object Manual : GadgetDestination {
        override val route = "manual"
        override val label = "Help"
        override val iconFilled = Icons.Filled.Help
        override val iconOutlined = Icons.Outlined.Help
    }

    /**
     * YouTube Downloader feature module — downloads videos and audio
     * (including private playlists) via the bundled yt-dlp + ffmpeg runtime.
     * Standard-flavor; runs unprivileged. Ships monitoring
     * ([dev.ranzlappen.gadget.feature.youtubedownloader.monitor.DownloadMetricSource])
     * and automation
     * ([dev.ranzlappen.gadget.feature.youtubedownloader.automation.DownloadActionHandler])
     * surfaces.
     */
    data object Youtubedownloader : GadgetDestination {
        override val route = "youtube_downloader"
        override val label = "Downloader"
        override val iconFilled = Icons.Filled.Download
        override val iconOutlined = Icons.Outlined.Download
    }

    companion object {
        /**
         * Destinations pinned to the **top** of the rail, above the
         * scrollable [modules] region. Index 0 is the canonical start
         * destination ([Dashboard]).
         */
        val pinnedTop: List<GadgetDestination> = listOf(Dashboard)

        /**
         * Feature modules shown in the rail's **scrollable middle**
         * region between [pinnedTop] and [pinnedBottom]. Each entry
         * gets a rail button — a module is never dashboard-only.
         *
         * Real modules replace the abstract placeholder areas as they
         * land: [Torch], [Vibration], [Apps], [Sensors], and
         * [Automation] (the rules list + builder) are live;
         * [Actuators] remains a coming-soon placeholder until its
         * feature module ships. Append new / legacy-migrated modules
         * here.
         */
        val modules: List<GadgetDestination> = listOf(
            Torch, Vibration, Apps, Sensors,
            Battery, Gps, Storage, RadiosIr, Camera,
            Motion, Audio, RadiosNfc, RadiosBt, RadiosWifi, RadiosSubghz,
            Ambient, Lock, Actuators, Youtubedownloader, Diagnostics, BugReport, Manual, Automation,
        )

        /**
         * Destinations pinned to the **bottom** of the rail, below the
         * scrollable [modules] region.
         */
        val pinnedBottom: List<GadgetDestination> = listOf(Settings)

        /**
         * Every destination that owns a rail button, in render order
         * (pinned-top → modules → pinned-bottom). Used for selection
         * highlighting and back-stack-trimming navigation decisions.
         */
        val railDestinations: List<GadgetDestination> = pinnedTop + modules + pinnedBottom

        /**
         * Look up a [GadgetDestination] by its route string, or
         * `null` if the route doesn't match a known rail entry. Used
         * to decide which rail item is selected.
         */
        fun byRouteOrNull(route: String?): GadgetDestination? =
            route?.let { r -> railDestinations.firstOrNull { it.route == r } }
    }
}
