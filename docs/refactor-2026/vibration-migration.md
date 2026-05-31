# Vibration migration plan (`:feature:vibration`)

> **Status: SHIPPED — single comprehensive PR, commit-stacked.** Executed as
> one PR mirroring torch 1:1, not the phased Follow-ups this doc originally
> proposed. Two decisions superseded the original plan:
> (1) the legacy `:app` vibration code is **left inert** (not renamed to
> `Legacy*` / deleted) — the new modular `dev.ranzlappen.gadget.feature.vibration.*`
> lives alongside it with distinct FQNs, so there is **no Hilt entry-point
> collision** and no rename was needed (verified: legacy
> `RootFeaturesEntryPoint.vibrationController()` returns the legacy FQN; the
> modular interface is separate). Legacy retirement is a tracked follow-up
> after on-device verification.
> (2) Full scope: 4-widget parity + a full freehand draw-canvas pattern builder.
>
> What shipped: `:feature:vibration` (+ `-rooted` / `-standard`) with the
> standard `VibrationController` (`VibratorManager`/`VibrationEffect`), the
> `VibrationRuntime` modelled-decay **poll** signal (the non-pollable-actuator
> answer to the `MetricSource` contract), `VibrationRootCapabilities` (the
> legacy rooted controller ported verbatim, gated by `RootSafetyGate` + the 4
> `RootFeatureKey.Vibration*` with their hard caps + `NonCancellable` cleanup),
> `VibrationMetricSource`, both monitor containers, `VibrationActionHandler`,
> the 4 widgets (vibrate / pattern / monitor / chart) with the
> `claimSolePending` first-pin fix, the full screen + pattern-builder canvas,
> and the test suite. The original per-row checklist below is retained as the
> historical sizing reference.

---

> Successor to the torch blueprint. The torch port (refactor-2026 Phase 2 /
> Batches 1–E) is the reference; this doc describes what the equivalent
> vibration migration needs to touch, sized against the blueprint pieces
> torch closed out so each can be ticked off in a follow-up.

## Current state

| Surface | Where it lives today | Modular target |
|---|---|---|
| Legacy `VibrationController` interface | `app/src/main/java/com/gadget/vibration/VibrationController.kt` | `:feature:vibration/legacy/LegacyVibrationController.kt` (rename + repackage to avoid clash with the new modular `VibrationController`) |
| Legacy `VibrationControllerResult` | `app/src/main/java/com/gadget/vibration/VibrationControllerResult.kt` | `:feature:vibration/legacy/LegacyVibrationControllerResult.kt` |
| Standard impl `StandardVibrationController` | `app/src/standard/java/com/gadget/vibration/StandardVibrationController.kt` | `app/src/standard/java/dev/.../feature/standard/vibration/LegacyStandardVibrationController.kt` (per the E1 standard-impl pattern) |
| Rooted impls (4 files) — `RootedVibrationController`, `DualActuatorDriver`, `RumbleMonitor`, `VibrationSysfsPaths` | `app/src/rooted/java/com/gadget/vibration/` | `feature/vibration-rooted/src/main/.../rooted/` (mirror `feature/torch-rooted/`) |

`:feature:vibration` is currently a skeleton (`build.gradle.kts` only, no
sources). `:feature:vibration-rooted` doesn't yet exist; it'd be created in
F3 mirroring `:feature:torch-rooted`.

## Blueprint checklist (torch is the worked example)

Each row mirrors a torch milestone — see `CLAUDE.md`'s "Module Authoring
Contract".

1. **Design system** — every token via `LocalGadgetTheme.current`; no raw
   `dp`; modifier-first; single-line + ellipsis text by default; required
   `contentDescription` on icon-only nodes.
2. **Screen shell** — `ModuleScreenScaffold` with a `ModuleInfo`
   (`VIBRATE` permission, `minSdk` notes, no firmware) **and** a
   `ModuleCapabilitiesSection` covering standard + rooted functions.
3. **Rooted seam** — `VibrationRootCapabilities` interface in
   `:feature:vibration`; app-flavor no-op (standard) + real (rooted)
   bindings reusing the legacy rooted controller, gated by
   `RootSafetyGate` + a `RootFeatureKey`.
4. **Reuse, don't reinvent** — `GadgetCircleControl` for the run / stop /
   pulse-pattern row; `DashCard` for tile sections; `GadgetSlider` for the
   amplitude knob.
5. **Widgets** — `VibrationWidgetConfig : WidgetKitConfig`,
   `VibrationWidgetProvider : BaseGadgetWidgetProvider<VibrationWidgetConfig>`.
   This is the blueprint-validation step: the kit should land **zero**
   feature-private plumbing copies of the torch-side mechanics. If any
   needed copy-pasting, the kit's contract is incomplete and a
   `:core:widgetkit` follow-up is filed.
6. **Monitoring-ready** — `VibrationMetricSource` (intensity / running),
   embed `MonitorContainer` + `LiveMonitorContainer` for persisted history
   + realtime traces.
7. **Automation-ready** — `VibrationActionHandler : ActionHandler` bound
   `@IntoMap` so the automation engine resolves the actions through the
   kit's `ModuleActionRegistry` registry.
8. **Tests + previews + CI traps** — `VibrationScreenContentTest`
   instrumented test (sealed `VibrationUiEvent` + capture-into-list pattern);
   preview matrix (`LightDark` + `LargeFont` + `RTL` + `SizeClasses` for
   layout-driven components).

## Scope split

| PR | What it lands |
|---|---|
| Phase F skeleton (this PR, F1) | This doc; `:feature:vibration` keeps its build skeleton; no source migration yet. |
| Follow-up A — Vibration migration v1 | Items 1–4 + 8 above. Mirrors the original torch v1 commit (Phase 2 / Batch 1). |
| Follow-up B — Vibration widget kit consumer | Item 5 (`VibrationWidgetConfig` + `VibrationWidgetProvider` extending the kit base). This is the actual blueprint validation step — if it requires touching `:core:widgetkit`, file kit follow-ups. |
| Follow-up C — Vibration monitoring + automation | Items 6 + 7. |
| Follow-up D — `:feature:vibration-rooted` | F3 plan: move the 4 rooted files; rooted-flavor Hilt module; `rootedImplementation(project(":feature:vibration-rooted"))`. Mirrors E1 + E2. |

## Why deferred from this PR

The torch port (Phases 1 / 1.1 in refactor-2026) was a multi-batch effort —
8 commits across several weeks of focused review. Replaying that for
vibration end-to-end inside the Phase 2 PR would balloon its diff well
past the modal reviewable size and entangle the kit-extraction story with
a second feature port. The pragmatic split: land the kit extraction
through Phase E; do vibration's port as a focused successor PR that pulls
exclusively on the now-stable kit + root-safety modules.
