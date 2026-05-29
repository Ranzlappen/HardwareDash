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

## ⚠️ Open input gap — the 4 external reviews

The task references **4 detailed external AI reviews of the Torch module
(PR #120 state)**. They are **not present** in the repo, the uploads dir, or
the plan scratchpad — I searched all three. Batch 1 (architecture +
`:core:widgetkit` route) is **review-independent**, so it proceeds on
codebase analysis + 2026 research + my own audit. **Before the prioritised
P0/P1/P2 list is finalised** (and before Batch 6+ scope is locked), the 4
reviews must be pasted/uploaded so every point they raise is cross-checked
against §"Prioritised recommendations" below. Items sourced purely from my
own analysis are tagged `[self]`; once the reviews arrive, each review point
gets a `[R1]`…`[R4]` tag and any gap is added.

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

> **Provisional — `[self]` items only until the 4 reviews are folded in.**
> Final ordering is set once reviews land (each becomes `[R1]`…`[R4]`).

### P0 — blueprint-blocking (must fix before Torch is "canonical")
- `[self]` Extract `:core:widgetkit`; remove all hand-rolled-per-feature
  widget plumbing. (Batches 2–5)
- `[self]` Kill `runBlocking` on broadcast/main-thread paths in widget
  providers/creator; replace with `goAsync` + structured suspend. (Batch 8)
- `[self]` Audit every settings/config surface for process-death round-trip;
  add corruption handler. (Batch 8)
- `[self]` Bring `StrobeService` into Android 15/16 FGS-type compliance
  (type, 3-min `shortService` cap behaviour, start-from-background rules). (Batch 7)

### P1 — hardening & scalability (needed for 30+ modules)
- `[self]` Widget count cap + graceful "limit reached" UX. (Batch 6)
- `[self]` Long-press quick-pin (skip the config sheet for a sensible default). (Batch 6)
- `[self]` Generated widget previews via `setWidgetPreviews` (Android 15+,
  rate-limited ~2/hr). (Batch 6)
- `[self]` Bitmap lifecycle / RemoteViews transaction-size guards in chart
  widget; recycle, size-to-widget. (Batch 9)
- `[self]` Compose stability + baseline profile for torch screen. (Batch 9)
- `[self]` `setCompoundButtonChecked` for any toggle-style RemoteViews to
  survive drag/resize. (Batch 6)

### P2 — polish & future-proofing
- `[self]` UI primitive consistency sweep across the catalog. (Batch 11)
- `[self]` Rendering-engine-agnostic seam so a future Glance migration is
  internal. (designed in Batch 2, no migration now)
- `[self]` Macrobenchmark coverage for widget render + screen scroll. (Batch 9)

---

## Batch roadmap (atomic, on `claude/refactor-2026`)

Each batch: implement → commit (clear message) → push → pause with
**"Batch X complete — ready for review."**

| Batch | Title | Output |
|---|---|---|
| **1** | Architecture + `:core:widgetkit` route | **This doc-set (no code).** |
| 2 | Scaffold `:core:widgetkit`; move generic appearance/render/icon/tint/feedback | new module compiles, torch still uses its own copies |
| 3 | Generic config store + pending bridge + pin-success base | kit pin plumbing, unit-tested |
| 4 | Base provider scaffold + generic pin requester (provider registry) | kit provider harness |
| 5 | Migrate Torch + monitoring widgets onto the kit | torch is first consumer; legacy torch widget plumbing deleted |
| 6 | Widget hardening: count cap, long-press quick-pin, generated previews, checked-state | hardened dynamic creation |
| 7 | `StrobeService` / FGS Android 15-16 compliance + ProgressStyle | FGS-correct |
| 8 | Persistence audit: process-death round-trip, corruption handler, kill `runBlocking` | bulletproof persistence |
| 9 | Perf/leak audit: RAM/CPU/GPU/storage, bitmaps, Compose stability, baseline profile, macrobench | measured |
| 10 | Rooted/standard flavor-separation audit incl. widgetkit seam | leak gate green by construction |
| 11 | UI primitive consistency sweep | immaculate UI |
| 12 | `CLAUDE.md` + Module Authoring Contract update; reconcile P0/P1/P2 vs 4 reviews | blueprint documented |

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
