# Module Migration Guide

> **Audience**: any future contributor (human or Claude session)
> bringing a feature from `legacy-main` into the new modular
> structure on `claude/refactor-2026`.
>
> **Status**: first published as part of Phase 2 / Batch 1. Updated
> as the migration cadence reveals new gotchas.

This guide documents the **repeatable process** for migrating a
feature from the legacy single-module codebase into the new
modular Compose codebase. The Torch / Flashlight migration shipped
in Phase 2 / Batch 1 is the **first worked example** — every step
below references its outcome so future migrations can mirror the
shape.

---

## The eight-step recipe

### Step 1 — Survey the legacy implementation

Read-only exploration of `legacy-main`. **Do not check out the
branch** — use `git show legacy-main:<path>` and
`git ls-tree -r legacy-main` to enumerate and read files in place.

Things to enumerate for every feature:

1. **Entry point screen(s)** — typically under
   `app/src/main/java/com/gadget/ui/screens/<Feature>Screen.kt`.
2. **Manager / controller / repository classes** — usually under
   `app/src/main/java/com/gadget/<feature>/`.
3. **Standard- and rooted-flavor specialisations** — split between
   `app/src/standard/` and `app/src/rooted/` per the
   `BuildConfig.IS_ROOTED` carve-out (CLAUDE.md flavor rules).
4. **AndroidManifest entries** — permissions, services, receivers,
   tile services, activity declarations.
5. **Resources** — drawables, vector icons, XML
   appwidget-provider definitions, layouts, string keys.
6. **Foreground services** — what type (camera / microphone /
   location / dataSync / specialUse)?
7. **Persistence** — DataStore, SharedPreferences, Room? Which
   keys?
8. **Cross-feature dependencies** — does the feature pull in
   `BackupManager`, `FlipperConnectionManager`,
   `RootCapabilityRegistry`, `RootSafetyGate`? If yes, those
   dependencies need to land **first** (separate migration
   batches).

**Output**: a short report (≤ 400 words) — file map, architecture
summary, permissions, resources, migration callouts.

**Worked example (Torch / Phase 2 / Batch 1)**: see the Explore
agent's report captured in the plan file. Key findings:

- Standard path: `TorchScreen.kt` + `CameraManager.setTorchMode()`
  + `TorchCallback` to sync state.
- Rooted path: `RootedTorchController`, `DutyCycleStrobe`,
  `MultiLedOrchestrator`, `ThermalOverrideController` — all gated
  by `RootSafetyGate` and `RootFeatureKey`. **Deferred** because
  the root infrastructure isn't ported yet.
- Widgets: `FlashlightWidgetProvider` (1×1 toggle),
  `StrobeWidgetProvider` (1×1 + foreground `StrobeService`).
- Permissions: `CAMERA`, `WRITE_SETTINGS` (brightness),
  `FOREGROUND_SERVICE_CAMERA`.
- No tile service in legacy (we add one as part of the migration).

### Step 2 — Decide what migrates now vs. what's deferred

A feature's legacy footprint is rarely small. Pick a **lean v1
cut** that:

- Lands a working end-to-end vertical slice of the feature.
- Avoids dependencies on infrastructure that hasn't been ported
  yet (rooted gates, BackupManager, FlipperConnectionManager,
  Room schemas, etc.).
- Leaves behind a clear list of "v2" features tracked as
  follow-up issues.

If the lean v1 isn't obviously discoverable, write the lean cut
in the batch plan first and review it before coding.

**Worked example (Torch v1)**: standard-flavor Camera2 toggle +
three widget surfaces (tile + on/off home widget + strobe home
widget). Deferred: rooted extras, brightness control, strobe rate
slider, SOS pattern, multi-LED panic.

### Step 3 — Wire the module's `build.gradle.kts`

> **Shortcut:** `scripts/new-feature.sh <name> [--rooted]` scaffolds a
> feature module wired to the design system. It auto-detects two modes:
>
> - **Base mode** (the `feature/<name>/` dir doesn't exist): creates the
>   whole module — build file, manifest, `<Name>Screen` / `<Name>ViewModel`
>   / `<Name>Navigation`, the standard/rooted sibling pair with `--rooted` —
>   and appends the `:feature:<name>` include(s) to `settings.gradle.kts`.
> - **Skeleton-fill mode** (the Batch-0 skeleton dir exists with a
>   `build.gradle.kts` but no Kotlin sources — every planned migration
>   target): generates **sources only**, reading `namespace = "…"` from the
>   existing build file (so hyphenated names like `radios-bt` work), never
>   overwriting the build file, and appending a `:core:ui` + `:core:navigation`
>   `dependencies { }` block only if none is present. It refuses if the
>   module already has sources.
>
> Verified by first real use: `scripts/new-feature.sh sensors` generated the
> `:feature:sensors` stub whose **sources compile in CI unedited** once the
> module is wired per the declared manual steps — the shared-file edits the
> script can't safely make: adding/using the `GadgetDestination` entry
> (`Sensors` already existed, so its `placeholderScreen(...)` route was
> swapped for `sensorsScreen()`), adding `implementation(project(":feature:sensors"))`
> to `:app`, and (for `--rooted`) the `app/build.gradle.kts` flavor-impl
> lines. Start here, then resume the recipe at Step 4.

The skeleton already has `id("gadget.android.feature")` (which
brings Compose + Hilt + lifecycle + material3 + activity-compose
via the convention plugin chain). Add only the project-level
dependencies the feature needs:

```kotlin
dependencies {
    implementation(project(":core:ui"))         // design system
    implementation(project(":core:navigation")) // GadgetDestination
    // Add others ONLY if the feature uses them:
    // implementation(project(":core:datastore"))
    // implementation(project(":core:hardware"))
    // implementation(project(":core:permissions"))
}
```

Resist the temptation to add deps "in case". Every extra
dependency is a Hilt graph node that has to load before the
feature can be tested.

### Step 4 — Build the controller / repository layer first

Land the non-UI domain code **before** writing any Compose. This
gives you a `state: StateFlow<...>` and setters that the UI can
consume.

**Pattern** (extracted from Torch):

```kotlin
interface TorchController {
    val state: StateFlow<TorchState>
    fun toggle()
    fun setOn(on: Boolean)
}

@Immutable data class TorchState(
    val isOn: Boolean = false,
    val isAvailable: Boolean = false,
    val error: TorchError? = null,
)

@Singleton
class StandardTorchController @Inject constructor(
    @ApplicationContext context: Context,
) : TorchController {
    private val _state = MutableStateFlow(TorchState())
    override val state = _state.asStateFlow()
    // ... Camera2 implementation ...
}
```

Decide `suspend` vs. non-suspend based on the **underlying call**.
Torch's setters wrap synchronous Camera2 binder calls that finish
in microseconds, so they're non-suspend — the screen + tile + widget
+ service all hit them directly without coroutine ceremony. A
controller wrapping a long-running network or disk operation
should expose `suspend` setters and let consumers launch into their
own scopes.

Provide via a Hilt module:

```kotlin
@Module @InstallIn(SingletonComponent::class)
abstract class TorchModule {
    @Binds @Singleton
    abstract fun bindTorchController(impl: StandardTorchController): TorchController
}
```

**Why this order**: the controller is the API contract. If you
write UI first, the UI shape locks in implementation assumptions.
Write the data flow first, then drive UI from it.

### Step 5 — Build the UI using the design system

Compose every public-facing screen using:

- `ModuleScreenScaffold` (`:core:ui`) for the screen frame.
- `DashCard` / `CompactCard` / `GlassSurface` for content tiles.
- `GadgetPrimaryButton` / `GadgetSecondaryButton` /
  `GadgetTertiaryButton` for actions.
- `GadgetFab` for the single primary action (e.g. the big torch
  toggle).
- `GadgetTextField` / `GadgetSearchField` for inputs.
- `GadgetEmptyState` for "no data" placeholders.
- `LocalGadgetTheme.current.spacing.x` for spacing inside
  `@Composable` bodies (never raw `dp` literals).
- All text wrappers default to `singleLine = true` +
  `TextOverflow.Ellipsis` — opt-in to multi-line via flag.

Hoist state into a `@HiltViewModel`. For a controller with
non-suspend setters the click handlers are direct passthroughs —
no `viewModelScope.launch` needed unless the handler also needs
to write to a `suspend` repository:

```kotlin
@HiltViewModel
class TorchViewModel @Inject constructor(
    private val controller: TorchController,
) : ViewModel() {
    val state: StateFlow<TorchState> = controller.state
    fun onToggleClick() {
        controller.toggle()
    }
}
```

Decompose the screen into a stateful Hilt-wrapped entry point
(`TorchScreen`) plus a stateless inner composable
(`TorchScreenContent`). The split keeps the inner content
exercisable in instrumented tests without standing up Hilt or
the real controller:

```kotlin
@Composable
fun TorchScreen(
    modifier: Modifier = Modifier,
    viewModel: TorchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TorchScreenContent(
        state = state,
        onToggleClick = viewModel::onToggleClick,
        modifier = modifier,
    )
}

@Composable
fun TorchScreenContent(
    state: TorchScreenState,
    onToggleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ... pure declarative layout ...
}
```

### Step 6 — Register in navigation

Add the route to `GadgetDestination` (in `:core:navigation`):

```kotlin
data object Torch : GadgetDestination {
    override val route = "torch"
    override val label = "Torch"
    override val iconFilled = Icons.Filled.FlashlightOn
    override val iconOutlined = Icons.Outlined.FlashlightOn
}
```

**Do NOT add to `topLevel`** unless the feature appears in the
nav rail. Most features are sub-routes navigated to from the
dashboard or actuator list.

Provide a `NavGraphBuilder` extension in the feature module:

```kotlin
fun NavGraphBuilder.torchScreen() {
    composable(route = GadgetDestination.Torch.route) {
        TorchScreen()
    }
}
```

Wire from `MainActivity.setContent { GadgetApp { … } }`:

```kotlin
GadgetApp {
    dashboardScreen(onNavigate = navController::navigateTopLevel)
    torchScreen()
    // placeholderScreen(...) for the rest
}
```

If a screen does need a back-navigation callback (most don't —
the nav rail handles it for top-level destinations), add an
`onBack: () -> Unit` parameter and thread it through. Don't add
it speculatively.

Don't forget `implementation(project(":feature:torch"))` in the
**app** module's `build.gradle.kts`.

### Step 7 — Add the deep-link / surface entry points

Most features have multiple ways the user reaches them. For Torch:

- **In-app**: Dashboard tile → TorchScreen.
- **Quick Settings tile**: `FlashlightTileService` → toggles
  `TorchController` directly (no screen).
- **Home-screen widget**: `FlashlightWidgetProvider` → toggles
  `TorchController` directly.

Each entry point gets its own AndroidManifest declaration in the
feature module (`<service>`, `<receiver>`, `<activity>`). Use
Hilt `EntryPointAccessors.fromApplication(...)` for system-
instantiated components (TileService, Service,
BroadcastReceiver) since `@AndroidEntryPoint` injection isn't
available on every component.

**Critical**: every entry point converges on the **same**
`@Singleton TorchController`. The QS tile and a widget tap should
produce the same on/off state visible from the screen. Don't
duplicate state.

### Step 8 — Add tests using `:core:testing`

Once the feature compiles, add tests under both source sets:

JVM tests (`src/test/`) for pure logic — strobe-rate maths,
@Serializable round-trips, in-memory data structures.
Dependencies (already wired by the feature convention plugin):

```kotlin
testImplementation(libs.junit)
testImplementation(libs.mockk)
testImplementation(libs.turbine)
testImplementation(libs.kotlinx.coroutines.test)
```

Instrumented tests (`src/androidTest/`) for Compose surfaces.
Wrap content in `GadgetTestTheme { … }` from `:core:testing`:

```kotlin
androidTestImplementation(project(":core:testing"))
androidTestImplementation(libs.androidx.junit)
```

Decompose the screen into a stateful `<Feature>Screen` (Hilt-wrapped
entry point) plus a stateless `<Feature>ScreenContent` so the
inner composable is testable without Hilt or the real controller.
Inject a curated view-state into `<Feature>ScreenContent` and
assert that the rendered text + control state match.

Instrumented tests don't run on PR CI yet — see issue
[#92](https://github.com/Ranzlappen/HardwareDash/issues/92). The
tests are still worth checking in: they run locally with
`./gradlew :feature:<name>:connectedDebugAndroidTest` against a
local emulator and will run automatically once the CI workflow
ships.

---

## Persistence patterns

`:core:datastore` exposes two distinct surfaces. Pick the right
one for the data you're persisting.

### App-wide singletons → `UserPreferences` / `UserPreferencesRepository`

For settings that have **one** value across the whole app — theme,
accessibility toggles, default-strobe-rate slider — add a field to
[`UserPreferences`](../core/datastore/src/main/kotlin/dev/ranzlappen/gadget/core/datastore/UserPreferences.kt)
and a setter on
[`UserPreferencesRepository`](../core/datastore/src/main/kotlin/dev/ranzlappen/gadget/core/datastore/UserPreferencesRepository.kt).
This is a four-line change:

1. Add a field + default to `UserPreferences`.
2. Add a key constant to `UserPreferencesKeys`.
3. Map the new key in `readFrom`.
4. Add the corresponding `suspend fun set…(…)`.

### Per-feature collections → `FeaturePreferences<T>`

For features that need to persist a small **collection** of
structured records keyed by an integer (AppWidget IDs, sensor
IDs, etc.), use
[`FeaturePreferences<T>`](../core/datastore/src/main/kotlin/dev/ranzlappen/gadget/core/datastore/FeaturePreferences.kt).
Each feature gets its own Preferences DataStore file backing a
`Map<Int, T>` with JSON-encoded values.

The factory is Hilt-provided; consume it from your feature's own
Hilt module:

```kotlin
@Module @InstallIn(SingletonComponent::class)
object VibrationDataModule {
    @Provides @Singleton
    fun provideVibrationPatterns(factory: FeaturePreferencesFactory) =
        factory.create(
            fileName = "vibration_patterns",
            keyPrefix = "pattern_",
            serializer = VibrationPattern.serializer(),
        )
}
```

Then inject the typed `FeaturePreferences<VibrationPattern>` into
your repository class and wrap its `all` / `save` / `delete`
surface in a feature-typed API.

**Worked example (Torch widgets, Phase 2 / Batch 1.1)**:
[`TorchWidgetConfigRepository`](../feature/torch/src/main/kotlin/dev/ranzlappen/gadget/feature/torch/widget/TorchWidgetConfigRepository.kt)
wraps `FeaturePreferences<TorchWidgetConfig>` keyed by
`appWidgetId`. Save happens when the user pins a widget; delete
fires from `AppWidgetProvider.onDeleted`.

**When to escalate to Room**: only when you need real query
expressiveness (multi-field filtering, foreign keys, indexed
range scans). A small collection of structured records is
better served by `FeaturePreferences<T>` — simpler, no schema
migrations, and consistent across the codebase.

---

## Dynamic widget creation

Modern launchers support
[`AppWidgetManager.requestPinAppWidget`](https://developer.android.com/reference/android/appwidget/AppWidgetManager#requestPinAppWidget(android.content.ComponentName,%20android.os.Bundle,%20android.app.PendingIntent))
(API 26+) — apps can request the launcher to pin a widget directly
from inside an in-app flow. Useful when a widget needs per-instance
configuration (rate, theme, source identifier) that the user
specifies before the widget appears on the home screen.

The pin API returns the newly-assigned `appWidgetId` **after** the
user accepts the launcher's pin dialog, via a caller-supplied
success
[`PendingIntent`](https://developer.android.com/reference/android/app/PendingIntent).
The Torch feature's pattern (which any future feature can mirror):

1. **In-app UI** invokes a per-feature `<Feature>WidgetCreator`
   (singleton) from a `viewModelScope.launch { … }` (the creator's
   `requestPin` is **suspend** — DataStore writes can't run on the
   UI thread).
2. **Creator** persists the config to a `Pending<Feature>WidgetConfigs`
   bridge backed by `FeaturePreferences` (so it **survives process
   death** between pin-request and the success callback), receives
   back a stable token, and calls `requestPinAppWidget(...)` with a
   success `PendingIntent` carrying the token and an explicit
   `ComponentName` for the receiver (implicit intents fail silently on
   some OEM launchers). Per-kind count cap enforced via
   `WidgetPinPolicy` (`:core:widgetkit`); the call returns a
   `WidgetPinResult` (`Requested` / `LauncherUnsupported` / `CapReached`)
   so the UI can show the right message.
3. **User** accepts the launcher's pin dialog.
4. **OS** fires the success `PendingIntent`, which routes into a
   manifest-declared `BroadcastReceiver` (e.g.
   [`WidgetPinSuccessReceiver`](../feature/torch/src/main/kotlin/dev/ranzlappen/gadget/feature/torch/widget/WidgetPinSuccessReceiver.kt)).
5. **Receiver** claims the pending config by token from the DataStore
   bridge (`claim` removes it atomically), persists it to the
   feature's `FeaturePreferences`-backed repository keyed by the new
   `appWidgetId`, then broadcasts `ACTION_APPWIDGET_UPDATE` so the
   widget renders with its config immediately. Receiver async work runs
   on `WidgetReceiverScope` (`:core:widgetkit`) — one shared scope, not
   a new `CoroutineScope` per `onReceive`.

Reverse path: `AppWidgetProvider.onDeleted` is the canonical
"widget left the home screen" signal — purge the config there to
keep the repository tidy. **In-app delete** of an already-placed
widget can't pull the instance off a third-party launcher, so set
`removed = true` on the config instead of deleting it (the provider
self-heal would otherwise recreate it). The placed widget then
repaints inert; `onDeleted` purges the config for real when the
user drags it off.

For older launchers without pin support
(`isRequestPinAppWidgetSupported() == false`), surface a
fallback message ("long-press the home screen to add a widget").
Don't pop a chooser; users on those launchers know the manual
flow.

---

## Anti-patterns to avoid

These are mistakes legacy code makes that the new code MUST NOT
inherit:

1. **God-object screens**. Legacy `SettingsScreen.kt` is 1,000+
   lines in one composable. New screens decompose into
   `@Composable` cards that live in `feature/<name>/components/`.
2. **Direct `SharedPreferences` reads inside Composables**. Use
   `UserPreferencesRepository` from `:core:datastore` and inject
   via Hilt.
3. **Hard-coded `dp` values at call sites**. Use
   `LocalGadgetTheme.current.spacing.x`.
4. **Raw `Surface(color = ...)` for glassy effects**. Use
   `Modifier.glassSurface()` or `GlassSurface(...)`.
5. **Branching on `BuildConfig.IS_ROOTED`**. Inject a flavor-
   aware controller via Hilt and let the build pick the right
   implementation per flavor.
6. **Foreground services without typed `foregroundServiceType`**.
   Required on API 34+. Use `shortService` (3-min OS cap) for brief
   user-initiated tasks like `setTorchMode` — no camera-typed FGS
   permission needed. Wrap every `startForegroundService` in try/catch
   for `IllegalStateException` (the
   `ForegroundServiceStartNotAllowedException` supertype, API 31+) and
   degrade gracefully — never crash on a stray broadcast.
7. **Imports from `com.gadget.**` in new code**. Hard
   review-blocker.
8. **`@Composable` accessor reads inside non-composable callbacks**.
   Capture the resolved value once at the top of the composable
   and read its plain `val`s from callbacks.

---

## Checklist before declaring a migration done

- [ ] Survey report written and reviewed (Step 1).
- [ ] Lean v1 cut documented; deferred items tracked as issues
      (Step 2).
- [ ] Module `build.gradle.kts` has only the project deps the
      feature actually uses (Step 3).
- [ ] Controller / repository compiles + has a Hilt provider
      (Step 4).
- [ ] Screen compiles using only design-system components +
      `LocalGadgetTheme.current` (Step 5).
- [ ] Route registered in `GadgetDestination`; nav-graph
      extension provided; MainActivity wired (Step 6).
- [ ] All entry points (screen + tile + widget + …) converge on
      the same `@Singleton` controller (Step 7).
- [ ] AndroidManifest entries in the feature module — merger picks
      them into the app's final manifest.
- [ ] CI builds green for both `standard` and `rooted` flavors.
- [ ] Manual smoke on a device confirms the v1 vertical slice
      works end-to-end.
- [ ] `MASTER-PLAN.md` Phase-2 sub-track table updated.
- [ ] `CLAUDE.md` component catalog updated if the feature added
      any new public composable.
- [ ] Tests queued in a follow-up batch (Step 8) — not blocking
      the initial landing.

---

## Worked example: Torch / Flashlight (Phase 2 / Batch 1)

A complete walkthrough is preserved across the Phase 2 / Batch 1
and Batch 1.1 commits on `claude/refactor-2026`. Key artefacts to
study when doing the next migration:

| File | Why study it |
|---|---|
| `feature/torch/.../TorchController.kt` | Interface shape — `StateFlow<TorchState>` + non-suspend setters wrapping fast binder calls. |
| `feature/torch/.../StandardTorchController.kt` | Hilt-injected implementation with Camera2 + `TorchCallback`. |
| `feature/torch/.../TorchScreen.kt` | Thin Hilt-wrapped entry point — observes flows, owns the sheet visibility, delegates rendering to `TorchScreenContent`. |
| `feature/torch/.../TorchScreenContent.kt` | Stateless screen — toggle card, strobe defaults, monitor + live-monitor slots, widgets card, optional root-tools card. Each section wrapped in `GadgetExpandableCard` (persisted collapse via `:core:monitoring`'s `CollapseStateRepository`). Testable without Hilt. |
| `feature/torch/.../TorchViewModel.kt` | `combine(...)` over five flows into `TorchScreenState`, folded with rooted-availability + collapse-state. The live strobe-running signal comes from `StrobeRuntime` (singleton `StateFlow`) — no polling. |
| `feature/torch/.../TorchNavigation.kt` | `NavGraphBuilder.torchScreen()` registration — no `onBack` because Torch isn't a sub-route. |
| `feature/torch/.../tile/FlashlightTileService.kt` | `EntryPointAccessors.fromApplication(...)` pattern for non-Hilt components. |
| `feature/torch/.../widget/FlashlightWidgetProvider.kt` | `AppWidgetProvider` with config-aware `onUpdate`, **live** state-reflecting icon (reads `TorchController.state.value`), `onDeleted` purge, soft-delete (`removed = true`) handling, `saveIfAbsent` self-heal, `WidgetReceiverScope` for async work. |
| `feature/torch/.../widget/StrobeWidgetProvider.kt` | Same shape; passes only `EXTRA_APPWIDGET_ID` to the service, which reads that widget's persisted config itself (rate / Morse mode + text); `startForegroundService` wrapped in try/catch for `ForegroundServiceStartNotAllowedException`. |
| `feature/torch/.../widget/TorchWidgetCreator.kt` | `AppWidgetManager.requestPinAppWidget` flow with the **DataStore-backed** pending-config bridge (survives process death); `suspend` API; per-kind cap (`WidgetPinPolicy.MAX_WIDGETS_PER_KIND`) returning a typed `WidgetPinResult`. |
| `feature/torch/.../widget/WidgetPinSuccessReceiver.kt` | `goAsync()`-based receiver that claims the pending config and saves it to the repository on `WidgetReceiverScope`. |
| `feature/torch/.../widget/TorchWidgetConfigRepository.kt` | `FeaturePreferences<TorchWidgetConfig>` wrapper — typed surface; `.all` is `WhileSubscribed(Long.MAX_VALUE)`. |
| `feature/torch/.../ui/WidgetConfigurationSheet.kt` | `GadgetBottomSheet` form for new/edit flows. |
| `feature/torch/.../strobe/StrobeService.kt` | Foreground service with `foregroundServiceType="shortService"` on API 34+ (no camera-typed FGS needed for `setTorchMode`); `onTimeout` clean shutdown on the OS cap; returns `START_NOT_STICKY`; notification carries a **Stop** action; channel `setSound(null,null)`; live state published to `StrobeRuntime` singleton. |
| `feature/torch/.../strobe/StrobeRuntime.kt` | `@Singleton` `StateFlow<Boolean>` source of truth for "is the strobe running?" — replaces the old `@Volatile companion` flag the VM had to poll. |
| `feature/torch/src/main/AndroidManifest.xml` | All entry-point declarations co-located in the feature module — no `CAMERA` permission. |
| `core/datastore/.../FeaturePreferences.kt` + `FeaturePreferencesFactory.kt` | Generic per-feature persistence basis — `ReplaceFileCorruptionHandler` so a single bad write can't permanently brick a feature's storage. |
| `core/widgetkit/.../*` | Reusable widget-framework foundation — the generic half of the widget subsystem a feature plugs into: `WidgetKitConfig` contract + shared `WidgetReceiverScope` + `WidgetPinPolicy`/`WidgetPinResult`, **plus** the appearance value-types (`config/WidgetAppearance`), the RemoteViews `render/WidgetAppearanceRenderer` + `WidgetIconResolver`, the `store/WidgetConfigStore<T>` + `Migrator<T>`, `pin/PendingWidgetConfigs<T>` + `BaseWidgetPinSuccessReceiver<T>`, `provider/BaseGadgetWidgetProvider<T>`, `feedback/WidgetFeedbackDispatcher`, and `boot/BootCompletedReceiver`. A feature contributes one `WidgetKitConfig` + a config store and subclasses the base provider/receiver — it never re-hand-rolls the pin flow or appearance rendering. |
| `feature/torch/consumer-rules.pro` | R8 keep rules for the module's `@Serializable` types so minified release builds don't strip the synthetic serializers into a runtime `SerializationException`. |

The next feature migration (Sensors / Actuators / Camera / etc.)
follows this exact shape — only the controller's underlying
hardware API differs.

## Two module templates: minimal vs. advanced

Torch is the **advanced** reference: it exercises every seam at once —
standard hardware control + QS tile + app widgets + dynamic pinning +
foreground service + monitoring + automation + a rooted capability
adapter. That breadth makes it the canonical proof that the architecture
holds, but **most feature migrations should not start by copying all of
it.** Pick the template that matches the feature's real surface and add
seams only when the feature actually needs them.

**Minimal feature migration** — the floor every module clears:

- `Controller` interface + standard impl (Camera2 / sensor manager / etc.),
  `@Singleton`, bound via the feature's Hilt `@Binds` module.
- `@HiltViewModel` + a stateless `<Feature>ScreenContent` (Hilt-free, so
  previews/instrumented tests stay simple) wired by a `<Feature>Screen`
  Hilt route.
- Built on `ModuleScreenScaffold` with a `ModuleInfo`
  (permissions / OS compatibility) **and** a tri-state
  `ModuleCapabilitiesSection`.
- Navigation entry in `GadgetDestination.modules` + a route in `GadgetApp`.
- Unit tests for any serialization/repo + one instrumented test of the
  stateless content; the `@Preview` matrix.

**Advanced (Torch-style) migration** — add these only as the feature
demands them, reusing `:core:widgetkit` / `:core:monitoring` /
`:core:automation` rather than hand-rolling:

- **Widgets / QS tile / notifications**: contribute a `WidgetKitConfig`,
  subclass `BaseGadgetWidgetProvider<T>` + `BaseWidgetPinSuccessReceiver<T>`,
  and persist via `WidgetConfigStore<T>` — never re-implement the pin flow
  or appearance rendering.
- **Monitoring**: implement a `MetricSource` per readable signal and embed
  `MonitorContainer` / `LiveMonitorContainer`.
- **Automation**: expose an `ActionHandler` with `ModuleAction` metadata.
- **Rooted extras**: a feature-side capability interface implemented by two
  sibling per-flavor modules — a no-op `:feature:<name>-standard` (pulled in
  via `standardImplementation`) and a real `:feature:<name>-rooted` (via
  `rootedImplementation`), each with its own small Hilt `@Binds` module so
  exactly one impl is on each variant's classpath. Every privileged call is
  gated by `RootSafetyGate` + a `RootFeatureKey`. Torch is the reference
  (`:feature:torch-rooted` + `:feature:torch-standard`).

Rule of thumb: a torch (an actuator with widgets + monitoring +
automation + rooted boost) is advanced; a read-only sensor readout is
minimal. Both satisfy the same Module Authoring Contract — the advanced
seams are just unused until the feature grows into them. Don't overbuild a
simple feature into a torch.
