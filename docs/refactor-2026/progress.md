# Refactor-2026 — per-batch progress log

Running log of what each batch actually landed (the durable record; the plan
lives in `README.md` + the plan scratchpad). No Android SDK in the container,
so CI is the compile gate — each batch is written to respect the CLAUDE.md
CI-only pitfalls.

## 2026-06 — Engine milestone follow-up (post PR #148)

> **STANDING BLOCKER (Mode C, load-bearing):** Room **`schemas/` JSON is not
> committed for any modular DB** — `automation.db` (new), `apps.db`,
> `monitoring.db`. Mode C physically cannot generate them (no Android SDK;
> `room.schemaLocation` export needs a real build), and CI doesn't archive
> them. **No Room migration test can exist until `schemas/` is backfilled,
> and that MUST happen before any of these DBs goes to v2.** Clears in one
> Mode L session (domain allowlist → local `assembleDebug` exports all three
> → commit). Until then, no schema-version bump on a modular DB.

**Batch F (engine runtime, branch `claude/automation-engine-runtime`) — F1
(pure-logic core) this push:**
- **Manual-vs-cooldown decision (Matthias):** Manual triggers **bypass the
  cooldown check** but the runtime still calls `markFired` (cooldown gates
  automated triggers only; a manual run still delays the next automatic
  fire). Implemented in `RuleEvaluator` (`rule.trigger is Trigger.Manual`),
  JVM-tested (manual fires inside the window; the same cooldown on a
  SystemEvent trigger is still enforced). Documented in the design doc's
  Runtime host section (the open item is now a decision) + the runtime
  contract that `evaluate` receives the rule's own trigger instance.
- **`AutomationBudget`** (pure, clock-injected) in `:core:automation/engine`:
  per-cycle cap (16) + rolling window cap (60 / 60 s), `admit(now, requested)
  → Admission(allowed, throttled)`, overflow dropped not queued. Exhaustive
  JVM tests (per-cycle clamp, rolling clamp across cycles, exact-windowMs
  expiry, partial expiry, zero-request no-op, spec defaults).
- **Next pushes (same PR):** F2 the Android runtime — `AutomationService`
  (FGS, resident only while ≥1 enabled rule has a metric-stream trigger),
  `AutomationScheduler` (AlarmManager degradation contract), system-event
  receivers + boot re-arm via the widgetkit `BootRearmHandler` multibinding,
  the throttle notification wired to `AutomationBudget`; F3 the end-to-end
  instrumented test (rule → dispatch → TorchController) added to the
  instrumented-tests matrix.


**Batch A (merge + baseline):** PR #148 merged to `main` (merge `d461c89`);
parity metric re-confirmed at **310** (`find app/src -path "*com/gadget*"
-name "*.kt" | wc -l`); engine design doc + ADR-0002 now canonical on `main`.

**Batch B (environment probe → MODE C):** this container **cannot build
Android locally** — the Android SDK host `dl.google.com` is blocked by the
network egress policy (`curl -sI …commandlinetools-linux… → HTTP/2 403
host_not_allowed`), and no SDK is pre-installed (`ANDROID_HOME` empty, no
`~/android-sdk`). Java is 21 (build targets 17), moot without the SDK.
**Decision: Mode C (CI-iterated)** — every code batch is verified through CI
via draft PRs (`ci-refactor` / `build-apk` on code paths, `instrumented-tests`
for emulator suites); batch design biases toward pure-JVM logic. Future
sessions: don't re-probe — egress is locked down.

**Batch C (scaffolder skeleton-fill + sensors stub):** branch
`claude/scaffolder-skeleton-mode`. Added auto-detected **skeleton-fill mode**
to `scripts/new-feature.sh` — for the Batch-0 empty skeletons it generates
sources only, reads the namespace from the existing build file (handles
hyphenated names like `radios-bt`), never overwrites the build file, skips the
settings.gradle.kts append, and appends a `:core:ui`+`:core:navigation`
`dependencies` block only when absent; refuses if sources already exist.
Verified by first real use: ran it against `:feature:sensors` and committed
the generated stub (the starting commit of Batch G), wired minimally into
`:app` per the scaffolder's declared manual steps so CI actually compiles it
— `implementation(project(":feature:sensors"))` + swapping
`placeholderScreen(GadgetDestination.Sensors)` for `sensorsScreen()` in
`MainActivity` (`GadgetDestination.Sensors` already existed). Without this the
module sits outside `:app`'s build graph and `assembleStandardDebug` never
compiles it. Guards (refuse-on-existing-sources, base-mode hyphen rejection)
and the hyphenated path tested locally. `docs/migration-guide.md` Step 3
updated (skeleton mode documented; "not CI-exercised" → verified).
Merged as PR #149 (merge `55e262b`) after a review follow-up commit applying
manual step #1 (SensorsNavigation registers at `GadgetDestination.Sensors
.route` instead of a string-coincident module-local const).

**Batch D (legacy-reachability audit — decision pending):** branch
`claude/legacy-reachability-audit`. Produced
`docs/refactor-2026/legacy-reachability.md` (the audit half of #147).
Headline: the legacy screens were already deleted by the clean-cut, so the
app is under a **de-facto strict clean-cut today** — every remaining
`com.gadget.*` capability package is an orphaned engine room (only consumers:
the retiring `RootFeaturesEntryPoint` + orphaned `root/ui` extras cards),
every manifest-registered legacy service is inert (zero reachable starters),
and `WidgetUpdateWorker` periodically services a provider that is no longer
registered. Recommendation: **strict clean-cut** — the bridge option would
mean resurrecting screens from the `legacy-main` archive at migration-scale
cost per feature. Three reversible interim cleanups listed (C1 stop the
useless worker, C2 = #107 deletion, C3 unregister inert components).
**HARD STOP: the policy decision + WS5 ordering are Matthias's** — recorded
in the doc's decision record when made; Batch G's migration scope waits on it
(the sensors *stub* from Batch C is unaffected).
**Resolved 2026-06-11:** strict clean-cut confirmed (PR #151, merge
`11122a1`); WS5 default order confirmed; C1+C2 approved, C3 deferred.

**Batch D follow-up — C1+C2 legacy-widget cleanup:** branch
`claude/legacy-widget-cleanup`. C1: deleted `WidgetUpdateWorker` and replaced
`MainActivity`'s `schedule(this)` with a one-shot
`cancelUniqueWork("widget_periodic_update")` so upgraded installs stop the
orphaned 15-minute wakeup (the KEEP-policy periodic work survives app
updates; the cancel is what kills it). C2 (#107): deleted the unregistered
legacy `com.gadget.widget.{Flashlight,Strobe}WidgetProvider` and their
`NotifActionEntry.FLASHLIGHT/STROBE` enum entries (sole compile-time
consumers; `BuilderPresetStore` decodes persisted entries via
`runCatching { valueOf } .getOrNull()`, so stale presets degrade to null).
`widget_action.xml` stays — 10 other (dead-but-compiled) providers reference
it; it dies with the package per the audit's per-feature deletions. Parity
310 → 307.

**Batch E (engine core, PR #150 — E1+E3 shipped, E2+E4 this push):**
branch `claude/automation-engine-core`, Mode C (draft PR, CI-iterated).
- **E1 — model + serialization:** `Rule`/`RuleAction`/`Trigger`/`Condition`
  sealed families in `:core:automation/model`, every persisted subtype with a
  pinned `@SerialName` FQN; `AutomationJson` (ignoreUnknownKeys,
  encodeDefaults=false); `RuleSerializationTest` (round-trips, 6 discriminator
  pins, enum wire names, defaults-tolerant + unknown-key decode). **Plus a
  found CI gap:** no workflow ran any JVM unit test (even widgetkit's
  wire-format pins never executed) — `ci-refactor.yml` gains a scoped
  `unit-tests` job (`:core:automation` + `:core:data` + `:core:widgetkit`).
- **E3 — evaluator:** pure-Kotlin `RuleEvaluator` (disabled → cooldown
  strict-< → trigger match → ALL/ANY fold → requiresRoot filter) +
  `MetricThresholdGate` (stateless arm/fire machine; edge + hysteresis; no
  fire-on-subscribe). Tests cover the design doc's named list incl. cooldown
  just-under/at/over and the proximity Lt5/clear8 noise sequence.
- **E2 — persistence:** `automation.db` in `:core:data`
  (`RuleEntity`/`RuleDao`/`AutomationDatabase` v1 + `RoomRuleRepository`
  bound to the `RuleRepository` contract in `:core:automation`); pure
  `RuleMapper` (JSON columns ↔ model, JVM-tested). **Review P2 folded in:**
  `Rule.normalized()` / `normalizedClearValue()` null a wrong-side
  `clearValue` on every save (the Falling-Lt5/clear8 degenerate-hysteresis
  footgun), with the validity table tested per (op, edge) cell and a warning
  KDoc on the gate. Design-doc amendments in the same PR (no silent drift):
  schema line gains `cooldown_seconds`; the module-invariant sentence fixed
  to the real dependency edge (`:core:data` → `:core:automation` — the
  reverse would be a cycle).
- **E4 — backup v5:** `automation.db` rides the existing generic
  `databases/` sweep, so the change is the `BACKUP_FORMAT_VERSION` 4→5 bump
  + version-history/comment docs; restored-from-rooted rules are defanged by
  the evaluator's root filter (layer 2).
- **Known gap:** Room schema JSON for `AutomationDatabase` v1 cannot be
  generated in this environment (no SDK) and CI doesn't export it — the
  committed-`schemas/` convention is currently unmet for *all three* modular
  DBs (monitoring/apps/automation alike); needs one local build to backfill.
- **Open item for Batch F (review P3):** Manual triggers currently obey
  cooldown — decide bypass-vs-"on cooldown" feedback; documented in the
  design doc's Runtime host section.

## 2026-06 — "Get back on track" plan (doc-resync + scaffold + automation design)

Branch `claude/repo-plan-execution-5ch33k`. Executed the low/zero-risk front
of the get-back-on-track plan (Workstreams 1–3.1). No Android SDK in this
container, so code-heavy workstreams (automation engine core/runtime/UI,
`:core:hardware`, feature migrations) are intentionally **not** landed blind —
they need a compiler in the loop. What landed here is docs + a verifiable bash
scaffolder:

- **WS1.1 — doc re-sync** (`docs:` commit). `CLAUDE.md` header now states
  Phase 2 (Accelerated Feature Migration, in progress) with Torch/Vibration/
  App-Organizer live and the `:core:root/widgetkit/monitoring/automation/
  hardware` infra layer landed; dropped the stale "no real hardware code yet"
  line. `MASTER-PLAN.md`: canonical branch is now `main`; Phase-2 sub-track
  table marked Done with PR ranges; follow-up issues reconciled; Phase-3
  forward plan added. `settings.gradle.kts`: corrected stale Batch-0 comments
  (Kotlin DSL, rooted wiring + build-logic already landed).
- **WS1.2 — issue triage** (GitHub). Closed #91 (GadgetBottomSheet tests —
  `ModalsTest`), #92 (instrumented-tests.yml gates PRs), #94 (rooted torch
  extras — `:feature:torch-rooted` on `:core:root`), each with an
  evidence-linked comment. Re-labelled #89 phase-1→phase-3 (posture seam
  landed but no consumer yet, so it stays open). Filed forward-plan epics:
  #145 (automation engine), #146 (`:core:hardware` registry), #147
  (legacy-reachability decision).
- **WS2.1 — `scripts/new-feature.sh`** (`feat(scripts):` commit). Replaced the
  Batch-0 placeholder with a working scaffolder (base module + optional
  `--rooted` sibling pair + settings.gradle.kts registration). Verified the
  generated tree + settings insertion against a throwaway run and confirmed
  every referenced `:core:ui` API exists with the used signature; the gradle
  build itself is unverified locally (no SDK). `docs/migration-guide.md`
  Step 3 now points at it.
- **WS3.1 — automation design** (`docs(automation):` commit). Replaced the
  placeholder `docs/automation-engine.md` with a real design and added
  `docs/adr/0002-automation-engine.md`. Specifies rule model, trigger/condition
  taxonomy with pinned `@SerialName`s, the pure-Kotlin evaluator, the
  self-stopping `AutomationService` runtime (AlarmManager not WorkManager,
  push-preferring metric subscriptions, widgetkit boot re-arm), `automation.db`
  persistence, and three-layer root gating.

**Deferred (need a compiler / out of this session's safe scope):** WS3.2–3.4
(engine core/runtime/UI), WS6 (`:core:hardware`), WS4.2 execution, WS5
feature long-tail. Epics #145/#146/#147 track them.

### Review fix-up (PR #148)

One docs-only follow-up commit addressing the PR #148 review:
- **P1-1** — fixed the CLAUDE.md applicationId contradiction (current install
  IDs are `dev.ranzlappen.gadget` / `.rooted`, not the legacy `com.gadget*`).
- **P1-2** — bounded automation storms in the v1 model (design doc + ADR
  Decision 8): per-rule `cooldownSeconds` (persisted via `last_fired_at`),
  `MetricThreshold.clearValue` hysteresis, and a runtime `AutomationBudget`
  (16/cycle + 60/60 s) with a throttle notification; evaluator gains a
  `sinceLastFiredMillis` param + cooldown/hysteresis tests.
- **P2-5** — exact-alarm degradation contract (`Schedule.exact` flag, the
  three-state table, `SCHEDULE_EXACT_ALARM` denied-by-default posture,
  `USE_EXACT_ALARM` explicitly unused).
- **P2-6** — `automation.db` joins the backup ZIP, bumping format v4 → v5.
- **P2-7** — honest legacy-parity metric (310 across all `:app` source sets;
  195 in `src/main`) with the canonical `find` command.
- **P3** — CLAUDE.md `:core:hardware` reworded as reserved-but-empty;
  settings.gradle.kts comment made present tense; MASTER-PLAN vibration row
  gets PR refs (#130 + #134/#135/#137); migration-guide "compiles standalone"
  softened to unverified; design-doc FGS-resident-only-for-stream-triggers
  sentence added.

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
