# Monitoring Framework

`:core:monitoring` (with `:core:model` + `:core:data`) is the reusable,
drop-in monitoring container every actuator/sensor module embeds to chart
+ persist a signal. It **never depends on a feature module** — features
plug in via Hilt map multibindings resolved at `:app`. Torch is the
reference consumer.

## The readable-signal seam — `MetricSource` (`:core:model`)

```kotlin
interface MetricSource {
    val descriptor: MetricDescriptor   // metricKey, displayName, unit, min, max, category
    suspend fun sample(): Float        // poll path (always required)
    fun stream(): Flow<Float>? = null  // optional push path (null = poll)
}
```

The **single** readable-signal contract — consumed by monitoring **and**
the automation trigger-evaluator (see [Automation
Engine](Automation-Engine)). Its only foundation dependency is pure-Kotlin
`kotlinx-coroutines-core` (the `-core`, **not** `-android` artifact) for
the optional `stream()` — no Android, no Compose, no feature.

A feature contributes one per signal:

```kotlin
@Binds @IntoMap @StringKey("<metricKey>") fun …(): MetricSource
```

**Push vs. poll** (chosen per signal so the shared service stays cheap):

- **Poll** (`stream()` returns null — default): the sampler calls
  `sample()` every `pollIntervalMs`. Use for genuinely sampled signals
  (battery %, temperature, RSSI) **and any continuously-charted signal** —
  a downsampled chart needs a sample per bucket, so an actuator whose chart
  should show a filled plateau (the torch reference) polls.
- **Push** (override `stream()`): emit only on change. Use for sparse /
  event-only signals and to feed automation; an idle source then causes
  **zero** wakeups.

`descriptor.max` is the metric's full-scale ceiling and may be
**capability-driven** (torch reports 100 standard, ~150 rooted) so the
chart/widget axes scale to the real range. This is the deliberate fix for
legacy `Link`, which hardcoded a 70-entry metric registry inside
`LinkService`.

## `MonitorContainer` — persisted history

```kotlin
@Composable fun MonitorContainer(metricKey: String, title: String,
    modifier: Modifier = Modifier, collapseId: String? = null)
```

Drop into any feature screen to chart + **persist** a metric for long
histories. Self-contained — supply a `metricKey` (matching a contributed
`MetricSource`) + a title; config + history come from the framework via
Hilt (`hiltViewModel(key = metricKey)`). A glass `DashCard` with the live
chart, an on/off toggle, and a persistent settings block (sample-interval
slider, **time-window slider** 1m–24h default 1m, chart-style chips, "show
as widget" + "show as notification" switches). Pass `collapseId` to render
inside a collapsible `GadgetExpandableCard` (state persists via
`CollapseStateRepository`).

**Embed via a `@Composable () -> Unit` slot** on the stateless screen
content, supplied by the Hilt route — so the stateless content + its
previews/tests stay Hilt-free. Reference: `TorchScreenContent`'s `monitor`
slot. The preview-safe stateless renderer is `MonitorContent`.

## `LiveMonitorContainer` — live in-memory

```kotlin
@Composable fun LiveMonitorContainer(metricKey: String, title: String,
    modifier: Modifier = Modifier, collapseId: String? = null)
```

The **live, in-memory** companion for realtime analysis. **Independent** of
`MonitorContainer` — embed both. It **bypasses Room and the FGS**:
`LiveMonitorViewModel` reads the `MetricSource` directly (prefers
`stream()`, else fast-polls `sample()` every `intervalMs`, default 100 ms)
into a bounded ring buffer and exposes a fast `LiveTrace` (samples +
current/min/max/avg). The card is a "Live stream" toggle, a `LiveChart`, a
stats row, a **Freeze** chip, and **ephemeral** refresh / live-window
sliders (live settings aren't persisted). Sampling runs only while the card
is composed (a `DisposableEffect` calls `start`/`stop`) and the toggle is
on — an off-screen / off / frozen card incurs no wakeups.

`LiveChart` is a hand-drawn `Canvas` with **soft auto-scale Y** (bounds
ease toward the visible buffer's padded min/max via `animateFloatAsState`,
honouring `LocalReducedMotion`) **plus pinch-zoom / vertical-drag** for a
manual Y viewport; an **Auto** chip / double-tap returns to soft-auto. X is
the live window anchored to now.

## Persistent collapsible cards — `CollapseStateRepository`

`GadgetExpandableCard` (`:core:ui`, stateless) is the primitive;
`CollapseStateRepository` (DataStore, mirrors `MonitorConfigRepository`)
persists expanded/collapsed state keyed by a stable string id (default
expanded). It's not monitoring-specific — it lives here only because
`:core:monitoring` is the shared Hilt + DataStore core every feature
depends on. **Blueprint:** hoist expanded state through the feature's
ViewModel (inject the repo, expose `expandedStates(ids)` + an
`onSectionToggle(id)` handler); the monitor containers manage their own
collapse via `collapseId`. Section ids: see `TorchSectionId`.

## Chart internals — `MonitorChart`

A **hand-drawn Compose `Canvas`** chart (line / area / column per
`chartLayout`), deliberately **not Vico** — Vico's scroll/zoom +
per-entry axis labelling fought the sliding-window model. It charts a
**downsampled** window (`history()` → peak-per-bucket via
`MonitorSampleDao.observeBucketedSince`, capped at ~500 points), so a 24h
window stays a few hundred points.

- **X is pinned to the full window, anchored to now.** A bucket's index is
  `(timestamp − windowStart) / bucketMs`; the chart pins x to the full
  range via `MonitorHistory.windowMs`, so the right edge is always the
  present and the left is `windowMs` ago regardless of how much data exists
  yet. X labels are **time-ago marks at nice round intervals** (`…3h, 2h,
  1h, now`), a fixed handful sharing one unit. Y is pinned `0..yMax` where
  `yMax` = the source's `descriptor.max`. No horizontal scroll/pinch — to
  "zoom in", shrink the window (the slider re-queries at a finer bucket).
- Axis text via `rememberTextMeasurer()` + `DrawScope.drawText`; the `< 2`
  bucket guard now branches on **`isEnabled`**: when `false` shows "Turn on
  monitoring to start collecting history" (`R.string.monitor_chart_enable_hint`);
  when `true` shows the original "Collecting data…" placeholder.
  `MonitorContainer` passes `isEnabled = config.enabled` so the chart never
  shows "Collecting data…" to a user who hasn't yet turned monitoring on.

`MonitorChartBitmapRenderer` is the reusable pure-`Canvas` sparkline →
`Bitmap` for the **chart widget** (RemoteViews can't host a Compose chart):
`RGB_565` from a size-keyed `BitmapPool`, released after `updateAppWidget`.

## Other pieces

- **`MonitorConfig`** (`@Serializable @Immutable`) — per-metric persisted
  settings (`enabled`, `pollIntervalMs`, `chartLayout`, `windowSeconds`
  default 1m / max 24h, `yMax`, `widgetEnabled`, `notificationEnabled`).
  Stored per metricKey by `MonitorConfigRepository`.
- **`MonitorService`** — the **single** `specialUse` FGS for the whole app
  (features never run their own monitoring service). Per metric, one
  structured coroutine follows the live config via `collectLatest` and
  reads by push (`stream()` — zero idle wakeups) or poll (`sample()` every
  `pollIntervalMs`, wrapped in `withTimeout`). Inserts are per-reading;
  **pruning is batched** (`PRUNE_INTERVAL_MS`); widget + notification
  repaints are **throttled** (`UI_UPDATE_THROTTLE_MS`). Posts a richer
  per-metric notification per `notificationEnabled` metric (see below),
  pushes widget repaints per `widgetEnabled`, and **self-stops** when no
  metric is enabled. `MonitorController.ensureStarted()` is the only start
  path.
- **`MonitorService` per-metric stats** — `MetricRuntime` now tracks
  `minValue`, `maxValue`, `sampleCount` across the service lifetime. The
  per-metric notification compact text is `"75% — min 45% max 92%"`;
  `BigTextStyle` expands to `"Vibration: 75% — min 45%, max 92%"`. The
  summary notification lists active metric display names up to 4
  (e.g., `"Torch, Vibration +1"`) instead of only a count. A conditional
  **"Stop monitoring"** action button appears on each per-metric notification
  when `MonitorGlobalPrefs.notificationActionsEnabled` is `true`; tapping it
  dispatches to `MonitorNotificationActionReceiver`, which sets that metric's
  `MonitorConfig.enabled = false`.
- **`MonitorGlobalPrefs`** (`core/monitoring/MonitorGlobalPrefs.kt`) —
  DataStore-backed singleton (`"monitor_global"` preferences file) with a
  single `notificationActionsEnabled: StateFlow<Boolean>` (default `true`).
  Exposed in `:feature:settings` via a `MonitoringCard` settings row so the
  user can hide the stop-action buttons from all monitoring notifications.
  Injected by `MonitorGlobalPrefsModule` (provides the named DataStore via
  `@MonitorGlobalDataStore` qualifier, `di/MonitorGlobalPrefsModule.kt`).
- **`MonitorNotificationActionReceiver`** — `BroadcastReceiver` registered
  in `:core:monitoring`'s manifest (`exported="false"`, action
  `ACTION_DISABLE_METRIC`). Receives `EXTRA_METRIC_KEY`; uses
  `EntryPointAccessors` (Kotlin-only module limitation — same pattern as
  `AutomationAlarmReceiver`) to get `MonitorConfigRepository`, saves
  `config.copy(enabled = false)`.
- **`MonitorWidgetNotifier`** — the seam letting a feature refresh its own
  monitor widget when a sample lands (`@IntoMap` keyed by metricKey).
- **Room lives in `:core:data`** (`MonitorSample` + `MonitorSampleRepository`;
  `observeBucketedSince` is the downsample query). Other modules read
  `:core:data` repositories, never Room directly.

## Scaling rules

As modules multiply, follow these so monitoring stays within Android's
process/battery/IPC limits:

- **One shared `MonitorService`** — never a per-module service/process.
- **Prefer push over poll** for event-driven signals.
- **Downsample for display** (~500 in-app / ~120 widget points), bound
  retention (24h), batch the prune, throttle repaints. Never feed tens of
  thousands of raw points to a chart.
- **Chart x = fixed-time-bucket index pinned to the full window**; "zoom"
  = change the window.
- `specialUse` FGS needs a Play-console justification; bitmaps sized to the
  widget; push via the notifier, never `updatePeriodMillis`.

See also: [Widgets, Tiles & Surfaces](Widgets-Tiles-and-Surfaces),
[Automation Engine](Automation-Engine), [Module Authoring
Contract](Module-Authoring-Contract).

---

> _Last reviewed: 2026-06-13 · Source: `CLAUDE.md` (monitoring),
> `docs/sensor-actuator-api.md`, `core/monitoring/*`, `core/model/*` ·
> Related modules: `:core:monitoring`, `:core:model`, `:core:data`,
> `:feature:settings`._
