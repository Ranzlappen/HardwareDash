# Feature Catalog

The product-facing index of app capabilities. For the Gradle-module view,
see [Module Catalog](Module-Catalog). Status legend: ✅ live · 🟡 partial
· ⬜ planned (skeleton).

## Dashboard ✅

- **What:** adaptive home screen — a grid of feature tiles that reflows
  by window size class (single-pane on phones, split on tablets).
- **Standard:** ✅ · **Rooted:** ✅ (same).
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
- **Standard:** ✅ · **Rooted:** ⬜ (`:feature:apps-rooted` skeleton).
- **Source:** `:feature:apps`.

## Sensors ✅

- **What:** proximity / light / acceleration readouts as push
  `MetricSource`s over the `DeviceSensors` seam.
- **Monitoring:** chartable + automatable (same `MetricSource`).
- **Automation:** these signals feed automation triggers/conditions.
- **Source:** `:feature:sensors`.

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
- **Automation:** `SubghzActionHandler` — assert-bridge-attached and
  assert-Sub-GHz-capable conditions.
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
- **Automation:** `flipper` `ActionHandler` — assert-connected, ping,
  transmit-`.sub`, transmit-`.ir`.
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
- **Motion / Ambient** (`:feature:motion`, `:feature:ambient`) — sensor
  readouts + monitors + (ambient) assert actions.
- **Storage** (`:feature:storage` + `-rooted`) — volume cards, used-%
  monitor, storage widget, rooted diskstats / fstrim actions.
- **Lock / Diagnostics / Health** (`:feature:lock`,
  `:feature:diagnostics`, `:feature:bugreport`, each + `-rooted`) —
  keyguard state + overlay, memory monitor + shell-dump actions, and the
  permission manager + `pm grant` one-up.
- **Radios** (`:feature:radios-wifi/-bt/-nfc/-ir` + `-rooted`) — all
  live with monitors and ActionHandlers.
- **Actuators** (`:feature:actuators`) — vibrator availability +
  haptic actions.
- **Manual** (`:feature:manual`) — thin static help screen.

## Controller-only (no screen yet) 🟡

Migrated rooted/standard controllers with **no UI, nav route,
monitoring, or automation wiring** — the build-out is tracked in the
[Completion Master Plan](Completion-Master-Plan) (W2):

| Feature | Module | Note |
|---|---|---|
| Display | `:feature:display` (+ `-rooted`) | Brightness / refresh / rotation controllers only |
| Microphone | `:feature:microphone` (+ `-rooted`) | dB/recording UI currently lives in `:feature:audio` |
| Notification tools | `:feature:notification` (+ `-rooted`) | Legacy builder presets still in `:app` |
| ADB / USB debugging | `:feature:adbdebug`, `:feature:usbdebug` (+ `-rooted`) | State + toggle controllers only |
| Cellular | `:feature:radios-cell` (+ `-rooted`) | Rooted modem-dump controller; standard screen pending |
| Automation controllers | `:feature:automation` (+ `-rooted`) | Role vs `automation-ui` to be resolved |

Also still pending: `:feature:apps-rooted` (empty skeleton).

---

> _Last reviewed: 2026-07-03 · Source: `settings.gradle.kts`,
> feature source counts, [Completion Master Plan](Completion-Master-Plan) ·
> Related: every `feature/*`._
