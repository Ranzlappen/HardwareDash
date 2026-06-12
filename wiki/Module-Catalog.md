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
  not-yet-migrated legacy `com.gadget.*` surface (~297 files).
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
| `feature:apps` | 34 | ✅ | App-Organizer (folders + folder widgets); content-widget archetype. |
| `feature:apps-rooted` | 0 | ⬜ | Rooted app surface pending. |
| `feature:sensors` | 6 | ✅ | Proximity / light / acceleration push `MetricSource`s. |
| `feature:automation-ui` | 6 | ✅ | Rules list + `RuleEditorSheet` builder. |
| `feature:actuators` | 0 | ⬜ | Coming-soon placeholder in the rail. |
| `feature:battery` | 0 | ⬜ | |
| `feature:audio` | 0 | ⬜ | `AudioManager` / `MediaRecorder`. |
| `feature:camera` | 0 | ⬜ | |
| `feature:gps` | 0 | ⬜ | `FusedLocationProvider`. |
| `feature:motion` | 0 | ⬜ | |
| `feature:ambient` | 0 | ⬜ | |
| `feature:radios-wifi` | 0 | ⬜ | |
| `feature:radios-bt` | 0 | ⬜ | |
| `feature:radios-nfc` | 0 | ⬜ | |
| `feature:radios-subghz` | 0 | ⬜ | |
| `feature:radios-ir` | 0 | ⬜ | `ConsumerIrManager`. |
| `feature:flipper` (+ `-rooted`) | 0 | ⬜ | Flipper Zero USB CDC-ACM + BLE GATT. |
| `feature:storage` (+ `-rooted`) | 0 | ⬜ | |
| `feature:lock` (+ `-rooted`) | 0 | ⬜ | |
| `feature:diagnostics` (+ `-rooted`) | 0 | ⬜ | |
| `feature:bugreport` (+ `-rooted`) | 0 | ⬜ | |
| `feature:manual` | 0 | ⬜ | In-app manual / help. |

## `benchmark`

- **Purpose:** macrobenchmark host (recomposition counts, frame timing).
- **Maturity:** skeleton; wired up properly in Phase 4.

## `lsposed-module`

- **Purpose:** bundled Xposed module for the rooted flavor.
- **Maturity:** included only when `-PenableLsposedModule=true`. Standard
  CI does not opt in; rooted CI does.
- **Related:** [Flavors & Root Safety](Flavors-and-Root-Safety).

---

> _Last reviewed: 2026-06-12 · Source: `settings.gradle.kts`, live
> `find … -name '*.kt'` counts · Related: every module._
