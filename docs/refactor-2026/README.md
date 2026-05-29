# Phase 2 — Torch Blueprint Hardening (`claude/refactor-2026`)

> **Goal:** make the Torch module the *canonical, immaculate* blueprint that
> every future feature migration (sensors, actuators, radios, …) is cloned
> from. Future-proof, 2026 best-practice, fully reusable. This directory is
> the planning record for the multi-batch hardening effort on branch
> `claude/refactor-2026`. **No production code lands in Batch 1** — it is
> architecture + the `:core:widgetkit` extraction route only.

Branch base: created from the tip of `claude/torch-module-refactor-4JDmF`
(`624159f`), which is the most complete Torch module (dynamic Plus-button
widget creation + persisted/live monitoring already present).

---

## The 4 external reviews — now folded in

The 4 external AI reviews of the PR-#120/main Torch state have been provided
and reconciled against the **current** tree (this branch is ahead of the
reviewed state, so some findings were already fixed here). All four land
**Approve with notes** and converge on one message: the migration is
excellent but premature as a "blueprint" because ~2,500 LOC of generic widget
framework lives inside `:feature:torch`. Review 4 names the highest-leverage
change explicitly: **extract `:core:widgetkit`**.

Every finding is tagged `[R1]`–`[R4]` (which review raised it) in the
prioritised list below, with its verified current status. Items I added from
my own analysis / 2026 research are tagged `[self]`.

---

## How this maps to the requirements

| Requirement (from task) | Where addressed |
|---|---|
| Torch = canonical, reusable blueprint | Whole effort; Module Authoring Contract update (final batch) |
| Consistent UI primitives (buttons/cards/charts/settings/widgets/notifications/toasts) | Batch 11 (UI sweep) + `:core:widgetkit` (widget/notification/toast primitives) |
| 100% persistent settings across restart + process death | Batch 8 (persistence audit) |
| Widgets/notifications/toasts reliable when app killed | `:core:widgetkit` (Batches 2–6) + Batch 7 (FGS) |
| Crystal-clear rooted vs standard separation | Batch 10 (flavor-seam audit) |
| RAM/CPU/GPU/storage leak + scalability for 30+ modules | Batch 9 (perf/leak audit) |
| Harden dynamic widget creation (count cap, long-press quick-pin) | Batch 6 (widget hardening) |
| Prioritised P0/P1/P2 list (4 reviews + own analysis) | §"Prioritised recommendations" (finalised once reviews arrive) |
| Detailed atomic-batch execution plan on `claude/refactor-2026` | §"Batch roadmap" |

---

## High-level architecture decisions (Batch 1)

These are the load-bearing decisions the rest of the effort is built on.
Rationale + sources are in
[`batch-01-architecture-and-widgetkit.md`](./batch-01-architecture-and-widgetkit.md).

1. **Extract a new `:core:widgetkit` module.** The Explore audit shows the
   Torch widget subsystem is ~70% generic plumbing tangled with ~30%
   torch-specific business logic. A 30-module app cannot have every module
   re-hand-rolling pin flows, pending-config bridges, RemoteViews appearance
   rendering, icon catalogs, and toast/notification feedback. The generic
   half graduates to `:core:widgetkit`; Torch becomes its first consumer and
   reference. **This is the single biggest blueprint win and is therefore
   Batch 1's design focus.**

2. **Stay on RemoteViews, not Glance — for now.** Glance is still
   RemoteViews-bound, has documented real-device layout/sizing surprises,
   and the repo already has a hardened, CI-aware RemoteViews discipline
   (the `@RemoteView`-safe layout rule in `CLAUDE.md`). `:core:widgetkit`'s
   API will be **rendering-engine-agnostic** (config + state + a render
   seam) so a later Glance migration is an internal swap, not a consumer
   break. Decision revisited per-batch; see sources.

3. **One shared foreground service, never per-module.** Already true for
   monitoring (`MonitorService`, `specialUse`). Re-affirm it as a blueprint
   invariant and bring `StrobeService` under the Android 15/16 FGS-type
   rules (`shortService` 3-min cap, `dataSync` 6-h cap, ProgressStyle
   notifications).

4. **Preferences DataStore for widget/feature config, Proto only where a
   strict schema pays for itself.** The existing `FeaturePreferencesFactory`
   (kotlinx-serialization-over-Preferences, shared `Json` with
   `ignoreUnknownKeys`) is the right seam and already process-death safe.
   Harden it with a `ReplaceFileCorruptionHandler` and eliminate
   `runBlocking` on the broadcast/main path. `:core:widgetkit` reuses this
   factory rather than inventing storage.

5. **Convention-plugin module, zero new build divergence.**
   `:core:widgetkit` applies the existing `gadget.android.library` +
   `gadget.android.library.compose` + `gadget.android.hilt` plugins — no
   bespoke gradle. It depends only on `:core:ui`, `:core:designsystem`,
   `:core:datastore` (and `:core:monitoring` is the *consumer* of the
   monitor-widget seam, not a dependency — avoid the cycle).

6. **Flavor isolation is a `:core:widgetkit` design constraint, not an
   afterthought.** The kit must never reference su/root types; rooted widget
   extras follow the existing app-flavor source-set + Hilt-binding pattern.
   The standard-APK leak gate stays green by construction.

---

## Target module shape after the effort

```
:core:widgetkit                ← NEW. Generic home-screen widget framework.
  config/                      Generic WidgetAppearance + sub-types, base config contract
  render/                      WidgetAppearanceRenderer, WidgetTint, RemoteViews helpers
  icons/                       WidgetIconCatalog infra (entries injected by feature)
  feedback/                    WidgetFeedbackDispatcher (toast/notification), channel seam
  pin/                         Generic pin flow: Creator + PendingConfig bridge + PinSuccessReceiver base
  provider/                    Base AppWidgetProvider scaffold (goAsync harness, self-heal, render loop)
  ui/                          Reusable appearance-config Compose section (sheet building blocks)

:core:monitoring               ← consumes :core:widgetkit for monitor/chart widgets
:feature:torch                 ← first consumer; torch-specific config + providers + sheet
```

---

## Full file-touch list (entire effort, all batches)

> Anticipated scope. Items may shift once the 4 reviews land; this is the
> working set, not a contract. **(N)** = new file, **(M)** = modified,
> **(D)** = deleted/moved.

### `:core:widgetkit` (new module) — Batches 2–4
- `settings.gradle.kts` (M) — `include(":core:widgetkit")`
- `core/widgetkit/build.gradle.kts` (N)
- `core/widgetkit/src/main/AndroidManifest.xml` (N)
- `core/widgetkit/.../config/WidgetAppearance.kt` (N — moved from torch)
- `core/widgetkit/.../config/WidgetKitConfig.kt` (N — base config contract)
- `core/widgetkit/.../render/WidgetAppearanceRenderer.kt` (N — moved)
- `core/widgetkit/.../render/WidgetTint.kt` (N — moved)
- `core/widgetkit/.../render/RemoteViewsExt.kt` (N — `playTapPressFrame`, helpers)
- `core/widgetkit/.../icons/WidgetIconCatalog.kt` (N — generalised; entries injected)
- `core/widgetkit/.../icons/WidgetIconSource.kt` (N — moved)
- `core/widgetkit/.../feedback/WidgetFeedbackDispatcher.kt` (N — moved, channel parametrised)
- `core/widgetkit/.../pin/WidgetPinRequester.kt` (N — generic `requestPin`, provider registry)
- `core/widgetkit/.../pin/PendingWidgetConfigs.kt` (N — generalised pending bridge)
- `core/widgetkit/.../pin/WidgetPinSuccessReceiver.kt` (N — base receiver scaffold)
- `core/widgetkit/.../provider/BaseGadgetWidgetProvider.kt` (N — lifecycle/coroutine/self-heal harness)
- `core/widgetkit/.../store/WidgetConfigStore.kt` (N — generic per-appWidgetId repo over FeaturePreferences)
- `core/widgetkit/.../ui/WidgetAppearanceSection.kt` (N — reusable Compose appearance editor)
- `core/widgetkit/.../di/WidgetKitModule.kt` (N)
- `core/widgetkit/src/main/res/...` (N — generic widget drawables/colors/layout contract)
- `core/widgetkit/src/test/...` (N — store round-trip, pending-claim, appearance serialization)

### `:feature:torch` — Batches 5–7
- `feature/torch/build.gradle.kts` (M) — depend on `:core:widgetkit`
- `feature/torch/.../widget/TorchWidgetConfig.kt` (M — wrap generic appearance, keep torch fields)
- `feature/torch/.../widget/WidgetType.kt` (M — feature discriminator + provider registry entry)
- `feature/torch/.../widget/TorchWidgetConfigRepository.kt` (M — thin wrapper over generic store)
- `feature/torch/.../widget/PendingTorchWidgetConfigs.kt` (D — replaced by generic)
- `feature/torch/.../widget/TorchWidgetCreator.kt` (M — delegate to generic requester + count cap)
- `feature/torch/.../widget/WidgetPinSuccessReceiver.kt` (M — extend base)
- `feature/torch/.../widget/WidgetUpdates.kt` (M — use generic helpers)
- `feature/torch/.../widget/FlashlightWidgetProvider.kt` (M — extend base scaffold)
- `feature/torch/.../widget/StrobeWidgetProvider.kt` (M — extend base scaffold)
- `feature/torch/.../widget/MonitorWidgetProvider.kt` (M — via widgetkit + monitoring seam)
- `feature/torch/.../widget/MonitorChartWidgetProvider.kt` (M — same)
- `feature/torch/.../widget/customization/WidgetIconCatalog.kt` (M — torch entries via injected list)
- `feature/torch/.../ui/WidgetConfigurationSheet.kt` (M — compose generic section + torch fields)
- `feature/torch/.../strobe/StrobeService.kt` (M — FGS-type + ProgressStyle hardening, Batch 7)
- `feature/torch/.../tile/FlashlightTileService.kt` (M — review for consistency)
- `feature/torch/src/main/AndroidManifest.xml` (M — provider registration, FGS types, previews)
- `feature/torch/.../TorchViewModel.kt` (M — widget count cap state, quick-pin)
- `feature/torch/.../TorchScreenContent.kt` (M — UI sweep + count-cap affordance)
- Torch widget tests (M/N)

### Monitoring seam — Batch 5
- `core/monitoring/build.gradle.kts` (M) — depend on `:core:widgetkit`
- `core/monitoring/.../MonitorWidgetNotifier.kt` (M — generic refresh seam)
- `core/monitoring/.../*` monitor-widget base contract (N) — so monitor widgets are kit-built

### Widget hardening — Batch 6
- widgetkit: `WidgetCountPolicy.kt` (N — per-feature cap), quick-pin long-press support
- widgetkit: generated-preview support (`setWidgetPreviews`, Android 15+) (N)
- compound-button checked-state + `onAppWidgetOptionsChanged` base handling (M)

### Persistence audit — Batch 8
- `core/datastore/.../FeaturePreferencesFactory.kt` (M — `ReplaceFileCorruptionHandler`)
- `core/datastore/.../FeaturePreferences.kt` (M — remove blocking reads on hot path)
- audit & fix any `runBlocking` in receivers/providers (M across torch + widgetkit)

### Perf / leak audit — Batch 9
- `:benchmark` macrobenchmark for torch widget render + screen (N/M)
- baseline profile wiring (M)
- bitmap lifecycle in `MonitorChartBitmapRenderer` / chart widget (M)
- Compose stability annotations sweep (M, targeted)

### Flavor separation audit — Batch 10
- `app/src/standard/.../torch/*` and `app/src/rooted/.../torch/*` binding review (M)
- `:core:widgetkit` rooted-extra seam doc + (if needed) flavor binding pattern (M/N)

### UI primitive consistency — Batch 11
- `core/ui/.../component/*` targeted consistency fixes (M)
- torch screen primitives sweep (M)

### Docs / contract — final batch
- `CLAUDE.md` (M — `:core:widgetkit` catalog, hardened Module Authoring Contract)
- `docs/refactor-2026/*` (M — fill P0/P1/P2 from reviews, per-batch notes)
- `docs/flavors.md` (M — widgetkit flavor rule)

---

## Prioritised recommendations (P0 / P1 / P2)

Reconciled from the 4 reviews + own analysis, verified against the current
tree. `[R1]`–`[R4]` = source review; `[self]` = own/2026. Items already fixed
on this branch (state-reflecting widget icon `[R3]`, `shortService` FGS +
corrected KDoc `[R2,R3]`, DataStore-backed pin bridge `[R2,R4]`, atomic
`saveIfAbsent` `[R4]`, standard-APK leak gate `[R4]`, app backup rules `[R4]`)
are **not** relisted. Batch column = where each lands.

### P0 — must fix before the kit (verified NOT DONE)
- **P0-1** `[R4]` `ReplaceFileCorruptionHandler` on every DataStore in
  `FeaturePreferencesFactory`. (Batch 2)
- **P0-2** `[R2,R4]` Kill `runBlocking` on UI/broadcast path: `requestPin`
  suspend; `purgeStale` off the constructor. (Batch 2)
- **P0-3** `[R2,R3,R4]` `StrobeRuntime` `StateFlow<Boolean>` replaces
  `@Volatile isRunning` + 250 ms poll. (Batch 2)
- **P0-4** `[R4]` Relocate `StandardTorchRootCapabilities` out of legacy
  `com.gadget.torch`. (Batch 3)
- **P0-5** `[R4]` `consumer-rules.pro` keep rules for all `@Serializable`
  types. (Batch 2)

### P1 — required for the blueprint claim
- **P1-6** `[R1,R4]` Extract `:core:widgetkit`. (Batches 4–7)
- **P1-7** `[R3,R4]` `:feature:torch-rooted` sibling + relocate to
  `dev.ranzlappen…torch.standard` + detekt rule blocking `com.gadget.*`. (Batch 3)
- **P1-8** `[R2,R4]` Decompose `TorchScreenContent` → `components/` + sealed
  `TorchUiEvent`. (Batch 10)
- **P1-9** `[R4]` `schemaVersion` + `Migrator<T>` on configs. (Batch 5)
- **P1-10** `[R4]` `GadgetColorPicker` → `:core:designsystem`. (Batch 4)
- **P1-11** `[R4]` `BootCompletedReceiver` re-arming `MonitorService`. (Batch 6)
- **P1-12** `[R2,R3,R4]` Rewrite stale `docs/migration-guide.md`. (Batch 13)
- **P1-13** `[R2]` `lifecycle-runtime-compose` + `collectAsStateWithLifecycle`. (Batch 10)

### P2 — scalability / correctness
- **P2-14** `[R4]` Per-kind widget count cap (`Result`). (Batch 8) *(task req.)*
- **P2-15** `[R4]` Long-press quick-pin. (Batch 8) *(task req.)*
- **P2-16** `[self]` Generated widget previews (`setWidgetPreviews`). (Batch 8)
- **P2-17** `[R4]` `WidgetType` carries provider `Class` / registry. (Batch 6)
- **P2-18** `[R4]` Monotonic-counter pending keying. (Batch 5)
- **P2-19** `[R4]` Symmetric `onAppWidgetOptionsChanged`. (Batch 8)
- **P2-20** `[R4]` try/catch `ForegroundServiceStartNotAllowedException`. (Batch 8)
- **P2-21** `[R4]` Debounce `TorchMonitorWidgetNotifier`. (Batch 8)
- **P2-22** `[R4]` `RGB_565` + `BitmapPool` for chart bitmaps. (Batch 11)
- **P2-23** `[R4]` `all` → `WhileSubscribed`. (Batch 11)
- **P2-24** `[R4]` Shared `WidgetReceiverScope`. (Batch 6)
- **P2-25** `[R2,R3]` `START_NOT_STICKY` + notification Stop + `setSound`. (Batch 9)
- **P2-26** `[R2,R3]` `TorchCallback` Closeable/unregister + `TorchHardware`. (Batch 9)
- **P2-27** `[R2,R3,R4]` Missing tests (pending/morse/handler/controller/VM/race). (Batch 12)

### P3 — polish
- **P3-28** `[R4]` Clamp `setMorseText`. (Batch 11)
- **P3-29** `[R4]` WEBP_LOSSY custom icons. (Batch 11)
- **P3-30** `[R3]` Adaptive hero-box height. (Batch 10)
- **P3-31** `[R4]` Chart colours → `colors.xml`. (Batch 13)
- **P3-32** `[R4]` APK-size CI delta. (Batch 11)
- **P3-33** `[R4]` Document remove-but-keep-inert pattern. (Batch 13)
- **P3-34** `[R4]` `:core:notifications` (optional/deferred).

---

## Batch roadmap (atomic, on `claude/refactor-2026`)

Each batch: implement → commit (clear message) → push → pause with
**"Batch X complete — ready for review."**

| Batch | Title | Output |
|---|---|---|
| **1** ✅ | Architecture + `:core:widgetkit` route | doc-set (committed `4f24da0`) |
| 2 | P0 safety pack | corruptionHandler · suspend `requestPin` + purgeStale · `StrobeRuntime` · consumer-rules.pro |
| 3 | Rooted seam realness | relocate standard no-op · ship `:feature:torch-rooted` · detekt `com.gadget.*` rule · leak-gate verify |
| 4 | `:core:widgetkit` scaffold + leaf-generic move | new module · move appearance/render/icon/tint + `GadgetColorPicker`→designsystem · torch untouched |
| 5 | Kit store + pending bridge + pin-success base | `WidgetConfigStore<T>` · `PendingWidgetConfigs<T>` (counter) · `schemaVersion`+`Migrator` · unit tests |
| 6 | Kit provider scaffold + requester + boot | `BaseGadgetWidgetProvider` · `WidgetReceiverScope` · provider registry · `WidgetPinRequester` · `BootCompletedReceiver` |
| 7 | Flip Torch onto the kit | `TorchWidgetConfig : WidgetKitConfig` · providers extend base · monitor widgets via kit · slot `WidgetTypeFields` |
| 8 | Dynamic-creation hardening | count cap · long-press quick-pin · generated previews · checked-state · symmetric resize · FGS try/catch · debounce |
| 9 | Service / FGS polish | `START_NOT_STICKY` + Stop action + `setSound` · ProgressStyle · `TorchCallback` Closeable + `TorchHardware` |
| 10 | Screen refactor | decompose → `components/` + sealed `TorchUiEvent` · `TorchInputs` · `collectAsStateWithLifecycle` · adaptive hero |
| 11 | Perf / leak | `RGB_565`+`BitmapPool` · `WhileSubscribed` · WEBP icons · clamp morseText · macrobench + baseline · APK-size CI |
| 12 | Tests | the six missing suites |
| 13 | Docs + contract | rewrite `migration-guide.md` · `CLAUDE.md` widgetkit catalog + hardened contract · remove-inert · final reconciliation |

---

## Sources (2026 best-practice research)

- Glance vs RemoteViews maturity / production guidance:
  [Build UI with Glance](https://developer.android.com/develop/ui/compose/glance/build-ui),
  [Manage and update GlanceAppWidget](https://developer.android.com/develop/ui/compose/glance/glance-app-widget),
  [The Things No One Tells You about Jetpack Glance](https://blog.boxbox.club/the-things-no-one-tells-you-about-jetpack-glance-d3a2993f51c9)
- Foreground service changes (14/15/16):
  [Changes to foreground services](https://developer.android.com/develop/background-work/services/fgs/changes),
  [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types),
  [Changes to FGS types for Android 15](https://developer.android.com/about/versions/15/changes/foreground-service-types),
  [Android Foreground Services in 2026](https://dev.to/joe_wang_6a4a3e51566e8b52/android-foreground-services-in-2026-what-changed-and-how-to-adapt-2o3d)
- Widget previews / RemoteViews reliability:
  [Add generated previews to your widget picker](https://developer.android.com/develop/ui/compose/glance/generated-previews),
  [Add previews to your widget picker](https://developer.android.com/develop/ui/views/appwidgets/previews),
  [Create an advanced widget](https://developer.android.com/develop/ui/views/appwidgets/advanced)
- Modularization / convention plugins:
  [Guide to Android app modularization](https://developer.android.com/topic/modularization),
  [Common modularization patterns](https://developer.android.com/topic/modularization/patterns),
  [Now in Android — Modularization Learning Journey](https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md)
- DataStore:
  [App Architecture: Data Layer — DataStore](https://developer.android.com/topic/libraries/architecture/datastore),
  [Stop Using SharedPreferences: Mastering Jetpack DataStore in 2026](https://medium.com/@kemal_codes/stop-using-sharedpreferences-mastering-jetpack-datastore-in-2026-b88b2db50e91)
</content>
</invoke>
