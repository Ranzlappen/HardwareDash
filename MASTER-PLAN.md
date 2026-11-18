# MASTER PLAN — Gadget v3 ("pragmatic")

> **The source of truth for direction.** If a wiki page, a code comment or an
> older plan contradicts this file, this file wins (the maintainer's explicit
> instruction in a task still wins over everything). Wiki pages describe *how*
> things work; this file says *what we build next and why*.
>
> Branch: `claude/v3-pragmatic-overhaul` · Started: 2026-11-18 ·
> Archives: `legacy-main` (v1), `archive/v2-modular-2026` (v2, `main` @ `2cf5b40`).

---

## 0. One-paragraph summary

v1 was a rough single-module app that felt like a **tool**: five tabs, a
dashboard with live status, and screens that did the obvious thing (see every
sensor, take a photo, look at a spectrum, write an NFC tag). v2 rebuilt it as a
79-module monorepo with a strong design system, safe root/standard flavors and
a real CI gate — but it optimised for **architectural completeness** instead of
**usefulness**: every module had to be "monitoring- and automation-ready",
every screen had to show rooted-capability rows, and the app lost several of
v1's most practical features along the way. v3 keeps v2's foundations, puts the
user back at the centre, and finishes fewer things properly.

---

## 1. Lineage

| Version | Where | Shape | What it got right | What hurt |
|---|---|---|---|---|
| **v1** | `legacy-main` (`e0c582c`) | 1 module, `com.gadget`, ~53k Kotlin lines, 14 screens | Task-oriented 5-tab nav, live dashboard, broad practical features | God-screens (Radios = 2,664 lines), `BuildConfig` branching, SharedPreferences in composables, no tests |
| **v2** | `archive/v2-modular-2026` (`2cf5b40`) | 79 Gradle modules, `dev.ranzlappen.gadget`, ~103k Kotlin lines, 33 top-level destinations | Design system, flavor/root safety seam + leak gate, widget kit, CI, clean package | Over-generalised, breadth over depth, lost v1 features, docs claim ✅ for unverified code |
| **v3** | `claude/v3-pragmatic-overhaul` → `main` | v2 codebase, trimmed and re-centred | — | — |

v3 is **not** a rewrite and **not** a big-bang re-merge. It starts from v2's
code and changes it in small reviewable batches.

---

## 2. What went wrong in v2 (evidence, measured 2026-11-18)

1. **Navigation stopped scaling.** All 33 destinations sit in one icon-only
   side rail on phones (`GadgetNavRail`, labels only at Expanded width). v1's
   five labelled tabs (Dashboard / Tools / Monitor / Logbook / More) were
   replaced by a scrolling column of icons you have to guess.
2. **The home screen became a launcher list.** `DashboardScreen` renders one
   empty `DashCard` per module (title + icon, no content). v1's dashboard
   showed battery, Wi-Fi, quick actions and the latest log entry.
3. **The contract made every module pay for infrastructure.** The Module
   Authoring Contract made `MetricSource` + `ActionHandler` "non-negotiable"
   and rooted-capability rows "required for all feature modules". Result:
   **46** MetricSources, **37** ActionHandlers, **135** `ModuleAction`s — of
   which **24 are `assert_*` "actions"** that only re-implement what
   `Condition.MetricCompare` / `Trigger.MetricThreshold` already do
   (and `ambient_assert_bright` duplicates `light_assert_bright`).
4. **Fragmentation instead of features.** One v1 Sensors screen became three
   partial modules (`sensors` = proximity/light/accel, `ambient` = light again,
   `motion` = gyro/steps). Magnetometer, barometer, gravity and rotation
   vector — all in v1 — are **gone**. Similar splits: `vibration` vs
   `actuators`, `audio` vs `microphone`, `adbdebug` vs `usbdebug` (same OS
   setting, two modules, two MetricSources), `diagnostics` vs `bugreport`
   vs the Settings permissions dashboard.
5. **v1's most practical features were lost in migration.**
   - Camera: v1 had live preview, lens picker (ultrawide/main/tele/front),
     zoom, focus, exposure and photo capture. v2's camera module is a
     **barcode scanner only**.
   - Microphone: v1 had a live FFT spectrum analyzer and playback of saved
     recordings. v2 has a dB meter and WAV recording, no spectrum, no player.
   - Sensors: see 4. File-metadata/EXIF viewer: gone.
6. **"Extreme tier" rooted controllers nobody has verified.** **95**
   `RootFeatureKey`s, many writing guessed vendor sysfs nodes (wireless coil
   current, IR-LED GPIO bursts, v4l2 HAL bypass, raw NCI over sysfs, modem
   dumps). Safety-gated, but there is no record of any of them working on a
   real device, and they shape every screen through mandatory rows.
7. **Docs certified intent, not behaviour.** Roadmap & Status marks ~38
   features ✅; the Completion Master Plan's definition of done counts
   seams (screen ✅ · MS ✅ · AH ✅ · tests ✅ · 4 languages ✅), never
   "a user did the thing on a phone". 27 wiki pages / ~4,800 lines had to be
   kept in sync on every PR, and drifted anyway (the plan itself records the
   drift).
8. **Scope creep away from hardware.** A YouTube downloader (yt-dlp + ffmpeg)
   lives next to the torch. The completion plan aimed at "god-app"
   breadth: widgets *everywhere*, tiles *everywhere*, four languages,
   benchmarks, Play release — before the core tools were solid.

None of this was malicious or careless; each rule was locally reasonable. The
compound effect was an app that is **comprehensive on paper and hard to use**.

---

## 3. Keep / simplify / drop / bring back

| Keep from v2 (it earns its cost) | Simplify | Drop / freeze | Bring back from v1 |
|---|---|---|---|
| `:core:designsystem` + `:core:ui` components and tokens | Navigation → 5 labelled sections | 24 `assert_*` ActionHandler actions (use metric conditions) | Five-tab, task-oriented navigation |
| Standard/rooted flavor split, `:core:root` seam, `RootSafetyGate`, leak gate | Module contract → small required core + opt-in extras | Mandatory rooted-capability rows on every screen | Live dashboard (status cards + quick actions) |
| `dev.ranzlappen.gadget` package, no `com.gadget` imports | Overlapping modules merged (see §5) | Mandatory MetricSource/ActionHandler | Full sensor explorer (every sensor, copy reading) |
| `:core:widgetkit` (where widgets exist) and the generic metric widget | Preview matrix → LightDark minimum | Unverified extreme-tier root actions (hidden → removed if never verified) | Camera preview + lens/zoom/capture |
| Automation engine (`:core:automation`) — it works and is tested | Wiki → fewer, shorter pages | `:benchmark` skeleton, Play/benchmark/4-language work until core is solid | Audio spectrum analyzer + recordings player |
| CI gates (unit tests, both flavors, leak gate) | Status reporting → verified-on-device | — | NFC write + tag details (verify v2 parity) |

---

## 4. v3 principles

1. **A feature is done when a person can do its job on a phone.** Not when it
   has seams. Status is reported as *verified on device*, not as checklists.
2. **Complete features over comprehensive skeletons.** Finish one tool
   properly before starting the next. No placeholder screens, no
   "architecture-ready" stubs.
3. **Modularity in service of usability.** A module boundary must reduce
   coupling or protect the standard APK. Don't split one screen's worth of
   function across modules; don't merge modules just to reduce a count.
4. **No abstraction until there are two real consumers.** A new framework,
   seam or base class needs two features that actually use it today.
5. **Integrations are opt-in.** MetricSource, ActionHandler, widgets, tiles and
   rooted rows are added when they give a user something (a chart worth
   watching, an action worth automating, a toggle worth one tap) — never to
   satisfy a contract.
6. **Root must be honest.** A rooted action ships visible only once verified on
   a named device; everything else is "Experimental" (hidden by default) and is
   removed if it stays unverified.
7. **Stay a hardware toolkit.** New work must be about exploring or
   controlling phone hardware (or the device itself). Anything else needs an
   explicit maintainer decision.
8. **Small, reviewable, phone-friendly batches.** ≈5–8 files per commit,
   CI verdict on every push, pause for review after each batch.
9. **Fewer docs, kept true.** This plan + a short status table + the few
   how-to pages that are actually used. Don't document what the code says.
10. **Don't repeat v2 in reverse.** No big-bang re-merge, no mass renames, no
    rewrite of working code for style. Change what blocks usability.

---

## 5. Target shape

### 5.1 Navigation — five sections

Phones (Compact width) get a labelled **bottom bar**; Medium/Expanded get a
labelled **rail**. Each section opens a hub listing its tools; tools are pushed
on top (system/predictive back returns to the hub).

| Section | Contents (v3 target) | Interim (before merges) |
|---|---|---|
| **Home** | Live dashboard: status cards (battery, storage, network, last log), quick toggles (torch, vibrate), pinned tools | current Dashboard |
| **Tools** | Torch · Vibration · Camera · Audio · Display · Files (metadata/EXIF) · Apps · Logbook | + Actuators, Microphone |
| **Sensors** | Sensor explorer · Battery · Storage · GPS | + Ambient, Motion |
| **Connect** | Wi-Fi · Bluetooth · Cellular · NFC · IR · Sub-GHz · Flipper | same |
| **More** | Automation · Notifications · Lock · Device & debug · Manual · Settings | + ADB, USB, Diagnostics, Health, Downloader |

### 5.2 Module fate map

| Today (v2) | v3 | Batch |
|---|---|---|
| `sensors`, `ambient`, `motion` | **`sensors`** — explorer over `SensorManager.getSensorList()` with per-sensor live values + chart + copy; keeps the useful MetricSources | V3-2 |
| `vibration`, `actuators` | **`vibration`** (absorbs amplitude/haptic test) | V3-2 |
| `audio`, `microphone` (+rooted) | **`audio`** (+`audio-rooted`); mic extreme tools become a rooted section | V3-2 |
| `adbdebug`, `usbdebug`, `diagnostics`, `bugreport` (+4 rooted) | **`devtools`** (+`devtools-rooted`): debug state, dumps, permission health | V3-2 |
| `camera` (+rooted) | **`camera`**: preview + lens/zoom/focus/exposure + capture, scanner as a mode | V3-3 |
| `torch-standard`, `vibration-standard` | folded into the app-flavor binding pattern used by every other module | when touched |
| `automation` (screenless) | merged into `automation-ui` | when touched |
| `benchmark` | removed (re-add when there is something to measure) | V3-5 |
| `youtubedownloader` | kept, frozen, listed under More (§8.1) | — |
| everything else | kept as is; fixed only where a smoke test fails | — |

Expected result: ~79 → ~65 Gradle modules. The count is a side-effect, not a
goal.

### 5.3 Module contract (replaces "Module Authoring Contract" v2)

**Required for every user-facing module**

1. Does its core job end-to-end on a real phone (smoke-test line in
   [`docs/v3-smoke-test.md`](docs/v3-smoke-test.md)).
2. Design system: `LocalGadgetTheme.current` tokens, `:core:ui` components,
   no raw `dp` at call sites, a11y basics (labels, 48 dp targets).
3. Flavor safety: rooted code only behind the `:core:root` Hilt seam; never
   `BuildConfig.IS_ROOTED`; no su under `src/main`; leak gate green.
4. Strings in `strings.xml` (English required; translations best-effort).
5. Unit tests for non-trivial pure logic it owns (parsers, serializers, safety
   clamps). UI tests optional.

**Add only when it earns its place**

- `MetricSource` — the signal is worth charting or triggering on.
- `ActionHandler` — the action has a real side effect someone would automate.
  Never add `assert_*` actions; conditions belong to the engine.
- Widget / QS tile — the job is commonly a one-tap thing.
- Rooted section — a rooted function exists *and* is verified (or flagged
  Experimental).
- Previews — `@GadgetPreviewLightDark` minimum; the full matrix only for
  shared `:core:ui` components.

**Still hard rules:** new code under `dev.ranzlappen.gadget.**`; no
`com.gadget.**` imports; no feature→feature dependencies (route via `core/*`);
small atomic commits; wiki/plan kept in sync.

### 5.4 Status vocabulary

| Mark | Meaning |
|---|---|
| ✅ **Works** | Smoke-tested on a named device by the maintainer |
| 🟡 **Partial** | Works, but a listed part of its job is missing/broken |
| ⚪ **Unverified** | Code exists and compiles; nobody confirmed it on hardware |
| 🧪 **Experimental** | Rooted/vendor-specific; hidden by default |
| ✂️ **Merged/cut** | Folded into another module or removed |

Every v2 "✅" starts v3 as ⚪ until it passes the smoke test.

---

## 6. Roadmap (phases → batches)

Each batch: one commit series on `claude/v3-pragmatic-overhaul`, CI verdict on
push, then **pause for review**. A phase is merged to `main` via one PR once CI
is green and its smoke-test lines pass.

### V3-0 · Reset (this batch series)
- [x] Archive v2 → `archive/v2-modular-2026`.
- [x] This plan, the v2 retrospective (§2), the smoke-test checklist.
- [x] CLAUDE.md + wiki aligned (contract, collaboration, roadmap, ADR-v3).
- [x] **B1** CI also runs on pushes to `claude/v3-**` (compile verdict without a PR).

### V3-1 · Navigation & Home
- [x] **B2** Five-section shell: bottom bar (phone) / rail (wide), section hubs
      with one-line tool summaries; re-tapping a section returns to its hub;
      dashboard tiles and "open settings" links push instead of resetting the
      stack. Mapping pinned by `GadgetSectionTest`.
- [ ] **B3** Live Home: status cards (battery, storage, Wi-Fi, last log entry)
      and quick toggles (torch, vibrate) reusing existing controllers; keep the
      pin/hide editor.
- [ ] **B4** Up-navigation on tool screens (back arrow in `ScreenHeader` when not
      a section root); deep links land inside the right section. Known bug:
      `MainActivity` never reads `GadgetDestination.EXTRA_ROUTE`, so tapping a
      logbook reminder notification opens Home instead of the Logbook.

### V3-2 · Consolidate overlaps
- [ ] **B5** Sensor explorer (all sensors, live values, chart, copy); retire
      `ambient` + `motion` routes, keep their MetricSource keys stable.
- [ ] **B6** Remove the 24 `assert_*` actions; migrate any saved rules to
      `MetricCompare` conditions (or drop them with a one-time notice).
- [ ] **B7** `vibration` ← `actuators`; `audio` ← `microphone`.
- [ ] **B8** `devtools` ← adb/usb/diagnostics/bugreport; one permissions UI
      (Settings) instead of two.

### V3-3 · Bring back v1's practical tools
- [ ] **B9** Camera: preview, lens picker, zoom, tap-to-focus, exposure,
      capture to MediaStore; scanner becomes a mode.
- [ ] **B10** Audio: live FFT spectrum; recordings list with playback/share/delete.
- [ ] **B11** NFC parity check vs v1 (write, tag type, protection info); fill gaps.
- [ ] **B12** File metadata / EXIF viewer (approved, §8.3): browse a file via
      the system picker, show name/size/MIME/dates, read + edit image EXIF,
      audio/video media metadata.

### V3-4 · Honest root
- [ ] **B13** Tag every `RootFeatureKey` Verified/Experimental; Settings toggle
      "Show experimental root tools" (default off); rooted sections render only
      verified + opted-in tools.
- [ ] **B14** Remove Experimental actions still unverified after one release.

### V3-5 · Polish & release
- [ ] Wiki slimmed to: Home, AI Collaboration, Module Contract, Design System,
      Flavors & Root Safety, Troubleshooting, Status. Others archived.
- [ ] Full smoke-test pass on the maintainer's device; signed standard + rooted
      release from `main`.

**Frozen until V3-5 is done:** new widgets/tiles beyond existing ones, new
languages, macrobenchmarks, Play-store work, LSPosed repackage, new
automation features, new feature modules.

---

## 7. Migration approach

- **Start point:** v2's `main` (`2cf5b40`). Nothing is deleted from history;
  both archives stay read-only references.
- **v1 as reference:** read with `git show legacy-main:<path>`; port *behaviour*,
  not code (no `com.gadget` imports, rebuild on the design system).
- **v2 → v3 merges:** move code into the surviving module, keep public keys that
  persist user data (MetricSource keys, DataStore keys, rule JSON
  `@SerialName`s) stable, delete the emptied module in the same batch.
- **Persisted data:** never break saved rules, widgets, logbook or app folders
  without a migration or an explicit maintainer OK.
- **CI is the compile gate** (no local SDK). Every push to the v3 branch runs
  `ci-refactor.yml`; a red build is fixed before the next batch.

---

## 8. Decisions (maintainer, 2026-11-18)

1. **YouTube downloader** — kept, **frozen**, listed under More. No further
   investment; revisit only if it breaks.
2. **Root verification device** — a rooted **Samsung Galaxy A17** is the test
   device for rooted tools (the daily Xiaomi 14T Pro is not rooted and covers
   the standard flavor). A rooted action becomes Verified only once it works
   on the A17; vendor-specific paths that only exist on other SoCs stay 🧪.
3. **File metadata / EXIF viewer** (v1) — **bring back** (B12 approved).
4. **Merge cadence** — one PR per phase onto `main` once CI is green and the
   phase's smoke-test lines pass. V3-0 + V3-1 go together in the first PR.

## 9. Working agreement

- Branch: `claude/v3-pragmatic-overhaul` only. Never push to `main`.
- Batches of ≈5–8 files; commit → push → pause for review.
- Each batch updates this file's checkboxes and, if behaviour changed, the
  matching wiki page and smoke-test line.
- When this file and the code disagree, fix the file in the same batch.
