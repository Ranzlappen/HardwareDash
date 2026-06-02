# Issue #94 — retiring `RootFeaturesEntryPoint` → per-feature `@Inject`

> **Status: IN PROGRESS — 94-A (vibration) + 94-B (torch sysfs) shipped.**
> 94-A (deleting the legacy vibration controller tree, the orphaned
> `VibrationRootExtrasSection` card, both flavors' `RootBindings` entries, and
> the `RootFeaturesEntryPoint.vibrationController()` getter) and 94-B (deleting
> the orphaned `TorchRootExtrasSection` card — the whole `RootedExtrasSections.kt`
> file — and the `RootFeaturesEntryPoint.torchSysfsController()` getter) have
> landed. The remaining features (radios / sensors / battery / diagnostics / av /
> …) follow the same shape, each as its own reviewable, device-verified slice.
> CI compile-checks every slice; on-device verification of the modular
> replacement is still recommended before merge.

## Why this exists

`app/src/main/.../root/RootFeaturesEntryPoint.kt` is a Hilt `@EntryPoint`
exposing **22 controller getters + 3 registries** (`capabilityRegistry`,
`featureRegistry`, `featureToggles`). It exists because the legacy rooted UI
(the `com.gadget.root.ui.*RootExtrasSection` composables in `:app/src/main`)
can't `@Inject` — `@Composable`s take no constructor params — and because the
22 legacy controllers still live in `app/src/main/java/com/gadget/<feature>/`,
which leaf `:feature:*` modules can't see.

**End-state:** each feature controller moves to its own `:feature:<name>`
module (torch + vibration already have modular standard controllers + sibling
`-rooted` modules); the rooted UI moves into the feature screens and injects the
modular capabilities directly; the few cross-feature aggregations migrate to a
`Map<FeatureId, X>` multibinding (the pattern `:core:automation`'s
`ModuleActionRegistry` established). Then `RootFeaturesEntryPoint` + the legacy
`RootBindings` are deleted.

## Why it can't be one PR

~2500 LOC across 22 controllers + 13 `ui/Rooted*ExtrasSection` composables + the
two per-flavor `RootBindings.kt`. Each controller must modularize before its
getter can go. So this is a **per-feature batch series**, each batch deleting one
feature's legacy controller + getter once its modular replacement is verified.

## 94-A — vibration (the worked first slice) — ✅ shipped

Vibration was the cleanest starting point and was fully investigated, then
executed (the deletion set below all landed):

- The modular `:feature:vibration` screen **already supersedes** the legacy
  vibration rooted tools — `components/VibrationRootToolsCard.kt` +
  `VibrationRootCapabilities` (+ the `:feature:vibration-rooted` impl) provide the
  4 privileged tools with `RootSafetyGate` gating, hard caps, and
  `NonCancellable` cleanup.
- The legacy `VibrationRootExtrasSection` (in
  `app/src/main/.../root/ui/RootedExtrasSections.kt`) is **orphaned** — its only
  references are its own definition and one doc comment in
  `RootedAvExtrasSections.kt`; **no screen renders it**.
- `RootFeaturesEntryPoint.vibrationController()` is used **only** inside that dead
  card, so the whole chain is self-contained.
- The `:feature:vibration-rooted` references to `com.gadget.vibration.*` are
  **doc comments only** ("ported verbatim from …") — no real dependency.

**Deletion set for 94-A:**
1. `app/src/main/.../root/ui/RootedExtrasSections.kt` — remove
   `VibrationRootExtrasSection` (dead card) + its demo-pattern helpers.
2. `RootFeaturesEntryPoint.kt` — remove `fun vibrationController()`.
3. Both flavors' `RootBindings.kt`
   (`app/src/{standard,rooted}/.../root/RootBindings.kt`) — remove the
   `VibrationController` binding/provide.
4. Legacy controller tree:
   `app/src/main/java/com/gadget/vibration/{VibrationController,VibrationControllerResult}.kt`,
   `app/src/standard/java/com/gadget/vibration/StandardVibrationController.kt`,
   `app/src/rooted/java/com/gadget/vibration/{RootedVibrationController,RumbleMonitor,VibrationSysfsPaths,DualActuatorDriver}.kt`.

**Verification (why it's a separate, device-gated PR):** CI compile-checks it
(`assembleStandardDebug` + `assembleRootedDebug`) and the standard-APK leak gate
guards regressions, but CLAUDE.md requires legacy rooted retirement to be
**device-verified** — confirm on a rooted device that the modular vibration
screen's rooted tools still work and nothing reached the deleted card before
deleting. Keep it one commit, surfaced for review.

## 94-B — torch sysfs (`torchSysfsController()` + `TorchRootExtrasSection`) — ✅ shipped

The cleanest possible slice — identical shape to 94-A:

- The `TorchSysfsController` interface already lives in `:feature:torch/sysfs`,
  and both flavor impls are bound by the feature modules' own Hilt modules
  (`RootedTorchModule` / `StandardTorchModule`) — **not** by `:app`'s
  `RootBindings`. The modular `RootedTorchRootCapabilities` injects
  `TorchSysfsController` **directly**, so those bindings stay untouched.
- The only consumer of `RootFeaturesEntryPoint.torchSysfsController()` was the
  `TorchRootExtrasSection` card, which **nothing rendered** (orphaned, exactly
  like vibration's). Deleted the whole `RootedExtrasSections.kt` file (the card
  + `describeTorchResult` + demo constants) and dropped the entry-point getter
  + its KDoc + the now-unused import. Refreshed three stale doc references
  (`RootedTorchModule`, `RootedAvExtrasSections`, CLAUDE.md naming note).

## 94-C … (remaining features)

Repeat the 94-A/B shape per feature — radios / sensors / battery /
diagnostics / av / etc. — one batch each, each gated on the feature's modular
replacement existing + device verification. When the last getter is gone,
delete `RootFeaturesEntryPoint` + the now-empty legacy `RootBindings`, and fold
any still-needed cross-feature aggregation into a `Map<FeatureId, X>`
multibinding.
