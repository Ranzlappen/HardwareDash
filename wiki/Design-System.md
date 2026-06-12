# Design System

Gadget's UI is **dark-first**, **glassy** (Material 3 + a glassmorphism
overlay), and **future-proof for custom themes**. Every design token flows
through `LocalGadgetTheme.current`, so a downstream "compact" or
"high-contrast" theme can override any slot without forking components.

For the full per-component reference (signatures, parameters, examples,
a11y behaviour), see **[Component Catalog](Component-Catalog)**. This page
is the rule set and the rationale.

## Tokens — `LocalGadgetTheme`

The umbrella `LocalGadgetTheme.current` exposes every token bag:

```kotlin
LocalGadgetTheme.current.colors      // ColorScheme
LocalGadgetTheme.current.typography  // Typography
LocalGadgetTheme.current.shapes      // Shapes
LocalGadgetTheme.current.spacing     // GadgetSpacingValues
LocalGadgetTheme.current.motion      // GadgetMotionValues
LocalGadgetTheme.current.glass       // GadgetGlassValues
```

**Rule 1 — Every token comes from `LocalGadgetTheme.current` inside
`@Composable` bodies.** The static `object GadgetSpacing` / `object
GadgetMotion` (in `core/designsystem/tokens/GadgetTokens.kt`) is **only**
the source for default-value `val` initialisers at file scope
(`*Defaults.ContentPadding`, etc.). Never read those statics from inside a
`@Composable` body — read from the local so custom themes flow through.

**Rule 2 — No raw `dp` literals at call sites.** The only exception is a
per-file `Defaults` value class documenting a fixed-size **design token**
(e.g. the 56 dp FAB diameter). Those constants live at the top of their
file with a KDoc explaining why they're allowed.

### Spacing / typography / colour / shape

- **Spacing** — `LocalGadgetTheme.current.spacing.{Small,Medium,Large,…}`.
  Never a raw `dp`.
- **Typography** — `LocalGadgetTheme.current.typography` (M3 type scale,
  themeable).
- **Colour** — `LocalGadgetTheme.current.colors` (M3 `ColorScheme`,
  dark-first). For green/amber/red tri-state readouts use
  `GadgetStatusKind` + `@Composable GadgetStatusKind.color()` — never
  hand-pick three colours.
- **Shape** — `LocalGadgetTheme.current.shapes`.

## Glass surfaces

**Rule 6 — Every glassy surface goes through `GlassSurface` (composable)
or `Modifier.glassSurface()` (extension).** Never reach for a raw
`Surface(color = …)` with a hand-tuned alpha — the glass alphas live in
`LocalGadgetTheme.current.glass` and must stay themeable.

`GlassIntensity` presets: `Subtle` (highest opacity) / `Standard` /
`Vivid`. `Modifier.blur(…)` requires API 31+ — below that the glass
degrades to a higher-opacity solid fallback.

## Cards, buttons, inputs, modals

The component families (full signatures in
[Component Catalog](Component-Catalog)):

- **Buttons** — `GadgetPrimaryButton` (one high-emphasis CTA per screen),
  `GadgetSecondaryButton` (outlined glassy sibling), `GadgetTertiaryButton`
  (ghost/inline), `GadgetIconButton` (chrome), `GadgetFab` (single
  creation action), `GadgetCircleControl` (captioned round controls).
- **Surfaces** — `GlassSurface` (primitive), `DashCard` (titled vertical
  tile), `CompactCard` (horizontal list row), `GadgetExpandableCard`
  (collapsible `DashCard`).
- **Inputs** — `GadgetTextField`, `GadgetSearchField`, `GadgetSlider`,
  `GadgetChip`.
- **Modals** — `GadgetBottomSheet` (full-bleed sheets),
  `GadgetDialog` (confirms / blocking decisions).
- **Status & indicators** — `GadgetBadge`, `GadgetStatusDot`,
  `GadgetChip`, the `GadgetStatusKind` tri-state mapping.
- **Loading** — `GadgetCircularProgress` / `GadgetLinearProgress`,
  `GadgetShimmerBlock`.
- **Empty / placeholder** — `GadgetEmptyState`.
- **Shell / structure** — `GadgetTheme`, `GadgetApp`, `ScreenHeader`,
  `SectionHeader`, `ModuleScreenScaffold`, and the `ModuleInfo` module
  blueprint sections.

## Layout & touch rules

- **Rule 3 — Modifier-first parameter ordering.** `modifier: Modifier =
  Modifier` appears immediately after the required non-`@Composable`
  parameters. `@Composable` content slots go last. No exceptions.
- **Rule 4 — `Modifier.defaultMinSize(48.dp, 48.dp)`** on every tappable
  surface (Material accessibility minimum).
- **Rule 5 — Single-line text + `TextOverflow.Ellipsis` by default** on
  every public text-bearing parameter. Opt in to multi-line via
  `singleLine = false` / `maxLines = N` — never default to wrapping.

## Accessibility rules

- **Rule 7 — Respect `LocalReducedMotion`.** When `true`, suppress
  spring/scale animations (pin target values) and degrade infinite
  transitions (shimmer → static `surfaceVariant`).
- **Rule 8 — Respect `LocalReducedTransparency`.** When `true`, swap any
  `GlassIntensity.Standard`/`Vivid` to `Subtle` (highest-opacity preset).
  Don't eliminate the glass surface — surfaces still need hierarchy.
- **Rule 9 — Required `contentDescription`** on every icon-only
  composable. Pass `null` only when a sibling element provides the
  accessible label, and KDoc must call that out.
- **Rule 10 — Progress + skeleton announcements.** Determinate
  `Gadget*Progress` set `progressBarRangeInfo`; `GadgetShimmerBlock`
  announces `liveRegion = Polite` + a "Loading" `contentDescription`.

### Per-component accessibility contract

| Component | Contract |
|---|---|
| `GadgetIconButton` / `GadgetFab` | `contentDescription` required (nullable only with a labelling sibling). |
| `GadgetStatusDot` | `contentDescription` required as a non-default param; `null` only with a labelled sibling. |
| `GadgetBadge` | Dot variant decorative; text variant announces a `stateDescription` (`"3 unread"`; override via `stateDescriptionOverride`). |
| `GadgetCircularProgress` / `GadgetLinearProgress` | Determinate publishes `progressBarRangeInfo(current, 0f..1f)`. |
| `GadgetShimmerBlock` | `contentDescription = "Loading"` + `liveRegion = Polite`. |
| `GadgetEmptyState` | Title + subtitle + action merge into one node via `semantics(mergeDescendants = true)`. |
| `GadgetDialog` / `GadgetBottomSheet` | Title carries `semantics { heading() }`. |

## Responsiveness

- **Rule 11 — `LocalWindowSizeClass.current` is the source of truth for
  breakpoints** (Compact / Medium / Expanded). For most layout decisions
  use the higher-level `rememberLayoutMode()` (`core/ui/adaptive/`), which
  returns `SinglePane` / `TwoPane` / `ThreePane`:

  ```kotlin
  when (rememberLayoutMode()) {
      SinglePane -> CompactDashboard()
      TwoPane, ThreePane -> SplitPaneDashboard()
  }
  ```

  `WindowSizeClass` is the implementation detail (its API may shift
  between M3 versions); `GadgetLayoutMode` is the stable seam. Use
  `BoxWithConstraintsAdaptive { mode -> … }` when a layout needs both
  pixel constraints and the semantic mode.
- **The shell adapts automatically:** `GadgetApp` passes `showLabels =
  true` to `GadgetNavRail` only at Expanded width. The rail is a
  pinned-anchors layout — `pinnedTop` (Dashboard), `pinnedBottom`
  (Settings), and `modules` in a scrollable middle region. Add a module
  by appending it to `GadgetDestination.modules` and registering its route
  in `GadgetApp { … }`.
- **`ModuleScreenScaffold`** exposes an optional `secondaryPane` slot
  rendered to the right of the primary column on TwoPane/ThreePane;
  omitted entirely on Compact.
- **Foldable posture** has a stable seam (#89): `GadgetPosture` +
  `rememberPosture()` returns `Flat` / `Tabletop` / `Book`. Read it for
  posture, `rememberLayoutMode()` for width — they're orthogonal. Wired
  ahead of need; go through the seam, never pull `material3-adaptive` into
  a feature ad-hoc.

## Performance & stability

- Data classes exposed by the design system are `@Immutable` so Compose
  can skip recompositions. Mirror this for any new UI-state data class
  (drop the annotation in non-Compose foundation modules — see
  [Troubleshooting](Troubleshooting)).
- `@Composable val foo: Foo @Composable get()` accessors must be evaluated
  **inside** a `@Composable` function. Capture the resolved object into a
  local once and read its plain `val`s from non-composable callbacks.

## Preview matrix policy

Every component file that ships a public composable also ships ≥1
`@Composable` preview. The annotation stack:

- **Always:** `@GadgetPreviewLightDark` + `@GadgetPreviewLargeFont` +
  `@GadgetPreviewRtl` (dark+light, 200 % font, RTL).
- **For layout-driven components** (cards, list rows, empty states,
  shimmers): add `@GadgetPreviewSizeClasses` (Compact / Medium /
  Expanded). Skip for width-invariant components (buttons, chips, badges,
  text fields, dots).

Multi-preview annotations live in
`core/ui/preview/GadgetPreviewMatrix.kt`.

## Anti-patterns

1. **God-object screens.** Decompose into `@Composable` cards under
   `feature/<name>/components/`.
2. **Direct `SharedPreferences` reads inside composables.** Use
   `UserPreferencesRepository` (`:core:datastore`) injected via Hilt.
3. **Hard-coded `dp` at call sites.** Use
   `LocalGadgetTheme.current.spacing.x`.
4. **Raw `Surface(color = …)` for glass.** Use `GlassSurface` /
   `Modifier.glassSurface()`.
5. **Branching on `BuildConfig.IS_ROOTED`.** Inject a flavor-aware
   controller via Hilt (see [Flavors & Root Safety](Flavors-and-Root-Safety)).
6. **Default text wrapping.** Single-line + ellipsis is the default.
7. **`@Composable` accessor reads inside non-composable callbacks.**
8. **Imports from `com.gadget.**` in new code** — a review-blocker.

---

> _Last reviewed: 2026-06-12 · Source: `CLAUDE.md` (design-system
> sections), `core/designsystem/*`, `core/ui/*` · Related modules:
> `:core:designsystem`, `:core:ui`, `:core:navigation`._
