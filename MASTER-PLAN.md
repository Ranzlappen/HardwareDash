**MASTER PLAN: HardwareDash (Gadget) – Future-Proof Modular Refactor (v1.0 – May 2026)**

**Vision**
HardwareDash (app name: Gadget) will become the definitive Android app for exploring every sensor and actuator, with sophisticated cross-automation, widgets, notification panels, granular permission management, and hardware-safety guardrails. The rooted version safely extends functionality while the standard version remains fully functional and safe.

**Current State**
- Branch `claude/refactor-2026`: New modular monorepo (Phase 0 **complete** — May 2026; Phase 1 starting)
- Branch `legacy-main`: Full archive of the old single-module codebase

**Phase 0: Future-Proof Repo Structure & Foundation** (✅ Complete — May 2026)
- Modular monorepo with `core/`, `feature/`, `build-logic/`
- Convention plugins for zero config drift
- New applicationId (`dev.ranzlappen.gadget` / `.rooted`) for side-by-side install
- Light preview / skeleton-first readiness

**Phase 1: Light Preview / Skeleton App**
- Architecture, modern Compose design system, navigation, mock screens only
- No real hardware code yet
- **Progress**: component library (1a–1g) + deferred theme / a11y / adaptive / preview / test infrastructure (X1–X7) shipped on `claude/refactor-2026` (PR #85)
- **Outstanding Phase 1 follow-ups** (tracked as separate issues, ordered by recommended pickup):
  - [#90](https://github.com/Ranzlappen/HardwareDash/issues/90) — `GadgetSpacing` → `LocalGadgetTheme.current.spacing` migration. Pick up **before Phase 2** so the spacing seam is exercised before feature migration begins. ~30 min refactor.
  - [#89](https://github.com/Ranzlappen/HardwareDash/issues/89) — `material3-adaptive` foldable hinge `HingeAware` utility. Pick up **when the first adaptive / split-pane layout is needed** (likely Phase 2 feature migration or Phase 3 god-app capabilities). Alpha API in current BOM — defer until a real consumer justifies the exposure.
  - [#91](https://github.com/Ranzlappen/HardwareDash/issues/91) — `GadgetBottomSheet` instrumented tests + sheet-host activity in `:core:testing`. Pick up during the **Phase 4 testing pass** alongside the broader test-coverage gate.
  - [#92](https://github.com/Ranzlappen/HardwareDash/issues/92) — CI emulator workflow to run `:core:ui:connectedDebugAndroidTest`. Pick up during **Phase 4 CI/CD** alongside the other release-readiness checks; manual `workflow_dispatch` trigger acceptable in the interim.

**Phase 2: Consistency & Gradual Feature Migration**
- Migrate existing functionality into new modules
- Unified permission & safety engine
- Clean up old code per deprecation policy

**Phase 3: Core God App Capabilities**
- Cross-automation engine
- Widgets + notification panels
- In-depth permission UI + rooted one-ups

**Phase 4: Polish, Testing, CI/CD & Release**

**AI Collaboration Workflow**
- Claude handles detailed code planning and implementation (Code Planning Mode)
- Grok (this team) reviews plans, enforces architecture/safety/consistency, and pushes approved code
- Work exclusively on named branch (`claude/refactor-2026`)
- Small atomic batches with pause after each commit for review

**Deprecation & Stale Code Policy**
- Old code lives forever in `legacy-main`
- Phase 1 builds everything from scratch
- Phase 2 migrates feature-by-feature with immediate deletion of old code once verified
- No stale snippets allowed on active branches

**Execution Rules**
- Every major phase ends with PR → `main` + CI green + release draft
- All decisions recorded in `docs/adr/`

This document is the single source of truth.