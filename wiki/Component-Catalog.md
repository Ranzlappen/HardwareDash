# Component Catalog

The exhaustive reference for every public composable in `:core:ui`.
Format per entry: **Signature → When → NOT when → Example → Notes**. The
ruleset behind these (tokens, a11y, responsiveness) is on the
[Design System](Design-System) page.

Component source files (`core/ui/.../component/`): `Buttons.kt`,
`GlassSurface.kt`, `DashCard.kt`, `CompactCard.kt`, `GadgetExpandableCard.kt`,
`GadgetCircleControl.kt`, `TextFields.kt`, `Sliders.kt`, `Modals.kt`,
`StatusIndicators.kt`, `LoadingStates.kt`, `EmptyState.kt`,
`SparklineChart.kt`, `GadgetColorPicker.kt`, `ScreenHeader.kt`,
`SectionHeader.kt`, plus `ModuleScreenScaffold.kt` and `module/`.

---

## Buttons (`component/Buttons.kt`)

### `GadgetPrimaryButton`
```kotlin
fun GadgetPrimaryButton(onClick: () -> Unit, text: String, modifier: Modifier = Modifier,
    enabled: Boolean = true, loading: Boolean = false, singleLine: Boolean = true,
    leadingIcon: ImageVector? = null, trailingIcon: ImageVector? = null,
    contentPadding: PaddingValues = GadgetButtonDefaults.ContentPadding)
```
- **When:** high-emphasis CTA. One per screen at most.
- **NOT when:** secondary actions (Secondary), inline rows (Tertiary),
  icon-only chrome (IconButton).
- **Example:** `GadgetPrimaryButton(onClick = onSave, text = "Save changes")`
- **Notes:** filled `colorScheme.primary`. Press-scale spring honours
  `LocalReducedMotion`. `loading = true` swaps the label for a spinner and
  suppresses clicks.

### `GadgetSecondaryButton`
Same signature shape as Primary.
- **When:** medium-emphasis sibling to a Primary CTA ("Save" + "Discard").
- **NOT when:** solo destructive actions.
- **Notes:** the **outlined glassy** tier — container paints via
  `Modifier.glassSurface(intensity = Standard)` over a transparent M3
  Surface; hairline outline in `colorScheme.outline`. Glass retunes flow
  through `LocalGadgetTheme.current.glass`.

### `GadgetTertiaryButton`
Same signature shape.
- **When:** low-emphasis inline action — "Learn more", dialog "Cancel".
- **NOT when:** anywhere the affordance must be noticed (ghost buttons
  read as text).
- **Notes:** transparent container, primary-tinted label, ripple on press.

### `GadgetIconButton`
```kotlin
fun GadgetIconButton(onClick: () -> Unit, icon: ImageVector, contentDescription: String?,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface)
```
- **When:** chrome / toolbar actions where a label is redundant.
- **NOT when:** the icon is ambiguous — add a sibling label.
- **A11y:** `contentDescription` required; `null` only with a labelling
  sibling. 48 dp hit target.

### `GadgetFab`
```kotlin
fun GadgetFab(onClick: () -> Unit, icon: ImageVector, contentDescription: String?,
    modifier: Modifier = Modifier, text: String? = null, enabled: Boolean = true,
    containerColor: Color = …primaryContainer, contentColor: Color = …onPrimaryContainer)
```
- **When:** the single primary creation action ("Add sensor", "New rule").
- **NOT when:** more than one — use a row of buttons.
- **Notes:** passing `text` upgrades to an Extended FAB. 56 dp circular
  diameter. `contentDescription` required.

### `GadgetCircleControl` (`component/GadgetCircleControl.kt`)
```kotlin
fun GadgetCircleControl(icon: ImageVector, contentDescription: String, caption: String,
    enabled: Boolean, modifier: Modifier = Modifier, active: Boolean = false,
    hero: Boolean = false, onClick: (() -> Unit)? = null, onHold: ((Boolean) -> Unit)? = null)
```
- **When:** a row of identical captioned round controls — Torch's
  toggle / hold / strobe / Morse row is the reference.
- **NOT when:** a single CTA (Primary/Fab) or toolbar chrome (IconButton).
- **Notes:** pass `onClick` for tap **or** `onHold` for press-and-hold
  (`true` on press, `false` on release/cancel via try/finally so it can't
  stick). `hero` = filled primary surface; `active` tints the on-state.
  56 dp target, `contentDescription` required.

## Surfaces

### `GlassSurface` (`component/GlassSurface.kt`)
```kotlin
fun GlassSurface(modifier: Modifier = Modifier, intensity: GlassIntensity = Standard,
    showBorder: Boolean = true, contentPadding: PaddingValues = …,
    onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit)
```
- **When:** low-level glassy container without title/icon chrome — the
  primitive `DashCard`/`CompactCard` build on.
- **Notes:** respects `LocalReducedTransparency` (Standard/Vivid → Subtle).

### `DashCard` (`component/DashCard.kt`)
```kotlin
fun DashCard(modifier: Modifier = Modifier, title: String? = null, icon: ImageVector? = null,
    intensity: GlassIntensity = Standard, onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(GadgetSpacing.Medium), content: @Composable () -> Unit)
```
- **When:** dashboard tiles — vertical layout with optional title + icon
  header above content.
- **NOT when:** list rows (`CompactCard`), bare containers (`GlassSurface`).

### `CompactCard` (`component/CompactCard.kt`)
```kotlin
fun CompactCard(modifier: Modifier = Modifier, title: String? = null, subtitle: String? = null,
    leadingIcon: ImageVector? = null, trailingContent: (@Composable () -> Unit)? = null,
    intensity: GlassIntensity = Subtle, onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = …, singleLineTitle: Boolean = true)
```
- **When:** horizontal list-row glassy cards — settings rows, sensor
  entries.

### `GadgetExpandableCard` (`component/GadgetExpandableCard.kt`)
```kotlin
fun GadgetExpandableCard(title: String, expanded: Boolean, onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier, icon: ImageVector? = null, intensity: GlassIntensity = Standard,
    contentPadding: PaddingValues = PaddingValues(GadgetSpacing.Medium), content: @Composable () -> Unit)
```
- **When:** a collapsible `DashCard` for foldable sections.
- **State:** stateless — `expanded` hoisted. For **persisted** collapse,
  pair with `:core:monitoring`'s `CollapseStateRepository`; for ephemeral,
  a `rememberSaveable`.
- **A11y:** header is one `toggleable` node announcing
  "Expanded"/"Collapsed"; chevron decorative; honours `LocalReducedMotion`.

## Inputs

### `GadgetTextField` / `GadgetSearchField` (`component/TextFields.kt`)
- **TextField:** wraps M3 `OutlinedTextField`; state hoisted; `singleLine
  = true` default with horizontal scroll; opt-in multi-line via
  `singleLine = false` + `maxLines`.
- **SearchField:** IME action = Search; auto-shows a clear button when
  non-empty; delivers `onSearch` on submit.

### `GadgetSlider` (`component/Sliders.kt`)
- **When:** continuous numeric settings (sample interval, brightness,
  amplitude). Wraps M3 `Slider` with theme colours.
- **Notes:** commit-on-release patterns (`pendingX` optimistic commit) are
  the convention for sliders that drive expensive work. A `100..100`
  range NaNs the M3 `Slider` — hide a degenerate slider entirely (the
  rooted boost-brightness slider does this when there's no headroom).

### `GadgetChip` (`component/StatusIndicators.kt`)
- **When:** filter / segmented selection; caller manages selection state.
  Wraps M3 `FilterChip` (which handles `assertIsSelected`).

### `GadgetColorPicker` (`component/GadgetColorPicker.kt`)
- **When:** picking a custom colour (widget accent, folder cover tint).
  Promoted to `:core:ui` from widgetkit (P1-10) so any feature can reuse.

## Modals (`component/Modals.kt`)

### `GadgetBottomSheet`
- **When:** full-bleed action sheets / detail surfaces, dismissible via
  swipe-down or back. **NOT** for simple confirms (use `GadgetDialog`).
- **Notes:** visibility owned by the caller via `SheetState`;
  conditionally place the composable to show/hide. Title carries
  `heading()`. Instrumented coverage in `ModalsTest`.

### `GadgetDialog`
- **When:** confirmations, info acknowledgements, blocking decisions.
  **NOT** for longer flows (use a sheet).
- **Notes:** confirm slot required, dismiss optional; body wraps to
  `bodyMaxLines` (default 10) then truncates. Title carries `heading()`.

## Status & indicators (`component/StatusIndicators.kt`)

### `GadgetBadge`
```kotlin
fun GadgetBadge(modifier: Modifier = Modifier, text: String? = null,
    containerColor: Color = …error, contentColor: Color = …onError,
    stateDescriptionOverride: String? = null)
```
- **When:** counter ("3", "99+") or unread dot anchored via `BadgedBox`.
- **A11y:** `text = null` → decorative dot; non-null → pill announcing
  `"$text unread"` (override via `stateDescriptionOverride`).

### `GadgetStatusDot`
```kotlin
fun GadgetStatusDot(contentDescription: String?, modifier: Modifier = Modifier,
    color: Color = …primary, size: Dp = StatusDotDefaultSize)
```
- **When:** paired with a label for "● Online" / "● Offline".
- **A11y:** `contentDescription` **required**; `null` only with a labelled
  sibling. 8 dp default.

### `GadgetStatusKind`
- The tri-state enum (`Success` → primary/teal, `Warning` →
  tertiary/amber, `Error` → error/red) + `@Composable
  GadgetStatusKind.color()`. **Reuse this anywhere a green/amber/red
  readout is needed; never hand-pick the three colours.**

## Loading (`component/LoadingStates.kt`)

### `GadgetCircularProgress` / `GadgetLinearProgress`
- **When:** in-flight async work. Determinate (`progress = 0f..1f`) when
  known, indeterminate otherwise. Determinate variants publish
  `progressBarRangeInfo`.

### `GadgetShimmerBlock`
- **When:** skeleton placeholders while data loads. **NOT** for a brief /
  single-frame load.
- **Notes:** animated gradient sweep; honours `LocalReducedMotion`
  (degrades to static `surfaceVariant`); sweep width computed via
  `BoxWithConstraints` so it scales from 32 dp avatars to 1024 dp banners;
  announces "Loading" + `liveRegion = Polite`.

## Empty / placeholder (`component/EmptyState.kt`)

### `GadgetEmptyState`
- **When:** "no data yet" / "no results".
- **Example:**
  ```kotlin
  GadgetEmptyState(title = "No sensors yet", subtitle = "Add your first sensor to begin.",
      icon = Icons.Outlined.Sensors,
      action = { GadgetPrimaryButton(onClick = onAdd, text = "Add sensor") })
  ```
- **Notes:** title required; merges into one a11y node. Wrap in
  `Modifier.fillMaxSize` to centre.

## Charts (`component/SparklineChart.kt`)

### `SparklineChart`
- **When:** a small inline trend line inside a card. Hand-drawn `Canvas`
  with cubic-Bézier half-step smoothing (the same smoothing reused by the
  monitoring charts and the widget bitmap renderer). Not Vico.

## Shell / structure

### `GadgetTheme` (`core/designsystem/theme/GadgetTheme.kt`)
- Wrap every Compose entry point. Provides `LocalGadgetTheme` +
  `LocalReducedMotion`.

### `GadgetApp` (`core/navigation/GadgetApp.kt`)
- The top-level shell: nav rail + nav host. Computes `WindowSizeClass`
  once and provides `LocalWindowSizeClass`. Wraps content in `GadgetTheme`.

### `ScreenHeader` / `SectionHeader` / `ModuleScreenScaffold`
- Standard layout primitives. `ModuleScreenScaffold` takes a primary
  content shape (`title` + `functional` + `disclaimer` slots + a
  declarative `moduleInfo`) **plus** an optional `secondaryPane` rendered
  on TwoPane/ThreePane (primary takes `1.5f` on TwoPane, `1f` on
  ThreePane).

## Module blueprint (`module/`)

`ModuleInfo` (`module/ModuleInfo.kt`) makes every feature self-describing
so the scaffold renders consistent metadata:

```kotlin
@Immutable data class ModuleInfo(
    val permissions: List<ModulePermission> = emptyList(),
    val compatibility: OsCompatibility,
    val firmware: FirmwareRequirement? = null,
    // val capabilities: List<ModuleCapability> = …  — tri-state per-function rows
)
```

Build it inside a `@Composable`, resolving `stringResource(…)` at
construction (feature-specific copy lives in the feature's resources).
Pass it as the scaffold's `moduleInfo` and the standard **Permissions → OS
compatibility → Firmware → Capabilities** cards render automatically.

Reusable sections (`module/ModuleInfoSections.kt`):
- `ModulePermissionsSection` — status row per permission + in-app **Grant**
  + **Open app settings**; live grant state refreshed on the callback and
  `ON_RESUME`.
- `ModuleCompatibilitySection` — `minSdk` vs `Build.VERSION.SDK_INT`.
- `ModuleFirmwareSection` — only when `firmware != null`.
- `ModuleCapabilitiesSection` — per-function green/amber/red rows
  (`ModuleCapability` + a `@Composable () -> CapabilityStatus` live check
  resolving to a `GadgetStatusKind` + message + optional `CapabilityAction`).
  Covers standard **and** rooted functions. Torch is the reference.

Reference impl: `feature/torch`'s `torchModuleInfo()`.

### `RootToolsSection` / `RootActionRow` (`module/RootToolsSection.kt`)

The reusable rooted-tools substrate (W6 / #94): a collapsible
`GadgetExpandableCard` a feature screen drops in to surface its dormant
rooted controller's interactive UI behind the root gate. `available` (the
feature's `RootReady` snapshot, resolved through the `:core:root` Hilt seam —
never `BuildConfig.IS_ROOTED`) toggles between the controls and an honest
"requires the rooted app" state. `RootActionRow` renders one labeled action
with an optional description, a run button, and a `GadgetStatusKind`-tinted
status line (the last `*ControllerResult` mapped to a string). Copy is passed
already-resolved (the `ModuleInfo` convention).

Each consumer maps its own `*ControllerResult` sealed type onto the shared
**`RootActionState`** (`module/RootActionState.kt`) holder —
`message` / `isError` / `running`, plus a derived `statusKind` so the row
tint stays consistent — rather than re-declaring a per-feature copy. Live
consumers: `storage`, `diagnostics`, `audio`, `radios-wifi`, `radios-bt`,
`gps`, `battery`, `display`, `radios-cell`, `usbdebug`, `adbdebug` (all
read-only rooted actions), on top of torch/vibration's own rooted cards.

**`RootConfirmActionRow`** (same file) is the **write-tier** variant: a
`RootActionRow` whose run button is gated behind a confirmation `GadgetDialog`
(title / message / confirm+cancel labels), so a device-mutating action can't
fire on a single tap. Live consumers: `microphone` (disable effects), `camera`
(HAL-bypass frame), `notification` (grant listener access / reset overrides),
`radios-ir` and `radios-nfc` (reset mutations). Config-bearing extreme actions
(exposure/sample-rate/NCI-command entry) still await a parameter-entry UI.

### `PermissionsDashboardCard` (`:core:permissions`)

The centralized permissions dashboard (W5) — a self-contained Hilt-injected
`DashCard` the Settings screen drops in via its `permissionsSection` slot.
Scans grant state through `PermissionRegistry` (per-feature `@IntoMap`
`FeaturePermissions` + an app baseline), requests runtime permissions
in-app, deep-links special permissions (overlay / exact-alarm /
WRITE_SETTINGS / notification-listener / all-files via `SpecialPermissions`),
and re-scans on `ON_RESUME`.

---

> _Last reviewed: 2026-07-11 · Source: `CLAUDE.md` (component catalog),
> `core/ui/src/main/.../component/*`, `core/ui/.../module/*`,
> `core/permissions/*` · Related
> module: `:core:ui`._
