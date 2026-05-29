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

## Batch 4 — `:core:widgetkit` foundation ✅
Created the `:core:widgetkit` module (registered in `settings.gradle.kts`) with
the safe, compiling, **new** generic seam — no risky cross-module moves:
- `WidgetKitConfig` — the per-instance config contract (`displayName`,
  `removed`, `schemaVersion`) future feature configs implement.
- `WidgetReceiverScope` — one process-lifetime scope replacing the per-tap
  `CoroutineScope(...)` each provider leaked (P2-24 seam; torch adopts later).
- `WidgetPinPolicy` + `WidgetPinResult` — per-kind cap + a typed pin outcome
  (foundation for P2-14 count cap; torch adopts in the hardening batch).

### Why the deep extraction is NOT done blind
The resource/Hilt/serialization-coupled layer (the `WidgetAppearance`
value-type family, `WidgetAppearanceRenderer`, `WidgetIconCatalog`, the
providers, the pin-flow store) is the bulk of the reusable code, but moving it
requires THREE things CI-less editing cannot verify:
1. **Android resource merging** — the renderer references `R.id.widget_*` /
   `R.drawable.widget_background_*`; moving them repoints R-class lookups
   across modules.
2. **Hilt graph** — `@Singleton`/`@EntryPoint` rewiring across the new edge.
3. **kotlinx.serialization wire format** — `ToggleFeedback` is a *sealed*
   (polymorphic) type whose on-disk JSON discriminator is derived from its
   package; moving it silently breaks every existing user's persisted widget
   feedback unless the discriminator is pinned exactly (unverifiable blind).
So the module + contract + shared infra are established now; the value-type /
renderer / provider migration is the explicit compiler-required follow-up. The
remaining review items that are *localized and safe* are completed in Batches
8–13 instead of waiting on the extraction.

## Batch 5 — Persistence hardening ✅
- **P1-9 (partial)** `TorchWidgetConfig` now implements `WidgetKitConfig` and
  carries `schemaVersion: Int = 1` (additive serialized field — wire-safe:
  old JSON missing it decodes to the default, the round-trip tests still
  pass). Torch now depends on `:core:widgetkit` (first real consumer). The
  `Migrator<T>` seam is deferred to the compiler-in-loop store extraction.
- **backup** Excluded the device-specific widget DataStore files
  (`torch_widgets` + `torch_pending_widgets`) from cloud backup AND
  device-transfer in `backup_rules.xml` / `data_extraction_rules.xml` —
  appWidgetIds can't map onto another device's launcher.
- **P3-28** `UserPreferencesRepository.setMorseText` clamps to 2048 chars so a
  pasted novel can't bloat the preferences file.

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
