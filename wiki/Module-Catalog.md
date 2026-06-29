# Module Catalog

The "parts catalog" for the codebase — a human-readable index of every
Gradle module. Source counts are `*.kt` under `src/main` as of 2026-06;
treat them as a maturity signal, not a contract. Modules with **0**
sources are wired skeletons awaiting their migration batch.

Module graph and dependency rules: [Architecture](Architecture).

## `:app`

- **Purpose:** the single application module. Hosts `GadgetApplication`
  (`@HiltAndroidApp`), `MainActivity` + `GadgetApp { … }` nav wiring,
  flavor/applicationId/signing config, the flavor `RootBindings`, and the
  not-yet-migrated legacy `com.gadget.*` surface (~284 files).
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
| `core:common` | 0 | Pure-Kotlin utilities (Result types, dispatchers, time, log tags). | — |
| `core:model` | 1 | **`MetricSource` + `MetricDescriptor`** — the readable-signal seam. No Android. | `kotlinx-coroutines-core` |
| `core:domain` | 0 | Use-cases / policy, no Android APIs. | `core:model` |
| `core:data` | 22 | Repositories; modular Room DBs (`apps.db`, `monitoring.db`, `automation.db`); `MonitorSampleRepository`, `RoomRuleRepository`, `DatabaseCheckpointer`. | `core:model`, `core:automation`, Room |
| `core:datastore` | 6 | `UserPreferences` + `FeaturePreferences<T>` factory (per-feature collections). | DataStore |
| `core:designsystem` | 9 | Theme, colour/typography/shape/spacing/motion/glass tokens, `LocalGadgetTheme`, `GadgetTheme`. | Compose |
| `core:ui` | 35 | The component library (`GadgetPrimaryButton`, `DashCard`, `GlassSurface`, `ModuleScreenScaffold`, `ModuleInfo` sections, …). | `core:designsystem` |
| `core:navigation` | 4 | `GadgetApp` shell + `GadgetDestination` contracts. | `core:ui` |
| `core:permissions` | 0 | Permission state objects + resume advancers. | — |
| `core:surfaces` | 0 | Widget / QS-tile / Wear surface registry. | — |
| `core:notifications` | 2 | Shared notification channels / helpers. | — |
| `core:automation` | 30 | **`ActionHandler` + `ModuleActionRegistry`** contract; `Rule` model + `RuleEvaluator` + `RuleRepository` contract; `AutomationService`/`AutomationScheduler`/receivers. | `core:model`, `core:root` |
| `core:hardware` | 2 | **`HardwareRegistry`** — read-side enumeration over the `MetricSource` map. | `core:model` |
| `core:monitoring` | 18 | Monitor containers, charts, `MonitorService`, `CollapseStateRepository`, bitmap renderer. | `core:data`, `core:ui`, `core:widgetkit` |
| `core:widgetkit` | 35 | Widget framework: `WidgetKitConfig`, `WidgetConfigStore<T>`, `PendingWidgetConfigs<T>`, `BaseGadgetWidgetProvider<T>`, `BaseContentWidgetProvider<T>`, `WidgetAppearanceRenderer`, boot re-arm. | `core:ui` |
| `core:root` | 25 | Root-safety seam: `RootCapabilityRegistry`, `RootSafetyGate`, `RootFeatureKey`, `RootSafetyPreferences`, `RootSoftLimiter`. | — |
| `core:testing` | 2 | Hilt-aware test helpers, fakes, `GadgetTestTheme`. | Compose-test |

Deep-dives: [Design System](Design-System) ·
[Component Catalog](Component-Catalog) ·
[Monitoring Framework](Monitoring-Framework) ·
[Widgets, Tiles & Surfaces](Widgets-Tiles-and-Surfaces) ·
[Automation Engine](Automation-Engine).

## `feature/*`

Migration status legend: ✅ migrated & live · 🟡 partial · ⬜ skeleton
(no sources yet).

| Module | Src | Status | Notes |
|---|---:|:--:|---|
| `feature:dashboard` | 2 | ✅ | Adaptive grid home screen. |
| `feature:settings` | 6 | ✅ | About / Appearance / Accessibility + `backupSection` + `rootFeatureToggles` slots. |
| `feature:torch` | 44 | ✅ | Advanced blueprint: hardware control + QS tile + 2 widgets + strobe FGS + monitoring + automation. |
| `feature:torch-rooted` | 7 | ✅ | DutyCycle / MultiLed / Thermal via `RootedTorchController`. |
| `feature:torch-standard` | 3 | ✅ | No-op root twin. |
| `feature:vibration` | 43 | ✅ | Second blueprint consumer; modelled poll signal + draw-canvas pattern builder. |
| `feature:vibration-rooted` | 5 | ✅ | 4-capability rooted tier. |
| `feature:vibration-standard` | 2 | ✅ | No-op root twin. |
| `feature:apps` | 34 | ✅ | App-Organizer (folders + folder widgets + canvas background renderer); content-widget archetype. |
| `feature:apps-rooted` | 0 | ⬜ | Rooted app surface pending. |
| `feature:sensors` | 6 | ✅ | Proximity / light / acceleration `MetricSource`s + rooted sensor capability rows. |
| `feature:automation-ui` | 6 | ✅ | Rules list + `RuleEditorSheet` builder. |
| `feature:actuators` | 0 | ⬜ | Coming-soon placeholder in the rail. |
| `feature:battery` | 9 | ✅ | Level / charging / temperature / voltage / health; dual live+history monitors; 3 rooted rows (FuelGaugeRaw, CellMonitor, ChargingProfile). |
| `feature:audio` | 9 | ✅ | dB meter + WAV voice recording; live dB monitor; 3 rooted rows (MicGainBoost, MicDirectPcm, MicCustomSampleRate). |
| `feature:camera` | 8 | ✅ | CameraX + MLKit barcode scanner (all formats), scan history, WiFi/URL deep-open; 3 rooted rows (HighFps, ManualOverride, HalBypass). |
| `feature:gps` | 11 | ✅ | OSMDroid map + coordinates card; live speed + altitude monitors; 3 rooted rows (NmeaRawTap, ConstellationDump, LocationOverride). |
| `feature:motion` | 9 | ✅ | Gyroscope / step counter / motion detect; live + history monitors per sensor; 3 rooted rows (HighPolling, RawUnfiltered, SysfsRead). |
| `feature:ambient` | 0 | ⬜ | |
| `feature:radios-wifi` | 0 | ⬜ | |
| `feature:radios-bt` | 11 | ✅ | Adapter status + bonded device list; GATT battery + RSSI (standard); hidden battery API + A2DP codec name (rooted via `BtEnhancedInfoProvider` seam); live + history BT-enabled monitors. |
| `feature:radios-nfc` | 10 | ✅ | NDEF tag read + HCE emulation + NDEF template library; live + history NFC-enabled monitor; rooted raw-NCI row. |
| `feature:radios-subghz` | 9 | ✅ | USB SDR / Sub-GHz transceiver detection (RTL-SDR, HackRF, YARD Stick One, …) via `UsbManager`; `subghz_bridge_connected` push metric (live + history monitors); `subghz` ActionHandler (bridge-attached + Sub-GHz-capable asserts); 3 rooted rows (RawRegisters, CustomTuning, OokFskCapture). Detection-only on standard (Android has no Sub-GHz radio API). |
| `feature:radios-ir` | 10 | ✅ | NEC / Pronto / RAW transmit; saved-signal library; remote-brand library; 2 rooted rows (CustomCarrier, RawGpioPattern). |
| `feature:flipper` | 21 | ✅ | Flipper Zero bridge: USB CDC-ACM + BLE GATT transport, hand-rolled protobuf RPC (framing + PB_Main), System/Storage/Sub-GHz/Infrared command suites, connection manager. `flipper_connected` + `flipper_battery` monitors; `flipper` ActionHandler (assert-connected / ping / transmit .sub / transmit .ir). Migrated from legacy `com.gadget.flipper`. |
| `feature:flipper-rooted` | 3 | ✅ | Root-grants USB access by relaxing the Flipper's `/dev/bus/usb` device-node permissions (`chmod 666`) so the port opens without the per-attach dialog. Gated by `RootFeatureKey.FlipperUsbGrant`; `flipper_root` ActionHandler. |
| `feature:storage` | 9 | ✅ | Volume cards (internal + removable) with progress bars; live used-% monitor; 3 rooted rows (DumpDiskstats, EnumerateMounts, Fstrim). |
| `feature:storage-rooted` | 0 | ⬜ | |
| `feature:lock` | 9 | ✅ | Keyguard lock/secure state + biometric enrollment; lock-state live + history monitors; assert-locked / -unlocked / -secure automation actions; informational rooted overlay row. |
| `feature:lock-rooted` | 4 | ✅ | Secure-keyguard `TYPE_APPLICATION_OVERLAY`: self-grants SYSTEM_ALERT_WINDOW via root appops, draws a bounded anti-phishing overlay above the lock screen, torn down in a `NonCancellable` finally. Gated by `RootFeatureKey.LockSecureOverlay`; `lock_root` ActionHandler. |
| `feature:diagnostics` (+ `-rooted`) | 0 | ⬜ | |
| `feature:bugreport` | 5 | ✅ | Permission manager: grant-state scan + per-permission runtime request + App-Settings fallback + granted/total summary (refreshes on resume); assert-permission automation action; informational rooted rows (ADB diagnostics, force-grant). |
| `feature:bugreport-rooted` | 3 | ✅ | Force-grants a declared runtime permission via `pm grant` (validated token, gated by `RootFeatureKey.PermissionForceGrant`); `bugreport_root` ActionHandler. |
| `feature:manual` | 0 | ⬜ | In-app manual / help. |
| `feature:youtubedownloader` | 17 | ✅ | YouTube video/audio downloader (yt-dlp + ffmpeg via youtubedl-android); private playlists via in-app cookie login; dataSync FGS; MediaStore export to Movies/Music; `download_progress` monitor + `youtube_downloader` ActionHandler. Standard-only (runs unprivileged). |

## `benchmark`

- **Purpose:** macrobenchmark host (recomposition counts, frame timing).
- **Maturity:** skeleton; wired up properly in Phase 4.

## `lsposed-module`

- **Purpose:** bundled Xposed module for the rooted flavor.
- **Maturity:** included only when `-PenableLsposedModule=true`. Standard
  CI does not opt in; rooted CI does.
- **Related:** [Flavors & Root Safety](Flavors-and-Root-Safety).

---

> _Last reviewed: 2026-06-29 · Source: `settings.gradle.kts`, live
> `find … -name '*.kt'` counts · Related: every module._
