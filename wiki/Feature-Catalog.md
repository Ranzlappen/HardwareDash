# Feature Catalog

The product-facing index of app capabilities. For the Gradle-module view,
see [Module Catalog](Module-Catalog). Status legend: ✅ live · 🟡 partial
· ⬜ planned (skeleton).

## Home (Dashboard) ⚪

- **What (v3 B3):** live status cards — battery (level + temperature),
  storage (used % + free GB), Wi-Fi (on/off + RSSI), latest logbook entry —
  each opening its tool; quick actions (torch on/off, short test buzz); the
  user's pinned tools with the pin/hide/reorder editor. Reads features only
  through core seams (MetricSource map, `ModuleActionRegistry`, `LogbookDao`).
- **Standard:** ⚪ · **Rooted:** ⚪ (same).
- **Source:** `:feature:dashboard`.

## Settings ✅

- **What:** About (app version), Appearance, Accessibility toggles,
  Backup/Restore (whole-app ZIP v5), and — on rooted — the root feature
  toggles card.
- **Permissions:** storage-access framework for backup files (no
  persistent permission; uses `ACTION_CREATE_DOCUMENT` /
  `ACTION_OPEN_DOCUMENT`).
- **Standard:** ✅ · **Rooted:** ✅ + root toggles.
- **Source:** `:feature:settings`, `:core:datastore`; backup via `:app`'s
  `BackupManager` (slot-injected). See
  [Troubleshooting](Troubleshooting) for the backup/restore design.

## Torch ✅ (advanced blueprint)

- **What:** Camera2 flashlight toggle; strobe / hold / Morse controls;
  on rooted, brightness boost + duty-cycle strobe + multi-LED + thermal
  override.
- **Widgets:** flashlight toggle, strobe, monitor gauge, and monitor
  chart widgets (function-driven). **QS tiles:** `FlashlightTileService`
  + `StrobeTileService`.
- **Automation:** `TorchActionHandler` exposes on/off/toggle/strobe
  actions. **Monitoring:** `torch_intensity` `MetricSource` (poll →
  filled plateau); `MonitorContainer` + `LiveMonitorContainer`.
- **Permissions:** none for standard `setTorchMode`;
  `FOREGROUND_SERVICE` (`shortService`) for the strobe service.
- **Rooted:** brightness cap 150 %, thermal override 45 s ceiling, all via
  `RootSafetyGate`.
- **Recent:** brightness slider (#95, API 33+
  `turnOnTorchWithStrengthLevel`) and floating overlay button (#101) are
  done.
- **Source:** `:feature:torch` (+ `-rooted`/`-standard`).
- **Deep-dive:** [Torch Blueprint](Torch-Blueprint).

## Vibration ✅

- **What:** amplitude/duration controls, a freehand draw-canvas pattern
  builder, and a vibrate widget + pattern widget.
- **Monitoring:** `VibrationRuntime` models the commanded amplitude (the
  OS exposes no "currently vibrating" query) → a decaying poll signal.
- **Automation:** `VibrationActionHandler`.
- **Rooted:** a 4-capability rooted tier (PWM/driver controls).
- **Standard:** ✅ · **Rooted:** ✅.
- **Source:** `:feature:vibration` (+ `-rooted`/`-standard`).

## Apps / App Organizer ✅

- **What:** folders of apps + folder home-screen widgets (content-widget
  archetype — renders a live folder cover / app-preview grid, opens a
  floating `FolderPopupActivity` on tap).
- **Widgets:** `FolderWidgetProvider` (content/launcher archetype) with
  tray-drop configure + in-app pin paths.
- **Persistence:** `apps.db` (Room) + folder cover photos + web-link
  favicons (all in the backup sweep).
- **Automation:** `AppsActionHandler` (`featureId = "apps"`) exposes
  `refresh_apps` / `open_folder` / `launch_app` actions, reusing
  `AppRepository`, `FolderPopupActivity`, and `AppLauncher` rather than
  duplicating any launch logic. **Monitoring:** `apps_folder_count`
  `MetricSource` (push, from `AppsDao.observeFolders()`) — folder count is
  the only already-modelled numeric signal in the domain layer; no
  `MonitorWidgetNotifier` yet (the folder widget is a content widget, not
  a metric gauge) and the screen doesn't embed `MonitorContainer` /
  `LiveMonitorContainer` yet.
- **Rooted:** per-app freeze / unfreeze / force-stop overflow menu (via
  `:feature:apps-rooted`'s `AppsRootController`, deny-list-protected). The
  two state-mutating actions (freeze / force-stop) are confirm-gated behind
  a `GadgetDialog`, matching the `RootConfirmActionRow` write-tier
  convention; unfreeze (the restore) runs immediately.
- **Standard:** ✅ · **Rooted:** ✅.
- **Source:** `:feature:apps`, `:feature:apps-rooted`.

## Sensors ⚪

- **What (v3 B5):** a sensor explorer over `SensorManager.getSensorList()`
  — every sensor the device reports (standard, vendor and wake-up
  variants), grouped motion / position / environment / other, with name,
  vendor and type. Each sensor opens a detail screen: live value per axis
  with a sparkline, accuracy and event rate, full specs (range,
  resolution, power, min delay, reporting mode, wake-up), "copy specs and
  readings", and the persisted history chart when the sensor backs a
  metric. "Copy sensor list" copies the whole inventory. Step sensors ask
  for activity recognition on their detail screen; one-shot trigger
  sensors are explained instead of streamed. Absorbs v2's `ambient` and
  `motion` modules and brings back v1's full list.
- **Monitoring:** `proximity`, `light`, `acceleration`, `rotation_rate`,
  `step_count`, `motion_detected` and `ambient_light` MetricSources (keys
  unchanged from v2; `ambient_light` duplicates `light` and is kept for
  saved rules and widgets).
- **Automation:** rules gate on those metrics (`MetricThreshold` /
  `MetricCompare`); there is no `ActionHandler` — nothing to *do* to a
  sensor.
- **Source:** `:feature:sensors` (+ `DeviceSensors` in `:core:hardware`).

## Automation ✅

- **What:** the cross-automation engine — `when <trigger> [if <conditions>]
  then <actions>` rules, built without writing Kotlin. Rules list + a
  `RuleEditorSheet` builder.
- **Triggers:** metric threshold (with hysteresis), schedule
  (AlarmManager), system event (power / connectivity / boot), manual.
- **Actions:** any feature's `ActionHandler` actions.
- **Safety:** three-layer root gating; an `AutomationBudget` bounds
  storms; a self-stopping `specialUse` FGS.
- **Permissions:** `SCHEDULE_EXACT_ALARM` (opt-in, degrades gracefully),
  `ACCESS_NETWORK_STATE`.
- **Source:** `:feature:automation-ui`, `:core:automation`,
  `:core:hardware`, `automation.db`.
- **Deep-dive:** [Automation Engine](Automation-Engine).

## Widgets ✅

- **What:** the home-screen-widget framework consumed by Torch,
  Vibration, and Apps. Function-driven (toggle/momentary) and
  content/launcher archetypes; dynamic in-app pinning; soft-delete
  ("remove but keep inert"); custom-icon import; per-instance appearance.
- **Source:** `:core:widgetkit`.
- **Deep-dive:** [Widgets, Tiles & Surfaces](Widgets-Tiles-and-Surfaces).

## Sub-GHz Radio ✅

- **What:** detects an attached SDR / Sub-GHz USB transceiver (RTL-SDR,
  HackRF, YARD Stick One, LimeSDR Mini, CC1101 bridges) on the host bus and
  reports whether it covers the 300–928 MHz ISM bands. Android exposes no
  first-party Sub-GHz radio API, so the standard flavor is **detection-only**
  (graceful-unavailable when no dongle / no USB-host bus).
- **Monitoring:** `subghz_bridge_connected` push `MetricSource` (emits on USB
  attach/detach only) → `MonitorContainer` + `LiveMonitorContainer`.
- **Automation:** gate rules on the `subghz_bridge_connected` metric (the
  read-only check actions were removed in v3 B6).
- **Permissions:** none (USB enumeration needs no runtime grant);
  `uses-feature android.hardware.usb.host` (not required).
- **Rooted:** 3 capability rows — raw register access, custom carrier tuning,
  OOK / 2-FSK capture (informational one-ups, gated on the rooted flavor).
- **Standard:** ✅ (detection) · **Rooted:** ✅ + raw-radio rows.
- **Source:** `:feature:radios-subghz`.

## YouTube Downloader ✅

- **What:** downloads videos and audio (including private playlists via an
  in-app cookie login) using the bundled yt-dlp + ffmpeg runtime
  (youtubedl-android). Finished downloads are exported to MediaStore
  (Movies / Music).
- **Monitoring:** `download_progress` `MetricSource`.
- **Automation:** `youtube_downloader` `ActionHandler`.
- **Service:** `dataSync` foreground service for in-progress downloads.
- **Standard:** ✅ (runs unprivileged) · **Rooted:** ✅ (same; standard-only
  feature).
- **Source:** `:feature:youtubedownloader`.

## Flipper Zero ✅

- **What:** bridge to a Flipper Zero over **USB CDC-ACM** (115200-8-N-1) or
  **BLE GATT** (Nordic-UART serial service). Shows live connection state,
  device name, firmware, and battery; transmits Sub-GHz `.sub` and IR `.ir`
  files to the device.
- **RPC:** a hand-rolled protobuf `PB_Main` stack (varint framing +
  command-id demux) driving the System / Storage / Sub-GHz / Infrared command
  suites — no protoc/codegen dependency.
- **Monitoring:** `flipper_connected` + `flipper_battery` `MetricSource`s.
- **Automation:** `flipper` `ActionHandler` — ping, transmit-`.sub`,
  transmit-`.ir` (gate on the `flipper_connected` metric).
- **Permissions:** USB host (no runtime grant); `BLUETOOTH_CONNECT/SCAN` for
  the BLE picker.
- **Rooted:** `flipper_root` action root-grants USB access by relaxing the
  Flipper's `/dev/bus/usb` device-node permissions, so the port opens without
  the per-attach dialog (gated by `RootFeatureKey.FlipperUsbGrant`).
- **Standard:** ✅ · **Rooted:** ✅ + USB auto-grant.
- **Source:** `:feature:flipper` (+ `:feature:flipper-rooted`). Migrated from
  legacy `com.gadget.flipper`.

## Rooted extras ✅ (per feature)

- **What:** root-only capabilities layered onto a feature without
  breaking flavor isolation — torch brightness/thermal/multi-LED,
  vibration PWM tier, future kernel-thermal / raw-input sensors.
- **Safety:** every privileged call goes through `RootSafetyGate` +
  `RootFeatureKey`; a two-stage opt-in (global Safety mode + per-feature
  toggle).
- **Source:** `feature/<name>-rooted` modules + `:core:root`.
- **Deep-dive:** [Flavors & Root Safety](Flavors-and-Root-Safety).

## More live features ✅

The former "planned features" have all shipped their v1 slice — each is
live in the shell with monitoring/automation hooks as noted in the
[Module Catalog](Module-Catalog) status table and the per-feature rows
of [Roadmap & Status](Roadmap-and-Status):

- **Battery** (`:feature:battery` + `-rooted`) — level / charging /
  temperature / voltage / health, dual monitors, battery widget, rooted
  fuel-gauge & charging-profile rows.
- **GPS / Location** (`:feature:gps` + `-rooted`) — OSMDroid map, live
  speed + altitude monitors, GPS-spoofing subsystem, rooted NMEA /
  constellation rows.
- **Camera / Barcode** (`:feature:camera` + `-rooted`) — CameraX + MLKit
  scanner, scan history, rooted high-FPS / RAW rows.
- **Audio** (`:feature:audio` + `-rooted`) — dB meter + WAV recording,
  live dB monitor, rooted mic rows.
- **Storage** (`:feature:storage` + `-rooted`) — volume cards, used-%
  monitor, storage widget, rooted diskstats / fstrim actions.
- **Lock / Diagnostics / Health** (`:feature:lock`,
  `:feature:diagnostics`, `:feature:bugreport`, each + `-rooted`) —
  keyguard state + overlay, memory monitor + shell-dump actions, and the
  permission manager + `pm grant` one-up.
- **Radios** (`:feature:radios-wifi/-bt/-nfc/-ir` + `-rooted`) — all
  live with monitors and ActionHandlers.
- **Cellular** (`:feature:radios-cell` + `-rooted`) — standard
  `TelephonyManager` screen (SIM state, carrier, network type, live
  signal bars) + rooted raw-modem-dump panel; `cell_signal` MetricSource.
- **Display** (`:feature:display` + `-rooted`) — brightness slider
  (standard) + density/refresh-rate override + SurfaceFlinger snapshot
  (rooted); `screen_brightness` MetricSource.
- **Microphone tools** (`:feature:microphone` + `-rooted`) — rooted-only
  extreme mic tools (gain/PCM/rate/multi-mic/effects/system-audio);
  baseline dB meter/recording stays in `:feature:audio`.
- **Notifications** (`:feature:notification` + `-rooted`) — channel
  inspector + test-notification builder (standard); sticky-importance
  override / listener opt-in / lock-screen-overlay test (rooted); a real
  `GadgetNotificationListenerService` backs `active_notifications`.
- **ADB / USB debugging** (`:feature:adbdebug`, `:feature:usbdebug` +
  `-rooted`) — debug-state readouts + Developer-options deep-links
  (standard); toggle/network/getprop/setprop and function-role/diagnostics
  dumps (rooted).
- **Apps root tools** (`:feature:apps-rooted`) — pm-based freeze /
  unfreeze / force-stop, surfaced as a per-app menu inside
  `:feature:apps`'s own screen rather than a dedicated one.
- **Automation power tools** (`:feature:automation` + `-rooted`) —
  screenless by design; its 3 rooted capabilities (privileged intent
  fire, settings override, dumpsys snapshot) surface as rule-builder
  actions in `automation-ui` instead.
- **Manual** (`:feature:manual`) — thin static help screen.
- **Logbook** (`:feature:logbook`) — session-note log entries (tagged) +
  a checkpoint/process tracker with per-checkpoint due dates and
  WorkManager reminders. `logbook_open_checkpoints` MetricSource (push over
  the open-checkpoint flow) + `LogbookActionHandler` (add-entry); registered in the build and reachable from the nav
  rail. Standard-only; data layer in `:core:data`. (Finished from the
  earlier inert draft — W2.)

---

> _Last reviewed: 2026-11-19 · Source: `settings.gradle.kts`,
> feature source counts, [Completion Master Plan](Completion-Master-Plan) ·
> Related: every `feature/*`._
