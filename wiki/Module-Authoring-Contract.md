# Module Authoring Contract

The **acceptance checklist** for a new feature module. Every legacy →
modular actuator/sensor migration MUST satisfy this so the cross-feature
automation engine can discover and drive the module with **zero central
hardcoding**. Torch is the worked example of every item; Vibration is the
validated second consumer — keep both green when adding seams.

A module that isn't both **monitoring-** and **automation-ready** is not
"done" (items 6 and 7 are the non-negotiable end-state).

## The checklist

### 1. Design system

Every token from `LocalGadgetTheme.current`; no raw `dp` at call sites
(per-file `Defaults` for fixed sizes); modifier after required params,
composable slots last; single-line + `Ellipsis` text by default; the full
a11y contract (required `contentDescription`, `defaultMinSize(48.dp)`,
reduced-motion / reduced-transparency). See [Design System](Design-System).

### 2. Screen shell

Build on `ModuleScreenScaffold` with a `ModuleInfo` (permissions / OS
compatibility / firmware) **and** a tri-state per-function
`ModuleCapabilitiesSection` (green/amber/red, grant / open-settings
actions) covering **standard and rooted** functions.

### 3. Rooted seam

A feature-side capability interface; an app-flavor (or sibling-module)
no-op (standard) + real (rooted) binding reusing the rooted controller,
gated by `RootSafetyGate` + a `RootFeatureKey`. **Never** branch on
`BuildConfig.IS_ROOTED`; **never** import su under `src/main`. See
[Flavors & Root Safety](Flavors-and-Root-Safety).

### 4. Reuse, don't reinvent

`GadgetCircleControl`, `DashCard` / `CompactCard` / `GlassSurface`,
`GadgetSlider`, the tri-state `GadgetStatusKind` badges, `MonitorContainer`
/ `MonitorChart`. Promote a genuinely reusable new primitive into
`:core:ui` rather than leaving it feature-private.

### 5. Widgets + notifications (when the feature has them)

AppWidget provider + pin/launcher flow + **RemoteViews-safe** (`@RemoteView`)
layouts only; determinate-progress FGS notification; atomic `saveIfAbsent`
self-heal; inert "removed" handling on in-app delete; custom-icon import.
**Pin reliability is mandatory:** the in-app pin's success callback MUST be
`FLAG_MUTABLE` + explicit `ComponentName`, the pre-pin config MUST ride the
`PendingWidgetConfigs` bridge, and the provider MUST override
`reconcilePendingConfig` with `claimSolePending`. See [Widgets, Tiles &
Surfaces](Widgets-Tiles-and-Surfaces).

### 6. Monitoring-ready (non-negotiable)

Implement a `MetricSource` per readable signal and bind it `@IntoMap`;
embed `MonitorContainer` (persisted history) and `LiveMonitorContainer`
(live realtime) — both read the same source. Make each card collapsible
(`GadgetExpandableCard` + persisted `CollapseStateRepository`). See
[Monitoring Framework](Monitoring-Framework).

### 7. Automation-ready (non-negotiable)

Expose invocable actions via an `ActionHandler` (with `ModuleAction`
metadata + param schema + `requiresRoot`) bound `@IntoMap`. The engine
resolves both maps (`MetricSource` for triggers/conditions, `ActionHandler`
for actions) from Hilt and drives the module without importing it. Define
signals once — the same `MetricSource` feeds monitoring **and** automation
triggers. See [Automation Engine](Automation-Engine).

### 8. Tests + previews + CI traps

Unit tests for serialization/repos; instrumented tests for the stateless
screen (kept Hilt-free); the `@Preview` matrix policy (LightDark +
LargeFont + RTL always, SizeClasses for layout-driven components); and the
CI-only pitfalls. See [Testing & CI](Testing-and-CI) and
[Troubleshooting](Troubleshooting).

## The two seams the engine stands on

| Seam | Module | Role |
|---|---|---|
| `MetricSource` + `MetricDescriptor` | `:core:model` | Read side — a feature's readable signals (push or poll). Consumed by monitoring + automation triggers/conditions + `HardwareRegistry` enumeration. |
| `ActionHandler` + `ModuleActionRegistry` | `:core:automation` | Write side — a feature's invocable actions by `featureId`/`actionKey`. Consumed by widgets + automation actions. |

A feature contributes:

```kotlin
// read side — one per readable signal
@Binds @IntoMap @StringKey("<metricKey>") fun bindSource(impl: TorchMetricSource): MetricSource

// write side — one handler per feature
@Binds @IntoMap @StringKey("<featureId>") fun bindHandler(impl: TorchActionHandler): ActionHandler
```

This is the deliberate fix for legacy `Link`, which hardcoded a 70-entry
metric registry and an enum of action types inside one service.

## Reference consumers

- **Torch** (`:feature:torch` + `-rooted`/`-standard`) — the advanced
  blueprint exercising every item. See [Torch Blueprint](Torch-Blueprint).
- **Vibration** (`:feature:vibration` + `-rooted`/`-standard`) — the
  validated second consumer; a non-pollable actuator signal (modelled via
  `VibrationRuntime` → a decaying poll source) + a 4-capability rooted tier
  + a freehand draw-canvas pattern builder.

---

> _Last reviewed: 2026-06-12 · Source: `CLAUDE.md` (Module Authoring
> Contract) · Related modules: `:feature:torch`, `:feature:vibration`,
> `:core:model`, `:core:automation`._
