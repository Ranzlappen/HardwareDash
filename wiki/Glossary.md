# Glossary

Shared vocabulary for HardwareDash / Gadget.

| Term | Meaning |
|---|---|
| **Gadget** | The app's internal name (and Gradle `rootProject.name`). The product name is HardwareDash. |
| **HardwareDash** | The product / repository name. Same app as Gadget. |
| **standard flavor** | The Play-store-safe, non-rooted build. `applicationId dev.ranzlappen.gadget`. Cannot compile against any `*-rooted` module. |
| **rooted flavor** | The side-loaded build with root-only capabilities. `applicationId dev.ranzlappen.gadget.rooted`. Installs side-by-side with standard. |
| **feature module** | `feature/<name>/` — one user-facing capability per Gradle module. Depends on `core/*` only, never another feature. |
| **core module** | `core/<name>/` — reusable infrastructure consumed by features; never consumes a feature. |
| **sibling rooted/standard module** | `feature/<name>-rooted` / `feature/<name>-standard` — per-flavor binding modules pulled in via `rootedImplementation` / `standardImplementation`. |
| **build-logic** | The composite build hosting the Gradle convention plugins every module applies by id. |
| **legacy-main** | The archived single-module codebase branch — read-only reference, never imported from new code. |
| **clean-cut migration** | Migrating a feature into a module and **deleting** its legacy `com.gadget.*` path once verified — no stale snippets on active branches. |
| **`LocalGadgetTheme`** | The umbrella Compose `CompositionLocal` exposing every design token (colors / typography / shapes / spacing / motion / glass). |
| **glass surface** | A glassy (glassmorphism) container; always via `GlassSurface` / `Modifier.glassSurface()`, never a raw `Surface(color=…)`. |
| **`ModuleScreenScaffold`** | The standard screen frame; renders `ModuleInfo` (permissions / OS / firmware / capabilities) sections automatically. |
| **`ModuleInfo` / `ModuleCapability`** | The self-describing metadata a feature passes to the scaffold; capabilities are per-function tri-state (green/amber/red) live checks. |
| **`GadgetStatusKind`** | The tri-state enum (Success/Warning/Error) + `@Composable color()` — the only sanctioned green/amber/red mapping. |
| **`GadgetDestination`** | The typed navigation destination contract in `:core:navigation`. |
| **widgetkit** | `:core:widgetkit` — the reusable home-screen-widget framework (config store, pin flow, base providers, RemoteViews rendering, boot re-arm). |
| **`WidgetKitConfig`** | A widget's per-instance persisted config base. |
| **function-driven widget** | A widget whose tap dispatches a `WidgetFunction` (Toggle/Momentary) through `ModuleActionRegistry`. `BaseGadgetWidgetProvider`. |
| **content/launcher widget** | A widget that renders dynamic content and launches an Activity on tap. `BaseContentWidgetProvider`; folder is the reference. |
| **`claimSolePending`** | The pin-rescue seam: pops the sole unclaimed pending config when an OEM launcher never fires the pin callback. |
| **soft-delete (remove-but-keep-inert)** | In-app widget delete sets `removed=true` (a non-host app can't pull a placed widget); the widget repaints inert until dragged off. |
| **monitoring** | `:core:monitoring` — the chart + persist framework features embed to chart a signal. |
| **`MetricSource`** | The readable-signal seam (`:core:model`): `descriptor` + `sample()` (poll) + optional `stream()` (push). Feeds monitoring **and** automation. |
| **push vs. poll** | A `MetricSource` either emits on change (`stream()`, zero idle wakeups) or is sampled every interval (`sample()`). |
| **`MonitorContainer` / `LiveMonitorContainer`** | The persisted-history vs. live-in-memory monitoring cards (independent; embed both). |
| **automation engine** | The `when <trigger> [if <conditions>] then <actions>` rule engine across `:core:automation` + `:core:hardware` + `:feature:automation-ui`. |
| **`ActionHandler` / `ModuleActionRegistry`** | The action (write) seam: a feature's invocable actions by `featureId`/`actionKey`; consumed by widgets + automation. |
| **`HardwareRegistry`** | The read-side enumeration over the `MetricSource` map (`:core:hardware`) — lets the rule builder list available signals without importing a feature. |
| **trigger vs. condition** | A trigger is the edge that *wakes* a rule; a condition is a state gate *re-checked* when it fires. |
| **hysteresis (`clearValue`)** | A threshold trigger re-arms only after the metric crosses back past `clearValue`, so noise can't machine-gun a rule. |
| **root safety gate** | `RootSafetyGate` (`:core:root`) — capability + opt-out + rate-limit check every privileged call passes before running. |
| **`RootFeatureKey` / `RootFeatureDescriptor`** | The identity + policy a rooted feature registers with the gate. |
| **source-set symmetry** | The (now-relaxed) rule that per-flavor impls shared FQNs; modular flavor modules now use distinct FQNs with flavor-scoped Hilt bindings. |
| **leak gate** | The CI step that hard-fails if the standard APK contains su strings, rooted assets, or root-tier permissions. |
| **`specialUse` FGS** | A foreground service type used by `MonitorService` and `AutomationService`; self-stops when idle; needs a Play-console justification. |
| **`shortService`** | A 3-min-capped FGS type used for brief user-initiated tasks (the strobe service) — no camera-typed FGS permission needed. |
| **RemoteViews** | The widget rendering model — only `@RemoteView` view classes inflate; no bare `<View>`. |
| **backup format v5** | The whole-app ZIP format (DBs + DataStore + SharedPrefs + asset sweeps); add new modular DBs to `DatabaseCheckpointer` and bump the version. |
| **Torch blueprint** | `:feature:torch` — the canonical advanced feature exercising every seam; the hardened reference, not to be copied wholesale into simple features. |

---

> _Last reviewed: 2026-06-12 · Source: `CLAUDE.md`, all wiki pages ·
> Related: [Home](Home)._
