# Torch Blueprint

`:feature:torch` is the **canonical advanced feature example** — the
hardened reference implementation every future migration is measured
against. It exercises every seam at once: standard hardware control, a QS
tile, two app widgets, dynamic widget pinning, a foreground strobe
service, monitoring, automation, and a rooted capability adapter.

> **Don't blindly copy all of it.** Torch's breadth proves the
> architecture holds; most features should start from the **minimal
> template** in the [Feature Migration Guide](Feature-Migration-Guide) and
> add seams only when needed. A read-only sensor is minimal; an actuator
> with widgets + monitoring + automation + a rooted boost is advanced.

## Why Torch is the advanced blueprint

It is the one feature that touches every framework: `:core:ui` (design
system), `:core:widgetkit` (widgets + pinning), `:core:monitoring`
(charting), `:core:automation` (actions), and `:core:root` (rooted
extras). If a change to a shared seam keeps Torch green, it's safe for
everyone.

## Standard torch controller

```kotlin
interface TorchController { val state: StateFlow<TorchState>; fun toggle(); fun setOn(on: Boolean) }
```

`StandardTorchController` wraps Camera2 `CameraManager.setTorchMode()` and
registers a `TorchCallback` so external toggles (QS tile, OEM gestures,
other apps) flow back into `state`. Setters are **non-suspend** — the
binder calls finish in microseconds, so the screen, tile, widget, and
service hit them directly.

> **`init`-block forward-reference trap.** The first migration tripped on
> registering the `TorchCallback` from an `init` block before the property
> it touched was declared. Declare the property above the `init` block.

## Rooted torch extras (`:feature:torch-rooted`)

The flavor-isolation blueprint:

1. The feature declares a capability interface in `src/main`
   (`TorchRootCapabilities` + `TorchRootResult` / `TorchRootAvailability`).
   The feature stays flavor-agnostic — it never imports libsu or
   `com.gadget.root.*`.
2. Two sibling per-flavor modules bind the impls: `:feature:torch-rooted`
   (via `rootedImplementation`, real impl + `RootedTorchModule`) and
   `:feature:torch-standard` (via `standardImplementation`, no-op +
   `StandardTorchModule`). Each is on exactly one variant's classpath →
   exactly one `@Binds` per interface → no Hilt duplicate-binding clash,
   and no flavor's impls live in `:app`.
3. The rooted impl reuses the privileged `TorchSysfsController`
   (implemented by `RootedTorchController`) rather than re-implementing
   sysfs/libsu. Every privileged call routes through `RootSafetyGate`
   (capability + opt-out + rate-limit) with a `RootFeatureKey`.
4. The `@HiltViewModel` injects the interface directly; Hilt resolves the
   binding at `:app` assembly. Each rooted function surfaces as a
   `ModuleCapability` row that reads red ("requires the rooted app
   version") on standard.

**Safety, by construction:** brightness hard-capped at 150 %; thermal
override bounded by a 45 s absolute ceiling and **cancelled immediately**
on a trip-point breach; both thermal + strobe restore device state in a
`NonCancellable finally` so a cancelled coroutine can't leave throttling
disabled or the LED latched on. See [Flavors & Root
Safety](Flavors-and-Root-Safety).

## Widgets + dynamic pinning

Torch ships two **function-driven** widgets (`FlashlightWidgetProvider`,
`StrobeWidgetProvider`) plus two monitoring widgets (progress + chart
sparkline). They are the reference for:

- **Function-driven model** — the widget stores
  `{ displayName, actionKey, params, sizePreset, appearance }`; a tap
  resolves the bound `WidgetFunction` and dispatches it through the same
  `ModuleActionRegistry` as in-app controls.
- **Pin reliability** — `TorchWidgetCreator` builds the success-callback
  `PendingIntent` with `FLAG_MUTABLE` + explicit `ComponentName`, carries
  the config through `PendingWidgetConfigs`, and
  `FlashlightWidgetProvider.reconcilePendingConfig` calls
  `claimSolePending { … }` so a pin binds correctly even on launchers that
  never fire the callback.
- **Soft-delete** — in-app delete sets `removed = true` (the provider
  self-heal would otherwise recreate it); the widget repaints inert until
  the user drags it off, when `onDeleted` purges for real.
- **External-state refresh** — `TorchWidgetStateObserver` repaints placed
  flashlight widgets when the torch is toggled from outside.

Full subsystem: [Widgets, Tiles & Surfaces](Widgets-Tiles-and-Surfaces).

## Strobe service

`StrobeService` is a foreground service with
`foregroundServiceType="shortService"` on API 34+ (no camera-typed FGS
needed for `setTorchMode`): `onTimeout` clean shutdown on the OS cap,
`START_NOT_STICKY`, a notification **Stop** action, and channel
`setSound(null, null)`. It publishes "is the strobe running?" to a
`@Singleton StrobeRuntime` `StateFlow` — so the ViewModel reads live state
instead of polling a `@Volatile` flag.

## Monitoring

`TorchMetricSource` (`"torch_intensity"`) is a **poll** source (a
continuously-charted plateau needs a sample per bucket). `descriptor.max`
is capability-driven (100 standard, ~150 rooted) so the chart/widget axes
scale to the real range. The screen embeds `MonitorContainer` (persisted
history) and `LiveMonitorContainer` (live realtime), each via a Hilt-free
slot. Full subsystem: [Monitoring Framework](Monitoring-Framework).

## Automation

`TorchActionHandler` (`featureId = "torch"`) exposes on/off/toggle/strobe
actions with `ModuleAction` metadata + param schemas + `requiresRoot`
flags, bound `@IntoMap`. The engine dispatches verbatim — the canonical
end-to-end rule "when proximity < 5 cm, turn the torch off" fires
`RuleAction(featureId="torch", actionKey="off")`. See [Automation
Engine](Automation-Engine).

## Tests

JVM tests for strobe-rate maths + `@Serializable` round-trips;
instrumented tests of the stateless `TorchScreenContent` under
`GadgetTestTheme`; the full preview matrix. Torch + Vibration + Apps gate
every PR via `instrumented-tests.yml`.

## Key files to study

| File | Why |
|---|---|
| `TorchController.kt` / `StandardTorchController.kt` | Interface + Camera2 impl with `TorchCallback`. |
| `TorchScreen.kt` / `TorchScreenContent.kt` | Stateful route vs. stateless testable content. |
| `TorchViewModel.kt` | `combine(...)` over flows + rooted availability + collapse state. |
| `tile/FlashlightTileService.kt` | `EntryPointAccessors.fromApplication(...)` for non-Hilt components. |
| `widget/FlashlightWidgetProvider.kt` | Function-driven provider + `reconcilePendingConfig` + soft-delete. |
| `widget/TorchWidgetCreator.kt` / `WidgetPinSuccessReceiver.kt` | The reliable pin flow. |
| `strobe/StrobeService.kt` / `StrobeRuntime.kt` | shortService FGS + singleton run-state. |
| `…torch.sysfs` (rooted) | The privileged controller reused by the rooted tier. |

## What future modules should NOT over-copy

The strobe FGS, dynamic pinning, the rooted sysfs adapter, and the
external-state observer exist because Torch *needs* them. A battery
read-out or a light-sensor screen needs none of that — copying them in
produces dead infrastructure that fails the "no dead seam" criterion.
Copy the **shape** (stateless content, `MetricSource`, `ActionHandler`,
`ModuleInfo`); add the heavy seams only when the hardware demands them.

---

> _Last reviewed: 2026-06-12 · Source: `CLAUDE.md` (torch sections),
> `docs/migration-guide.md`, `feature/torch/*` · Related modules:
> `:feature:torch` (+ `-rooted`/`-standard`), `:core:widgetkit`,
> `:core:monitoring`, `:core:automation`, `:core:root`._
