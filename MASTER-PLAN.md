**MASTER PLAN: HardwareDash (Gadget) – Future-Proof Modular Refactor (v2.0 – May 2026)**

**Vision**
HardwareDash (app name: Gadget) will become the definitive Android app for exploring every sensor and actuator, with sophisticated cross-automation, widgets, notification panels, granular permission management, and hardware-safety guardrails. The rooted version safely extends functionality while the standard version remains fully functional and safe.

**Current State**
- Branch `main`: canonical. Phase 0 + Phase 1 + Phase 1.1 hardening complete; Phase 2 (accelerated feature migration) **in progress** (June 2026). All work now lands on `main` via one PR per `claude/<topic>` batch branch — the long-lived `claude/refactor-2026` integration branch is retired.
- **Migrated & live in the shell:** Torch (+ rooted/standard siblings), Vibration (+ rooted/standard siblings), App-Organizer (folders + folder widgets), Settings, Dashboard.
- **Infrastructure landed:** `:core:root` (root-safety seam), `:core:widgetkit` (home-screen-widget framework), `:core:monitoring` (chart + persist framework), whole-app backup format v5, plus the full cross-automation engine — `:core:automation` (contract + rule model + evaluator + runtime), `automation.db` in `:core:data`, `:core:hardware`'s `HardwareRegistry`, and the `:feature:automation-ui` rules list + builder.
- **Legacy surface shrinking:** 310 legacy `com.gadget.*` Kotlin files remain
  across ALL `:app` source sets (195 in `src/main`; the rest in the
  rooted/standard flavor + test source sets — the flavor trees are future
  migration work too). Canonical metric, tracked per batch in
  `docs/refactor-2026/progress.md`:
  `find app/src -path "*com/gadget*" -name "*.kt" | wc -l`
- Branch `legacy-main`: Full archive of the old single-module codebase — reference material only, never imported from new code.

---

**Phase 0: Future-Proof Repo Structure & Foundation** (✅ Complete — May 2026)
- Modular monorepo with `core/`, `feature/`, `build-logic/`
- Convention plugins for zero config drift
- New applicationId (`dev.ranzlappen.gadget` / `.rooted`) for side-by-side install
- Light preview / skeleton-first readiness

**Phase 1: Light Preview / Skeleton App** (✅ Complete — May 2026)
- Phase 1.0 — component library 1a–1g (Buttons / GlassSurface / CompactCard / TextFields / Modals / StatusIndicators / LoadingStates / EmptyState)
- Phase 1.0 — deferred infrastructure X1–X7 (`LocalGadgetTheme` data class, a11y locals, `WindowSizeClass`-aware shell, `:core:testing` module, preview matrix, instrumented tests)
- Phase 1.1 — hardening sweep (full `LocalGadgetTheme` wiring, accessibility semantics, responsiveness, glass consistency, shimmer width, preview matrix expansion, SSOT `CLAUDE.md`)

**Phase 2: Accelerated Feature Migration** (🚧 In Progress — May 2026)

**Accelerated approach** — no more placeholder screens. Each feature migrates directly from `legacy-main` into a `:feature:<name>` module using the new design system (`:core:ui`), component library (`DashCard` / `GadgetPrimaryButton` / etc.), token plumbing (`LocalGadgetTheme.current`), and Hilt-injected state.

**Clean-cut policy** — `legacy-main` is reference material. New code never imports from `com.gadget.**` paths; it lives entirely under `dev.ranzlappen.gadget.feature.<name>.*`. Legacy code stays archived in `legacy-main` per the deprecation policy.

**Repeatable migration process** — see [`docs/migration-guide.md`](docs/migration-guide.md). Every future module migration follows the eight-step recipe documented there, with the Phase-2-Batch-1 Torch migration as the first worked example.

| Sub-track | Status | Notes |
|---|---|---|
| Settings v1 (About + Appearance + Accessibility) | ✅ Done | `:core:datastore` + `:feature:settings` |
| Torch / Flashlight (standard flavor) | ✅ Done (PRs #108–#120) | `:feature:torch` + Camera2 controller |
| Flashlight widgets (QS tile + 2× home widgets) | ✅ Done (PRs #108–#137) | `FlashlightWidgetProvider`, `StrobeWidgetProvider` + `StrobeService`, extracted into `:core:widgetkit` |
| Rooted torch extras (DutyCycle / MultiLed / Thermal) | ✅ Done (PRs #126–#132) | `:feature:torch-rooted` + `:core:root` `RootCapabilityRegistry` / `RootSafetyGate` (closes #94) |
| Vibration (standard + rooted) | ✅ Done (PR #130, follow-ups #134/#135/#137) | `:feature:vibration` (+ `-rooted`/`-standard`); second validated migration consumer |
| App-Organizer + folder widgets | ✅ Done (PRs #138–#144) | `:feature:apps` (+ `-rooted`); content-widget archetype in `:core:widgetkit` |
| Settings v2 — Backup / Restore | ✅ Done (PRs #142–#144) | Whole-app backup format v4 + legacy in-process import |
| Settings v2 — Flipper Zero | ⏳ Phase 2 tail | After `FlipperConnectionManager` migration to `:feature:flipper` |
| Sensors (proximity / light / acceleration) | ✅ Done (PR #158) | `:feature:sensors` — push `MetricSource`s over the `DeviceSensors` seam; legacy `com.gadget.sensors` retired (94-A pattern) |
| Radios / GPS / Camera / Storage / etc. | ⏳ Phase 2 tail | One feature per batch, following the migration guide |
| Cross-automation engine | ✅ Done (PRs #150/#152/#155/#157/#158 + Batch H) | Engine core + runtime in `:core:automation`, `automation.db` in `:core:data`, `HardwareRegistry` in `:core:hardware`, rules list + builder in `:feature:automation-ui` — see `docs/automation-engine.md` (closes #145/#146 pending the physical-device demo) |

**Phase 2 Outstanding Follow-up Issues** (reconciled against the live tracker, June 2026)

Resolved & closed:
- ✅ [#91](https://github.com/Ranzlappen/HardwareDash/issues/91) — `GadgetBottomSheet` instrumented tests — covered by `core/ui`'s `ModalsTest` (the ui-test-manifest activity hosts the sheet; no bespoke host activity needed).
- ✅ [#92](https://github.com/Ranzlappen/HardwareDash/issues/92) — CI emulator workflow — shipped as `.github/workflows/instrumented-tests.yml`, gating every PR (`:core:ui` + `:feature:torch`/`vibration`/`apps`).
- ✅ [#94](https://github.com/Ranzlappen/HardwareDash/issues/94) — Rooted torch extras (DutyCycle / MultiLed / Thermal) — `:feature:torch-rooted` (commit `c53d711`) on the extracted `:core:root` safety framework (commit `7110f96`).

Still open:
- [#89](https://github.com/Ranzlappen/HardwareDash/issues/89) — `material3-adaptive` foldable hinge utility — the `GadgetPosture` / `rememberPosture()` seam landed in `:core:ui`, but no screen consumes it yet, so the "no dead infrastructure" acceptance criterion is unmet. Kept open for the first adaptive consumer.
- [#107](https://github.com/Ranzlappen/HardwareDash/issues/107) — delete dead legacy `com.gadget.widget` torch providers — folded into the per-feature clean-cut deletion step as each module's legacy package is removed.
- [#95](https://github.com/Ranzlappen/HardwareDash/issues/95) — torch brightness control (`WRITE_SETTINGS` UX) — feature enhancement, pickup anytime.
- Tech-debt: [#72](https://github.com/Ranzlappen/HardwareDash/issues/72) / [#68](https://github.com/Ranzlappen/HardwareDash/issues/68) detekt cleanup; [#101](https://github.com/Ranzlappen/HardwareDash/issues/101) floating/accessibility buttons exploration.

**Phase 3 forward plan (new epics — see Workstreams 3/6 of the get-back-on-track plan):**
- ✅ Automation engine: rule model + Room persistence + evaluator + scheduler/runtime (Batches E/F), then the `:feature:automation-ui` rule builder (Batch H). `docs/automation-engine.md` carries the design.
- ✅ `:core:hardware` registry: `HardwareRegistry` (Batch G) — the symmetric read-side partner to the `ActionHandler` action registry; the rule-builder's trigger/condition pickers enumerate it without importing any feature.

---

**Phase 3: Core God App Capabilities**
- Cross-automation engine (rules → triggers → actions)
- Widgets + notification panels (full coverage of legacy widget surface)
- In-depth permission UI + rooted one-ups
- Custom theme picker (high-contrast / amoled-true / pastel)

**Phase 4: Polish, Testing, CI/CD & Release**
- Per-feature instrumented tests using the `:core:testing` fixtures
- CI emulator workflow runs `connectedDebugAndroidTest` (closes #92)
- `GadgetBottomSheet` test-host activity (closes #91)
- Performance benchmarks (recomposition counts, frame timings)
- Release-candidate flow + Play Store metadata

---

**AI Collaboration Workflow**
- Claude handles detailed code planning and implementation (Code Planning Mode)
- Grok (this team) reviews plans, enforces architecture/safety/consistency, and pushes approved code
- Work exclusively on named branch (`claude/refactor-2026`)
- Small atomic batches with pause after each commit for review

**Deprecation & Stale Code Policy**
- Old code lives forever in `legacy-main` (read-only reference)
- Phase 1 built everything from scratch using the new design system
- Phase 2 migrates feature-by-feature with immediate deletion of legacy paths once the new module is verified
- No stale snippets allowed on active branches
- New code MUST live under `dev.ranzlappen.gadget.**`; importing from `com.gadget.**` is a review-blocker

**Execution Rules**
- Every major phase ends with PR → `main` + CI green + release draft
- All decisions recorded in `docs/adr/`
- Every new module follows [`docs/migration-guide.md`](docs/migration-guide.md)
- `CLAUDE.md` is the single source of truth for the design system; updated after every batch

This document is the single source of truth for the roadmap.
