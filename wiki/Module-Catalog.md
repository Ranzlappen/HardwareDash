# Module Catalog

The "parts catalog" for the codebase — a human-readable index of every
Gradle module. Source counts are `*.kt` under `src/main` as of
2026-07-03; treat them as a maturity signal, not a contract. Modules
with **0** sources are wired skeletons awaiting their migration batch.

Module graph and dependency rules: [Architecture](Architecture).
Gap analysis and the road to done:
[Completion Master Plan](Completion-Master-Plan).

## `:app`

- **Purpose:** the single application module. Hosts `GadgetApplication`
  (`@HiltAndroidApp`), `MainActivity` + `GadgetApp { … }` nav wiring,
  flavor/applicationId/signing config, the flavor `RootBindings`, and the
  app shell (backup / localization / notifications / root launch UI).
  **The legacy `com.gadget.*` surface is fully migrated out** — `:app`'s
  `namespace` is now `dev.ranzlappen.gadget` (equal to the standard
  applicationId), and everything under `src/main` lives under
  `dev.ranzlappen.gadget.*`.
- **Maturity:** production; shrinking as features migrate out.
- **Dependencies:** every standard `feature/*`; rooted flavor adds
  `feature/*-rooted` via `rootedImplementation`.
- **Related:** [Flavors & Root Safety](Flavors-and-Root-Safety),
  [Roadmap & Status](Roadmap-and-Status).

## `build-logic/`

- **Purpose:** composite build hosting the convention plugins every
  module applies by id — `gadget.android.application[.compose]`,
  `gadget.android.library[.compose]`, `gadget.android.feature`,
  `gadget.android.hilt`, `gadget.android.room`, `gadget.jvm.library`.
- **Maturity:** production. Single source of build config — no per-module
  drift.
- **Related:** [Testing & CI](Testing-and-CI).

## `core/*`

| Module | Src | Purpose / public contracts | Key dependencies |
|---|---:|---|---|
| `core:common` | 0 | Pure-Kotlin utilities (Result types, dispatchers, time, log tags). Empty — candidate for removal from the graph until needed ([Completion Master Plan](Completion-Master-Plan)). | — |
| `core:model` | 1 | **`MetricSource` + `MetricDescriptor`** — the readable-signal seam. No Android. | `kotlinx-coroutines-core` |
| `core:domain` | 0 | Use-cases / policy, no Android APIs. Empty — candidate for removal from the graph until needed. | `core:model` |
| `core:data` | 20 | Repositories; modular Room DBs (`apps.db`, `monitoring.db`, `automation.db`); `MonitorSampleRepository`, `RoomRuleRepository`, `DatabaseCheckpointer`. | `core:model`, `core:automation`, Room |
| `core:datastore` | 5 | `UserPreferences` + `FeaturePreferences<T>` factory (per-feature collections). | DataStore |
| `core:designsystem` | 9 | Theme, colour/typography/shape/spacing/motion/glass tokens, `LocalGadgetTheme`, `GadgetTheme`. | Compose |
| `core:ui` | 23 | The component library (`GadgetPrimaryButton`, `DashCard`, `GlassSurface`, `ModuleScreenScaffold`, `ModuleInfo` sections, …). | `core:designsystem` |
| `core:navigation` | 4 | `GadgetApp` shell + `GadgetDestination` contracts. | `core:ui` |
| `core:permissions` | 0 | Permission state objects + resume advancers. Empty — to be built for real as the centralized permission framework (W5 of the [Completion Master Plan](Completion-Master-Plan)); runtime-permission logic currently lives ad-hoc inside features. | — |
| `core:surfaces` | 0 | Widget / QS-tile / Wear surface registry. Empty and unreferenced — candidate for removal from the graph until needed. | — |
| `core:notifications` | 2 | Shared notification channels / helpers. | — |
| `core:automation` | 22 | **`ActionHandler` + `ModuleActionRegistry`** contract; `Rule` model + `RuleEvaluator` + `RuleRepository` contract; `AutomationService`/`AutomationScheduler`/receivers. | `core:model`, `core:root` |
| `core:hardware` | 2 | **`HardwareRegistry`** — read-side enumeration over the `MetricSource` map. | `core:model` |
| `core:monitoring` | 19 | Monitor containers, charts, `MonitorService`, `CollapseStateRepository`, bitmap renderer. | `core:data`, `core:ui`, `core:widgetkit` |
| `core:widgetkit` | 33 | Widget framework: `WidgetKitConfig`, `WidgetConfigStore<T>`, `PendingWidgetConfigs<T>`, `BaseGadgetWidgetProvider<T>`, `BaseContentWidgetProvider<T>`, `WidgetAppearanceRenderer`, boot re-arm. | `core:ui` |
| `core:root` | 26 | Root-safety seam: `RootCapabilityRegistry`, `RootSafetyGate`, `RootFeatureKey`, `RootSafetyPreferences`, `RootSoftLimiter`, shared `AlsaMixerControl`. | — |
| `core:testing` | 2 | Hilt-aware test helpers, fakes, `GadgetTestTheme`. | Compose-test |

Deep-dives: [Design System](Design-System) ·
[Component Catalog](Component-Catalog) ·
[Monitoring Framework](Monitoring-Framework) ·
[Widgets, Tiles & Surfaces](Widgets-Tiles-and-Surfaces) ·
[Automation Engine](Automation-Engine).

## `feature/*`

Migration status legend: ✅ migrated & live · 🟡 partial (controllers
migrated, **no screen / nav route / monitoring / automation wiring
yet**) · ⬜ skeleton (no sources yet).

Per-module definition-of-done matrix (MetricSource / ActionHandler /
widgets / tiles / tests / strings): see the
[Completion Master Plan](Completion-Master-Plan).

| Module | Src | Status | Notes |
|---|---:|:--:|---|
| `feature:dashboard` | 2 | ✅ | Adaptive grid home screen (thin; no user reorder/hide yet). |
| `feature:settings` | 7 | ✅ | About / Appearance / Accessibility / Monitoring + `backupSection` + `rootFeatureToggles` slots. No language picker yet. |
| `feature:torch` | 46 | ✅ | Advanced blueprint: hardware control + 2 QS tiles + 4 widgets + strobe FGS + monitoring + automation. |
| `feature:torch-rooted` | 7 | ✅ | DutyCycle / MultiLed / Thermal via `RootedTorchController`. |
| `feature:torch-standard` | 3 | ✅ | No-op root twin. |
| `feature:vibration` | 43 | ✅ | Second blueprint consumer; modelled poll signal + draw-canvas pattern builder + 4 widgets. |
| `feature:vibration-rooted` | 5 | ✅ | 4-capability rooted tier. |
| `feature:vibration-standard` | 2 | ✅ | No-op root twin. |
| `feature:apps` | 38 | ✅ | App-Organizer (folders + folder widgets + canvas background renderer); content-widget archetype; `apps_folder_count` `MetricSource` + `AppsActionHandler` (`refresh_apps`/`open_folder`/`launch_app`). |
| `feature:apps-rooted` | 0 | ⬜ | Rooted app surface pending. |
| `feature:sensors` | 6 | ✅ | Proximity / light / acceleration `MetricSource`s + rooted sensor capability rows; proximity-near/far / light-bright/dark / acceleration-above/below assert actions (no controller to drive — mirrors `feature:ambient`'s assert pattern, with an extra `stream() == null` presence guard so an absent sensor fails rather than trusting its zero absent-value). |
| `feature:automation-ui` | 6 | ✅ | Rules list + `RuleEditorSheet` builder. |
| `feature:automation` (+ `-rooted` 4) | 4 | 🟡 | Controller-only — no screen; role vs `automation-ui` to be resolved (fold into `:core:automation` or become the engine-status surface). |
| `feature:actuators` | 6 | ✅ | Vibrator availability + amplitude; haptic-click / heavy-click / assert-available actions; rooted extreme/PWM/dual/rumble rows; `vibrator_available` `MetricSource` (static `Vibrator.hasVibrator()` presence poll — haptic pulses are fire-and-forget with no "currently vibrating at X" state to model). |
| `feature:battery` (+ `-rooted` 9) | 17 | ✅ | Level / charging / temperature / voltage / health; dual live+history monitors; battery widget; rooted fuel-gauge / cell-monitor / charging-profile rows; `battery` ActionHandler (charging-profile override / thermal bypass / charging-type override / full dump / reset overrides / hold-SoC / wireless coil-current cap / health snapshot — all rooted-only, since baseline telemetry is read-only). |
| `feature:audio` (+ `-rooted` 5) | 13 | ✅ | dB meter + WAV voice recording; live dB monitor; audio ActionHandler; rooted mic-gain / direct-PCM / custom-sample-rate rows. |
| `feature:microphone` (+ `-rooted` 5) | 4 | 🟡 | Controller-only — the dB/recording UI lives in `feature:audio`; own screen pending. |
| `feature:camera` (+ `-rooted` 6) | 13 | ✅ | CameraX + MLKit barcode scanner (all formats), scan history, WiFi/URL deep-open; rooted HighFps / ManualOverride / RawCapture / MultiCamera / HalBypass / shutter-sound rows via `CameraController`; `camera` ActionHandler (`clear_scan_history` plus the six rooted `CameraController` extreme-tier ops, each `requiresRoot = true`). Discrete-event module (MetricSource-exempt). |
| `feature:gps` (+ `-rooted` 5) | 29 | ✅ | OSMDroid map + coordinates; live speed + altitude monitors; GPS-spoofing subsystem (GPX/KML playback, `LocationSpoofService`); rooted NMEA / constellation / override rows; `GpsActionHandler` (track start/stop, static spoof start/stop, rooted NMEA-tap / constellation-dump / reset-mutations). |
| `feature:motion` | 7 | ✅ | Gyroscope / step counter / motion detect; live + history monitors per sensor; assert-motion-detected / assert-idle / assert-steps-above / assert-rotation-above actions (no controller to drive — mirrors `feature:ambient`'s assert pattern); rooted rows. |
| `feature:ambient` | 8 | ✅ | Live lux + level descriptor; ambient-light history monitor; assert-bright / assert-dark actions; rooted brightness / refresh-rate / density rows. |
| `feature:display` (+ `-rooted` 5) | 4 | 🟡 | Controller-only — no screen / monitoring / automation yet. |
| `feature:keepalive` (+ `-rooted` 3) | 5 | ✅ | Persistent keep-alive: contract + standard controller + the shared `PersistentKeepAliveService` (both flavors); rooted Doze-whitelist + `pm grant` via `RootedKeepAliveController`, gated by `RootSafetyGate`. Migrated from the legacy `com.gadget.keepalive` / `com.gadget.services`. Surfaced from Settings (no dedicated screen). |
| `feature:notification` (+ `-rooted` 4) | 4 | 🟡 | Controller-only — builder/channel-inspector screen pending (legacy `BuilderPresetStore` still in `:app`). |
| `feature:adbdebug` (+ `-rooted` 5) | 4 | 🟡 | Controller-only — no screen / monitoring / automation yet. |
| `feature:usbdebug` (+ `-rooted` 5) | 4 | 🟡 | Controller-only — no screen / monitoring / automation yet. |
| `feature:radios-wifi` (+ `-rooted` 6) | 13 | ✅ | Adapter status + network details; live signal + enabled history monitors; enabled + connected actions; rooted `wifi_root` ActionHandler (rfkill / TX-power / channel / monitor-probe). |
| `feature:radios-bt` (+ `-rooted` 3) | 15 | ✅ | Adapter status + bonded device list; GATT battery + RSSI (standard); hidden battery API + A2DP codec name (rooted via `BtEnhancedInfoProvider` seam); live + history BT-enabled monitors. |
| `feature:radios-nfc` (+ `-rooted` 2) | 16 | ✅ | NDEF tag read + HCE emulation + NDEF template library; live + history NFC-enabled monitor; rooted raw-NCI row. |
| `feature:radios-cell` (+ `-rooted` 2) | 3 | 🟡 | Screenless so far — rooted read-only `CellController` (Qualcomm modem dump, per-band RSRP/RSRQ/SINR). Standard `TelephonyManager` screen + `cell_signal` MetricSource pending. |
| `feature:radios-subghz` | 9 | ✅ | USB SDR / Sub-GHz transceiver detection (RTL-SDR, HackRF, YARD Stick One, …); `subghz_bridge_connected` push metric (live + history monitors); `subghz` ActionHandler; 3 rooted rows. Detection-only on standard (Android has no Sub-GHz radio API); SDR data path is a rooted follow-up. |
| `feature:radios-ir` (+ `-rooted` 3) | 16 | ✅ | NEC / Pronto / RAW transmit; saved-signal library; remote-brand library; ir ActionHandler; 2 rooted rows (CustomCarrier, RawGpioPattern). Discrete-event module (MetricSource-exempt). |
| `feature:flipper` | 21 | ✅ | Flipper Zero bridge: USB CDC-ACM + BLE GATT transport, hand-rolled protobuf RPC (framing + PB_Main), System/Storage/Sub-GHz/Infrared command suites. `flipper_connected` + `flipper_battery` monitors; `flipper` ActionHandler. Migrated from legacy `com.gadget.flipper`. |
| `feature:flipper-rooted` | 3 | ✅ | Root-grants USB access by relaxing the Flipper's `/dev/bus/usb` device-node permissions (`chmod 666`). Gated by `RootFeatureKey.FlipperUsbGrant`; `flipper_root` ActionHandler. |
| `feature:storage` | 16 | ✅ | Volume cards (internal + removable) with progress bars; live used-% monitor; storage widget; 3 rooted rows. Standard-tier ActionHandler pending (rooted one exists). |
| `feature:storage-rooted` | 6 | ✅ | Diskstats / mounts / fstrim (allow-listed) / drop_caches actions via `StorageController`; `storage_root` ActionHandler. |
| `feature:lock` | 8 | ✅ | Keyguard lock/secure state + biometric enrollment; lock-state live + history monitors; assert-locked / -unlocked / -secure automation actions. |
| `feature:lock-rooted` | 4 | ✅ | Secure-keyguard `TYPE_APPLICATION_OVERLAY`: self-grants SYSTEM_ALERT_WINDOW via root appops, bounded anti-phishing overlay. Gated by `RootFeatureKey.LockSecureOverlay`; `lock_root` ActionHandler. |
| `feature:diagnostics` (+ `-rooted` 6) | 11 | ✅ | Standard `memory_used_percent` MetricSource (live + history + trigger); rooted logcat / meminfo / cpuinfo / procstats actions via `DiagnosticsController`. |
| `feature:bugreport` | 6 | ✅ | Permission manager: grant-state scan + per-permission runtime request + App-Settings fallback + granted/total summary (refreshes on resume); assert-permission automation action; `bugreport_permission_readiness` `MetricSource` (granted/total percent, the same state the summary chip already shows). |
| `feature:bugreport-rooted` | 3 | ✅ | Force-grants a declared runtime permission via `pm grant` (gated by `RootFeatureKey.PermissionForceGrant`); `bugreport_root` ActionHandler. |
| `feature:manual` | 2 | ✅ | In-app manual / help (thin static screen; per-module deep links pending). |
| `feature:youtubedownloader` | 17 | ✅ | YouTube video/audio downloader (yt-dlp + ffmpeg via youtubedl-android); private playlists via in-app cookie login; dataSync FGS; MediaStore export; `download_progress` monitor + `youtube_downloader` ActionHandler. Standard-only. |

## `benchmark`

- **Purpose:** macrobenchmark host (recomposition counts, frame timing).
- **Maturity:** skeleton; wired up properly in Phase 4.

## `lsposed-module`

- **Purpose:** bundled Xposed module for the rooted flavor.
- **Maturity:** included only when `-PenableLsposedModule=true`. Standard
  CI does not opt in; rooted CI does. Still packaged as
  `com.gadget.spoofer.xposed` — repackage tracked in the
  [Completion Master Plan](Completion-Master-Plan) (W1 endgame).
- **Related:** [Flavors & Root Safety](Flavors-and-Root-Safety).

---

> _Last reviewed: 2026-07-09 · Source: `settings.gradle.kts`, live
> `find … -name '*.kt'` counts · Related: every module._
