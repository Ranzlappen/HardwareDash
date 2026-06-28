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
- **Widgets:** flashlight toggle widget + strobe widget (both
  function-driven). **QS tile:** `FlashlightTileService`.
- **Automation:** `TorchActionHandler` exposes on/off/toggle/strobe
  actions. **Monitoring:** `torch_intensity` `MetricSource` (poll →
  filled plateau); `MonitorContainer` + `LiveMonitorContainer`.
- **Permissions:** none for standard `setTorchMode`;
  `FOREGROUND_SERVICE` (`shortService`) for the strobe service;
  `WRITE_SETTINGS` is deferred (#95).
- **Rooted:** brightness cap 150 %, thermal override 45 s ceiling, all via
  `RootSafetyGate`.
- **Known gaps:** brightness UX (#95).
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

## Rooted extras ✅ (per feature)

- **What:** root-only capabilities layered onto a feature without
  breaking flavor isolation — torch brightness/thermal/multi-LED,
  vibration PWM tier, future kernel-thermal / raw-input sensors.
- **Safety:** every privileged call goes through `RootSafetyGate` +
  `RootFeatureKey`; a two-stage opt-in (global Safety mode + per-feature
  toggle).
- **Source:** `feature/<name>-rooted` modules + `:core:root`.
- **Deep-dive:** [Flavors & Root Safety](Flavors-and-Root-Safety).

## Planned features ⬜

Skeleton modules awaiting their migration batch — each follows the
[Feature Migration Guide](Feature-Migration-Guide):

| Feature | Legacy hook | Module |
|---|---|---|
| Camera | Camera2 | `:feature:camera` |
| Audio | `AudioManager` / `MediaRecorder` | `:feature:audio` |
| GPS | `FusedLocationProvider` | `:feature:gps` |
| Battery | `BatteryManager` | `:feature:battery` |
| Motion / Ambient | `SensorManager` | `:feature:motion`, `:feature:ambient` |
| Flipper Zero | USB CDC-ACM + BLE GATT | `:feature:flipper` (+ `-rooted`) |
| Storage | StorageManager | `:feature:storage` (+ `-rooted`) |
| Lock / Diagnostics / Bugreport | device admin / dumpsys | `:feature:lock`, `:feature:diagnostics`, `:feature:bugreport` (+ `-rooted`) |
| Actuators (hub) | — | `:feature:actuators` |
| Manual | — | `:feature:manual` |

---

> _Last reviewed: 2026-06-28 · Source: `MASTER-PLAN.md`, `settings.gradle.kts`,
> feature source counts · Related: every `feature/*`._
