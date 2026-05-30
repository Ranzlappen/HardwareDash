# Batch 1 — Architecture decisions + `:core:widgetkit` extraction route

> **No code in this batch.** This is the design contract that Batches 2–5
> implement. It records *what* `:core:widgetkit` is, *why* it exists, the
> generic-vs-feature seam (derived from a full read of the live Torch widget
> subsystem), and the step-by-step extraction route — with the 2026
> Android-platform constraints each decision must satisfy.

---

## 1. Why a `:core:widgetkit` module at all

A full read of the Torch widget subsystem (18 files, ~2,400 LoC) shows it
splits cleanly into **generic plumbing** and **torch-specific business
logic**:

| Already 100% generic (zero torch refs) | Mostly generic (params to lift) | Torch-specific |
|---|---|---|
| `WidgetAppearance` + sub-types (`BackgroundMode`, `IconStyle`, `IconTint`, `TapBehavior`, `ToggleFeedback`) | `WidgetIconCatalog` (entries hardcoded), `WidgetFeedbackDispatcher` (channel id) | `TorchWidgetConfig` extra fields (`type`, `rateHz`, `morseMode`, `morseText`) |
| `WidgetAppearanceRenderer` | `TorchWidgetConfigRepository` (file name + type) | `WidgetType` enum |
| `WidgetTint` (`iconTintArgb`) | `PendingTorchWidgetConfigs` (file name) | `FlashlightWidgetProvider` / `StrobeWidgetProvider` toggle logic |
| `playTapPressFrame` | `WidgetPinSuccessReceiver` (action const), `TorchWidgetCreator` (provider branch) | `MonitorWidgetProvider` / `MonitorChartWidgetProvider` metric key |

**The blueprint problem:** a 30-module app means up to 30× re-implementation
of: the pin request flow, the process-death-safe pending-config bridge, the
pin-success receiver, the per-`appWidgetId` config store, RemoteViews
appearance rendering, the icon catalog (incl. custom-PNG import + EXIF), and
toast/notification feedback. That is exactly the legacy mistake the modular
refactor exists to kill (see `CLAUDE.md` — legacy `Link`'s hardcoded
70-metric registry, `LinkActionType`). The fix is the same shape already
proven by `:core:monitoring` and `:core:automation`: **a generic core that
features plug into, with zero central hardcoding.**

> Modularization guidance backs this: "core modules contain code that other
> modules frequently use and reduce redundancy" and each module should depend
> only on lower-level modules to avoid cycles
> ([Common modularization patterns](https://developer.android.com/topic/modularization/patterns),
> [Now in Android Modularization](https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md)).

---

## 2. RemoteViews vs Glance — decision: stay RemoteViews, keep the seam swappable

**Decision: `:core:widgetkit` ships on RemoteViews now; its public API hides
the rendering engine so Glance can be adopted later without breaking
consumers.**

Rationale:
- Glance is **still RemoteViews underneath** and is "restricted by the
  limitations of AppWidgets and RemoteViews"; teams report layout/sizing
  surprises that "only become clear when you ship and test widgets on real
  devices"
  ([Build UI with Glance](https://developer.android.com/develop/ui/compose/glance/build-ui),
  [The Things No One Tells You about Jetpack Glance](https://blog.boxbox.club/the-things-no-one-tells-you-about-jetpack-glance-d3a2993f51c9)).
- The repo already encodes a **hard-won RemoteViews discipline** (the
  `@RemoteView`-safe-layout rule in `CLAUDE.md` that the torch widget
  migration was bitten by). Throwing that away for an alpha-era ergonomics
  win is the wrong trade for a *blueprint*.
- **Seam design:** the kit exposes config + state + a `render(state) ->
  RemoteViews` seam and a `BaseGadgetWidgetProvider`. A future Glance swap
  replaces the renderer internals; `TorchWidgetConfig`, the pin flow, the
  store, and the feedback dispatcher are unaffected. So this is reversible by
  design — we are not betting the blueprint on RemoteViews forever.

Revisit trigger: when Glance reaches the layout fidelity our widgets need and
the team has device-tested it; tracked as a P2 item.

---

## 3. The generic-vs-feature contract (`:core:widgetkit` public API shape)

> Signatures below are **illustrative shape, not final code.** Naming/params
> get finalised in Batches 2–4. The point is to lock the *seam*.

### 3.1 Config layer
- `WidgetAppearance` (+ `BackgroundMode`, `IconStyle`, `IconTint`,
  `TapBehavior`, `TapAnimation`, `ToggleFeedback`) move verbatim into
  `:core:widgetkit` — they are already torch-free.
- A base contract a feature config implements:
  ```
  interface WidgetKitConfig {
      val appearance: WidgetAppearance
      val displayName: String
      val removed: Boolean        // in-app "removed" self-heal flag
  }
  ```
  `TorchWidgetConfig` *implements* it and adds `type`, `rateHz`, `morseMode`,
  `morseText`. The kit never sees torch fields.

### 3.2 Per-`appWidgetId` store
- `WidgetConfigStore<T : WidgetKitConfig>` — generic version of
  `TorchWidgetConfigRepository`, constructed from `FeaturePreferencesFactory`
  with a feature-supplied `fileName`, `keyPrefix`, and `KSerializer<T>`.
  Keeps the hot `StateFlow<Map<Int,T>>` cache, `saveIfAbsent` self-heal,
  `getFresh` cache-bypass. Feature wraps it (or injects it directly).

### 3.3 Pin flow (the dynamic Plus-button path), parameterised
The end-to-end flow stays exactly as audited, but the torch-specific bits
become parameters:
1. **`WidgetPinRequester`** — generic `requestPin(featureId, config,
   providerClass)`. Resolves the provider `ComponentName` from a
   **provider registry** (a Hilt-injected `Map<WidgetKind, Class<out
   AppWidgetProvider>>` the feature contributes) instead of the hardcoded
   `when (config.type)` branch.
2. **`PendingWidgetConfigs<T>`** — generic version of
   `PendingTorchWidgetConfigs`: UUID-token → config in a feature-named
   DataStore, stale-purge janitor, survives process death.
3. **`WidgetPinSuccessReceiver`** base — claims token, saves to the store,
   broadcasts an update to the right provider. The feature subclass only
   supplies its `EntryPoint` (its store + pending bridge) and its action
   constant. The receiver stays manifest-declared + `exported="true"`
   (required to receive the launcher callback) and Hilt-free via
   `EntryPointAccessors` (the audited pattern).

### 3.4 Provider scaffold
- **`BaseGadgetWidgetProvider`** captures the repeated lifecycle: `onUpdate`
  → `goAsync` coroutine → read configs → self-heal missing via
  `saveIfAbsent` → build RemoteViews via the appearance renderer → push;
  `onDeleted` → purge config; `onReceive` action routing. The torch
  providers override only the **action handler** (`toggle()` /
  start-service) and the **active-state resolver** (`isOn`). This removes
  ~80% of the duplicated provider body.

### 3.5 Rendering + icons + feedback
- `WidgetAppearanceRenderer`, `WidgetTint`, `playTapPressFrame`,
  `WidgetIconSource` move in verbatim.
- `WidgetIconCatalog` generalises: the **infrastructure** (custom-PNG import,
  downscale, EXIF, file storage, key resolution) is generic; the **entries
  list** becomes a feature-injected `List<Entry>` (Hilt-provided per
  feature) instead of the hardcoded torch entries.
- `WidgetFeedbackDispatcher` moves in; the notification **channel id/name**
  becomes a constructor/param so each feature (or the app) owns its channel.
  `POST_NOTIFICATIONS` guard stays (API 33+).

### 3.6 Reusable config UI
- The appearance half of `WidgetConfigurationSheet` (background picker, icon
  picker + custom import, tint, tap animation, feedback editor) extracts to a
  `WidgetAppearanceSection` composable in `:core:widgetkit/ui`. Torch's sheet
  composes that section + its torch-only fields (rate, morse). Keeps the
  design-system rules (tokens from `LocalGadgetTheme`, a11y, single-line
  text) and the `:core:ui` Hilt-free discipline.

---

## 4. Module placement, dependencies, and cycle-avoidance

`:core:widgetkit` build (existing convention plugins, no bespoke gradle):
```
plugins {
    id("gadget.android.library")
    id("gadget.android.library.compose")
    id("gadget.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}
dependencies {
    api(project(":core:ui"))            // GlassSurface/tokens for the appearance section
    implementation(project(":core:designsystem"))
    implementation(project(":core:datastore"))  // FeaturePreferencesFactory
    // NO dependency on :core:monitoring or any :feature:* — avoid cycles.
}
```

**Cycle rule:** monitoring widgets currently live in `:feature:torch` but use
`:core:monitoring` repos. After extraction, the *monitor-widget provider
scaffold* is built on `:core:widgetkit`, and `:core:monitoring` gains a
`:core:widgetkit` dependency (monitoring → widgetkit, one direction). The kit
must **not** depend back on monitoring. The metric key stays a parameter the
feature supplies. This keeps the DAG acyclic (`feature → monitoring →
widgetkit → ui/datastore`).

---

## 5. 2026 platform constraints the kit must bake in

These are folded into the kit API now so consumers inherit them for free
(detailed work lands in Batches 6–8, but the *seam* is reserved in Batch 2):

- **Generated previews (Android 15+):** kit exposes a `setWidgetPreviews`
  hook. The API is **rate-limited (~2 calls/hour)** and has **no system
  callback**, so the app decides cadence (on launch / after data)
  ([generated previews](https://developer.android.com/develop/ui/compose/glance/generated-previews),
  [previews](https://developer.android.com/develop/ui/views/appwidgets/previews)).
  Requires `compileSdk ≥ 35` (repo is at 35).
- **Toggle checked-state:** any compound-button RemoteViews must explicitly
  `setCompoundButtonChecked`, or state corrupts on drag/resize
  ([advanced widget](https://developer.android.com/develop/ui/views/appwidgets/advanced)).
- **Resize:** `onAppWidgetOptionsChanged` handling belongs in the base
  provider so every resizable widget (chart widget today) re-renders to the
  new size and bitmaps stay under the RemoteViews transaction-size limit.
- **No `updatePeriodMillis` for fast refresh** — the ~30-min native floor
  means pushes go through the monitoring notifier seam (already true).
- **FGS types (Android 15/16):** `StrobeService` review (Batch 7) — short
  user-initiated flashes fit `shortService` (**3-min hard cap, cannot be
  promoted**), `dataSync` now has a **6-h cap**, and Android 16 enforces
  job-quota + richer `ProgressStyle` notifications
  ([FGS changes](https://developer.android.com/develop/background-work/services/fgs/changes),
  [Android 15 FGS-type changes](https://developer.android.com/about/versions/15/changes/foreground-service-types)).
- **Process-death-safe persistence:** the pin flow already persists the
  pending config to disk *before* `requestPinAppWidget`, so a kill between
  request and callback is recoverable. Keep that invariant; add a
  `ReplaceFileCorruptionHandler` to the DataStore factory (Batch 8) and
  remove `runBlocking` from the broadcast path.

---

## 6. Flavor isolation (standard vs rooted) — kit-level rule

`:core:widgetkit` lives under `src/main` and **must never** import su/root
types. Rooted-only widget extras (e.g. a future brightness-boost widget)
follow the proven pattern: feature-side capability interface, app-flavor
no-op (standard) + real (rooted) Hilt bindings — the kit only ever sees the
interface. This keeps the **standard-APK leak gate** green by construction
(no `topjohnwu`/`libsu`/root-permission strings can reach the kit). Documented
in `docs/flavors.md` in the final batch.

---

## 7. Extraction route — ordered, atomic, reversible

The extraction is sequenced so the build is **green after every batch** and
Torch keeps working throughout (no big-bang swap):

1. **Batch 2 — scaffold + move the leaf-generic types.** Create the module,
   apply convention plugins, wire `settings.gradle.kts`. Move the
   zero-coupling types in (`WidgetAppearance` family, `WidgetTint`,
   `WidgetAppearanceRenderer`, `WidgetIconSource`, `playTapPressFrame`). At
   this point torch still references its own copies — **no torch change yet**,
   module just compiles standalone. *(Lowest-risk first.)*
2. **Batch 3 — generic store + pending bridge + pin-success base.** Port
   `WidgetConfigStore`, `PendingWidgetConfigs`, `WidgetPinSuccessReceiver`
   base; unit-test round-trip + token claim + stale purge. Still no torch
   change.
3. **Batch 4 — base provider + pin requester + provider registry.** Port the
   `goAsync`/self-heal/render harness and the registry-driven `requestPin`.
4. **Batch 5 — flip Torch onto the kit.** `TorchWidgetConfig` implements
   `WidgetKitConfig`; torch providers extend `BaseGadgetWidgetProvider`;
   `TorchWidgetCreator` uses the registry; delete `PendingTorchWidgetConfigs`
   and the duplicated bodies; move monitor widgets onto the kit (monitoring →
   widgetkit dep). Torch is now the **reference consumer**. Run the existing
   torch widget tests + add migration tests.
5. **Batches 6–12** — hardening, FGS, persistence, perf, flavors, UI sweep,
   docs (see roadmap).

**Reversibility:** Batches 2–4 add code without touching torch, so they can
ship and bake before the Batch 5 flip. If Batch 5 reveals a seam problem, the
fix is localised to the kit API, not scattered across torch.

---

## 8. Acceptance for Batch 1

- [x] Full read of the torch widget subsystem; generic-vs-feature seam mapped.
- [x] Architecture decisions recorded with 2026 sources (Glance, FGS,
      previews, modularization, DataStore).
- [x] `:core:widgetkit` public-API shape + module deps + cycle rule defined.
- [x] Ordered, reversible extraction route (green-after-every-batch).
- [x] Full file-touch list for the whole effort (see `README.md`).
- [ ] **Blocked on input:** reconcile against the 4 external reviews (not yet
      provided) before locking P0/P1/P2 and Batch 6+ scope.

**No production code changed in Batch 1.**
</content>
