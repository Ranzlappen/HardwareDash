# Widgets, Tiles & Surfaces

`:core:widgetkit` is the reusable home-screen-widget framework — the
generic half of the torch widget subsystem, extracted so a 30-module app
doesn't re-hand-roll the pin flow, per-`appWidgetId` persistence,
RemoteViews appearance rendering, the icon catalog, and feedback for every
feature. Features plug in via a `WidgetKitConfig`; the kit never depends on
a feature module.

Torch and Vibration are the function-driven reference consumers; the
App-Organizer folder widget is the content/launcher reference.

## Core contracts

- **`WidgetKitConfig`** — per-instance config base (`displayName`,
  `removed`, `schemaVersion`, `appearance`).
- **`WidgetConfigStore<T : WidgetKitConfig>`** (`store/`) — hot-StateFlow
  cache + a `Migrator<T>` seam. Replaces every per-feature
  `<Feature>WidgetConfigRepository`. Bind once per feature.
- **`PendingWidgetConfigs<T>`** (`pin/`) — DataStore-backed bridge that
  survives process death between pin-request and the success callback,
  with **monotonic-counter keying under a `Mutex`** (replaces the legacy
  collision-prone `token.hashCode()`).
- **`BaseGadgetWidgetProvider<T>`** (`provider/`) — captures the
  `onUpdate` / `onDeleted` / `renderAll` / tap→dispatch→feedback→repaint
  chain + adaptive density. Function-driven archetype.
- **`BaseContentWidgetProvider<T>`** (`provider/`) — the content/launcher
  archetype: renders dynamic content and launches an Activity on tap (no
  function/feedback/toggle machinery); `buildRemoteViews` is **suspend**.
- **`WidgetAppearanceRenderer`** (`render/`) + `WidgetIconResolver` — the
  generic RemoteViews paint path.
- **`WidgetFeedbackDispatcher`** (`feedback/`) — per-feature channel /
  notification feedback.
- **`BootCompletedReceiver` + `BootRearmHandler`** (`boot/`) — features
  bind a `BootRearmHandler` into a `Map<FeatureId, BootRearmHandler>`
  multibinding; the kit receiver iterates them under one `goAsync`
  coroutine. **Don't add a second boot receiver.**

## Per-feature multibinding contract

Both `WidgetAppearanceRenderer` and `WidgetFeedbackDispatcher` are **one
app-wide `@Singleton`** serving every feature via a `Map<String, X>`
multibinding keyed by the feature's stable id (the same `FEATURE_ID` used
for boot-rearm + automation). Every widget-bearing feature MUST bind both
`@IntoMap @StringKey(FEATURE_ID)` and override `featureId` in its provider
so `renderer.apply(…, featureId)` / `dispatch(…, featureId)` select the
right catalog.

> **Why a map, not one shared resolver:** icon keys (`default_active`,
> `custom:…`) are shared constants, so a single resolver would resolve to
> the *wrong* feature's drawables. The **feature id** picks the catalog,
> not the key. A second *bare* `@Binds @Singleton X` is a
> `[Dagger/DuplicateBindings]` clash in `SingletonC` (torch shipped bare;
> vibration, the second consumer, forced the migration to the map).

## Function-driven widgets

A widget no longer hardcodes its action in its provider class. Each config
stores an `actionKey` naming a **`WidgetFunction`** the user picks in the
single comprehensive `WidgetCustomizationSheet` (name → function picker →
auto-generated param editor from each function's `ActionParam` schema →
size → appearance/preview). A tap resolves the bound function and
dispatches it through `WidgetFunctionDispatcher` → `ModuleActionRegistry`
— so a widget runs the *same* actions as in-app controls and feeds the
same runtime/monitoring.

- **Toggle** functions = two paired actions + a live `WidgetStateSource`
  keyed `"<featureId>:<stateKey>"` (drives the active/inactive icon swap).
- **Momentary** functions = one action, resting icon + press frame.
- Rooted functions are flavor-filtered out of the picker on standard.
- Feedback is `WidgetFeedbackState` (Toggle / Triggered / Failed) — the
  fix for the vibration widget's misleading always-"off" toast.

Each feature has ONE designated new-pin provider
(torch→`FlashlightWidgetProvider`, vibration→`VibrateWidgetProvider`); the
other per-type provider classes stay registered only to keep already-placed
legacy widgets alive, and a `Migrator<T>` folds the old `type`-based v1
config into v2.

## Content/launcher widgets

`BaseContentWidgetProvider<T>` renders a live preview from the feature's
own data and launches an Activity on tap. Its config screen builds on
`ContentWidgetCustomizationSheet` (name → content slot → background →
accent/content tint → label + size → preview slot). Content-source →
repaint is driven by `ContentWidgetUpdater.requestUpdate(context,
providerClass)` (an explicit `ACTION_APPWIDGET_UPDATE` self-broadcast) from
a feature `@Singleton` observer — the analogue of the monitoring
widget-notifier seam.

**Tap animations** route through `tapAction` (broadcast → launch-first,
then a concurrent press frame via `applyContentPressedFrame` on
`@id/widget_background`, since content widgets have no `@id/widget_icon`).
A content widget's layout must include an `@id/widget_background`
ImageView as the backmost child; the provider calls
`WidgetAppearanceRenderer.applyBackground` in `buildRemoteViews`.

**Generic metric widget** (`:feature:metricwidget`, W4) — the first
*cross-cutting* content widget: instead of one feature's hardwired signal
it binds to **any** registered `MetricSource` the user picks in its
`APPWIDGET_CONFIGURE` activity (`MetricWidgetConfigActivity` — a grouped
metric picker + Value / Value+bar display mode layered into the
`ContentWidgetCustomizationSheet` `content` slot). `MetricWidgetConfig`
persists the chosen `metricKey` per `appWidgetId`; `MetricWidgetProvider`
resolves the source through a `SingletonComponent` `@EntryPoint` exposing
the app-wide `Map<String, MetricSource>` multibinding and paints
`source.sample()` scaled to `descriptor.currentMax()`.
`MetricWidgetController` repaints on each bound source's push `stream()`
plus a 30 s ticker for poll-only sources. Placement is launcher-tray +
config-activity only (config written synchronously under the real
`appWidgetId`), so it needs no pin receiver. Three display modes: **Value**,
**Value + bar** (scaled to `descriptor.currentMax()`), and **Sparkline** — a
windowed history chart rendered to a bitmap via the shared
`MonitorChartBitmapRenderer` (the torch `MonitorChartWidgetProvider` path),
fed by `MonitorSampleRepository.observeBucketedSince`. The sparkline shows a
"collecting" state until the metric has ≥2 history points (history exists only
while `MonitorService` is sampling that metric). Because
`BaseContentWidgetProvider` owns the `updateAppWidget` call, the bitmap is
rendered **without** returning it to the renderer's pool — never releasing
means the pool always allocates fresh, so no in-flight bitmap is aliased.

**Folder widget icon catalog** — `FolderWidgetIconCatalog`
(`feature/apps/.../widget/customization/`) implements `WidgetIconResolver`
for the folder widget. Built-in entries map each `MaterialSymbol` id to its
vendored `drawableRes` (usable by RemoteViews). Custom icons are downscaled
WEBP files stored under `filesDir/widget_icons/apps/` keyed
`custom:<uuid>.webp` (same EXIF-corrected import pipeline as
`VibrationIconCatalog`). Accessed via `FolderWidgetEntryPoint` in the
provider (not via the `WidgetAppearanceRenderer` multibinding — the folder
widget renders its own content, not via `apply(…, featureId)`).

`FolderWidgetConfig.iconKey: String?` (schema v2) stores the chosen key.
When non-null, `FolderWidgetProvider.buildRemoteViews()` checks it first:
a built-in key renders through `@id/widget_folder_cover_symbol` with the
catalog's drawable + accent tint; a `custom:` key loads the WEBP file as a
bitmap into `@id/widget_folder_cover_image`. Falls back to the folder's own
cover / app-grid preview when `null`.

`FolderWidgetConfigActivity` injects the catalog and adds a `WidgetIconPicker`
composable inside the `content` slot of `ContentWidgetCustomizationSheet`:
a chip row of built-in symbols (with icon), an "Auto" chip to clear the
override, and a `GlassSurface` row to import a custom image from the
gallery. The selected `iconKey` is saved into `FolderWidgetConfig`.

**In-app widget management** — `FolderEditorScreen` now shows a
"Home-screen widgets" section (mirrors `VibrationScreenContent`'s
`WidgetsCard`) listing all placed widgets for the current folder (filtered
from `WidgetConfigStore` via `FolderEditorViewModel.placedWidgets`). Each
row has a delete button that soft-deletes via `removed = true`. An "Add
widget" button re-uses the existing `onPinToHome()` path.

Reference: `:feature:apps`'s `FolderWidgetProvider` + `FolderWidgetConfigActivity`
+ `FolderWidgetIconCatalog` + `FolderEditorScreen` / `PinFolderHelper`.

## Dynamic widget pinning — the reliability rules

`AppWidgetManager.requestPinAppWidget` (API 26+) lets an in-app flow ask
the launcher to pin a widget. The OS returns the new `appWidgetId` by
filling it into the success-callback `PendingIntent`. **This is
CI-invisible and has bitten widget work more than once.** Two
non-negotiable halves, required for **every** widget-bearing feature
(function-driven and content):

1. Build the success-callback `PendingIntent` with `FLAG_UPDATE_CURRENT or
   FLAG_MUTABLE` and an **explicit `ComponentName`**. A `FLAG_IMMUTABLE`
   callback **silently drops the fill-in** — the receiver reads
   `INVALID_APPWIDGET_ID`, bails, never persists the config, and the placed
   widget self-heals to a blank/inert default. (Mutability is safe because
   the explicit component can't be hijacked; the flag is ignored < API 31,
   mutable by default there, so minSdk 29 is unaffected.)
2. Carry the pre-pin config through `PendingWidgetConfigs` (token in the
   callback) **and** override `reconcilePendingConfig` to
   `claimSolePending { … }` — the fallback for OEM launchers that never
   fire the callback at all.

`claimSolePending(predicate)` pops the **sole** unclaimed entry matching
the predicate, and deliberately **defers (returns null) when 2+ match** —
without the callback's token, two same-type widgets' configs would be
swapped. It's idempotent against `claim` (both delete under the same
mutex). The rescued config is written with `saveIfAbsent` so a racing
authoritative callback `save` still wins.

The tray-drop **configure-activity** path is exempt from half 1 (it gets
the `appWidgetId` straight from the `APPWIDGET_CONFIGURE` intent) but
should still write through the same store the rescue reads.

Reference: torch (`TorchWidgetCreator` + `WidgetPinSuccessReceiver` +
`FlashlightWidgetProvider.reconcilePendingConfig`); folder regressed by
shipping an immutable callback + skipping the rescue and was fixed to
match (PR #138).

## Soft-delete: "remove but keep inert"

A non-host app can't pull a placed widget off a third-party launcher, so
an in-app delete sets `removed = true` instead of deleting the config (the
provider self-heal would otherwise recreate it on next `onUpdate`). The
placed widget repaints inert (dimmed icon, click target cleared) until the
user drags it off — at which point `onDeleted` purges the config for real.
`TorchWidgetConfig.removed` is the reference; every future widget honours
this.

## QS tile entry points

QS `TileService`s toggle the feature's `@Singleton` controller / runtime
directly via `EntryPointAccessors.fromApplication`, producing the same state
visible from the screen and widgets. Declared in the feature module's
manifest (`BIND_QUICK_SETTINGS_TILE` + `exported=true`).

Torch ships two tiles; vibration adds a third (W4):

- **`FlashlightTileService`** — toggles `TorchController` on each tap; tile
  state mirrors `TorchController.state` (UNAVAILABLE on flashless devices).
- **`VibrateTileService`** (`:feature:vibration`) — a tap starts / stops a
  **held continuous** vibration via the shared `VibrationController`
  (`startContinuous` / `stop`). Active-state reads `VibrationState.isSustained`
  (not `isActive`, which a decaying one-shot also sets); UNAVAILABLE on
  vibrator-less devices. Same `EntryPointAccessors` recipe as the torch tiles.
- **`StrobeTileService`** — a tap starts / stops a constant-rate strobe
  (`TorchWidgetConfig.DEFAULT_RATE_HZ`) by start/stopping `StrobeService`
  (the same start path as `TorchViewModel` — `startForegroundService` inside
  the tile-tap FGS-allowlist window, `IllegalStateException` caught). Tile
  active-state reads `StrobeRuntime.running` (the process-wide `StateFlow`),
  so a strobe started from the screen / widget / automation lights the tile
  too. UNAVAILABLE gated on `TorchController.state.value.isAvailable`.

A tile subscribes to its state flow only for the `onStartListening` ↔
`onStopListening` window (the tile is visible in the panel), re-rendering on
every emission; no work happens while the panel is closed.

## RemoteViews gotchas

- **Only `@RemoteView` classes inflate.** `FrameLayout`, `LinearLayout`,
  `RelativeLayout`, `GridLayout`, `ImageView`, `ImageButton`, `TextView`,
  `Button`, `ProgressBar`, `Chronometer`, `AnalogClock`, the adapter views,
  and `ViewStub`. A bare `<View>`/`<Space>` throws `InflateException` →
  "Couldn't add widget". For a background use `ImageView`/`FrameLayout`,
  never `<View>`.
- **Non-transitive R + cross-module ids.** XML refs merge across modules;
  **Kotlin** refs to a dep module's id must qualify the R class:
  `import …core.widgetkit.R as WidgetKitR` + `WidgetKitR.id.widget_icon`.
- **Bitmaps must be sized to the widget** (chart widget caps its bitmap)
  to stay under the Binder transaction limit. The chart bitmap uses
  `RGB_565` from a size-keyed `BitmapPool`, released back after
  `updateAppWidget`.
- **The AppWidget native update floor is ~30 min** — push repaints via the
  notifier/updater, never `updatePeriodMillis`.

## Boot restore

`BootCompletedReceiver` re-arms via the `BootRearmHandler` multibinding.
Torch uses it to rearm `MonitorService` iff a monitor widget is placed and
monitoring is enabled. Automation re-arms its scheduler the same way.

See also: [Torch Blueprint](Torch-Blueprint),
[Monitoring Framework](Monitoring-Framework), [Asset
Catalog](Asset-Catalog), [Troubleshooting](Troubleshooting).

---

> _Last reviewed: 2026-07-11 · Source: `CLAUDE.md` (widgetkit),
> `feature/torch/.../tile/*TileService.kt`,
> `docs/widgets/content-widget-customization.md`
> · Related modules: `:core:widgetkit`, `:feature:torch`,
> `:feature:vibration`, `:feature:apps`._
