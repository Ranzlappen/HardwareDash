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
gives you a `state: StateFlow<...>` and `suspend fun` setters
that the UI can consume.

**Pattern** (extracted from Torch):

```kotlin
interface TorchController {
    val state: StateFlow<TorchState>
    suspend fun toggle()
    suspend fun setOn(on: Boolean)
}

@Immutable data class TorchState(
    val isOn: Boolean = false,
    val isAvailable: Boolean = false,
    val error: TorchError? = null,
)

@Singleton
class StandardTorchController @Inject constructor(
    @ApplicationContext private val context: Context,
) : TorchController {
    private val _state = MutableStateFlow(TorchState())
    override val state = _state.asStateFlow()
    // ... Camera2 implementation ...
}
```

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

Hoist state into a `@HiltViewModel`:

```kotlin
@HiltViewModel
class TorchViewModel @Inject constructor(
    private val controller: TorchController,
) : ViewModel() {
    val state: StateFlow<TorchState> = controller.state
    fun onToggleClick() {
        viewModelScope.launch { controller.toggle() }
    }
}
```

Then in the screen:

```kotlin
@Composable
fun TorchScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // ... screen layout ...
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
fun NavGraphBuilder.torchScreen(onBack: () -> Unit) {
    composable(route = GadgetDestination.Torch.route) {
        TorchScreen(onBack = onBack)
    }
}
```

Wire from `MainActivity.setContent { GadgetApp { … } }`:

```kotlin
GadgetApp {
    dashboardScreen(onNavigate = navController::navigateTopLevel)
    torchScreen(onBack = navController::popBackStack)
    // placeholderScreen(...) for the rest
}
```

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

Once the feature compiles, add instrumented tests under
`feature/<name>/src/androidTest/`:

- `androidTestImplementation(project(":core:testing"))` in the
  module's `build.gradle.kts`.
- Wrap test content in `GadgetTestTheme { … }` from
  `:core:testing`.
- Mock the controller via Hilt test rules or inject a fake.
- Test the contracts the legacy implementation had (click toggles
  state, disabled state suppresses click, etc.).

For Phase 2 / Batch 1, tests are queued as a follow-up batch
once the basic vertical slice is verified manually — landing
working code first, then locking it in with tests.

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
   Camera-using services need `foregroundServiceType="camera"`,
   etc. Required on API 34+.
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

A complete walkthrough is preserved in the Phase 2 / Batch 1
commit on `claude/refactor-2026`. Key artefacts to study when
doing the next migration:

| File | Why study it |
|---|---|
| `feature/torch/.../TorchController.kt` | Interface shape — `StateFlow<TorchState>` + suspend setters. |
| `feature/torch/.../StandardTorchController.kt` | Hilt-injected implementation with Camera2 + `TorchCallback`. |
| `feature/torch/.../TorchScreen.kt` | Big-FAB layout using `ModuleScreenScaffold` + `GadgetFab`. |
| `feature/torch/.../TorchViewModel.kt` | Passthrough VM — controller state in, click in, suspend out. |
| `feature/torch/.../TorchNavigation.kt` | `NavGraphBuilder.torchScreen(...)` registration. |
| `feature/torch/.../tile/FlashlightTileService.kt` | `EntryPointAccessors.fromApplication(...)` pattern for non-Hilt components. |
| `feature/torch/.../widget/FlashlightWidgetProvider.kt` | `AppWidgetProvider` reaching the singleton controller via Hilt entry-point. |
| `feature/torch/.../strobe/StrobeService.kt` | Foreground service with `FOREGROUND_SERVICE_CAMERA` type. |
| `feature/torch/src/main/AndroidManifest.xml` | All entry-point declarations co-located in the feature module. |

The next feature migration (Sensors / Actuators / Camera / etc.)
follows this exact shape — only the controller's underlying
hardware API differs.
