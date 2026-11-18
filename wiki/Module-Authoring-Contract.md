# Module Authoring Contract (v3)

What every user-facing feature module must satisfy, and what it may add when
it earns its place. This replaces the v2 contract, which made monitoring,
automation and rooted-capability rows mandatory for every module — see
[`MASTER-PLAN.md` §2](https://github.com/Ranzlappen/HardwareDash/blob/main/MASTER-PLAN.md)
for why that was dropped.

## Required core

1. **It does its job on a real phone.** The module's core job is a line in
   [`docs/v3-smoke-test.md`](https://github.com/Ranzlappen/HardwareDash/blob/main/docs/v3-smoke-test.md),
   and it is only reported ✅ once that line is ticked on a named device.
   No placeholder screens; no "architecture-ready" stubs.
2. **Design system.** Tokens from `LocalGadgetTheme.current`; `:core:ui`
   components (`ModuleScreenScaffold`, `DashCard`, `CompactCard`,
   `GadgetSlider`, …); no raw `dp` at call sites (per-file `Defaults` for fixed
   sizes); a11y basics — `contentDescription` on icon-only controls, 48 dp
   touch targets, single-line + ellipsis by default. See
   [Design System](Design-System).
3. **Flavor safety.** Rooted code only behind the `:core:root` Hilt seam,
   gated by `RootSafetyGate` + a `RootFeatureKey`. Never branch on
   `BuildConfig.IS_ROOTED`; never import su under `src/main`; the standard-APK
   leak gate stays green. See [Flavors & Root Safety](Flavors-and-Root-Safety).
4. **Strings in resources.** English `strings.xml` required; `de`/`es`/`fr`
   best-effort (don't block a batch on translations).
5. **Tests for the logic it owns.** Unit tests for parsers, serializers,
   maths and safety clamps. Screen tests are optional; keep the stateless
   `<Feature>ScreenContent` split where it already exists so they stay easy.

## Add only when it earns its place

| Extra | Add it when… | Don't add it when… |
|---|---|---|
| `MetricSource` (`@IntoMap`) | the signal is worth charting or triggering a rule on (battery %, signal strength, lux) | it is static presence ("vibrator available") or duplicates another source |
| `ActionHandler` (`@IntoMap`) | the action has a real side effect someone would automate (torch on, vibrate pattern, send IR code) | it only checks a value — **never add `assert_*` actions**; rules use `Condition.MetricCompare` / `Trigger.MetricThreshold` |
| `MonitorContainer` / `LiveMonitorContainer` | the screen's main value is a changing signal | the signal is incidental to the job |
| Widget / QS tile | the job is commonly a one-tap thing (torch, strobe, vibrate) | it would just open the app |
| Rooted section | a rooted function exists **and** is verified on a device — or it is flagged 🧪 Experimental and hidden by default | as a "required" row on every screen |
| Previews | `@GadgetPreviewLightDark` for the screen content | the full LightDark + LargeFont + RTL + SizeClasses matrix — reserve that for shared `:core:ui` components |

When you do add an extra, follow its existing seam exactly (pin-reliability
halves for widgets, stable metric keys for MetricSources, `requiresRoot` +
param schema for actions). Those rules live on the subsystem pages:
[Widgets, Tiles & Surfaces](Widgets-Tiles-and-Surfaces),
[Monitoring Framework](Monitoring-Framework),
[Automation Engine](Automation-Engine).

## Hard rules (unchanged from v2)

- New code under `dev.ranzlappen.gadget.**`; no `com.gadget.**` imports.
- `app → feature → core`; no feature → feature dependency.
- Every entry point (screen, tile, widget, action) converges on the same
  `@Singleton` controller.
- Persisted identifiers — MetricSource keys, DataStore keys, rule JSON
  `@SerialName`s, widget config — never change without a migration.

## The two engine seams (reference)

| Seam | Module | Role |
|---|---|---|
| `MetricSource` + `MetricDescriptor` | `:core:model` | Read side — readable signals (push or poll) for monitoring, rule triggers/conditions, the metric widget. |
| `ActionHandler` + `ModuleActionRegistry` | `:core:automation` | Write side — invocable actions by `featureId` / `actionKey` for rules and widgets. |

```kotlin
@Binds @IntoMap @StringKey("<metricKey>") fun bindSource(impl: TorchMetricSource): MetricSource
@Binds @IntoMap @StringKey("<featureId>") fun bindHandler(impl: TorchActionHandler): ActionHandler
```

## Reference modules

- **Torch** — the fullest example (widgets, tiles, strobe service, rooted
  extras). Copy a seam from it when you need that seam, not the whole shape.
- **Vibration** — pattern editor + rooted tier; a good mid-size example.

---

> _Last reviewed: 2026-11-18 (v3 reset) · Source: `MASTER-PLAN.md` §5.3 ·
> Related modules: `:core:model`, `:core:automation`, `:core:root`, all
> feature modules._
