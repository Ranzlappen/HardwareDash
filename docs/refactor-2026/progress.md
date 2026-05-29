# Refactor-2026 — per-batch progress log

Running log of what each batch actually landed (the durable record; the plan
lives in `README.md` + the plan scratchpad). No Android SDK in the container,
so CI is the compile gate — each batch is written to respect the CLAUDE.md
CI-only pitfalls.

## Batch 1 — Architecture + `:core:widgetkit` route (docs only) ✅
Commit `4f24da0`. Master plan + extraction route + 2026 sourcing.

## Batch 1.x — Reviews folded in ✅
Commit `8839142`. Reconciled the 4 reviews into a tagged P0/P1/P2 list with
verified current-tree status + the three scope decisions.

## Batch 2 — P0 safety pack ✅
Commit `1583506`.
- P0-1 corruptionHandler on every DataStore (`FeaturePreferencesFactory`).
- P0-2 suspend `requestPin`; janitor off the constructor `runBlocking`.
- P0-3 `StrobeRuntime` `StateFlow<Boolean>` replaces `@Volatile isRunning` +
  the 250 ms poll.
- P0-5 `feature/torch/consumer-rules.pro` + `consumerProguardFiles`.

## Batch 3 — Rooted seam realness ✅
- **P0-4** Relocated `StandardTorchRootCapabilities` out of legacy
  `com.gadget.torch` → `dev.ranzlappen.gadget.feature.torch.standard`
  (stays in `app/src/standard/`, the standard-only flavor source set; updated
  the standard `RootBindings` import).
- Created the `:feature:torch-rooted` sibling module (skeleton parity with
  `:feature:diagnostics-rooted` et al.) + registered it in
  `settings.gradle.kts`.
- Added a CI guard (`build-apk.yml` lint job): hard-fails if any `core/` or
  `feature/` file imports `com.gadget.*` (modular code currently has zero —
  verified). This is the correct scoped enforcement of P1-7's intent; a
  detekt `ForbiddenImport` rule was rejected because detekt is only applied
  to `:app`, which legitimately uses `com.gadget.*` throughout.

### Scope note (important)
A *fully-wired* `:feature:torch-rooted` (real `RootedTorchRootCapabilities`
delegating to the legacy rooted controllers) is **blocked** on extracting the
`com.gadget.root.*` safety framework (`RootSafetyGate`,
`RootCapabilityRegistry`, `RootFeatureKey`, …) — which lives in `:app`'s
source sets — into a `:core:root` module. That is a repo-wide change touching
20+ feature controllers and is out of scope for the torch-blueprint effort;
attempting it blind (no local Android SDK) would risk breaking both flavor
builds and the leak gate. The skeleton module + the relocated standard
namespace bring torch to parity with every other `-rooted` sibling today and
reserve the seam. Tracked at issue #94.
</content>
