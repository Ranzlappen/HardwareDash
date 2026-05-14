**MASTER PLAN: HardwareDash (Gadget) – Future-Proof Modular Refactor (v2.0 – May 2026)**

**Vision**
HardwareDash (app name: Gadget) will become the definitive Android app for exploring every sensor and actuator, with sophisticated cross-automation, widgets, notification panels, granular permission management, and hardware-safety guardrails. The rooted version safely extends functionality while the standard version remains fully functional and safe.

**Current State**
- Branch `claude/refactor-2026`: Phase 0 + Phase 1 + Phase 1.1 hardening complete; Phase 2 (accelerated feature migration) **in progress** (May 2026)
- Branch `legacy-main`: Full archive of the old single-module codebase — reference material only, never imported from new code

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
| Settings v1 (About + Appearance + Accessibility) | 🚧 Batch 1 | `:core:datastore` + `:feature:settings` |
| Torch / Flashlight (standard flavor) | 🚧 Batch 1 | `:feature:torch` + Camera2 controller |
| Flashlight widgets (QS tile + 2× home widgets) | 🚧 Batch 1 | `FlashlightTileService`, `FlashlightWidgetProvider`, `StrobeWidgetProvider` + `StrobeService` |
| Rooted torch extras (DutyCycle / MultiLed / Thermal) | ⏳ Next | After `RootCapabilityRegistry` infra lands |
| Settings v2 — Backup / Restore | ⏳ Next | After Room DB + `BackupManager` port |
| Settings v2 — Flipper Zero | ⏳ Next | After `FlipperConnectionManager` port |
| Settings v2 — Keep-Alive / Metric logging / DND bypass | ⏳ Next | Each becomes its own batch |
| Sensors / Actuators / Radios / Camera / etc. | ⏳ Phase 2 tail | One feature per batch, following the migration guide |

**Phase 2 Outstanding Follow-up Issues**
- [#89](https://github.com/Ranzlappen/HardwareDash/issues/89) — `material3-adaptive` foldable hinge utility (deferred until first adaptive consumer)
- [#91](https://github.com/Ranzlappen/HardwareDash/issues/91) — `GadgetBottomSheet` instrumented tests + sheet-host activity (Phase 4 testing pass)
- [#92](https://github.com/Ranzlappen/HardwareDash/issues/92) — CI emulator workflow for `:core:ui:connectedDebugAndroidTest` (Phase 4 CI/CD pass)

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
