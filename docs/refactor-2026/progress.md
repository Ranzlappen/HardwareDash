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

### Skipped (with analysis)
- **P2-22 (RGB_565 + BitmapPool for `MonitorChartBitmapRenderer`)** — R4
  asserted "the chart has no alpha", but it actively does: the bitmap starts
  *transparent* (the widget background shows through) and the Area layout uses
  a *translucent* `fillColor` (alpha `0x33` for torch). `RGB_565` drops both,
  which would replace transparency with solid black/junk and turn the
  translucent fill opaque. Keep ARGB_8888. Bitmap pooling + `recycle()` after
  `setImageViewBitmap` is also unsafe — RemoteViews marshals the bitmap into
  IPC asynchronously; recycling too early can crash the launcher process. The
  size cap (600×280 ≈ 0.67 MB, already documented) keeps it well under the
  Binder transaction limit.
- **P3-29 (WEBP_LOSSY for custom icons)** — would regress transparency on
  user-imported PNG icons (lossy WEBP doesn't preserve alpha well). Custom
  icons are user images; alpha matters. Keep PNG.

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
