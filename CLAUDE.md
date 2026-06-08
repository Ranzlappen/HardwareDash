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
    refinement. The rail itself is a **pinned-anchors** layout:
    `GadgetDestination.pinnedTop` (Dashboard) at the top,
    `GadgetDestination.pinnedBottom` (Settings) at the bottom, and
    `GadgetDestination.modules` in a `verticalScroll`-backed middle
    region that scrolls independently as the module list grows.
    Modules replace the placeholder areas as real features land —
    Torch is the first; Sensors / Actuators / Automation stay as
    coming-soon placeholders in the module region until their feature
    modules ship. Add a new module by appending it to
    `GadgetDestination.modules` and registering its route in the
    `GadgetApp { … }` builder.
13. The `ModuleScreenScaffold` exposes an optional `secondaryPane`
    slot rendered to the right of the primary column when
    `rememberLayoutMode()` is `TwoPane` / `ThreePane`. Wired
    ahead of need — no Phase-1 consumer yet.
14. Foldable-posture detection (hinge / tabletop) has a **stable seam**
    (closes #89): `GadgetPosture` + `rememberPosture()` in
    `core/ui/adaptive/AdaptiveLayout.kt`, backed by `material3-adaptive`'s
    `currentWindowAdaptiveInfo()`. Like `rememberLayoutMode()` it returns
    the Gadget enum (`Flat` / `Tabletop` / `Book`), never a
    `material3-adaptive` type — read it for posture, `rememberLayoutMode()`
    for width (they're orthogonal). It's **wired ahead of need** (no
    Phase-1 consumer; `Flat` on non-folding devices), so still **don't**
    pull `material3-adaptive` into a feature ad-hoc — go through the seam.

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

#### `GadgetCircleControl` (`core/ui/component/GadgetCircleControl.kt`)
```kotlin
fun GadgetCircleControl(
    icon: ImageVector,
    contentDescription: String,
    caption: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    hero: Boolean = false,
    onClick: (() -> Unit)? = null,
    onHold: ((Boolean) -> Unit)? = null,
)
```
- **When**: a row of identical captioned round controls — a circle
  (icon) over a short label. Torch's toggle / hold / strobe / morse row
  is the reference.
- **NOT when**: a single CTA (use `GadgetPrimaryButton`/`GadgetFab`) or
  toolbar chrome (use `GadgetIconButton`).
- **Example**:
  ```kotlin
  GadgetCircleControl(icon = Icons.Filled.FlashlightOn,
      contentDescription = "Torch", caption = "Torch", enabled = true,
      active = isOn, hero = true, onClick = onToggle)
  ```
- **Notes**: pass `onClick` for tap **or** `onHold` for press-and-hold
  (`true` on press, `false` on release/cancel via try/finally so it
  can't stick). `hero` marks the primary action with a filled `primary`
  surface; `active` tints the on-state. 56 dp touch target.
  `contentDescription` required (icon-only).

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

#### `GadgetExpandableCard` (`core/ui/component/GadgetExpandableCard.kt`)
```kotlin
fun GadgetExpandableCard(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    intensity: GlassIntensity = GlassIntensity.Standard,
    contentPadding: PaddingValues = PaddingValues(GadgetSpacing.Medium),
    content: @Composable () -> Unit,
)
```
- **When**: a collapsible `DashCard` — same glass surface + icon/title
  header, but the header is a toggle that shows/hides the body with a
  rotating chevron. Use it for screen sections the user may want to fold
  away.
- **NOT when**: a always-open tile (use `DashCard`) or a list row
  (`CompactCard`).
- **Stateless** — `expanded` is hoisted. For **persisted** collapse state
  pair it with `:core:monitoring`'s `CollapseStateRepository` (see the
  collapse-state blueprint in the monitoring section); for ephemeral state a
  `rememberSaveable { mutableStateOf(true) }` suffices.
- **Notes**: header is one `toggleable` node (role Button) announcing
  "Expanded"/"Collapsed" via `stateDescription`; the chevron is decorative.
  Honors `LocalReducedMotion` (chevron + body enter/exit pinned instant).

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
```kotlin
fun GadgetBadge(
    modifier: Modifier = Modifier,
    text: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.error,
    contentColor: Color = MaterialTheme.colorScheme.onError,
    stateDescriptionOverride: String? = null,
)
```
- **When**: small counter ("3", "99+") or unread dot indicator
  anchored to another composable via `BadgedBox`.
- **Notes**: `text = null` → dot variant (decorative); non-null →
  pill with the text inside. Text variant announces
  `"$text unread"` to screen readers; override via
  [stateDescriptionOverride] for a non-default semantic
  (e.g. `"3 errors"`).

#### `GadgetStatusDot` (`core/ui/component/StatusIndicators.kt`)
```kotlin
fun GadgetStatusDot(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = StatusDotDefaultSize,
)
```
- **When**: paired with a label for "● Online" / "● Offline"
  affordances.
- **Notes**: 8 dp diameter default. Caller picks the colour-to-
  semantic mapping. **`contentDescription` is required** —
  passing `null` is acceptable only when a sibling labelled
  composable (the `Text("Online")` next to the dot in a `Row`)
  carries the accessible label.

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
- Standard layout primitives for feature screens.
- `ModuleScreenScaffold` takes a primary content shape (`title` +
  `functional` + `disclaimer` free-form slots, plus a declarative
  `moduleInfo`) **plus** an optional `secondaryPane` slot rendered to
  the right of the primary column when `rememberLayoutMode() ≠
  SinglePane`. On Compact widths the secondary pane is omitted
  entirely — treat it as supplementary content for wider screens, not
  something the primary flow depends on. Primary takes `1.5f` weight
  on TwoPane and `1f` on ThreePane (50/50 split).

### Module blueprint (`core/ui/module/`)

Every feature module is **self-describing** via a `ModuleInfo` so the
shared scaffold renders a consistent metadata block without each
module hand-rolling the chrome. This is the future-proof seam for new
modules and for legacy migrations.

#### `ModuleInfo` (`core/ui/module/ModuleInfo.kt`)
```kotlin
@Immutable
data class ModuleInfo(
    val permissions: List<ModulePermission> = emptyList(),
    val compatibility: OsCompatibility,
    val firmware: FirmwareRequirement? = null,
)
```
- **When**: every screen built on `ModuleScreenScaffold` that
  represents a hardware/feature module. Pass it as the scaffold's
  `moduleInfo` and the standard **Permissions → OS compatibility →
  Firmware** cards render automatically (after `functional`, before
  `disclaimer`).
- **Build it inside a `@Composable`** and resolve `stringResource(…)`
  at construction — module-specific copy (permission rationales, OS
  notes) lives in the **feature's** resources; the generic section
  chrome lives in `:core:ui` (`core/ui/src/main/res/values/strings.xml`).
- `firmware = null` (the common case) omits the firmware card. An
  empty `permissions` list renders a "no permissions required" state.
- **Reference impl**: `feature/torch`'s `torchModuleInfo()` — no
  permissions, `minSdk 29` + two foreground-service notes, no
  firmware.

#### Reusable sections (`core/ui/module/ModuleInfoSections.kt`)
- `ModulePermissionsSection` — one status row per permission (coloured
  `GadgetStatusDot` + label + rationale); when any required permission
  is missing it shows **both** a primary in-app **Grant** request
  button (`rememberLauncherForActivityResult` /
  `RequestMultiplePermissions`) **and** a secondary **Open app
  settings** link. Grant state is live, refreshed on the request
  callback and on `ON_RESUME` (so it updates after the settings
  round-trip).
- `ModuleCompatibilitySection` — compares `minSdk` against the live
  `Build.VERSION.SDK_INT` for a supported/unsupported verdict, then
  lists `OsNote`s tagged by the API level they apply from.
- `ModuleFirmwareSection` — rendered only when `firmware != null`.
- `ModuleCapabilitiesSection` — **per-function** green/amber/red status
  block (the "Functions & compatibility" card). Driven by
  `ModuleInfo.capabilities: List<ModuleCapability>`. Each
  `ModuleCapability` carries a name, optional detail, and a
  `@Composable () -> CapabilityStatus` live check that resolves to a
  `GadgetStatusKind` (Success → `primary`/teal, Warning →
  `tertiary`/amber, Error → `error`/red) + a message + an optional
  `CapabilityAction` (`RequestPermissions`, `OpenAppSettings`, or a
  `Custom` handler). Statuses re-evaluate on permission results and
  `ON_RESUME` (the section owns the `RequestMultiplePermissions`
  launcher + a `key(refreshKey)`). Use this to report, per button /
  per function and **per flavor**, exactly what works on the device and
  what's missing. Torch is the reference: flash-hardware + OS-version
  rows for the standard functions, and four rooted rows whose status
  comes from the `TorchRootCapabilities` probe.
- The tri-state mapping lives in
  `core/ui/component/StatusIndicators.kt` (`enum GadgetStatusKind` +
  `@Composable GadgetStatusKind.color()`). Reuse it anywhere a
  green/amber/red readout is needed; never hand-pick the three colours.
- These usually flow through the scaffold's `moduleInfo` param; call
  them directly only for a bespoke layout.

#### Rooted-flavor module extension (`TorchRootCapabilities` reference)

The blueprint for adding **root-only** capabilities to a *modular*
feature without breaking flavor isolation or the Hilt graph:

1. Declare a capability interface + result/availability types in the
   feature module's `src/main` (e.g. `TorchRootCapabilities`,
   `TorchRootResult`, `TorchRootAvailability`). The feature stays
   flavor-agnostic and never imports libsu or `com.gadget.root.*`.
2. Bind the no-op (standard) and real (rooted) impls from **sibling
   per-flavor feature modules**, each pulled into only its own variant:
   `:feature:<name>-rooted` via `rootedImplementation` and
   `:feature:<name>-standard` via `standardImplementation`. Each sibling
   carries its impls **plus** a small Hilt `@Binds` module
   (`RootedTorchModule` / `StandardTorchModule`). Because each module is on
   exactly one variant's classpath, exactly one `@Binds` per interface is
   ever present — no Hilt duplicate-binding clash, and **no flavor's impls
   live in `:app`**. Torch is the reference (`:feature:torch-rooted` +
   `:feature:torch-standard`).
   *Older variant (still used by un-migrated features):* bind from
   `:app`'s flavor source sets instead — a no-op in
   `app/src/standard/.../<feature>/` + a real impl in
   `app/src/rooted/.../<feature>/`, both added to the matching flavor
   `RootBindings`. Prefer the sibling-module pattern for new work; the
   feature module itself still never binds the impl either way.
3. The rooted impl should **reuse the existing privileged sysfs
   controller** (the feature's `<Feature>SysfsController` surface, e.g.
   torch's `TorchSysfsController` implemented by `RootedTorchController`)
   where one exists — adapt its result type rather than re-implementing
   sysfs/libsu. All privileged calls must route through `RootSafetyGate`
   (capability + opt-out + rate-limit) and use a `RootFeatureKey`, and
   must be hardware-safe by construction: clamp to a hard ceiling
   (torch's 150 % brightness cap), bound any override with an absolute
   time ceiling, and always restore device state in a `NonCancellable`
   `finally` so a cancelled coroutine can't leave throttling disabled or
   an LED latched on.
4. The feature's `@HiltViewModel` injects the interface directly; Hilt
   resolves the binding at `:app` assembly. Probe availability once and
   fold it into the screen state; show root controls only when
   available, and surface each rooted function as a
   `ModuleCapability` row so the badges read red ("requires the rooted
   app version") on standard.

   Reference impl: `TorchRootCapabilities` (interface) +
   `StandardTorchRootCapabilities` / `RootedTorchRootCapabilities`
   (app flavor bindings, the latter delegating to `RootedTorchController`,
   the rooted `TorchSysfsController` impl).

---

## Monitoring framework (`:core:monitoring`, `:core:model`, `:core:data`)

The reusable, drop-in monitoring container every actuator/sensor module
embeds to chart + persist a signal. **Never depends on a feature
module** — features plug in via Hilt map multibindings resolved at
`:app`. Torch is the reference consumer.

### The readable-signal seam — `MetricSource` (`:core:model`)
```kotlin
interface MetricSource {
    val descriptor: MetricDescriptor   // metricKey, displayName, unit, min, max, category
    suspend fun sample(): Float        // poll path (always required)
    fun stream(): Flow<Float>? = null  // optional push path (null = poll)
}
```
- The **single** readable-signal contract — consumed by monitoring
  **today** and the automation trigger-evaluator **later**. Lives in
  `:core:model`; its only foundation dependency is pure-Kotlin
  `kotlinx-coroutines-core` (the `-core`, **not** the `-android`, artifact)
  for the optional `stream()` — no Android, no feature, no Compose.
- A feature contributes one per signal:
  `@Binds @IntoMap @StringKey("<metricKey>") fun …(): MetricSource`.
  Reference: `TorchMetricSource` (`"torch_intensity"`).
- **Push vs. poll** (chosen per signal so the shared service stays cheap as
  modules multiply):
  - **Poll** (`stream()` returns null, the default): the sampler calls
    `sample()` every `pollIntervalMs`. Use for genuinely sampled signals
    (battery %, temperature, RSSI) **and for any continuously-charted
    signal** — a downsampled time chart needs a sample in every bucket, so
    an actuator whose chart should show a filled plateau (the torch
    reference) polls rather than emitting only on change.
  - **Push** (override `stream()`): emit only when the value changes. Use
    for sparse/event-only signals and to feed the automation evaluator; an
    idle source then causes **zero** wakeups. `descriptor.max` is the
    metric's full-scale ceiling and is allowed to be **capability-driven**
    (the torch reports 100 on standard, ~150 on the rooted boost flavor) so
    the chart/widget axes scale to the real range.
- This is the deliberate fix for legacy `Link`, which hardcoded a
  70-entry metric registry inside `LinkService`.

### `MonitorContainer` (`core/monitoring/MonitorContainer.kt`)
```kotlin
@Composable fun MonitorContainer(metricKey: String, title: String, modifier: Modifier = Modifier, collapseId: String? = null)
```
- **When**: drop into any feature screen to chart + **persist** a metric for
  **long histories**. Self-contained — supply a `metricKey` (matching a
  contributed `MetricSource`) and a title; config + history come from the
  framework via Hilt (`hiltViewModel(key = metricKey)`).
- A glass `DashCard` with the live chart, an on/off toggle, and a
  persistent settings block (sample-interval `GadgetSlider`, **time-window
  `GadgetSlider` in minutes** (1m–24h, default 1m), chart-style
  `GadgetChip`s, "show as widget" + "show as notification" switches). The
  window drives both the in-app chart and the chart widget.
- Pass `collapseId` to render inside a collapsible `GadgetExpandableCard`
  whose expanded state persists via `CollapseStateRepository` (the stateless
  `MonitorContent` / extracted `MonitorBody` stay Hilt-free).
- **Embed it via a `@Composable () -> Unit` slot on the stateless screen
  content**, supplied by the Hilt route — so the stateless content +
  its previews/tests stay Hilt-free. Reference: `TorchScreenContent`'s
  `monitor` slot, supplied by `TorchScreen`. The stateless renderer is
  `MonitorContent` (preview-safe).

### `LiveMonitorContainer` (`core/monitoring/LiveMonitorContainer.kt`)
```kotlin
@Composable fun LiveMonitorContainer(metricKey: String, title: String, modifier: Modifier = Modifier, collapseId: String? = null)
```
- **When**: the **live, in-memory** companion to `MonitorContainer`, for
  realtime analysis. The two are **independent** — embed both for a metric.
  This one **bypasses Room and the foreground service**: `LiveMonitorViewModel`
  reads the `MetricSource` directly (prefers `stream()`, else fast-polls
  `sample()` every `intervalMs`, default 100 ms) into a bounded ring buffer
  and exposes a fast `LiveTrace` (samples + current/min/max/avg).
- The card is a "Live stream" on/off toggle, a `LiveChart`, a stats row, a
  **Freeze** chip (hold the trace for inspection), and **ephemeral** refresh /
  live-window sliders (live is a transient surface — settings aren't
  persisted, unlike `MonitorConfig`).
- **Lifecycle**: sampling runs only while the card is composed (a
  `DisposableEffect` calls `start`/`stop`) **and** the toggle is on; freeze
  pauses it. So an off-screen or off/frozen card incurs no wakeups.
- `LiveChart` — a hand-drawn `Canvas` with **soft auto-scale Y** (displayed
  bounds *ease* toward the visible buffer's padded min/max via
  `animateFloatAsState`, honoring `LocalReducedMotion`) **plus pinch-zoom /
  vertical-drag** for a manual Y viewport; an **Auto** chip / double-tap
  returns to soft-auto. X is the live window anchored to now. Reuses the
  cubic-Bézier half-step smoothing from `core/ui`'s `SparklineChart`.
- Embed via its own slot (`liveMonitor`), the same Hilt-free seam as
  `monitor`. `LiveMonitorContent` is the preview-safe stateless renderer.

### Persistent collapsible cards — `CollapseStateRepository`
- **`GadgetExpandableCard`** (`:core:ui`, stateless) is the collapsible card
  primitive; **`CollapseStateRepository`** (`core/monitoring/CollapseStateRepository.kt`,
  DataStore — mirrors `MonitorConfigRepository`) persists expanded/collapsed
  state keyed by a stable string id, **defaulting to expanded**. It is **not**
  monitoring-specific — it lives in `:core:monitoring` only because that's the
  shared Hilt + DataStore core every feature already depends on.
- **Blueprint for making a feature's cards collapsible** (torch is the
  reference): the screen content stays Hilt-free, so **hoist** expanded state
  through the feature's ViewModel — inject `CollapseStateRepository`, expose
  `expandedStates(ids).` as a map into the view-state + an `onSectionToggle(id)`
  handler, and wrap each card in `GadgetExpandableCard`. The reusable monitor
  containers instead take a `collapseId` and manage their own collapse via
  their VM. Section ids: see `TorchSectionId`.

### Other pieces
- `MonitorConfig` (`@Serializable @Immutable`) — per-metric persisted
  settings (`enabled`, `pollIntervalMs`, `chartLayout`, `windowSeconds`,
  `yMax`, `widgetEnabled`, `notificationEnabled`). `windowSeconds` defaults
  to **1m** and is user-editable from 1m to **24h** (`MIN/MAX_WINDOW_SECONDS`;
  the 24h cap matches `MonitorService.RETENTION_MS` so the chart never asks
  for pruned data). Stored per metricKey by `MonitorConfigRepository`
  (mirrors `TorchWidgetConfigRepository`).
- `MonitorChart` — a **hand-drawn Compose `Canvas`** chart (line/area/column
  per `chartLayout`), deliberately **not Vico**. Vico's scroll/zoom + per-
  entry axis labelling fought the sliding-window model (labels relative to
  the oldest present sample, erratic auto-scroll, coarse duplicate ticks
  cramming on zoom across three failed x-encodings); drawing it ourselves is
  fully controllable and matches the widget sparkline the user trusts. It
  charts a **downsampled** window (`MonitorViewModel.history()` → peak-per-
  bucket via `MonitorSampleDao.observeBucketedSince`, capped at ~500 points),
  so a 24h window stays a few hundred points instead of tens of thousands.
  - **X-axis is pinned to the full window, anchored to *now*.** A bucket's
    index is `(timestamp − windowStart) / bucketMs` (range `0..windowMs/
    bucketMs`); the chart pins x to that full range via `MonitorHistory
    .windowMs`, so the right edge is always the present and the left is
    `windowMs` ago **regardless of how much data exists yet** (fixes the old
    "stuck at 0s, empty axis" bug). Data flows in from the right as it
    accumulates; gaps stay proportional. X labels are **time-ago marks at
    nice round intervals** (`…3h, 2h, 1h, now`), a fixed handful (~5) sharing
    one unit derived from the interval — no per-entry labelling, so nothing
    crams. Y is pinned `0..yMax`, where `yMax = MonitorViewModel.maxValue()`
    = the source's `descriptor.max` (capability-driven; 150 on the rooted
    torch). No horizontal scroll/pinch-zoom: to "zoom in", shrink the window
    (the slider) — it re-queries at a finer bucket size.
  - Axis text is drawn with `rememberTextMeasurer()` + `DrawScope.drawText`;
    guard `< 2` points with a "collecting…" placeholder.
- `MonitorChartBitmapRenderer` (`core/monitoring`) — reusable pure-`Canvas`
  sparkline (line/area/column, `0..yMax`) → `Bitmap` for the **chart widget**
  (RemoteViews can't host a Compose chart). Ships via `setImageViewBitmap`
  (the same path the torch custom-icon widget uses). No Compose deps, safe
  off the main thread.
- `MonitorService` — the **single** `specialUse` FGS for the whole app
  (features never run their own monitoring service/process). Per metric it
  runs one structured coroutine that follows the metric's live config via
  `collectLatest` and reads the source by **push** (`stream()` collected —
  zero idle wakeups) or **poll** (`sample()` every `pollIntervalMs`, wrapped
  in `withTimeout` so a slow source can't stall the shared dispatcher).
  Scaling guards: DB **inserts** are per-reading (full-resolution history),
  but **pruning is batched** (`PRUNE_INTERVAL_MS`, once/min) and **widget +
  notification repaints are throttled** (`UI_UPDATE_THROTTLE_MS`) so a fast
  poll rate can't cause an I/O / RemoteViews storm. Posts a **determinate**
  ongoing notification per `notificationEnabled` metric, pushes widget
  repaints per `widgetEnabled`, and **self-stops** when no metric is
  enabled. `MonitorController.ensureStarted()` is the only start path.
- `MonitorWidgetNotifier` — seam letting a feature refresh its own monitor
  widget(s) when a sample lands without monitoring depending on it
  (`@IntoMap` keyed by metricKey; the service already coalesces the calls).
  Reference: `TorchMonitorWidgetNotifier` repaints **both** torch monitor
  widgets — `MonitorWidgetProvider` (determinate `ProgressBar`) and
  `MonitorChartWidgetProvider` (sparkline bitmap). Both share the metric's
  `MonitorConfig` (window + enable toggle) and read its `descriptor.max` for
  the progress ceiling / y-scale.
- **Room lives in `:core:data`** (the `MonitorSample` time-series store +
  `MonitorSampleRepository`; `observeBucketedSince` is the downsample query).
  Repo convention: other modules read `:core:data` repositories, never Room
  directly. First modular DB — `schemas/` committed.

### Monitoring scaling & limits (blueprint rules)
As actuator/sensor modules multiply, follow these so monitoring stays within
Android's process/battery/IPC limits:
- **One shared `MonitorService`** for the whole app — never a per-module
  service or process. N monitored modules cost one FGS.
- **Prefer push (`stream()`) over poll** for event-driven signals; poll only
  genuinely-sampled signals and continuously-charted ones (a downsampled
  chart needs a sample per bucket — see `MonitorChart`).
- **Downsample for display** (~500 in-app / ~120 widget points), keep a
  **bounded retention** (24h), **batch the prune**, and **throttle** widget /
  notification repaints. Never feed Vico tens of thousands of raw points.
- **Chart x = fixed-time-bucket index, axis pinned to the full window
  anchored to now** (hand-drawn `Canvas`, not Vico); "zoom" = change the
  window so it re-queries at a finer bucket size.
- **Other device limits to respect**: `specialUse` FGS needs a Play-console
  justification (`<property>` in the manifest) and a continuous FGS depends
  on `POST_NOTIFICATIONS` + Doze allowances; **RemoteViews bitmaps** must be
  sized to the widget (the chart widget caps its bitmap) to stay under the
  transaction-size limit; the AppWidget native update floor is ~30 min so
  push via the notifier, never `updatePeriodMillis`; DB size/IOPS grow with
  `poll rate × metric count` (retention + downsample + batched prune bound
  it); prefer adaptive intervals and respect battery-saver for future
  sensor sources.

---

## Widgetkit framework (`:core:widgetkit`)

The reusable home-screen-widget framework — the generic half of the torch
widget subsystem, extracted so a 30-module app doesn't re-hand-roll the pin
flow, per-`appWidgetId` persistence, RemoteViews appearance rendering, the
icon catalog, and toast/notification feedback for every feature. Features
plug in via a config implementing `WidgetKitConfig`; the kit never depends
on a feature module.

**Established in `refactor-2026` Phase 1 (Batches 4–9):** `WidgetKitConfig`
contract (`displayName`, `removed`, `schemaVersion`, `appearance`),
`WidgetReceiverScope`, `WidgetPinPolicy` + `WidgetPinResult`.

**Filled in by `refactor-2026` Phase 2 (Batches C1–C7):**
- **`config/`** — `WidgetAppearance` value-type family (`BackgroundMode`,
  `IconStyle`, `IconTint`, `TapBehavior`, `TapAnimation`, `ToggleFeedback`)
  + `WidgetIconSource` + `WidgetIconKeys`. `ToggleFeedback`'s polymorphic
  serial-name discriminators are **pinned to the legacy FQN** via explicit
  `@SerialName` so users' on-disk configs decode unchanged across the
  package move. A `WidgetAppearanceSerializationTest` enforces this.
- **`render/`** — `WidgetAppearanceRenderer` + `WidgetIconTint` +
  `RemoteViewsExt` (`playTapPressFrame`, `hasPressFrame`,
  `PRESS_FRAME_MILLIS`) + the `WidgetIconResolver` interface every feature
  catalog implements. The renderer is generic; resource refs use the
  kit's R; feature layouts reference `@id/widget_background` and
  `@id/widget_icon` (no `+` — kit's `values/ids.xml` declares them).
- **`feedback/`** — `WidgetFeedbackDispatcher` + per-feature
  `WidgetFeedbackConfig` (channel id / display name / description / small
  icon / notification-id base). Channel id pinned to legacy
  `"widget_feedback"` so system-settings overrides users already set
  carry across the migration.
- **Per-feature multibinding contract (renderer + dispatcher).** Both
  `WidgetAppearanceRenderer` (consuming `WidgetIconResolver`) and
  `WidgetFeedbackDispatcher` (consuming `WidgetFeedbackConfig`) are **one
  app-wide `@Singleton`** serving every feature via a
  `Map<String, X>` multibinding keyed by the feature's stable id (the
  same `<Feature>BootRearmHandler.FEATURE_ID` used for the boot-rearm
  + automation maps). Every widget-bearing feature MUST bind both as
  `@Binds/@Provides @IntoMap @StringKey(FEATURE_ID)` and have its
  `BaseGadgetWidgetProvider` subclass override `featureId` so
  `renderer.apply(…, featureId)` / `dispatcher.dispatch(…, featureId)`
  select the right entry. **Why a map and not one shared resolver:** icon
  keys (`default_active`, `custom:…`) are shared constants, so a single
  resolver would resolve to the wrong feature's drawables — the feature
  id, not the key, picks the catalog. Binding either as a **bare**
  `@Binds @Singleton X` works for the *first* feature but a second bare
  bind is a `[Dagger/DuplicateBindings]` clash in `SingletonC` (torch
  shipped bare; vibration, the second consumer, forced the migration).
  Torch + vibration are the reference.
- **`store/`** — `WidgetConfigStore<T : WidgetKitConfig>` (hot-StateFlow
  cache + `Migrator<T>` seam) replaces every per-feature
  `<Feature>WidgetConfigRepository`. Bind once per feature from the
  feature's Hilt module.
- **`pin/`** — `PendingWidgetConfigs<T>` (generic DataStore-backed
  bridge with **monotonic-counter keying under a `Mutex`** — replaces the
  legacy collision-prone `token.hashCode().absoluteValue`) +
  `BaseWidgetPinSuccessReceiver<T>` abstract base. Feature receivers
  subclass + plug in the action / extra-key / EntryPoint accessors.
  Also exposes `claimSolePending(predicate)` — the **broken-callback
  recovery seam**: `requestPinAppWidget`'s success `PendingIntent` is
  optional and silently never fires on some OEM launchers, so a first-pin
  config would otherwise be stranded in the bridge while the placed widget
  self-heals a blank default (the bug where a strobe widget lost its Morse
  setting until manually re-edited). It pops the **sole** unclaimed entry
  matching the predicate — deliberately **defers (returns null) when 2+
  match**, since without the callback's token a specific `appWidgetId`
  can't be correlated to a specific pending entry and guessing would
  **swap** two same-type widgets' configs. Idempotent against `claim`
  (both delete under the same mutex).
- **`provider/`** — `BaseGadgetWidgetProvider<T : WidgetKitConfig>`
  capturing the `onUpdate` / `onDeleted` / `renderAll` / post-tap chain
  every feature provider used to copy. Feature subclasses only own the
  Hilt EntryPoint shape + `buildRemoteViews` + the synchronous
  feature-specific part of `onReceive`. `renderAll`'s missing-config
  self-heal first calls the overridable `reconcilePendingConfig(context)`
  hook (default null) — torch's providers override it to
  `claimSolePending { it.type == … }` so a freshly-pinned widget recovers
  its real config even when the OS callback never fires; the rescued config
  is written with `saveIfAbsent` so a racing authoritative callback `save`
  still wins. Monitor / chart providers do **not** follow this pattern
  (they read a shared metric config, not a per-`appWidgetId`
  `WidgetKitConfig`) and stay as standalone `AppWidgetProvider`s.
- **`provider/` (content/launcher archetype)** — `BaseContentWidgetProvider<T :
  WidgetKitConfig>` is the **second** archetype alongside the function-driven
  `BaseGadgetWidgetProvider`. A content widget **renders dynamic content**
  (a live preview painted from the feature's own data) and, on tap,
  **launches an Activity** rather than dispatching a `WidgetFunction` — so it
  has no function / feedback / toggle / active-state machinery. The base owns
  the `onUpdate` / `onDeleted` / `onAppWidgetOptionsChanged` lifecycle +
  `renderAll` (config read, `saveIfAbsent` self-heal, `reconcilePendingConfig`
  rescue, adaptive density) + a `launchPendingIntent` tap helper; `buildRemoteViews`
  is **suspend** (content widgets load their preview from the data layer). The
  feature subclass supplies `buildRemoteViews` / `launchIntent` / `sizePresetOf`
  / `defaultConfig`. Content-source → repaint is driven by `ContentWidgetUpdater
  .requestUpdate(context, providerClass)` (an explicit `ACTION_APPWIDGET_UPDATE`
  self-broadcast) from a feature `@Singleton` observer — the analogue of the
  monitoring widget-notifier seam. **Reference consumer:** `:feature:apps`'s
  `FolderWidgetProvider` (the App-Organizer folder widget — renders a folder
  cover / app-preview grid, opens the floating `FolderPopupActivity` on tap),
  with `FolderWidgetController` driving repaints and `FolderWidgetConfigActivity`
  / `PinFolderHelper` the two placement paths (tray-drop configure + in-app pin).
- **`boot/`** — `BootCompletedReceiver` + `BootRearmHandler` `fun
  interface`. Features bind a `BootRearmHandler` into a
  `Map<FeatureId, BootRearmHandler>` Hilt multibinding; the kit
  receiver iterates each handler under one `goAsync` coroutine. Torch
  uses this to rearm `MonitorService` iff (a) a monitor widget is
  placed and (b) monitoring is enabled.
- **`ui/`** — `WidgetAppearancePreview` (the in-app live mock of the
  RemoteViews-rendered widget). `GadgetColorPicker` moved to `:core:ui`
  (P1-10). **Two customization sheets, one per archetype**:
  `WidgetCustomizationSheet` (function-driven: name → function → params → size
  → `WidgetAppearanceSection` → preview) and `ContentWidgetCustomizationSheet`
  (content/launcher: name → content slot → background → accent/content tint →
  label + size → preview slot; reuses the shared `WidgetAppearance.background`/
  `solidColor` + `WidgetAppearanceRenderer.applyBackground` paint path, so
  chrome is identical across archetypes, with no shared-type serialization
  change). A new widget builds on the sheet matching its archetype rather than
  hand-rolling a config screen — folder is the content reference. Design +
  remaining slices (tap-animation for content widgets): `docs/widgets/content-widget-customization.md`.

**Function-driven widgets (the comprehensive customization model).** A
widget no longer hardcodes its action in its provider class. Each per-instance
config stores `{ displayName, actionKey, params: Map<String,String>,
sizePreset, appearance }`; `actionKey` names a **`WidgetFunction`**
(`:core:widgetkit` `function/`) the user picks in the **single** comprehensive
`WidgetCustomizationSheet` (name → function picker → auto-generated param
editor from each function's `ActionParam` schema → size → appearance/preview).
A tap resolves the bound function and dispatches it through
`WidgetFunctionDispatcher` → `:core:automation`'s `ModuleActionRegistry`, so a
widget runs the *same* actions as in-app controls (and feeds the same
runtime/monitoring). Functions are **Toggle** (two paired actions + a live
`WidgetStateSource` keyed `"<featureId>:<stateKey>"`, drives the active/inactive
icon swap, reports on/off) or **Momentary** (one action, resting icon + press
frame, reports "triggered"). `BaseGadgetWidgetProvider` is now generic over the
function — it owns the whole tap→dispatch→feedback→repaint chain plus adaptive
density (`onAppWidgetOptionsChanged` + resizable info-XML + a
`@id/widget_label` shown at larger sizes); a feature provides only
`resolveFunction`/`paramsOf`/`sizePresetOf`/`buildRemoteViews`. Each feature has
ONE designated new-pin provider (torch→`FlashlightWidgetProvider`,
vibration→`VibrateWidgetProvider`); the other per-type provider classes stay
registered only to keep already-placed legacy widgets alive, and a
`Migrator<T>` folds the old `type`-based v1 config into v2 (`schemaVersion=2`;
v1 fields kept as decode-only `@Deprecated` carriers because `sharedJson` uses
`ignoreUnknownKeys`). Rooted functions are flavor-filtered out of the picker on
standard. Feedback is `WidgetFeedbackState` (Toggle/Triggered/Failed) — the fix
for the vibration widget's misleading always-"off" toast. Torch + vibration are
the reference consumers.

**Appearance editor (kit-generic).** The shared appearance editor —
`WidgetAppearanceSection` (`:core:widgetkit/ui`) — owns every shared control
(background/tint/tap-animation/feedback chip rows, the icon picker +
custom-import flow, and the live `WidgetAppearancePreview`) and is rendered
*inside* the generic `WidgetCustomizationSheet`. It takes a generic
`appearance` + `onAppearanceChange` plus the per-feature seam params
`iconChoices: List<WidgetIconChoice>`, `resolveIcon`, and `onImportCustomIcon`;
the ~40 shared labels live in the kit (`widget_kit_appearance_*` in
`core/widgetkit/.../res/values/strings.xml`) and `WidgetIconChoice`
(`:core:widgetkit/config`) is the generic swatch type. With the function-driven
model above, each feature's `ui/WidgetConfigurationSheet.kt` is now a **thin
mapping shell** — it only maps the feature config in/out of the kit dialog. The
old feature-specific fields (torch's strobe rate / morse, vibration's amplitude
/ duration) are **gone**, replaced by params auto-generated from each function's
`ActionParam` schema. Torch + vibration are the reference consumers; future
widget-bearing features build their config sheet the same way.

**The "remove-but-keep-inert" widget pattern** (also documented in the
migration guide): a non-host app can't pull a placed widget off a
third-party launcher, so an in-app delete sets `removed = true` on the
config instead of deleting it (the provider's self-heal would otherwise
recreate it on next `onUpdate`). The placed widget then repaints inert
(dimmed icon, click target cleared) until the user drags it off — at which
point `onDeleted` purges the config for real. Every future widget-bearing
feature must honour this pattern; `TorchWidgetConfig.removed` is the
reference.

---

## Automation contract (`:core:automation`)

The per-module **action** surface the final automation tool drives modules
through. `:core:automation` holds **only the contract**; the rule model,
condition-tree evaluator, scheduler, and builder UI are the final
`:feature:automation-ui` feature (deferred — every module satisfies the
contract first).
```kotlin
interface ActionHandler {
    val featureId: String
    val actions: List<ModuleAction>   // metadata: key, label, requiresRoot, params
    suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult
}
```
- A feature binds one handler: `@Binds @IntoMap @StringKey("<featureId>")
  … : ActionHandler`. `ModuleActionRegistry` (injects the map) is what the
  automation engine enumerates + dispatches through — **no central
  hardcoding** (the fix for legacy `Link`'s hardcoded `LinkActionType`).
- Reuse the feature's existing controllers/services in `dispatch` rather
  than re-implementing hardware control. Reference: `TorchActionHandler`
  (torch on/off, strobe start/stop, morse) delegating to `TorchController`
  + `StrobeService`.
- Contract types are plain (no `@Immutable` — `:core:automation` is not a
  Compose module).

---

## Module Authoring Contract — the migration checklist

> **Every** legacy→modular actuator/sensor migration (torch was the
> first of many) MUST satisfy this so the final feature — a combined
> **automation + monitoring** tool, the modular successor to the legacy
> `Link` module — can discover and drive the module with **zero central
> hardcoding**. Treat this as the acceptance checklist for a new feature
> module. Torch is the worked example of every item; **`:feature:vibration` is
> the validated second consumer** (same contract, a non-pollable actuator
> signal + a 4-capability rooted tier + a freehand draw-canvas pattern builder),
> so torch + vibration are the canonical blueprint pair. New seam additions
> should keep both green.

### Vibration migration notes (second blueprint consumer)

`:feature:vibration` (+ `-rooted` / `-standard`) mirrors torch 1:1. Two
worth-knowing specifics:

- **Modelled poll signal.** Vibration is fire-and-forget — the OS exposes no
  "currently vibrating at X%" query. `VibrationRuntime` (`@Singleton`,
  mirror of `StrobeRuntime`) holds the commanded amplitude + a replace-on-new
  decay coroutine that zeroes it after the command duration;
  `VibrationMetricSource` polls it (`stream()`=null) → a filled plateau that
  decays to 0. This is the reference answer for the next non-pollable
  (sensor) signals.
- **Legacy controller + getter retired (94-A, issue #94).** The legacy `:app`
  `com.gadget.vibration.*` controller tree (interface + result + `PwmPulse`,
  the standard/rooted impls, and the sysfs/driver helpers), the orphaned
  `VibrationRootExtrasSection` card, both flavors' `RootBindings` vibration
  entries, and the `RootFeaturesEntryPoint.vibrationController()` getter are now
  **deleted** — the modular `:feature:vibration` (+ `-rooted`/`-standard`) fully
  supersedes them. Distinct FQNs from the modular
  `dev.ranzlappen.gadget.feature.vibration.*` meant **no entry-point getter
  rename was required** (unlike torch's sysfs-controller case). This is the
  first executed slice of the `RootFeaturesEntryPoint` retirement; the remaining
  features (torch sysfs next) follow the same shape — see
  `docs/refactor-2026/issue-94-root-entrypoint-retirement.md`.

1. **Design system** — every token from `LocalGadgetTheme.current`; no
   raw `dp` at call sites (per-file `Defaults` for fixed sizes); modifier
   after required params, composable slots last; single-line +
   `Ellipsis` text by default; the a11y contract (required
   `contentDescription`, `defaultMinSize(48.dp)`, reduced-motion /
   reduced-transparency).
2. **Screen shell** — build on `ModuleScreenScaffold` with a `ModuleInfo`
   (permissions / OS compatibility / firmware) **and** a tri-state
   per-function `ModuleCapabilitiesSection` (green/amber/red, grant /
   open-settings actions) covering **standard and rooted** functions.
3. **Rooted seam** — feature-side capability interface; app-flavor no-op
   (standard) + real (rooted) bindings reusing the legacy rooted
   controller, gated by `RootSafetyGate` + a `RootFeatureKey`. Never
   branch on `BuildConfig.IS_ROOTED`; never import su under `src/main`.
4. **Reuse, don't reinvent** — `GadgetCircleControl`, `DashCard` /
   `CompactCard` / `GlassSurface`, `GadgetSlider`, the tri-state
   `GadgetStatusKind` badges, `MonitorContainer` / `MonitorChart`. Promote
   a genuinely-reusable new primitive into `:core:ui` (as `GadgetCircleControl`
   was) rather than leaving it feature-private.
5. **Widgets + notifications** (when the feature has them) — AppWidget
   provider + pin/launcher flow + **RemoteViews-safe** (`@RemoteView`)
   layouts only; determinate-progress FGS notification; atomic
   `saveIfAbsent` self-heal; inert "removed" handling on in-app delete;
   custom-icon import (read-once + EXIF + `GetContent`). **Pin
   reliability is mandatory, not optional**: the in-app pin's
   success callback MUST be `FLAG_MUTABLE` + explicit `ComponentName`,
   the pre-pin config MUST ride the `PendingWidgetConfigs` bridge, and
   the provider MUST override `reconcilePendingConfig` with
   `claimSolePending` (see the pin-reliability pitfall below — skipping
   either half ships a widget that pins blank + inert). Patterns in
   `:feature:torch/widget`.
6. **Monitoring-ready** — implement a `MetricSource` per readable signal
   and bind it `@IntoMap`; embed `MonitorContainer` for the **persisted**
   history chart and `LiveMonitorContainer` for the **live** realtime chart
   (the two are independent — both read the same `MetricSource`); history
   persists through `:core:data`. The same `MetricSource` feeds automation
   triggers later — define signals once. Make each card collapsible
   (`GadgetExpandableCard` + persisted `CollapseStateRepository`; torch is
   the reference).
7. **Automation-ready** — expose invocable actions via an `ActionHandler`
   (with `ModuleAction` metadata + param schema + `requiresRoot`) bound
   `@IntoMap`. The future automation engine resolves both maps
   (`MetricSource` for triggers/conditions, `ActionHandler` for actions)
   from Hilt and drives the module without importing it. **This is the
   non-negotiable end-state requirement**: a module that isn't both
   monitoring- and automation-ready is not "done".
8. **Tests + previews + CI traps** — unit tests for serialization /
   repos; instrumented tests for the stateless screen (kept Hilt-free);
   the `@Preview` matrix policy (LightDark + LargeFont + RTL always,
   SizeClasses for layout-driven components); and the CI-only pitfalls
   below (no Android SDK locally → CI compiles, device verifies).

---

## Batch 1.1 status checklist

Live status of the **Hardening, Responsiveness, Theming &
Self-Driving Documentation** batch:

- [x] **1.1.0** — `CLAUDE.md` SSOT foundation rewrite
- [x] **1.1.1** — `LocalGadgetTheme` full wiring (closes #90)
- [x] **1.1.2** — Accessibility semantics sweep
- [x] **1.1.3** — `WindowSizeClass`-aware shell
- [x] **1.1.4** — Glass consistency for Secondary button
- [x] **1.1.5** — Shimmer width polish (BoxWithConstraints-aware sweep)
- [x] **1.1.6** — `@Preview` matrix expansion (RTL / LargeFont / SizeClasses)
- [x] **1.1.7** — Final status refresh + catalog verification

**Batch 1.1 is shipped end-to-end.** Eight commits stacked on PR #88:

| Sub-batch | Commit | One-line |
|---|---|---|
| 1.1.0 | `27a8d86` | `CLAUDE.md` SSOT foundation rewrite |
| 1.1.1 | `3fcdef1` | Full `LocalGadgetTheme` wiring (closes #90) |
| 1.1.2 | `e490b1e` | A11y semantics sweep |
| 1.1.3 | `3585986` | `WindowSizeClass`-aware shell + `GadgetLayoutMode` |
| 1.1.4 | `eb41adf` | Glass consistency for `GadgetSecondaryButton` |
| 1.1.5 | `f4bc3da` | Shimmer `BoxWithConstraints`-aware width |
| 1.1.6 | `01cd935` | Preview matrix expansion (RTL / LargeFont / SizeClasses) |
| 1.1.7 | `01cd935`+ | Catalog verification + status refresh |

### Torch blueprint final-polish batch

A short series of commits closing the last items from three external reviews so
torch is the hardened reference for every future migration:

- **Perf** — `MonitorChartBitmapRenderer` now allocates `RGB_565` (half the
  heap + Binder payload of `ARGB_8888`) from a size-keyed `BitmapPool`
  (`:core:monitoring`), and `MonitorChartWidgetProvider` releases each bitmap
  back after `updateAppWidget`. Eliminates the per-repaint allocation churn.
- **Concurrency** — `PendingWidgetConfigs.purgeStale` now runs under the same
  `enqueueMutex` as the key allocator, closing the counter-key race.
- **Naming** — the privileged sysfs surface `LegacyTorchController` →
  `TorchSysfsController` (package `…torch.sysfs`), `LegacyStandardTorchController`
  → `StandardTorchSysfsController`. "Legacy" framing dropped — it is the current
  rooted-tier contract, not deprecated code. (The transitional
  `RootFeaturesEntryPoint.torchSysfsController()` getter — and the orphaned
  `TorchRootExtrasSection` card that was its only consumer — were later removed
  in 94-B once the modular `RootedTorchRootCapabilities` injected the controller
  directly; see issue #94.)
- **Storage** — custom widget icons save as `WEBP_LOSSY` (API 30+, q80) instead
  of PNG@100.
- **Docs** — "flawless precedent" → "hardened reference implementation"; the
  migration guide's `:core:widgetkit` row reflects the already-extracted
  appearance/render/store layers; new **minimal-vs-advanced module template**
  section so simple features aren't overbuilt into torches.
- **Safety** — every rooted sysfs write is gated (`RootSafetyGate` +
  `RootFeatureKey`), brightness is hard-capped at 150 %, thermal override has a
  45 s absolute ceiling, and both thermal + strobe restore device state in a
  `NonCancellable finally`. Hardened further: the thermal monitor now **cancels
  the privileged block immediately** on a trip-point breach (the block's own
  `NonCancellable` LED-off cleanup still runs) instead of waiting for the
  timeout — the LED stops the instant the zone gets hot.
- **External-state widget refresh** — `TorchWidgetStateObserver` repaints placed
  flashlight widgets when the torch is toggled from outside the widget (system
  QS tile, other apps, OEM gestures) by watching `TorchController.state`
  (already fed by `CameraManager.TorchCallback`). Lazily + idempotently armed
  from the provider's `onReceive`; `distinctUntilChanged().drop(1)` + empty-id
  early-return keep an idle/widget-less app at zero cost.
- **Flavor-seam symmetry (E2)** — the standard no-ops moved out of
  `app/src/standard` into a new `:feature:torch-standard` module (mirror of
  `:feature:torch-rooted`), bound by `StandardTorchModule` and pulled in via
  `standardImplementation`. Neither flavor's Torch impls live in `:app` now.
- **Rooted-tool parameter controls** — the four rooted tools used to fire
  hardcoded one-tap presets (no way to tune them — the settings were
  "nowhere to be found"). They now read a persisted `TorchRootToolsConfig`
  (`RootToolsConfigRepository`, single-record DataStore mirroring
  `MonitorConfigRepository`) edited via sliders + an include-screen toggle in
  `RootToolsCard`, committed on slider release (the `pendingRateHz`
  optimistic-commit pattern). The boost-brightness slider's max is the **live**
  `maxBrightnessPercentFlow` ceiling (hidden entirely when the device has no
  boost headroom — a 100..100 range would NaN the M3 `Slider`), and the
  duration sliders render seconds while storing ms (with a matching
  `valueParser` so the editable field round-trips). `coercedTo(ceiling)` clamps
  every run to the live hardware limit + the 45 s thermal ceiling.
- **First-pin config reliability** — see the `:core:widgetkit` `pin/` +
  `provider/` notes above (`claimSolePending` + `reconcilePendingConfig`):
  a strobe widget pinned with Morse on a flaky-callback launcher now reliably
  plays Morse on the first tap instead of only after a manual re-edit.
- **Rooted opt-in UI re-wired** — the rooted tools toasted "turned off in
  settings" with no reachable way to enable them: the opt-in UI
  (`RootedFeatureTogglesCard` — safety-mode master switch + per-feature
  toggles + risk-confirm dialog) was **orphaned** by the modular refactor
  (it lives in `:app/src/main` because it depends on the legacy
  `RootFeaturesEntryPoint` + 22 controllers, and the new modular
  `:feature:settings` `SettingsScreen` never placed it). Fixed by adding a
  `rootFeatureToggles: @Composable () -> Unit = {}` **slot** to
  `SettingsScreen` / `settingsScreen()` that `:app` fills with
  `RootedFeatureTogglesCard()` (the card self-hides on standard / no-root via
  its `hasRootAccess()` guard, so the slot stays Hilt-free + flavor-safe), and
  an `onNavigateToSettings` callback on `torchScreen()` so the `OptedOut`
  snackbar offers a **"Settings"** action that deep-links to the toggles. The
  gate is two-stage: a global **Safety mode** master switch (default ON, blocks
  every `isWriteCapable` feature) **and** each feature's own toggle (default
  OFF, `requiresExplicitConfirm`). This is the seam every future rooted feature
  (vibration next) reuses; the slot pattern is the leaf-module-can't-see-`:app`
  workaround until `RootFeaturesEntryPoint` is replaced by per-feature
  `@Inject` (issue #94).

### Preview matrix policy

Every component file that ships a public composable also ships at
least one `@Composable` preview function. The annotation stack
follows this policy:

- **Always**: `@GadgetPreviewLightDark` + `@GadgetPreviewLargeFont`
  + `@GadgetPreviewRtl`. Ensures every component is exercised on
  the dark + light theme, at 200 % font scale, and under RTL
  locale.
- **For layout-driven components** (cards, list rows, empty states,
  shimmer skeletons): add `@GadgetPreviewSizeClasses` so the
  preview pane renders at Compact / Medium / Expanded widths. Skip
  for components that don't change with width (buttons, chips,
  badges, text fields, dots).

Multi-preview annotations are defined in
`core/ui/preview/GadgetPreviewMatrix.kt`.

Open follow-up issues (Phase 2+ pickup):
- [#89](https://github.com/Ranzlappen/HardwareDash/issues/89) —
  `material3-adaptive` foldable hinge utility — **addressed**:
  `GadgetPosture` / `rememberPosture()` (see rule #14).
- [#91](https://github.com/Ranzlappen/HardwareDash/issues/91) —
  `GadgetBottomSheet` instrumented tests — **addressed**: covered in
  `core/ui`'s `ModalsTest` (the ui-test-manifest activity hosts the
  sheet; no bespoke host activity needed).
- [#92](https://github.com/Ranzlappen/HardwareDash/issues/92) —
  CI emulator workflow for `connectedDebugAndroidTest` — **addressed**:
  `.github/workflows/instrumented-tests.yml`.

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
- **Compose-only annotations in non-Compose modules.**
  `:core:datastore` (and other foundation modules) deliberately
  don't pull in `androidx.compose.runtime`. `@Immutable` /
  `@Stable` won't resolve there. Compose treats `data class`es and
  enums of stable members as stable automatically — drop the
  annotation rather than adding the Compose dep to the foundation
  module.
- **`lifecycle-runtime-compose` vs `lifecycle-runtime-ktx`.**
  `collectAsStateWithLifecycle()` lives in
  `androidx.lifecycle:lifecycle-runtime-compose`, **not** in the
  `lifecycle-runtime-ktx` artifact that the feature convention
  plugin (`gadget.android.feature`) brings in by default.
  Two valid fixes:
  1. Add `androidx-lifecycle-runtime-compose` to `libs.versions.toml`
     and to the feature convention plugin's default `implementation`
     set.
  2. Use `androidx.compose.runtime.collectAsState()` instead — fine
     when the flow is already `stateIn(…)`-backed (the ViewModel is
     still keeping it hot), which describes most feature ViewModels
     in this repo.
- **Library `BuildConfig.VERSION_NAME` / `VERSION_CODE`.** Only
  `:app`'s `defaultConfig` generates these. A feature module's
  generated `BuildConfig` carries `BUILD_TYPE` + `DEBUG` only.
  When a feature card needs the version (the Settings About card,
  for example), add `buildConfigField` entries to the feature's
  `build.gradle.kts` that re-derive both values from the same
  `CI_VERSION_NAME` / `CI_VERSION_CODE` gradle properties `:app`
  reads:
  ```kotlin
  defaultConfig {
      val ciVersionName = providers.gradleProperty("CI_VERSION_NAME").getOrElse("1.0-dev")
      val ciVersionCode = providers.gradleProperty("CI_VERSION_CODE").orNull?.toInt() ?: 1
      buildConfigField("String", "VERSION_NAME", "\"$ciVersionName\"")
      buildConfigField("int", "VERSION_CODE", ciVersionCode.toString())
  }
  ```
- **Adding a value to `GadgetDestination`** (or any other sealed /
  enum hierarchy used in non-exhaustive `when` branches). After
  adding the value, grep for `when (destination)` /
  `when (it: GadgetDestination)` across `:core:navigation`,
  `:app`, and every feature module and add the missing branch.
  `:core:navigation`'s `ComingSoonScreen` was the canonical site
  the first time around; expect more as feature modules grow.
- **`init` block forward-reference to a property.** Kotlin runs
  `init` blocks and property initializers in **declaration order**.
  An `init` block that reads or registers a property declared
  *below* it sees an uninitialized field (`Variable 'foo' must be
  initialized`). Move the property declaration **above** the `init`
  block, or move the registration logic into the property's
  initializer expression. The first migrated controller
  (`StandardTorchController`'s `TorchCallback` registration) tripped
  on this.
- **Hilt entry-point method name collisions.** Hilt synthesises a
  single `SingletonC` that implements every `@EntryPoint` interface
  in the graph. Two entry points with a getter of the **same name**
  but **different return types** (e.g. legacy
  `com.gadget.torch.TorchController` vs new modular
  `dev.ranzlappen.gadget.feature.torch.TorchController`) fail
  `hiltJavaCompile*` with `Found conflicting entry point
  declarations`. When you migrate a controller, the legacy entry
  point's getter must be renamed (`legacyXController()`) until the
  legacy implementation is deleted — the new modular feature keeps
  the idiomatic `xController()` name.
- **App-widget layouts are `RemoteViews` — only `@RemoteView`
  classes inflate.** Home-screen widget layouts (the
  `initialLayout` / `previewLayout` referenced from an
  `<appwidget-provider>`) are inflated by the launcher as
  `RemoteViews`, which accepts **only** the framework's
  `@RemoteView`-annotated classes: `FrameLayout`, `LinearLayout`,
  `RelativeLayout`, `GridLayout`, `ImageView`, `ImageButton`,
  `TextView`, `Button`, `ProgressBar`, `Chronometer`, `AnalogClock`,
  the adapter views (`ListView` / `GridView` / `StackView` /
  `ViewFlipper` / `AdapterViewFlipper`), and `ViewStub`. A bare
  `android.view.View` (or `Space`) is **not** on that list — it
  throws `InflateException` at inflation time and the launcher shows
  **"Couldn't add widget"** in the picker and on the home screen. For
  a background/surface element use `ImageView` or `FrameLayout`
  (both support `setBackgroundResource`), never a bare `<View>`. This
  is CI-invisible (no Android SDK locally, and the layout compiles
  fine — it only fails at runtime inside the launcher process); it
  broke the torch widget migration. `:feature:torch`'s
  `widget_flashlight.xml` / `widget_strobe.xml` are the reference
  RemoteViews-safe layouts for follow-up modules and legacy
  migrations to copy.
- **`requestPinAppWidget` success callback MUST be `FLAG_MUTABLE`, and
  the pin flow MUST use the kit's pending-config rescue.** The OS
  delivers the newly-assigned `appWidgetId` to the in-app pin flow by
  **filling it into** the success-callback `PendingIntent`'s intent
  (`EXTRA_APPWIDGET_ID`). A `FLAG_IMMUTABLE` callback **silently drops
  that fill-in** — the receiver reads `INVALID_APPWIDGET_ID`, bails,
  and never persists the per-`appWidgetId` `WidgetKitConfig`. The placed
  widget then self-heals to its default config, so it renders blank /
  inert **and** (for a content widget) returns before wiring its tap
  `PendingIntent` — i.e. "the widget pinned but the picture didn't
  apply and tapping does nothing". This is CI-invisible (runs only
  inside the launcher round-trip on a device) and has bitten widget
  work **more than once**. Two non-negotiable halves, both required for
  **every** widget-bearing feature (function-driven *and* content
  archetypes):
  1. Build the success-callback `PendingIntent` with
     `FLAG_UPDATE_CURRENT or FLAG_MUTABLE` and an **explicit
     `ComponentName`** (mutability is safe because the explicit
     component can't be hijacked; the flag is ignored < API 31, mutable
     by default there, so minSdk 29 is unaffected).
  2. Carry the pre-pin config through the `:core:widgetkit`
     **`PendingWidgetConfigs`** bridge (token in the callback) **and**
     override **`reconcilePendingConfig`** on the provider to
     `claimSolePending { … }` — so a pin still binds correctly even on
     the OEM launchers that never fire the callback at all (or fire it
     into a freshly-spawned process after low-RAM death). The two
     layers are complementary: the mutable callback gives a precise
     `appWidgetId ↔ config` binding on compliant launchers; the
     sole-pending rescue is the fallback when the callback is missing.
  Torch (`TorchWidgetCreator` + `WidgetPinSuccessReceiver` +
  `FlashlightWidgetProvider.reconcilePendingConfig`) is the reference;
  the App-Organizer folder widget regressed by shipping an immutable
  callback **and** skipping the rescue (both symptoms above) and was
  fixed to match (PR #138 — `PinFolderHelper` + `FolderWidgetPinReceiver`
  + `FolderWidgetProvider.reconcilePendingConfig`). The configure-activity
  (tray-drop) path is exempt from half 1 — it gets the `appWidgetId`
  straight from the launcher's `APPWIDGET_CONFIGURE` intent — but should
  still write through the same store the rescue reads.
- **`android.nonTransitiveRClass=true` + cross-module resource
  references.** With `nonTransitiveRClass=true` (set in
  `gradle.properties`), each module's `R` class contains only its
  **own** resources. Kotlin references like `R.id.widget_icon` in a
  feature module compile against the feature's `R` — and fail with
  `Unresolved reference` if the id actually lives in a dep module's
  `R`. **XML** references (`@drawable/widget_background_glass` in a
  layout) keep working because AAPT2's resource merger fuses the
  pool. **Kotlin** references must qualify the dep's R class
  explicitly. Pattern: `import dev.ranzlappen.gadget.core.widgetkit.R
  as WidgetKitR` + `WidgetKitR.id.widget_icon`. Broke C2; see
  `feature/torch/.../widget/{Flashlight,Strobe}WidgetProvider.kt` for
  the reference fix.
- **kotlinx-serialization polymorphic discriminator drift on
  package moves.** `@Serializable sealed class` subtypes encode their
  type discriminator (default JSON key `"type"`) as the subtype's
  fully-qualified name. Moving a sealed root or its subtypes to a
  new package silently breaks decoding of every existing user's
  persisted record — the wire string changes. Two recovery paths:
  pin every subtype with explicit `@SerialName("<legacy.FQN.path>")`
  so the wire string stays put (rock-solid, ugly magic string), OR
  bump the persisted record's `schemaVersion` and write a `Migrator
  <T>` that rewrites the discriminator on read. Phase 2 / C1 used
  the pin for `ToggleFeedback`; `:core:widgetkit`'s
  `WidgetAppearanceSerializationTest` regression-tests it.
- **Companion-object `@Provides` on an abstract `@Module`** is
  fragile across Hilt / KSP versions — some configurations silently
  skip the `@Provides`, leaving the binding unresolved and the
  whole graph red. Convention everywhere else in this repo
  (`DataModule`, `DataStoreModule`, …) is a top-level `object`
  module for `@Provides` + a separate abstract class for `@Binds`.
  Stick to it. See `feature/torch/.../di/TorchProvidesModule.kt`
  for the split.
- **Room schema-export 0-byte file.** A 0-byte `.gitkeep` (or any
  unparseable file) in the configured `room.schemaLocation` causes
  Room's KSP processor to throw
  `IllegalStateException("Empty schema file")` from
  `SchemaBundle.deserialize`. The processor iterates every file in
  the dir; it doesn't filter by `.json` extension on every version.
  Use a non-empty placeholder (e.g. delete it and let Room create
  the dir on first export) or commit a real schema. Surfaced + fixed
  on PR #123 in Phase 2 / C7 cycle.
- **Same-package symbol resolution across module-move.** Kotlin
  source can reference any same-package type without an `import`.
  After a module move that lifts an interface out of `package
  com.foo.bar` (now in a new module), every impl that stayed at
  `package com.foo.bar` loses access — the unimported reference
  becomes `error.NonExistentClass` at KSP time. Audit before / after
  moves with `grep -rn "interface X\|: X\b"` and inject an explicit
  `import` to the new home. Phase 2 / D1 hit this on every
  `com.gadget.root.*` flavor impl.

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
   lives in `app/src/rooted/`. **Older guideline** said the two
   directories must share FQNs so Gradle picks one per flavor; the
   refactor-2026 D2 + D3 + E1 batches relaxed that — flavor impls
   now live under modular per-feature packages
   (`dev.ranzlappen.gadget.feature.{standard,rooted}.<feature>.*`)
   with distinct FQNs per flavor. Hilt bindings still pick the right
   impl per flavor because each flavor's `RootBindings.kt` (and the
   per-feature Hilt modules in sibling `:feature:<name>-rooted`
   modules) is itself flavor-scoped and contributes only its impl to
   the build variant.
3. Never branch on `BuildConfig.IS_ROOTED`. Inject
   `RootCapabilityRegistry` / `RootSafetyGate` (from `:core:root` —
   `dev.ranzlappen.gadget.core.root.*`) and let the Hilt seam pick
   the right implementation per flavor.
4. Never put rooted-specific imports (e.g. anything that talks to
   su) under `src/main/`. They belong in `src/rooted/` (or a sibling
   `:feature:<name>-rooted` module wired in via
   `rootedImplementation`) with a no-op twin in `src/standard/`.
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
- Backup: `app/src/main/java/com/gadget/backup/BackupManager.kt` — whole-app
  ZIP (format v4): legacy `gadget_db`, modular `databases/{apps,monitoring}.db`,
  every `shared_prefs/*.xml` + `datastore/*`, and the asset sweeps
  (`folder_covers/`, `apps_favicons/`, `widget_icons/`). WAL must be
  checkpointed via `query("PRAGMA wal_checkpoint(FULL)")`, not `execSQL` —
  `execSQL` rejects statements that return rows. **Legacy backwards-compat**: a
  backup with `gadget_db` but no `databases/apps.db` is a legacy backup —
  restore wipes the current `apps.db` and clears `LegacyAppsImporter`'s
  `apps_migration` SharedPrefs guard so it rebuilds App-Organizer data from the
  restored `gadget_db` on next launch (without the reset an already-imported
  install would silently drop the backup's folders). Restore applies the
  modular DBs only on the next process start, so it prompts a restart. Wired
  into the modular `:feature:settings` via a `backupSection` slot that `:app`
  fills with `BackupCard()` (reaches `BackupManager` through
  `BackupManagerEntryPoint` — the leaf-module-can't-see-`:app` seam). When you
  add a new asset dir under `filesDir`, append it to `filesDirAssetSweeps` and
  bump `BACKUP_FORMAT_VERSION`. Design: `docs/backup.md`.
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
