# Roadmap & Status

> The canonical roadmap page. Absorbs the useful parts of the former
> `MASTER-PLAN.md`. Git history, commit messages, and PR descriptions are
> the authoritative record of shipped work — this page is the readable
> summary.

## Vision

Gadget will be the definitive Android app for exploring every sensor and
actuator, with sophisticated cross-automation, widgets, notification
panels, granular permission management, and hardware-safety guardrails.
The rooted flavor safely extends functionality; the standard flavor stays
fully functional and Play-store-safe.

## Current phase — Phase 2: Accelerated Feature Migration (✅ feature-complete)

> All Phase-2 skeleton modules are now filled — `flipper` (+ `-rooted`) was
> the last, closing the tail. What remains of Phase 2 is the clean-cut
> deletion of the ~legacy `com.gadget.*` sources, tracked per-feature.

Each feature migrates directly from the archived `legacy-main` branch
into a `:feature:<name>` module using the new design system (`:core:ui`),
component library, token plumbing (`LocalGadgetTheme.current`), and
Hilt-injected state — **no placeholder screens**.

**Clean-cut policy:** `legacy-main` is reference material only. New code
never imports from `com.gadget.**`; it lives entirely under
`dev.ranzlappen.gadget.feature.<name>.*`. Legacy paths are deleted once
the new module is verified.

All work lands on `main` via one PR per `claude/<topic>` batch branch;
the long-lived `claude/refactor-2026` integration branch is retired.

### Migrated & live in the shell

| Feature | Modules | Status |
|---|---|---|
| Settings (About + Appearance + Accessibility + Backup) | `:feature:settings` + `:core:datastore` | ✅ |
| Dashboard | `:feature:dashboard` | ✅ |
| Torch / Flashlight (+ QS tile + 2 home widgets + strobe service) | `:feature:torch` (+ `-rooted`/`-standard`) | ✅ |
| Rooted torch extras (DutyCycle / MultiLed / Thermal) | `:feature:torch-rooted` + `:core:root` | ✅ (closes #94) |
| Vibration (standard + rooted) | `:feature:vibration` (+ `-rooted`/`-standard`) | ✅ |
| App-Organizer + folder widgets (shape / gradient / stroke / grid-preview customization) | `:feature:apps` (+ `-rooted` skeleton) | ✅ |
| Sensors (proximity / light / acceleration) | `:feature:sensors` | ✅ (PR #158) |
| Battery (level / charging / temperature / voltage / health; dual live monitors; rooted fuel-gauge / cell-monitor / charging-profile rows; rooted extreme-tier `BatteryController` — power-supply sysfs fuel-gauge + per-cell reading, charging-profile + charging-type override, hold-SoC loop, thermal-throttle bypass, wireless coil-current write, gated by `RootSafetyGate` — clean-cut migrated out of legacy `com.gadget.battery` into the modules) | `:feature:battery` + `:feature:battery-rooted` | ✅ |
| GPS / Location (map, position, speed, altitude; live speed + altitude monitors; rooted NMEA / constellation / location-override rows; rooted extreme-tier `GpsController` — read-only NMEA raw tap (30 s ceiling) + constellation dump over vendor sysfs / tty nodes, gated by `RootSafetyGate` — controller clean-cut migrated out of legacy `com.gadget.gps` into the modules. The GPS **spoofing** subsystem (GPX/KML/route playback, LSPosed + test-provider anti-detection, legal-ack gate, `LocationSpoofService` foreground service) is fully modularized too — shared engine in `:feature:gps`, rooted installer/controller in `:feature:gps-rooted`, with the LSPosed handshake FQN updated in lock-step; `com.gadget.gps` is now fully deleted) | `:feature:gps` + `:feature:gps-rooted` | ✅ |
| Storage (volumes, used / free / total, live used-% monitor; rooted diskstats / mounts / fstrim / drop_caches rows; rooted extreme-tier `StorageController` — read-only `dumpsys diskstats` + `/proc/mountinfo` enumeration + `fstrim -v` against a hard `/data` `/cache` allow-list + `drop_caches` write, gated by `RootSafetyGate` — clean-cut migrated out of legacy `com.gadget.storage` into the modules) | `:feature:storage` + `:feature:storage-rooted` | ✅ |
| IR Blaster (NEC / Pronto / RAW, saved-signal library, automation action; rooted custom-carrier / GPIO-burst rows; rooted extreme-tier `IrController` — custom LIRC carrier frequency (20–100 kHz) + direct IR-LED GPIO toggling (≤50 % duty, 5 s burst ceiling), gated by `RootSafetyGate` — clean-cut migrated out of legacy `com.gadget.ir` into the modules) | `:feature:radios-ir` + `:feature:radios-ir-rooted` | ✅ |
| Barcode Scanner (CameraX + MLKit, all formats, scan history, WiFi/URL; rooted high-fps / manual-override / HAL-bypass rows; rooted extreme-tier `CameraController` — high-FPS (≤240 fps, 30 s ceiling) / manual-exposure / RAW-DNG / multi-camera (≤3 streams, 15 s) / v4l2 HAL-bypass / shutter-sound via direct Camera2 + vendor sysfs, gated by `RootSafetyGate` — clean-cut migrated out of legacy `com.gadget.camera` into the modules) | `:feature:camera` + `:feature:camera-rooted` | ✅ |
| Motion (gyroscope / step counter / motion detect; per-sensor live monitors; rooted high-polling / raw-unfiltered / sysfs-read rows) | `:feature:motion` | ✅ |
| Audio (dB meter + WAV voice recording; live dB monitor; rooted mic-gain / direct-PCM / custom-sample-rate rows; rooted extreme-tier `AudioRoutingController` — stream-volume bypass, force-routing, mute-all via the shared ALSA mixer, dumpsys — and rooted `MicrophoneController` — mic-gain boost, direct-PCM, custom sample rate, multi-mic + system-audio capture, audio-effect override — both gated by `RootSafetyGate`; clean-cut migrated out of legacy `com.gadget.audio` + `com.gadget.microphone` into `:feature:audio(-rooted)` + `:feature:microphone(-rooted)`, with the shared `AlsaMixerControl` tinymix wrapper relocated to `:core:root`) | `:feature:audio` + `:feature:audio-rooted` + `:feature:microphone` + `:feature:microphone-rooted` | ✅ |
| NFC (NDEF tag read + HCE emulation + template library; live + history NFC-state monitors; rooted raw-NCI row; rooted `NfcController` — raw NCI command exchange over the vendor sysfs node with a 256-byte payload ceiling + 5 s read-timeout, gated by `RootSafetyGate` — clean-cut migrated out of legacy `com.gadget.nfc` into the modules) | `:feature:radios-nfc` + `:feature:radios-nfc-rooted` | ✅ |
| Bluetooth (adapter status + bonded devices; GATT battery + RSSI standard; hidden battery + A2DP codec rooted; live + history BT-state monitors; rooted hidden-battery / A2DP-codec rows; rooted extreme-tier `BluetoothController` — rfkill toggle / TX-power override capped at the 10 dBm Class-1 ceiling via bluetoothctl+hcitool / read-only HCI-snoop-log tail, gated by `RootSafetyGate` — clean-cut migrated out of legacy `com.gadget.bluetooth` into the modules) | `:feature:radios-bt` + `:feature:radios-bt-rooted` | ✅ |
| WiFi (adapter status + network details SSID/BSSID/freq/speed; live signal + enabled history monitors; rooted rfkill / TX-power / channel-select rows; enabled + connected automation actions; rooted `wifi_root` ActionHandler — rfkill toggle / TX-power override capped at 20 dBm / channel override on a regulatory allow-list / read-only monitor-IBSS injection probe, each gated by `RootSafetyGate`) | `:feature:radios-wifi` + `:feature:radios-wifi-rooted` | ✅ |
| Cellular (screenless rooted-extras controller; rooted extreme-tier `CellController` — read-only modem dump over Qualcomm `qcom_smd*` / `qmi_devices` / `rmnet` sysfs + per-band RSRP/RSRQ/SINR signal deep-dump, no AT-command write path, gated by `RootSafetyGate` — clean-cut migrated out of legacy `com.gadget.cell` into the modules) | `:feature:radios-cell` + `:feature:radios-cell-rooted` | ✅ |
| Ambient Light (live lux reading + level descriptor; ambient-light history monitor; assert-bright / assert-dark automation actions; rooted brightness / refresh-rate / density rows) | `:feature:ambient` | ✅ |
| Lock / Security (keyguard lock state + biometrics enrollment; lock-state live + history monitors; assert-locked / assert-unlocked / assert-secure automation actions; rooted secure-keyguard overlay — self-grants SYSTEM_ALERT_WINDOW via appops and draws a bounded anti-phishing overlay, gated by `RootFeatureKey.LockSecureOverlay`, exposed as the `lock_root` action) | `:feature:lock` + `:feature:lock-rooted` | ✅ |
| Actuators / Haptics (vibrator availability + amplitude control; haptic-click / heavy-click / assert-available automation actions; rooted extreme/PWM/dual/rumble capability rows) | `:feature:actuators` | ✅ |
| Diagnostics (rooted shell dump overview; logcat / meminfo / cpuinfo / procstats automation actions via `:feature:diagnostics-rooted`; standard no-root `memory_used_percent` MetricSource — live + history monitors on-screen + automation trigger; rooted extreme-tier `DiagnosticsController` — read-only `logcat -b` tail + `dumpsys meminfo/cpuinfo/procstats` excerpts, tail-capped, gated by `RootSafetyGate` — clean-cut migrated out of legacy `com.gadget.diagnostics` into the modules) | `:feature:diagnostics` + `:feature:diagnostics-rooted` | ✅ |
| Health / BugReport (permission manager — grant-state scan + per-permission runtime request + App-Settings fallback + granted/total summary, refreshes on resume; assert-permission automation action; rooted ADB-diagnostics row + `pm grant` force-grant one-up via `:feature:bugreport-rooted`) | `:feature:bugreport` + `:feature:bugreport-rooted` | ✅ |
| Help / Manual (static documentation screen for all modules, capabilities, and automation engine) | `:feature:manual` | ✅ |
| Rooted Storage actions (diskstats / mounts / fstrim / drop_caches) | `:feature:storage-rooted` | ✅ |
| Sub-GHz Radio (USB SDR / transceiver detection — RTL-SDR / HackRF / YARD Stick One / LimeSDR / CC1101; bridge-connected push monitor; assert-bridge / assert-Sub-GHz-capable automation actions; rooted raw-register / custom-tuning / OOK-FSK-capture rows) | `:feature:radios-subghz` | ✅ |
| YouTube Downloader (yt-dlp + ffmpeg video/audio downloads, private playlists via cookie login, MediaStore export, dataSync FGS; `download_progress` monitor + `youtube_downloader` action) — standard-only, runs unprivileged | `:feature:youtubedownloader` | ✅ |
| Flipper Zero bridge (USB CDC-ACM + BLE GATT transport, hand-rolled protobuf RPC, System/Storage/Sub-GHz/Infrared command suites; `flipper_connected` + `flipper_battery` monitors; `flipper` ActionHandler — assert-connected / ping / transmit .sub / .ir; rooted USB device-node auto-grant via `RootFeatureKey.FlipperUsbGrant`) | `:feature:flipper` + `:feature:flipper-rooted` | ✅ |
| Cross-automation engine + rule builder | `:core:automation` + `:core:hardware` + `:feature:automation-ui` | ✅ (epics #145/#146) |

### Shared infrastructure landed

- **`:core:root`** — the root-safety seam (`RootCapabilityRegistry`,
  `RootSafetyGate`, `RootFeatureKey`). Extended in PR #172 with
  `BluetoothHiddenBatteryApi` and `BluetoothA2dpCodecReflection` keys;
  wired into all 19 sensor/radio/actuator/lock/diagnostics feature modules
  via the rooted-capability rows pattern.
- **`:core:root` → `…core.root.audio.AlsaMixerControl`** — the shared
  `tinymix` ALSA-mixer wrapper (snapshot / restore / gain-set), relocated
  out of the legacy microphone package so both `:feature:microphone-rooted`
  and `:feature:audio-rooted` consume it without a feature-to-feature
  dependency (the mixer-side mute path is shared by both).
- **`:core:widgetkit`** — the home-screen-widget framework (config store,
  pin flow, RemoteViews rendering, base providers, boot re-arm).
- **`:core:monitoring`** — the chart + persist framework (MetricSource
  consumption, monitor containers, hand-drawn charts). Enhanced: per-signal
  min/max/count stats in `MonitorService`, richer notifications with
  conditional "Stop monitoring" action button, `MonitorGlobalPrefs` DataStore
  preference, `MonitorNotificationActionReceiver`, and correct "enable hint"
  vs "collecting" chart placeholder logic.
- **Cross-automation engine** — `:core:automation` (contract + rule model
  + evaluator + runtime), `automation.db` in `:core:data`,
  `:core:hardware`'s `HardwareRegistry`, and `:feature:automation-ui`.
- **Whole-app backup format v5** — ZIP of every DB / DataStore /
  SharedPrefs / asset sweep, with legacy in-process import. Fixed: WAL data
  staged alongside `gadget_db`; `LegacyAppsImporter` uses upsert to survive
  ID collisions; manual re-import UI added to Apps screen overflow.

### Remaining legacy surface

~96 legacy `com.gadget.*` Kotlin files remain across all `:app` source
sets (of which one, the separate `:lsposed-module`'s
`com.gadget.spoofer.xposed`, is out of scope), migrating feature-by-feature
per the [Feature Migration Guide](Feature-Migration-Guide). The radios/GPS
rooted controllers (wifi/bt/nfc/ir/cell/gps/gps-spoof), `diagnostics`,
`storage`, `camera`, `battery`, the five screenless controllers `display` /
`adbdebug` / `usbdebug` / `automation` / `notification`, and `microphone` +
`audio` (their shared `AlsaMixerControl` tinymix wrapper pushed down into
`:core:root` under `…core.root.audio` so neither feature depends on the other)
have all been clean-cut into `:feature:<name>` (+ `-rooted`) pairs. **Just one
legacy rooted controller remains** in the `RootFeaturesEntryPoint` seam —
`keepalive`, held back because its controller depends on the app-shell
`PersistentKeepAliveService` foreground service, which must be relocated (into
`:feature:keepalive-rooted` or a core module) before the controller can follow
the same clean-cut.

**Entry-point seam dissolution (partial, [#94](https://github.com/Ranzlappen/HardwareDash/issues/94)).**
All 19 rooted feature-controller accessors were removed from
`RootFeaturesEntryPoint`, and the 16 interactive `com.gadget.root.ui.Rooted*ExtrasSection`
composables (+ the shared `RootExtrasDisclaimerCard`) they fed were deleted —
they were orphaned dead code, reachable only through the non-navigable legacy
`LinkScreen`. The migrated controllers stay wired (dormant) via the flavor
`RootBindings` and are live on the automation + monitoring seams; re-surfacing
the hands-on rooted UX natively inside each modular feature screen is tracked
as a Phase-3 epic. The entry point now exposes only the live root-safety
framework (`capabilityRegistry` / `featureRegistry` / `featureToggles` /
`emergencyResetCoordinator`), consumed by `MainActivity`'s `FatalLaunchScreen`
and the `RootedFeatureTogglesCard`.

**Dead-island sweep.** Ten provably-dead legacy files (zero external
referrers) were deleted: the legacy chart stack (`com.gadget.ui.charts` —
`MetricHistoryScreen` / `MetricChartViewModel` / `ChartTheme`, superseded by
`:core:monitoring`), `com.gadget.ui.onboarding.OnboardingScreen`, the legacy
`com.gadget.export` CSV/PDF exporters, and the four safety-framework cards left
stranded by the seam dissolution (`RootedLegalNoticeCard` / `RootedFirstAckDialog`
/ `EmergencyResetCard` / `PackageNotes`). `com.gadget.root.ui` is now down to the
two live composables (`FatalLaunchScreen`, `RootedFeatureTogglesCard`). Canonical
metric:

```bash
find app/src -path "*com/gadget*" -name "*.kt" | wc -l
```

### Phase-2 tail (skeleton modules — all filled ✅)

**No skeleton modules remain** — `flipper` (+ `-rooted`) shipped the full
USB CDC-ACM + BLE GATT transport, the hand-rolled protobuf RPC stack, and
all four command suites (System / Storage / Sub-GHz / Infrared), closing the
tail. `radios-subghz` shipped USB SDR / Sub-GHz transceiver detection (the
SDR data path remains a rooted follow-up); `lock-rooted` shipped its
secure-keyguard overlay (migration of the legacy `LockScreenOverlayHelper`).

The `com.gadget.flipper` legacy sources have now been clean-cut deleted (no
non-legacy code referenced them). The app-level USB-attach launch hook
(`.MainActivity` intent-filter + `@xml/flipper_usb_filter`) is retained — it
is app glue, not feature code. Remaining Phase-2 cleanup is the same
per-feature clean-cut for the other migrated modules' legacy sources.

`:feature:radios-wifi-rooted` adds the automation seam (`wifi_root`
ActionHandler) for the privileged Wi-Fi controls, and the **legacy
`com.gadget.wifi` controller subsystem has now been clean-cut migrated
into the modules**:

- the contract + config/result types + the standard no-op
  (`WifiController`, `RfkillConfig`/`TxPowerConfig`/`ChannelConfig`,
  `WifiControllerResult`, `StandardWifiController`) → `:feature:radios-wifi`
  under `…radios.wifi.control`;
- the rooted controller + helpers (`RootedWifiController`,
  `WifiRfkillHelper`, `WifiSysfsHelper`, `WifiInjectionProbe`) →
  `:feature:radios-wifi-rooted` under `…radios.wifi.rooted.control`.

The `com.gadget.wifi.*` sources are deleted; the app-level consumers
(`RootedRadiosExtrasSections` UI, `RootFeaturesEntryPoint`, both
flavors' `RootBindings`) now import the modular packages. This is the
**first of the ~20 legacy feature controllers** in the
`RootFeaturesEntryPoint` cluster to migrate out under the #94 plan; the
entry point + the shared radios UI stay in `:app` (they still aggregate
the other 19 legacy controllers) but now source their Wi-Fi types from
the modules. The `wifi_root` ActionHandler keeps its own self-contained
`WifiRootCommands` (rfkill / `iw` shapes + 20 dBm ceiling + channel
allow-list), independent of the migrated controller.

## Completed phases

- **Phase 0 — Future-Proof Repo Structure & Foundation (✅ May 2026).**
  Modular monorepo with `core/`, `feature/`, `build-logic/`; convention
  plugins; new applicationIds for side-by-side install.
- **Phase 1 — Light Preview / Skeleton App (✅ May 2026).** The full
  component library, `LocalGadgetTheme`, accessibility locals, the
  `WindowSizeClass`-aware shell, `:core:testing`, and the preview matrix.
- **Phase 1.1 — Hardening sweep (✅).** Full `LocalGadgetTheme` wiring
  (#90), accessibility semantics, responsiveness, glass consistency,
  shimmer width, preview matrix expansion.

## Forward plan

> The full measured gap analysis (per-module definition-of-done matrix,
> legacy inventory, cross-cutting coverage) and the phased workstreams
> that take the app from here to **complete** — legacy extinction,
> skeleton-feature builds, consistency sweep, widgets/tiles everywhere,
> `:core:permissions`, root-flavor completion, high-end automation, full
> localization, design/assets, and release engineering — live in the
> **[Completion Master Plan](Completion-Master-Plan)**.

- **Phase 3 — Core God-App Capabilities (🚧 started).** Shipped: the custom
  theme picker (high-contrast / amoled-true / pastel; `GadgetCustomTheme` +
  `Settings → Appearance → Palette`) and the in-depth permission UI (the Health
  screen is now an actionable permission manager — runtime grant requests +
  App-Settings fallback) plus its rooted one-up (`:feature:bugreport-rooted`
  force-grants permissions via `pm grant`, gated by `RootSafetyGate`), the
  first widget-coverage gauges (battery + internal-storage status home-screen
  widgets on the `:core:widgetkit` content archetype), and a second Quick
  Settings tile (`StrobeTileService` joins `FlashlightTileService` —
  start/stop the flashlight strobe straight from the panel). In flight on
  feature branches: WiFi-signal + ambient-light gauge widgets, and
  theme-picker live preview swatches. Still open: remaining
  notification-panel / QS-tile coverage where a feature has a clean toggle.
- **Phase 4 — Polish, Testing, CI/CD & Release.** Per-feature
  instrumented tests on `:core:testing` fixtures, emulator CI (#92),
  performance benchmarks, release-candidate flow + Play metadata.

## Open follow-up issues

- **[#89](https://github.com/Ranzlappen/HardwareDash/issues/89)** —
  ✅ `material3-adaptive` foldable posture consumer landed.
  `ModuleScreenScaffold` now reads `rememberPosture()` and stacks the
  `secondaryPane` below the primary content on `GadgetPosture.Tabletop`
  (side-by-side on Flat/Book). `DashboardScreen` is the first consumer
  (feature shortcut pane on wide/foldable screens).
- **[#107](https://github.com/Ranzlappen/HardwareDash/issues/107)** —
  delete dead legacy `com.gadget.widget` torch providers; folded into the
  per-feature clean-cut deletion step.
- **[#95](https://github.com/Ranzlappen/HardwareDash/issues/95)** torch
  brightness slider (API 33+ `turnOnTorchWithStrengthLevel`) — **done**, in
  `claude/open-issues-xktnze`.
- **[#101](https://github.com/Ranzlappen/HardwareDash/issues/101)** floating
  torch overlay button — **done**, in `claude/open-issues-xktnze`.
- Tech-debt: **[#72](https://github.com/Ranzlappen/HardwareDash/issues/72)**
  / **[#68](https://github.com/Ranzlappen/HardwareDash/issues/68)** detekt
  cleanup — **done** (PR #164).

**Resolved & closed:** #89 (foldable posture consumer — `ModuleScreenScaffold`
Tabletop layout + `DashboardScreen` secondary pane), #90 (theme wiring),
#91 (`GadgetBottomSheet` instrumented tests, covered by `ModalsTest`),
#92 (emulator CI, `instrumented-tests.yml`), #94 (rooted torch extras +
`:core:root`).

## Release readiness

CI produces `standard-debug.apk`, `standard-release.apk` + `.aab`, and
`rooted-debug.apk` on every push. The standard-APK leak gate hard-fails
any PR that lets rooted code/assets/permissions into the standard APK.
`rooted-release.apk` (signed) ships once the rooted modules are feature
complete. See [Testing & CI](Testing-and-CI).

Every push to `main` also regenerates the **[App Preview Gallery](App-Preview-Gallery)**
— an approximate visual render of every screen (Roborazzi, JVM, no emulator),
published to GitHub Pages with a per-version archive and diffs. Entry point:
[`PREVIEW.md`](https://github.com/Ranzlappen/HardwareDash/blob/main/PREVIEW.md).

## Historical milestones

- **`claude/refactor-2026`** carried the Phase-0 → Phase-2 migration
  (Torch blueprint hardening, `:core:widgetkit` extraction, the automation
  engine epic) and is now retired in favour of per-batch branches onto
  `main`.
- **`legacy-main`** is the full archive of the old single-module
  codebase — read-only reference, never imported from new code.

---

> _Last reviewed: 2026-07-03 · Source: `MASTER-PLAN.md`,
> `docs/refactor-2026/*`, `README.md`, `.github/workflows/app-preview.yml`,
> [Completion Master Plan](Completion-Master-Plan) ·
> Related modules: all, `:screenshots`._
