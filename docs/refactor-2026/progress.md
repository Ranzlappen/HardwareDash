# Refactor-2026 — per-batch progress log

Running log of what each batch actually landed (the durable record; the plan
lives in `README.md` + the plan scratchpad). No Android SDK in the container,
so CI is the compile gate — each batch is written to respect the CLAUDE.md
CI-only pitfalls.

## Status: complete for this autonomous run ✅

11 batches landed (1 docs + 10 code/docs). All P0 review items + the
overwhelming majority of P1/P2/P3 items are addressed; the explicit
deferrals (the deep widgetkit value-type/renderer/store extraction, screen
sealed-event rewrite, long-press quick-pin UI, `BootCompletedReceiver`,
`collectAsStateWithLifecycle` rollout, and the fully-wired
`:feature:torch-rooted` impl) are documented below with the precise reason
each needs a compiler in the loop or a separate `:core:root` extraction.

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
  `com.gadget.torch` → `dev.ranzlappen.gadget.feature.torch.standard`.
  Originally kept in `app/src/standard/`; **E2 (final-polish batch) completed
  the move** into the new `:feature:torch-standard` module (mirror of
  `:feature:torch-rooted`) bound by `StandardTorchModule` via
  `standardImplementation`.
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

## Batch 6 — Widget runtime hardening ✅
- **P2-24** All widget receivers (Flashlight / Strobe / Monitor / Chart +
  `WidgetPinSuccessReceiver`) now use the shared `WidgetReceiverScope.scope`
  instead of allocating a fresh `CoroutineScope(SupervisorJob()+Dispatchers.IO)`
  per `onUpdate` / `onReceive` (which leaked the scope object every tap).
- **P2-20** `startForegroundService` is wrapped in try/catch
  (`IllegalStateException`, the `ForegroundServiceStartNotAllowedException`
  supertype) in `StrobeWidgetProvider.onReceive` and
  `TorchViewModel.startStrobeService`; on refusal it logs + shows a graceful
  "couldn't start strobe — open the app" toast instead of crashing.
- **P2-19** `MonitorWidgetProvider` now overrides `onAppWidgetOptionsChanged`
  to re-render on resize, matching `MonitorChartWidgetProvider`.
- **P2-21** `TorchMonitorWidgetNotifier.onSample` self-throttles to one
  repaint per 250 ms so a future high-rate metric can't pelt the launcher
  with RemoteViews broadcasts.

## Batch 7 — Dynamic-creation hardening: count cap ✅
- **P2-14** `TorchWidgetCreator.requestPin` now returns `WidgetPinResult`
  (`Requested` / `LauncherUnsupported` / `CapReached`) instead of a bare
  `Boolean`, and enforces `WidgetPinPolicy.MAX_WIDGETS_PER_KIND` (20) by
  counting placed instances of the provider via `AppWidgetManager`. The
  ViewModel maps the result to the existing snackbar event channels + a new
  `pinCapReachedEvents`; `TorchScreen` shows a "you can pin at most N of each
  kind" message. First real consumer of the kit's `WidgetPinPolicy`/`WidgetPinResult`.
- **P2-15 (long-press quick-pin)** deferred to the screen-refactor batch — it
  is a UI affordance on the widget card best added while that file is
  restructured.

## Batch 8 — Service / FGS polish ✅
- **P2-25** `StrobeService.onStartCommand` returns `START_NOT_STICKY` (was
  `START_STICKY`) so a user-initiated strobe isn't resurrected after a kill;
  added a **Stop** notification action (`PendingIntent.getService` →
  `ACTION_STOP`) so the strobe is controllable from the shade, not only the
  widget; and the channel now explicitly `setSound(null, null)` +
  `enableVibration(false)` to defeat OEM default-sound quirks.
- **P2-26** `StandardTorchController` implements `Closeable` (`close()`
  unregisters the `CameraManager.TorchCallback`) so a test constructing it
  directly can release the OS subscription. Production keeps the
  process-lived singleton.

## Batch 9 — Perf / leak (partial) ✅
- **P2-23** `TorchWidgetConfigRepository.all` is now
  `WhileSubscribed(Long.MAX_VALUE)` instead of `Eagerly`. The original
  `Eagerly` was justified by "providers read via `.value`" — but they
  actually use the suspend `getAll()`/`get()` (for cold-process correctness),
  so the eager subscription paid for nothing. Bounds per-feature idle cost as
  the module count grows.

### Previously skipped — revisited and **implemented** in the final-polish batch
The two items below were skipped in Batch 9 with the analysis preserved here;
the final-polish batch revisited both and shipped them. The original concerns
were real and are addressed head-on rather than ignored:

- **P2-22 (RGB_565 + BitmapPool for `MonitorChartBitmapRenderer`)** — ✅ done.
  - *"The chart has alpha" concern:* true — the old bitmap was transparent so
    the glass card showed through, and the Area fill is translucent
    (`0x33`). The fix makes the chart panel **opaque on purpose**: `render`
    now takes a `backgroundColor` and the provider passes
    `R.color.widget_chart_bg` (`#FF222222`, the alpha-dropped twin of the
    widget's `#33222222` glass fill), so the panel reads as a dark glass-toned
    surface and the translucent teal fill composites over it once. A minor,
    deliberate aesthetic change traded for halving the bitmap.
  - *"Async marshalling makes reuse unsafe" concern:* `updateAppWidget` parcels
    the RemoteViews (and copies the bitmap's pixels) **synchronously** before
    it returns. We don't recycle mid-flight: the bitmap is returned to the
    `BitmapPool` only **after** `updateAppWidget` returns, and a pooled bitmap
    is reused only on the **next** same-size render — i.e. after another full
    synchronous update cycle. So a bitmap is never overwritten while the
    framework still references it. `obtain` also removes the instance from the
    free list, so overlapping repaints get distinct bitmaps.
  - Net: `RGB_565` halves heap + Binder payload (600×280 → ~0.34 MB) and the
    pool removes the ~1 Hz per-widget allocation churn the reviews flagged.
- **P3-29 (WEBP_LOSSY for custom icons)** — ✅ done. The alpha concern is
  unfounded for this encoder: Android's `WEBP_LOSSY` keeps the alpha channel
  (WebP stores alpha separately from the lossy RGB), so icon-shaped
  transparency survives. Saved at q80 (visually lossless at ≤192 px) with a
  deprecated-`WEBP` fallback below API 30. Existing `.png` icons keep decoding
  (format is sniffed, not inferred from the extension).

## Batch 10 — Safe new unit tests ✅
Three additive JVM test files (no integration setup required):
- `MorseCodecTest` (6 tests) — empty/unencodable inputs yield empty timelines;
  single-letter S timing; the canonical SOS timeline shape (18 steps, 9 ON
  steps with the right per-letter durations, trailing word-gap, intra-word
  letter-gaps); `isEncodable` cases.
- `StrobeRuntimeTest` (3 tests) — initial state, set/observe round-trip,
  idempotent writes. Pins the singleton's `StateFlow` contract that widget
  providers + the ViewModel now read instead of the removed
  `@Volatile StrobeService.isRunning`.
- `TorchActionHandlerTest` (4 tests) — `ACTION_TORCH_ON` / `OFF` reach the
  injected `TorchController`; unknown action key returns `ActionResult
  .Unsupported`; `featureId` matches `FEATURE_ID` (a rename would silently
  break the automation engine's `@StringKey @IntoMap` binding). The strobe /
  morse branches go through `Context.startForegroundService` and live behind
  instrumented tests.

### Deferred refactors (compiler-required to land safely)
Two items from the plan are deferred with explicit rationale, since blind
edits would risk the build:
- **Screen refactor (Batch-10-plan):** decompose `TorchScreenContent` →
  `feature/torch/components/` and replace its 17 callbacks with a sealed
  `TorchUiEvent` + `onEvent`. The composable-extraction is mechanical (file
  moves + imports) but the sealed-event rewrite touches the
  `TorchScreenContentTest` androidTest which pins the exact callback
  signature — a missed argument silently breaks the test source set. Safer
  with a compiler in the loop.
- **Long-press quick-pin** (P2-15) — `GadgetSecondaryButton` has no
  `onLongClick`, and wrapping in `combinedClickable` over the button absorbs
  clicks unreliably. The clean fix needs either a `GadgetSecondaryButton`
  signature change in `:core:ui` (touches every consumer) or restructuring
  the WidgetsCard with a separate quick-pin affordance — both compiler-
  required to verify. The cap (P2-14) landed in Batch 7, which is the
  hardening rule the reviews care about most.
- **`collectAsStateWithLifecycle`** (P1-13) — needs `lifecycle-runtime-compose`
  added to `libs.versions.toml` + the feature convention plugin (touches
  every feature module). Additive but version-catalog edits are unsafe to
  ship without compile verification.
- **`BootCompletedReceiver`** (P1-11) — needs a manifest + permission change
  with cross-module Hilt entry-point wiring; defer until widgetkit hosts it.

## Batch 11 — Docs (the final batch) ✅
- **P1-12** Rewrote the stale parts of `docs/migration-guide.md`:
  - The pin flow correctly describes the **DataStore-backed** pending bridge
    (not in-memory), the **suspend** creator API, the explicit `ComponentName`
    on the success callback, the `WidgetPinPolicy` cap + typed
    `WidgetPinResult`, and the `WidgetReceiverScope` for receiver async work.
  - The Torch worked-example table reflects the **current** module shape —
    state-reflecting widget icon, `WidgetReceiverScope` adoption, FGS
    try/catch, `shortService` FGS, `START_NOT_STICKY` + Stop notification
    action, `StrobeRuntime` singleton, `WhileSubscribed` repository, and the
    `consumer-rules.pro` keep rules.
  - Documented the "remove-but-keep-inert" widget pattern in the pin-flow
    section.
  - Updated the FGS anti-pattern entry to require try/catch +
    `shortService` over the obsolete `camera` example.
- **CLAUDE.md** Added a `:core:widgetkit` framework section mirroring the
  monitoring-framework section's shape: what's established (the contract,
  shared scope, pin policy/result), what's deferred (and why), and the
  remove-but-keep-inert blueprint rule every future widget feature must
  honour.

---

## Final summary (everything that landed)

13 commits on `claude/refactor-2026` since the branch was cut from the tip
of the torch refactor work. Coverage of each review item is tagged
`[R1]`–`[R4]` / `[self]` in this log + `docs/refactor-2026/README.md`.

**Persistence:** `FeaturePreferencesFactory` adds `ReplaceFileCorruptionHandler`
(P0-1); widget DataStore files excluded from cloud backup + device transfer
(P1 backup); `TorchWidgetConfig` implements `WidgetKitConfig` and gains
`schemaVersion = 1` (P1-9 partial); `setMorseText` clamped to 2048 chars
(P3-28).

**Pin flow:** `TorchWidgetCreator.requestPin` is **suspend** (no
`runBlocking` on the UI path, P0-2); `PendingTorchWidgetConfigs` janitor
runs on an internal IO scope (P0-2); the creator returns a typed
`WidgetPinResult` and enforces a per-kind cap via `WidgetPinPolicy`
(P2-14); the ViewModel surfaces a `pinCapReachedEvents` snackbar.

**Runtime state:** `StrobeRuntime` singleton `StateFlow<Boolean>` replaces
the `@Volatile StrobeService.isRunning` companion + the 250 ms ViewModel
poll (P0-3). All four widget providers + the pin-success receiver use the
shared `WidgetReceiverScope` (P2-24). `MonitorWidgetProvider` now overrides
`onAppWidgetOptionsChanged` (P2-19). `TorchMonitorWidgetNotifier` throttles
repaints to one per 250 ms (P2-21). `TorchWidgetConfigRepository.all` is
`WhileSubscribed(Long.MAX_VALUE)` (P2-23). `startForegroundService` is
wrapped in try/catch in `StrobeWidgetProvider` and `TorchViewModel` (P2-20).

**Service / FGS:** `StrobeService` returns `START_NOT_STICKY` (P2-25),
notification carries a **Stop** action (P2-25), channel `setSound(null,null)`
+ `enableVibration(false)` (P2-25). `StandardTorchController` implements
`Closeable` to release the OS `TorchCallback` from tests (P2-26).

**Flavor seam:** `StandardTorchRootCapabilities` moved out of the legacy
`com.gadget.torch` package into `dev.ranzlappen.gadget.feature.torch.standard`
(P0-4), and **E2 (final-polish batch)** relocated both standard no-ops into the
new `:feature:torch-standard` module — the mirror of `:feature:torch-rooted` —
so neither flavor's Torch impls live in `:app`. New CI step hard-fails on any
`import com.gadget.*` in `core/` or `feature/` (the leak-rule intent, correctly
scoped — detekt only runs on `:app`).

**Blueprint foundation:** `:core:widgetkit` module created with
`WidgetKitConfig`, `WidgetReceiverScope`, and `WidgetPinPolicy`/`WidgetPinResult`
(P1-6 foundation). Torch is its first consumer.

**R8:** `feature/torch/consumer-rules.pro` keeps the module's
`@Serializable` serializers (P0-5).

**Tests:** `MorseCodecTest`, `StrobeRuntimeTest`, `TorchActionHandlerTest`
(P2-27 partial — pure-JVM coverage; the instrumented + integration ones
need a real Android runtime).

**Docs:** `docs/migration-guide.md` rewritten to match the current state;
`CLAUDE.md` gets a `:core:widgetkit` section + the remove-but-keep-inert
blueprint rule; this `progress.md` is the durable per-batch record.

---

## Explicit deferrals (compiler-required to land safely)

Each item below was rejected for this autonomous run because no Android SDK
is available locally and CI is the only compile/runtime gate — a botched
change risks taking the branch red and burning the other 13 commits' value.
Each is a clean follow-up once a compiler is in the loop:

1. **The deep `:core:widgetkit` extraction (P1-6 remainder)** — moving
   `WidgetAppearance` (sealed `ToggleFeedback`), `WidgetAppearanceRenderer`,
   `WidgetIconCatalog`, the generic resources, `WidgetConfigStore<T>`,
   `PendingWidgetConfigs<T>`, the `BaseGadgetWidgetProvider`, and the pin
   requester. Requires Android resource merging across modules, Hilt
   entry-point rewiring, and pinning the polymorphic discriminator to
   preserve users' on-disk feedback configs. Foundation (contract + scope +
   policy) shipped — torch already consumes it.
2. **Screen refactor (P1-8)** — decompose 759-line `TorchScreenContent.kt`
   into `feature/torch/components/` + replace the 17 callbacks with a sealed
   `TorchUiEvent` + `onEvent`. The instrumented `TorchScreenContentTest`
   pins the exact callback signature; rewriting without a compiler risks
   silently breaking the androidTest source set.
3. **Long-press quick-pin UI (P2-15)** — `GadgetSecondaryButton` has no
   `onLongClick` and `combinedClickable` over a button absorbs taps
   unreliably. Clean fix touches `:core:ui` (signature change) or
   restructures the WidgetsCard. The cap (P2-14) shipped, which is the
   hardening rule the reviews care about most.
4. **`collectAsStateWithLifecycle` rollout (P1-13)** — needs
   `lifecycle-runtime-compose` added to `libs.versions.toml` + the feature
   convention plugin (affects every feature module). Version-catalog edits
   are unsafe to ship without compile verification.
5. **`BootCompletedReceiver` re-arming `MonitorService` (P1-11)** — needs a
   manifest + permission change and cross-module Hilt entry-point wiring.
   Belongs in widgetkit; defer until widgetkit hosts it.
6. **Fully-wired `:feature:torch-rooted` (P0-4 remainder / P1-7)** — gated
   on extracting the `com.gadget.root.*` safety framework (currently in
   `:app/src/rooted/`) to a `:core:root` module — a repo-wide change
   touching 20+ feature controllers, out of scope and unsafe to do blind.
   Sibling module + namespace relocation + CI guard shipped now.
7. **P2-22 `RGB_565` + `BitmapPool` for the chart renderer** — ✅ shipped in
   the final-polish batch. The alpha + reuse-safety concerns were addressed
   rather than worked around — see "Previously skipped — revisited" above.
8. **P3-29 WEBP_LOSSY for custom icons** — ✅ shipped in the final-polish
   batch (`WEBP_LOSSY` preserves alpha; see above).

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
