# Feature Migration Guide

The repeatable playbook for bringing a feature from the archived
`legacy-main` branch into a `:feature:<name>` module. Torch was the first
worked example; Vibration validated it as a second consumer. Pair this
with the [Module Authoring Contract](Module-Authoring-Contract) (the
acceptance checklist) and the [Torch Blueprint](Torch-Blueprint) (the
advanced reference).

> **Don't overbuild.** Torch exercises *every* seam at once. Most
> migrations should start from the **minimal template** (below) and add
> seams only when the feature actually needs them.

## The eight-step recipe

### Step 1 — Survey the legacy implementation

Read-only exploration of `legacy-main`. **Do not check out the branch** —
use `git show legacy-main:<path>` and `git ls-tree -r legacy-main`.
Enumerate, for the feature:

1. Entry-point screen(s) (`app/src/main/java/com/gadget/ui/screens/<Feature>Screen.kt`).
2. Manager / controller / repository classes (`com/gadget/<feature>/`).
3. Standard- and rooted-flavor specialisations.
4. AndroidManifest entries — permissions, services, receivers, tile
   services, activities.
5. Resources — drawables, vector icons, appwidget-provider XML, layouts,
   string keys.
6. Foreground services — what type (camera / microphone / location /
   dataSync / specialUse / shortService)?
7. Persistence — DataStore / SharedPreferences / Room? Which keys?
8. Cross-feature dependencies — does it pull in `BackupManager`,
   `FlipperConnectionManager`, `RootCapabilityRegistry`,
   `RootSafetyGate`? Those land **first**, as separate batches.

**Output:** a short report (≤ 400 words) — file map, architecture
summary, permissions, resources, migration callouts.

### Step 2 — Decide what migrates now vs. deferred

Pick a **lean v1 cut** that lands a working end-to-end vertical slice,
avoids un-ported infrastructure, and leaves a clear "v2" list tracked as
issues. If the lean cut isn't obvious, write it in the batch plan and
review it before coding. (Torch v1: standard Camera2 toggle + 3 widget
surfaces. Deferred: rooted extras, brightness, strobe slider, SOS.)

### Step 3 — Wire the module's `build.gradle.kts`

Use `scripts/new-feature.sh <name> [--rooted]` — it auto-detects:

- **Base mode** (dir doesn't exist): creates the whole module + the
  standard/rooted sibling pair (with `--rooted`) and appends the
  `:feature:<name>` include to `settings.gradle.kts`.
- **Skeleton-fill mode** (Batch-0 skeleton dir exists): generates
  **sources only**, reads `namespace` from the existing build file (so
  hyphenated names like `radios-bt` work), never overwrites the build
  file, appends a `:core:ui` + `:core:navigation` deps block only if
  none is present, and refuses if sources already exist.

The skeleton already has `id("gadget.android.feature")` (brings Compose +
Hilt + lifecycle + material3). Add only the project deps the feature
actually uses — resist adding deps "in case" (each is a Hilt graph node):

```kotlin
dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    // only if used: :core:datastore, :core:hardware, :core:permissions, …
}
```

### Step 4 — Build the controller / repository layer first

Land the non-UI domain code **before** any Compose, so the UI has a
`StateFlow` to consume. The controller is the API contract — write the
data flow first.

```kotlin
interface TorchController {
    val state: StateFlow<TorchState>
    fun toggle(); fun setOn(on: Boolean)
}
@Singleton class StandardTorchController @Inject constructor(
    @ApplicationContext context: Context) : TorchController { … }
```

Decide `suspend` vs non-suspend by the **underlying call** — Torch's
setters wrap fast synchronous Camera2 binder calls (non-suspend); a
network/disk controller exposes `suspend`. Bind via a Hilt `@Binds`
module (use a top-level `object` for `@Provides`, an abstract class for
`@Binds` — see [Troubleshooting](Troubleshooting)).

### Step 5 — Build the UI using the design system

Compose every screen with `ModuleScreenScaffold` + a `ModuleInfo`, the
design-system components, and `LocalGadgetTheme.current` (never raw `dp`).
**Decompose** into a stateful Hilt-wrapped `<Feature>Screen` + a stateless
`<Feature>ScreenContent` so the inner content is testable without Hilt:

```kotlin
@Composable fun TorchScreen(modifier: Modifier = Modifier,
    viewModel: TorchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TorchScreenContent(state, viewModel::onToggleClick, modifier)
}
```

See [Design System](Design-System) for the full ruleset.

### Step 6 — Register in navigation

Add the route to `GadgetDestination` (`:core:navigation`), provide a
`NavGraphBuilder.<name>Screen()` extension in the feature module, and wire
it into `MainActivity`'s `GadgetApp { … }`. Add
`implementation(project(":feature:<name>"))` to `:app`. **Grep every
non-exhaustive `when (destination)`** after adding a `GadgetDestination`
value (see [Troubleshooting](Troubleshooting)). Only add to the nav rail
(`modules`) if the feature appears there; don't add an `onBack` callback
speculatively.

### Step 7 — Add the deep-link / surface entry points

Most features have several entry points (in-app screen, QS tile, home
widget). **Every entry point converges on the same `@Singleton`
controller** — the tile and a widget tap produce the same state visible
from the screen. Declare each surface in the feature module's manifest;
use `EntryPointAccessors.fromApplication(...)` for system-instantiated
components (TileService, Service, BroadcastReceiver). See [Widgets, Tiles
& Surfaces](Widgets-Tiles-and-Surfaces).

### Step 8 — Add tests using `:core:testing`

JVM tests (`src/test/`) for pure logic (serialization round-trips, maths,
in-memory structures) with `junit` / `mockk` / `turbine` /
`coroutines-test`. Instrumented tests (`src/androidTest/`) for the
stateless `<Feature>ScreenContent`, wrapped in `GadgetTestTheme { … }`.
See [Testing & CI](Testing-and-CI).

## Persistence patterns (`:core:datastore`)

- **App-wide singletons** (theme, accessibility, a default slider value) →
  add a field to `UserPreferences` + a key + a map line in `readFrom` + a
  `suspend fun set…`.
- **Per-feature collections** keyed by an int (AppWidget ids, sensor ids)
  → `FeaturePreferences<T>` (its own DataStore file backing a `Map<Int,
  T>`, JSON-encoded, with a `ReplaceFileCorruptionHandler`). Provide the
  typed instance from the feature's Hilt module.
- **Escalate to Room** only for real query expressiveness (multi-field
  filtering, foreign keys, indexed scans).

## Dynamic widget creation

See the full flow + reliability rules in [Widgets, Tiles &
Surfaces](Widgets-Tiles-and-Surfaces). The two non-negotiable halves:
build the pin success-callback `PendingIntent` with `FLAG_MUTABLE` + an
explicit `ComponentName`, **and** carry the config through
`PendingWidgetConfigs` + override `reconcilePendingConfig` with
`claimSolePending`. Skipping either ships a widget that pins blank +
inert.

## Two templates: minimal vs. advanced

**Minimal** (the floor every module clears):

- `Controller` interface + standard impl, `@Singleton`, Hilt `@Binds`.
- `@HiltViewModel` + stateless `<Feature>ScreenContent` + `<Feature>Screen`
  route.
- `ModuleScreenScaffold` + `ModuleInfo` + a tri-state
  `ModuleCapabilitiesSection`.
- Navigation entry + route in `GadgetApp`.
- Unit tests for serialization/repo + one instrumented test + the preview
  matrix.

**Advanced (Torch-style)** — add only as the feature demands, reusing the
core frameworks rather than hand-rolling:

- Widgets / QS tile / notifications → contribute a `WidgetKitConfig`,
  subclass `BaseGadgetWidgetProvider<T>` + `BaseWidgetPinSuccessReceiver<T>`,
  persist via `WidgetConfigStore<T>`.
- Monitoring → a `MetricSource` per signal + `MonitorContainer` /
  `LiveMonitorContainer`.
- Automation → an `ActionHandler` with `ModuleAction` metadata.
- Rooted extras → a feature-side capability interface + `-standard`/
  `-rooted` sibling modules, gated by `RootSafetyGate` + `RootFeatureKey`.

Rule of thumb: an actuator with widgets + monitoring + automation + a
rooted boost is advanced; a read-only sensor readout is minimal. Both
satisfy the same [Module Authoring Contract](Module-Authoring-Contract).

## Done checklist

- [ ] Survey report written + reviewed.
- [ ] Lean v1 cut documented; deferred items tracked as issues.
- [ ] `build.gradle.kts` has only the project deps actually used.
- [ ] Controller/repository compiles + has a Hilt provider.
- [ ] Screen compiles using only design-system components +
      `LocalGadgetTheme.current`.
- [ ] Route registered; nav-graph extension provided; `GadgetApp` wired;
      `:app` dep added.
- [ ] All entry points converge on the same `@Singleton` controller.
- [ ] AndroidManifest entries in the feature module.
- [ ] CI green for **both** standard and rooted flavors.
- [ ] Manual smoke confirms the v1 slice works end-to-end.
- [ ] [Roadmap & Status](Roadmap-and-Status) + relevant catalogs updated.
- [ ] [Component Catalog](Component-Catalog) updated if a new public
      composable was added.

## Anti-patterns to avoid

The mistakes legacy code makes that new code MUST NOT inherit: god-object
screens; direct `SharedPreferences` reads in composables; hard-coded `dp`;
raw `Surface(color=…)` for glass; branching on `BuildConfig.IS_ROOTED`;
foreground services without a typed `foregroundServiceType`; imports from
`com.gadget.**`; `@Composable` accessor reads inside non-composable
callbacks. Full list with rationale: [Design System →
Anti-patterns](Design-System).

---

> _Last reviewed: 2026-06-12 · Source: `docs/migration-guide.md` · Related
> modules: `:feature:torch`, `:feature:vibration`, `:core:datastore`,
> `:core:widgetkit`._
