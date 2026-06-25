# Decision Records (ADRs)

The architecture decision records. Per the "repo holds only code"
principle, the full ADRs live here in the wiki; the repo keeps no
long-form ADR files. New ADRs are added here as numbered sections; record
**every major architectural choice**.

| ADR | Title | Status | Date |
|---|---|---|---|
| 0001 | Monorepo refactor onto module-per-feature layout | Accepted | 2026-05-12 |
| 0002 | Cross-automation engine — model, runtime, persistence | Accepted | 2026-06-10 |
| 0003 | YouTube downloader — youtubedl-android engine + cookie auth | Accepted | 2026-06-25 |

---

## ADR-0001 — Monorepo refactor onto module-per-feature layout

**Status:** Accepted · **Deciders:** project owner, Claude (implementer),
Grok (reviewer).

### Context

The legacy layout put every screen, Hilt module, Compose helper, and
hardware integration under `app/src/main/java/com/gadget/` in one Gradle
module (~80 kLOC). Cold-cache builds exceeded 2 minutes; most PRs touched
unrelated files (no architectural seam between features); adding a rooted
feature meant trusting AGP source-set scoping + the CI leak gate with no
IDE-visible seam.

### Decision

Adopt the Now-in-Android modular layout: `app/` (single application),
`core/<name>/` (infrastructure), `feature/<name>/` (one capability),
`feature/<name>-rooted/` (root-only sibling), `benchmark/`, `build-logic/`
(convention plugins), `lsposed-module/`. Invariants: `core/*` never depends
on `feature/*`; a feature never depends on another feature directly
(contracts go through `core:domain` / `core:navigation` / `core:hardware`);
the standard APK never compiles against a `*-rooted` module
(`rootedImplementation` only); each module declares its own `namespace`.

### Consequences

**Positive:** faster incremental builds; compile-time enforcement of the
no-rooted-leak invariant on top of the runtime leak gate; per-module
`BuildConfig`; single-feature PRs touch a single module. **Negative:**
onboarding cost (which module?) mitigated by `scripts/new-feature.sh` + the
[Architecture](Architecture) page; per-batch CI cost mitigated by small
bounded batches; Hilt complexity at module boundaries mitigated by the
convention plugins.

### Alternatives rejected

Stay monolithic (build time + merge contention dominate); multi-repo split
(loses the shared rooted/standard CI matrix); dynamic feature modules (the
rooted flavor is sideloaded, not Play install-on-demand); layer-only
modularisation (`ui/`/`data/`/`domain/` re-creates "every PR touches every
module"). Per-feature is the seam that maps to how users think about the
app.

See [Architecture](Architecture), [Flavors & Root
Safety](Flavors-and-Root-Safety).

---

## ADR-0002 — Cross-automation engine: model, runtime, persistence

**Status:** Accepted (design; implemented per epics #145/#146) ·
**Supersedes:** the placeholder design in the old
`docs/automation-engine.md` (now [Automation Engine](Automation-Engine)).

### Context

The product vision *is* the cross-automation engine (`when <trigger> [if
<conditions>] then <actions>`). `:core:automation` already shipped the
action-side contract (`ActionHandler` / `ModuleActionRegistry`, consumed by
widgets); the read-side seam (`MetricSource`) existed for monitoring. What
was missing: a rule model, trigger taxonomy, evaluator, scheduler, runtime,
and UI.

### Decisions

1. **Trigger ≠ Condition** — triggers are the edge that wakes a rule;
   conditions are state gates re-checked at that moment. Event-driven, not
   continuous polling. *Rejected:* a flat "poll all conditions" model.
2. **Reuse both registries verbatim** — actions via
   `ModuleActionRegistry.dispatch`, reads via the `MetricSource` map; the
   engine imports **no feature module**. *Rejected:* a parallel
   automation-specific abstraction (re-introduces `Link`'s central
   hardcoding).
3. **Flat conditions + ALL/ANY, not a nested tree (v1)** — covers most
   rules, keeps the builder legible; the sealed shape leaves room for a
   `Group` later.
4. **Dedicated `AutomationService` FGS, self-stopping when no rule is
   enabled** — metric triggers via `stream()` (push) with a poll fallback;
   schedule triggers via **AlarmManager** (not WorkManager's 15-min floor),
   inexact by default with an `exact` opt-in behind `SCHEDULE_EXACT_ALARM`;
   system events via receivers, `BootCompleted` re-arm reusing widgetkit's
   `BootRearmHandler`. *Rejected:* folding into `MonitorService` (the "one
   FGS" rule targets per-module services; automation is one cross-cutting
   engine); WorkManager as primary scheduler.
5. **Persist rules in `automation.db` with JSON columns for the sealed
   graphs** — flat schema; sealed hierarchies evolve via `@SerialName`
   pinning + a migrator. *Rejected:* normalising into relational tables.
6. **Three-layer root gating** — builder filter, evaluator `rootAvailable`
   filter, dispatch through the `:core:root` gate; never branch on
   `BuildConfig.IS_ROOTED`. *Rejected:* a single layer (the standard APK
   must be *structurally* unable to fire a root action).
7. **Pure-Kotlin evaluator** — `RuleEvaluator.evaluate(...)` is a pure
   function, so the hardest logic is the cheapest to test (JVM, no
   emulator). *Rejected:* an evaluator coupled to the coroutine runtime.
8. **Storms are bounded, not detected** — per-rule cooldown (persisted via
   `last_fired_at`), `clearValue` hysteresis, and an `AutomationBudget`
   (per-cycle + rolling caps) with a throttle notification. *Rejected:*
   static cross-rule cycle detection (undecidable, unnecessary once
   budgeted).

### Consequences

The engine drives every module through seams already tested by widgets +
monitoring; event-driven + push-preferring + self-stopping ⇒ an idle device
costs ~zero wakeups; evaluation (the hardest logic) is the cheapest to
test; root safety is structural. **Negative:** a second FGS notification
when both monitoring and automation are active (mitigated by self-stop;
possible future service consolidation); `:core:automation` gains Android
deps beyond a pure contract (the evaluator stays pure); AlarmManager
exactness depends on Doze + the special permission (degradation contract
specified).

Full design: [Automation Engine](Automation-Engine).

---

## ADR-0003 — YouTube downloader: youtubedl-android engine + cookie auth

**Status:** Accepted · **Deciders:** project owner, Claude (implementer).

### Context

We wanted a `:feature:youtubedownloader` that downloads YouTube video and
audio — including complete **private** playlists — using yt-dlp + ffmpeg,
reusing the option logic from `tools.ranzlappen.com`'s YouTube MP3 Studio.
yt-dlp is Python and ffmpeg is native; neither existed in the app. HardwareDash
is otherwise a hardware/gadget dashboard, so this module is deliberately
off-theme but self-contained.

### Decisions

1. **Engine:** bundle `io.github.junkfood02.youtubedl-android` (library +
   ffmpeg), which ships yt-dlp + ffmpeg + a Python runtime as per-ABI native
   libs and runs **unprivileged** → standard flavor only, no `-rooted` sibling.
2. **Distribution:** the JunkFood02 fork is on **Maven Central** (the original
   yausername artifacts are JitPack-only). This sidesteps the repo's
   `seed-jitpack-cache` machinery entirely — no CI seeding needed.
3. **Private auth:** capture YouTube/Google session cookies via an in-app
   WebView (`CookieManager` → Netscape `cookies.txt`) and pass `--cookies` to
   yt-dlp. Chosen over Google OAuth (which still needs cookies for media) and
   manual cookies.txt import (poor UX).
4. **Execution:** a `dataSync` foreground service drives downloads so long
   playlists survive backgrounding; a `@Singleton YoutubeDlEngine` holds the
   single task-state `StateFlow` consumed by UI, monitoring and automation.
5. **Contract fit:** progress (%) is surfaced as `DownloadMetricSource`
   (monitoring) and download/cancel as `DownloadActionHandler` (automation),
   satisfying the mandatory monitoring + automation seams.

### Consequences

**Positive:** no root requirement; standard option set ported 1:1 from the web
tool; clean Maven Central dependency. **Negative:** **+30–80 MB APK** from the
bundled native runtimes (single biggest cost; ABI splits can trim it); cookie
capture loses HttpOnly cookies the WebView won't surface, and cookies expire so
the UI must prompt re-login on auth failure; downloads currently land in
app-scoped external storage (MediaStore export is a follow-up). Downloading
one's own private content may conflict with YouTube's ToS — left to the user.

### Alternatives rejected

- **Chaquopy** (run yt-dlp as real Python): more upgradable but adds a Python
  build step and larger toolchain.
- **Rooted-only ProcessBuilder + system binaries:** smaller standard APK but
  excludes non-rooted users (the majority).

---

## Adding a new ADR

Append a numbered `## ADR-000N — <title>` section here (Status / Deciders /
Context / Decision(s) / Consequences / Alternatives rejected), add a row to
the index table, and link it from the relevant subsystem page. Update [AI
Collaboration](AI-Collaboration)'s "major architectural choice → Decision
Records" rule if the process changes.

---

> _Last reviewed: 2026-06-12 · Source: `docs/adr/0001-monorepo-refactor.md`,
> `docs/adr/0002-automation-engine.md` · Related: [Architecture](Architecture),
> [Automation Engine](Automation-Engine), [Flavors & Root
> Safety](Flavors-and-Root-Safety)._
