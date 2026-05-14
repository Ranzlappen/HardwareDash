# Gadget — Claude single source of truth

> **This file is the entry point for every Claude session on this
> repository.** Read it once at the start and you'll know the
> vision, every non-negotiable rule, the full component catalog
> with usage examples, the live Phase-1 status checklist, and the
> hard-won engineering pitfalls that don't show up in a local syntax
> check. Updated after every sub-batch.

---

## Vision

**Gadget** (Android applicationId `dev.ranzlappen.gadget` /
`.rooted`) is the definitive Android app for exploring every sensor
and actuator a phone exposes. The UI is **dark-first**, **glassy**
(M3 + glassmorphism overlay), and **future-proof for custom
themes** — every design token (colour / shape / typography /
spacing / motion / glass alpha) flows through `LocalGadgetTheme.current`
so a downstream "compact" or "high-contrast" theme overrides any
slot without forking components.

Master roadmap is in `MASTER-PLAN.md`. Current phase: **Phase 1
(Light Preview / Skeleton)** — architecture, design system,
navigation, mock screens; no real hardware code yet.

Tech: Kotlin 1.9.10 + Jetpack Compose (BOM 2024.04.01) + Hilt +
Room. minSdk 29, targetSdk 35, Java/Kotlin target 17.

---

## Non-negotiable rules

Every code change must satisfy these. CI doesn't enforce all of
them — they're enforced by review.

### Tokens

1. **Every design token sourced from `LocalGadgetTheme.current`**
   inside `@Composable` bodies. The umbrella exposes:
   ```kotlin
   LocalGadgetTheme.current.colors    // ColorScheme
   LocalGadgetTheme.current.typography
   LocalGadgetTheme.current.shapes
   LocalGadgetTheme.current.spacing   // GadgetSpacingValues
   LocalGadgetTheme.current.motion    // GadgetMotionValues
   LocalGadgetTheme.current.glass     // GadgetGlassValues
   ```
   The static `object GadgetSpacing` / `object GadgetMotion` in
   `core/designsystem/tokens/GadgetTokens.kt` is only the source for
   **default-value `val` initialisers** at file scope
   (`*Defaults.ContentPadding`, etc.). Never read those statics from
   inside a `@Composable` body — read from the local.
2. **No raw `dp` literals** in call sites. Exception: per-file
   `Defaults` value classes documenting fixed-size **design tokens**
   like the 56 dp FAB diameter — those constants live at top of
   their file with a KDoc explaining why they're allowed.

### Layout & touch

3. **Modifier-first parameter ordering** — `modifier: Modifier = Modifier`
   appears immediately after the required non-`@Composable`
   parameters. No exceptions.
4. **`Modifier.defaultMinSize(48.dp, 48.dp)`** on every tappable
   surface. Matches the Material accessibility minimum.
5. **Single-line text + `TextOverflow.Ellipsis`** by default on
   every public text-bearing parameter. Opt-in to multi-line via a
   `singleLine = false` / `maxLines = N` flag — never default to
   wrapping.

### Glass surfaces

6. **Every glassy surface goes through `GlassSurface` (composable)
   or `Modifier.glassSurface()` (extension).** Never reach for a
   raw `Surface(color = …)` with a hand-tuned alpha — the glass
   alphas live in `LocalGadgetTheme.current.glass` and must stay
   themeable.

### Accessibility

7. **Respect `LocalReducedMotion`** — when `true`, suppress
   spring/scale animations (pin target values) and degrade infinite
   transitions (shimmer → static `surfaceVariant`).
8. **Respect `LocalReducedTransparency`** — when `true`, swap any
   `GlassIntensity.Standard` / `Vivid` to `Subtle` (highest-opacity
   preset). Don't eliminate the glass surface — surfaces still need
   visual hierarchy.
9. **Required `contentDescription`** parameter on every icon-only
   composable (icon buttons, status dots). Pass `null` only when a
   sibling element provides the accessible label, and KDoc must
   call that out.
10. **Progress + skeleton announcements** — determinate
    `Gadget*Progress` set `progressBarRangeInfo`; `GadgetShimmerBlock`
    announces `liveRegion = Polite` + a "Loading" `contentDescription`.

#### Accessibility contract (per-component)

What every component **must** do for accessibility:

| Component | Contract |
|---|---|
| `GadgetIconButton` | `contentDescription` required (nullable only when a sibling labels the button). |
| `GadgetFab` | `contentDescription` required. Same nullable-with-sibling exception. |
| `GadgetStatusDot` | `contentDescription` **required** as a non-default parameter; pass `null` only when a labelled sibling carries the semantic. |
| `GadgetBadge` | Dot variant decorative; text variant announces a `stateDescription` (`"3 unread"` default; override via `stateDescriptionOverride`). |
| `GadgetCircularProgress` / `GadgetLinearProgress` | Determinate variant publishes `progressBarRangeInfo(current, 0f..1f)` so screen readers announce percent complete. |
| `GadgetShimmerBlock` | Announces `contentDescription = "Loading"` + `liveRegion = Polite` so screen readers say "Loading" once the user reaches a polite pause. |
| `GadgetEmptyState` | Title + subtitle + action merge into one a11y node via `semantics(mergeDescendants = true)` — announced as a single coherent unit. |
| `GadgetDialog` | Title carries `semantics { heading() }` so TalkBack emits a heading earcon before reading. |
| `GadgetBottomSheet` | Title carries `semantics { heading() }`. |

### Responsiveness

11. **`LocalWindowSizeClass.current`** is the source of truth for
    breakpoint decisions (Compact / Medium / Expanded). For most
    layout decisions, use the higher-level `rememberLayoutMode()`
    helper in `core/ui/adaptive/AdaptiveLayout.kt` which returns
    `SinglePane` / `TwoPane` / `ThreePane`:
    ```kotlin
    when (rememberLayoutMode()) {
        SinglePane -> CompactDashboard()
        TwoPane, ThreePane -> SplitPaneDashboard()
    }
    ```
    `WindowSizeClass` is the implementation detail (and its API may
    shift between M3 versions); `GadgetLayoutMode` is the stable
    seam. Use `BoxWithConstraintsAdaptive { mode -> … }` when a
    layout needs both pixel constraints and the semantic mode.
12. The shell adapts automatically: `GadgetApp` passes
    `showLabels = true` to `GadgetNavRail` when the active width
    class is **Expanded** (tablet landscape, Chromebook). Compact /
    Medium widths keep the rail icon-only to maximise content area.
    Compact-landscape → bottom-bar collapse is queued as Phase-2
    refinement.
13. The `ModuleScreenScaffold` exposes an optional `secondaryPane`
    slot rendered to the right of the primary column when
    `rememberLayoutMode()` is `TwoPane` / `ThreePane`. Wired
    ahead of need — no Phase-1 consumer yet.
14. Foldable-posture detection (hinge / tabletop) is **deferred**
    until a real consumer needs it — see open issue
    [#89](https://github.com/Ranzlappen/HardwareDash/issues/89).
    Don't pull in `material3-adaptive` ad-hoc.

### Performance & stability

13. Data classes exposed by the design system are marked `@Immutable`
    so Compose can skip recompositions. Mirror this for any new
    UI-state data class.
14. `@Composable val foo: Foo @Composable get()` accessors must be
    evaluated **inside** a `@Composable` function. Capture the
    resolved object into a local once and read its plain `val`s from
    non-composable callbacks.

---

## Component catalog

Every public composable in `:core:ui` has an entry below. Format:
**Signature** → **When** → **NOT when** → **Example** → **Behaviour
notes**.

### Buttons (`core/ui/component/Buttons.kt`)

#### `GadgetPrimaryButton`
```kotlin
fun GadgetPrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentPadding: PaddingValues = GadgetButtonDefaults.ContentPadding,
)
```
- **When**: high-emphasis call to action. One per screen at most.
- **NOT when**: secondary actions (use Secondary), inline list-row
  buttons (use Tertiary), icon-only chrome (use IconButton).
- **Example**:
  ```kotlin
  GadgetPrimaryButton(onClick = onSave, text = "Save changes")
  ```
- **Notes**: filled `colorScheme.primary`. Press-scale spring
  honours `LocalReducedMotion`. `loading = true` swaps the label
  for a `CircularProgressIndicator` and suppresses clicks.

#### `GadgetSecondaryButton`
```kotlin
fun GadgetSecondaryButton(
    onClick: () -> Unit, text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true, loading: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null, trailingIcon: ImageVector? = null,
    contentPadding: PaddingValues = GadgetButtonDefaults.ContentPadding,
)
```
- **When**: medium-emphasis sibling to a Primary CTA (e.g. "Save"
  + "Discard").
- **NOT when**: solo destructive actions — use Primary with
  appropriate phrasing instead, or a confirm dialog.
- **Example**:
  ```kotlin
  Row { GadgetPrimaryButton(...); GadgetSecondaryButton(...) }
  ```
- **Notes**: the **outlined glassy** tier of the button family.
  The container's paint comes from
  `Modifier.glassSurface(intensity = Standard)` — the M3 Surface
  underneath sits at `Color.Transparent` so the glass gradient +
  border shine through. The hairline outline still paints in
  `colorScheme.outline`. Custom themes that retune glass alphas via
  `LocalGadgetTheme.current.glass` flow through automatically.

#### `GadgetTertiaryButton`
```kotlin
fun GadgetTertiaryButton(
    onClick: () -> Unit, text: String, modifier: Modifier = Modifier,
    enabled: Boolean = true, loading: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null, trailingIcon: ImageVector? = null,
    contentPadding: PaddingValues = GadgetButtonDefaults.ContentPadding,
)
```
- **When**: low-emphasis inline action — "Learn more", "Cancel" in a
  dialog dismiss slot.
- **NOT when**: anywhere the user must notice the affordance —
  ghost buttons read as text.
- **Example**: `GadgetTertiaryButton(onClick = onCancel, text = "Cancel")`
- **Notes**: transparent container, primary-tinted label, ripple on
  press.

#### `GadgetIconButton`
```kotlin
fun GadgetIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
)
```
- **When**: chrome / toolbar actions where a label is redundant.
- **NOT when**: the icon is ambiguous — add a sibling label or use a
  labelled button.
- **Example**:
  ```kotlin
  GadgetIconButton(onClick = onSearchOpen, icon = Icons.Outlined.Search,
                   contentDescription = "Open search")
  ```
- **Notes**: 48 dp hit target. `contentDescription = null` is only
  acceptable when a sibling composable provides the accessible label.

#### `GadgetFab`
```kotlin
fun GadgetFab(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    text: String? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
)
```
- **When**: the single primary creation action on a screen ("Add
  sensor", "New rule").
- **NOT when**: more than one — use a row of buttons.
- **Example**:
  ```kotlin
  GadgetFab(onClick = onAdd, icon = Icons.Outlined.Add,
            contentDescription = "Add sensor", text = "Add sensor")
  ```
- **Notes**: passing `text` upgrades to an Extended FAB (icon +
  label). 56 dp diameter for the circular variant.

### Surfaces

#### `GlassSurface` (`core/ui/component/GlassSurface.kt`)
```kotlin
fun GlassSurface(
    modifier: Modifier = Modifier,
    intensity: GlassIntensity = GlassIntensity.Standard,
    showBorder: Boolean = true,
    contentPadding: PaddingValues = ...,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
)
```
- **When**: low-level glassy container without title/icon/header
  chrome. The primitive that `DashCard` / `CompactCard` build on.
- **NOT when**: you want a titled tile (use `DashCard`) or a
  horizontal list row (use `CompactCard`).
- **Example**:
  ```kotlin
  GlassSurface(intensity = GlassIntensity.Vivid) {
      Text("Hero panel content")
  }
  ```
- **Notes**: respects `LocalReducedTransparency` by swapping
  Standard/Vivid → Subtle (highest opacity preset).

#### `DashCard` (`core/ui/component/DashCard.kt`)
```kotlin
fun DashCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    intensity: GlassIntensity = GlassIntensity.Standard,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(GadgetSpacing.Medium),
    content: @Composable () -> Unit,
)
```
- **When**: dashboard tiles — vertical layout with optional title +
  icon header above content.
- **NOT when**: list rows (`CompactCard`), bare glass containers
  (`GlassSurface`).
- **Example**:
  ```kotlin
  DashCard(title = "Battery", icon = Icons.Outlined.BatteryFull) {
      Text("87%")
      SparklineChart(...)
  }
  ```

#### `CompactCard` (`core/ui/component/CompactCard.kt`)
```kotlin
fun CompactCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    intensity: GlassIntensity = GlassIntensity.Subtle,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = ...,
    singleLineTitle: Boolean = true,
)
```
- **When**: horizontal list-row glassy cards — settings rows,
  sensor entries.
- **NOT when**: dashboard tiles, hero surfaces.
- **Example**: see `BatteryListRow` placeholder in `:feature:dashboard`.

### Inputs

#### `GadgetTextField` (`core/ui/component/TextFields.kt`)
- **When**: any form input.
- **Notes**: wraps M3 `OutlinedTextField` with theme colours. State
  hoisted (`value` + `onValueChange`). `singleLine = true` default
  with horizontal scroll on overflow; opt-in multi-line via
  `singleLine = false` + `maxLines`.

#### `GadgetSearchField` (`core/ui/component/TextFields.kt`)
- **When**: search affordance with IME action = Search.
- **Notes**: auto-shows a clear button when `value.isNotEmpty()`;
  delivers `onSearch` on IME submit.

### Modals

#### `GadgetBottomSheet` (`core/ui/component/Modals.kt`)
- **When**: full-bleed action sheets / detail surfaces dismissible
  via swipe-down or back press.
- **NOT when**: simple confirms (use `GadgetDialog`).
- **Notes**: sheet visibility owned by caller via `SheetState`.
  Conditionally place the composable in the tree to show/hide.
- **Testing**: instrumented sheet tests are deferred — see open
  issue [#91](https://github.com/Ranzlappen/HardwareDash/issues/91).

#### `GadgetDialog` (`core/ui/component/Modals.kt`)
- **When**: confirmations, info acknowledgements, blocking
  decisions.
- **NOT when**: longer flows — use a bottom sheet instead.
- **Notes**: confirm slot required, dismiss slot optional. Body
  text wraps up to `bodyMaxLines` (default 10) then truncates.

### Status & indicators

#### `GadgetChip` (`core/ui/component/StatusIndicators.kt`)
- **When**: filter / segmented selection. Caller manages
  selected/unselected state and single-vs-multi-select semantics.
- **Notes**: wraps M3 `FilterChip`; M3 handles the
  `assertIsSelected` semantics automatically.

#### `GadgetBadge` (`core/ui/component/StatusIndicators.kt`)
- **When**: small counter ("3", "99+") or unread dot indicator
  anchored to another composable via `BadgedBox`.
- **Notes**: `text = null` → dot variant; non-null → pill with the
  text inside.

#### `GadgetStatusDot` (`core/ui/component/StatusIndicators.kt`)
- **When**: paired with a label for "● Online" / "● Offline"
  affordances.
- **Notes**: 8 dp diameter default. Caller picks the colour-to-
  semantic mapping.

### Loading

#### `GadgetCircularProgress` / `GadgetLinearProgress` (`core/ui/component/LoadingStates.kt`)
- **When**: in-flight async work. Determinate variant when progress
  is known (`progress = 0f..1f`), indeterminate otherwise.
- **Notes**: determinate variants populate `progressBarRangeInfo`
  in semantics so screen readers announce percent complete.

#### `GadgetShimmerBlock` (`core/ui/component/LoadingStates.kt`)
- **When**: skeleton placeholders while real data loads.
- **NOT when**: a brief / single-frame load — use a static
  surface or a progress spinner.
- **Notes**: animated linear-gradient sweep; honours
  `LocalReducedMotion` (degrades to static `surfaceVariant`).
  Gradient sweep width is computed via `BoxWithConstraints` so the
  effect scales correctly from 32 dp avatars to 1024 dp banners.

### Empty / placeholder

#### `GadgetEmptyState` (`core/ui/component/EmptyState.kt`)
- **When**: "no data yet" / "no results" placeholders.
- **Example**:
  ```kotlin
  GadgetEmptyState(
      title = "No sensors yet",
      subtitle = "Add your first sensor to begin.",
      icon = Icons.Outlined.Sensors,
      action = { GadgetPrimaryButton(onClick = onAdd, text = "Add sensor") },
  )
  ```
- **Notes**: title required, all other slots optional. Wrap in
  `Modifier.fillMaxSize` if you want it vertically centred in a
  screen.

### Shell / structure

#### `GadgetTheme` (`core/designsystem/theme/GadgetTheme.kt`)
- **When**: wrap every entry point into Compose content.
- **Notes**: provides `LocalGadgetTheme` (umbrella token bag) +
  `LocalReducedMotion` (from system animator-duration-scale).

#### `GadgetApp` (`core/navigation/GadgetApp.kt`)
- **When**: the top-level app shell. Hosts the nav rail + nav host.
- **Notes**: computes `WindowSizeClass` once from the host activity
  and provides it via `LocalWindowSizeClass`. Wraps content in
  `GadgetTheme`.

#### `ScreenHeader` / `SectionHeader` / `ModuleScreenScaffold`
(`core/ui/component/*.kt` + `core/ui/ModuleScreenScaffold.kt`)
- Standard layout primitives for feature screens. `ModuleScreenScaffold`
  takes a primary content slot + an optional `secondaryPane` slot
  rendered alongside when `rememberLayoutMode() ≥ TwoPane`.

---

## Batch 1.1 status checklist

Live status of the **Hardening, Responsiveness, Theming &
Self-Driving Documentation** batch:

- [x] **1.1.0** — `CLAUDE.md` SSOT foundation rewrite
- [x] **1.1.1** — `LocalGadgetTheme` full wiring (closes #90)
- [x] **1.1.2** — Accessibility semantics sweep
- [x] **1.1.3** — `WindowSizeClass`-aware shell
- [x] **1.1.4** — Glass consistency for Secondary button
- [ ] **1.1.5** — Shimmer width + blur-fallback polish
- [ ] **1.1.6** — `@Preview` matrix expansion (RTL / LargeFont / SizeClasses)
- [ ] **1.1.7** — Final status refresh + catalog verification

Open follow-up issues (Phase 2+ pickup):
- [#89](https://github.com/Ranzlappen/HardwareDash/issues/89) —
  `material3-adaptive` foldable hinge utility.
- [#91](https://github.com/Ranzlappen/HardwareDash/issues/91) —
  `GadgetBottomSheet` instrumented tests + sheet-host activity.
- [#92](https://github.com/Ranzlappen/HardwareDash/issues/92) —
  CI emulator workflow for `connectedDebugAndroidTest`.

---

## Engineering pitfalls (don't show up in local syntax checks)

There is no Android SDK in the local container, so
`./gradlew compileDebugKotlin` won't run here — CI catches build
errors. Be extra careful about the things below.

### Kotlin visibility — `internal` must not leak through public API

The Kotlin compiler rejects this with `'public' function exposes
its 'internal' return type containing declaration X`. Common ways
this happens in this repo:

- A `public` function (default) returns or accepts an `internal`
  type / nested type (`internal object Foo { data class Bar(...) }`
  referenced by a public signature).
- A `public class` has a `public` member that touches an `internal`
  type in its signature (parameter, return type, or property type).

Rules of thumb:
1. If a type is `internal` (or nested in an `internal` object/class),
   every public function or property that mentions it in its
   **signature** must also be `internal`. Bodies are fine.
2. Prefer `internal` for module-private helpers (codecs, framing,
   wire format) and keep the public API of a feature small + stable.
3. After adding new files, scan for `^internal (object|class|interface)`
   and confirm every consumer of those types is also `internal`,
   OR widen the type to `public`.

### Other CI-only traps

- `compileSdk = 35` constants (e.g. `Context.RECEIVER_NOT_EXPORTED`,
  API 33+) must be guarded with
  `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU`. Do not
  use `VERSION_CODES.S` (= 31) as a guard for API-33-only APIs.
- `usb-serial-for-android` exposes `setDTR/getDTR/setRTS/getRTS`.
  Kotlin maps those to property `DTR`/`RTS` (uppercase), not
  `dtr`/`rts`. Call the methods explicitly to avoid surprises.
- `return@OutlinedButton` / `return@Button` for lambdas passed as
  **named** arguments (e.g. `OutlinedButton(onClick = { … })`) is
  unreliable — the label resolves to the parameter name, not the
  function. Restructure with `if/else` instead of early-returning.
- `Modifier.blur(…)` requires API 31+. Below that it's a no-op.
  Components that rely on blur for the glass effect must provide a
  higher-opacity solid fallback under
  `Build.VERSION.SDK_INT < Build.VERSION_CODES.S`.

---

## Plan-mode file hygiene

The plan file at `/root/.claude/plans/<name>.md` is a **per-task
scratchpad, not a historical log**. Apply these rules in every
plan-mode session on this repo:

1. Each new plan-mode session must **replace** the file's contents
   with the current task's plan — never append.
2. Prior batch plans live in git commits, the PR description, and
   this `CLAUDE.md` — not in the plan file.
3. Git is the source of truth for shipped work. The plan file's job
   is to describe what's about to happen *now*, not what already
   happened.
4. If the same plan file balloons past ~500 lines after a session,
   it has accumulated stale content; truncate at the start of the
   next session.

---

## Flavors (standard vs rooted)

The app ships as two product flavors built from one repo:

- `standard` (applicationId `com.gadget`) — non-rooted behavior.
- `rooted` (applicationId `com.gadget.root`) — adds root-only
  capabilities.

Rules (full details in `docs/flavors.md`):

1. Shared code lives in `app/src/main/`. New features default here.
2. Standard-only stubs live in `app/src/standard/`. Rooted-only code
   lives in `app/src/rooted/`. Files in those two directories MUST
   share fully-qualified class names — Gradle picks one at build
   time based on the active flavor.
3. Never branch on `BuildConfig.IS_ROOTED`. Inject
   `RootCapabilityRegistry` / `RootSafetyGate` (in
   `com.gadget.root`) and let the Hilt seam pick the right
   implementation per flavor.
4. Never put rooted-specific imports (e.g. anything that talks to
   su) under `src/main/`. They belong in `src/rooted/` with a no-op
   twin in `src/standard/`.
5. CI produces `standard-debug.apk`, `standard-release.apk` +
   `.aab`, and `rooted-debug.apk`. `versionCode = CI_VERSION_CODE *
   10 + flavor_offset` (standard=+0, rooted=+1).

### Standard-APK leak gate

CI's `Assert standard APK has no rooted leakage` step in
`.github/workflows/build-apk.yml` runs on both
`assembleStandardDebug` and `assembleStandardRelease` and hard-fails
the matrix leg if the assembled APK contains su-related strings
(`topjohnwu`, `libsu`, `/system/bin/su`, `/system/xbin/su`,
`chainfire`, `hiddenapibypass`), rooted assets (`lsposed`, `magisk`,
`spoofer`, `.magisk.`, `/su/`), or root-tier permissions
(`WRITE_SECURE_SETTINGS`, `MOUNT_UNMOUNT*`, `INSTALL_PACKAGES`,
`DELETE_PACKAGES`, `READ_LOGS`, `MANAGE_USERS`, `CHANGE_CONFIGURATION`,
`MASTER_CLEAR`, `REBOOT`, `ACCESS_SUPERUSER`). The dex pattern is
deliberately precise — bare `magisk`/`superuser` would trip on the
shared `RootProvider` sealed-class variant names and on cosmetic
localization strings; the libsu/topjohnwu/chainfire/hiddenapibypass
markers cover the real-leak case without that noise. If you add a
new rooted-only library, asset, or permission, scope it to
`rootedImplementation` / `app/src/rooted/assets/` /
`app/src/rooted/AndroidManifest.xml` — the gate will catch the
mistake on PR.

---

## Layout pointers

- Settings: `app/src/main/java/com/gadget/ui/screens/SettingsScreen.kt`
- Radios (Sub-GHz / IR / NFC / WiFi / Cell): `…/ui/screens/RadiosScreen.kt`
- Backup: `…/backup/BackupManager.kt` — ZIP of Room DB +
  `shared_prefs/*.xml` + `datastore/*`. WAL must be checkpointed
  via `query("PRAGMA wal_checkpoint(FULL)")`, not `execSQL` —
  `execSQL` rejects statements that return rows.
- Flipper Zero connection: `…/flipper/` (USB CDC-ACM and BLE GATT)
  + `…/flipper/rpc/` (hand-rolled minimal protobuf encoder; field
  numbers track flipperzero-protobuf ≥0.80).
- Localized strings: single source of truth at
  `…/localization/Strings.kt`, `m(lang, en, de, es, fr)` helper.
  Add new feature blocks following the `Backup` / `Flipper` pattern.
- Hilt: `@HiltAndroidApp` on `GadgetApplication`; non-Composable
  Compose consumers reach singletons via
  `EntryPointAccessors.fromApplication(...)` (see the
  `BackupManagerEntryPoint` / `FlipperManagerEntryPoint` examples).
