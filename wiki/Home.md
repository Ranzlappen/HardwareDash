# HardwareDash / Gadget

**HardwareDash** (internal app name **Gadget**, applicationId
`dev.ranzlappen.gadget` / `dev.ranzlappen.gadget.rooted`) is a modular
Android app for exploring and controlling every sensor and actuator a
phone exposes — torch, vibration, sensors, radios, camera, storage, GPS,
app organization — plus cross-device **automation**, home-screen
**widgets**, **monitoring/charting**, and a **rooted** flavor that safely
extends into deeper system surfaces.

The UI is **dark-first**, **glassy** (Material 3 + a glassmorphism
overlay), and **future-proof for custom themes**: every design token
(colour / shape / typography / spacing / motion / glass alpha) flows
through `LocalGadgetTheme.current`.

> This wiki is the **documentation product**. The repository is the
> **implementation product** — it holds only code, build logic, tests,
> CI, and tiny bootstrap files. All long-form documentation lives here.

---

## What is HardwareDash / Gadget?

- **Tech stack:** Kotlin 1.9.10 + Jetpack Compose (BOM 2024.04.01) + Hilt
  + Room. `minSdk 29`, `targetSdk 35`, Java/Kotlin target 17.
- **Shape:** a modular monorepo — `app → feature/* → core/*`, with no
  feature-to-feature dependencies (see [Architecture](Architecture)).
- **Two flavors:** `standard` (Play-store-safe, non-rooted) and `rooted`
  (side-loaded, root-only capabilities). They install side-by-side. See
  [Flavors & Root Safety](Flavors-and-Root-Safety).

## Current development status

- **v3 ("pragmatic") in progress** on `claude/v3-pragmatic-overhaul` — see
  [`MASTER-PLAN.md`](https://github.com/Ranzlappen/HardwareDash/blob/main/MASTER-PLAN.md).
  v2's foundations (design system, root safety seam, widget kit, automation
  engine, CI) are kept; the app is being re-centred on usable tools:
  five-section navigation, a live home screen, merged overlapping modules and
  v1's lost practical features (camera capture, spectrum analyzer, full sensor
  list).
- **Archives:** `legacy-main` (v1, single module) and
  `archive/v2-modular-2026` (v2, modular monorepo). Read-only.
- **Status reporting changed:** a feature is ✅ only after it is smoke-tested
  on a real device; everything else is ⚪ Unverified.

Full detail: **[Roadmap & Status](Roadmap-and-Status)**.

---

## For users

- **[Feature Catalog](Feature-Catalog)** — what the app can do today,
  per-feature standard/rooted support, widgets, automation hooks,
  permissions, and known gaps.

## For contributors

Start here, in order:

1. **[`MASTER-PLAN.md`](https://github.com/Ranzlappen/HardwareDash/blob/main/MASTER-PLAN.md)**
   — the v3 direction, principles and current batch.
2. **[Architecture](Architecture)** — the module graph and dependency
   rules.
3. **[Design System](Design-System)** — tokens, theming, accessibility,
   responsiveness.
4. **[Module Authoring Contract](Module-Authoring-Contract)** — the v3
   required core + opt-in extras.
5. **[Torch Blueprint](Torch-Blueprint)** — the canonical advanced
   example exercising every seam.
6. **[Testing & CI](Testing-and-CI)** and
   **[Troubleshooting](Troubleshooting)** — how to verify, and the
   CI-only traps that don't show up in a local syntax check.

## For maintainers

- **[Roadmap & Status](Roadmap-and-Status)** — what works today, per
  feature, with the v3 plan for each.
- **[Completion Master Plan](Completion-Master-Plan)** — superseded by
  `MASTER-PLAN.md` (v3).
- **[Decision Records](Decision-Records)** — the ADRs.
- **[Module Catalog](Module-Catalog)** / **[Asset Catalog](Asset-Catalog)**
  — the parts inventory.

## For AI collaborators

- **[AI Collaboration](AI-Collaboration)** — roles, planning/coding/review
  modes, branch + PR conventions, the source-of-truth hierarchy, and the
  "pause after batch" protocol.
- **[AI Prompt Library](AI-Prompt-Library)** — reusable prompts for
  migrations, reviews, and CI diagnosis.

The repo keeps a short `CLAUDE.md` bootstrap that points an agent at
`MASTER-PLAN.md` and the AI Collaboration, Module Authoring Contract,
Design System and Roadmap pages before any planning or coding begins.

---

## Important links

- **Main repo:** <https://github.com/Ranzlappen/HardwareDash>
- **Subsystem deep-dives:** [Widgets, Tiles & Surfaces](Widgets-Tiles-and-Surfaces)
  · [Automation Engine](Automation-Engine) ·
  [Monitoring Framework](Monitoring-Framework) ·
  [Flavors & Root Safety](Flavors-and-Root-Safety)
- **Vocabulary:** [Glossary](Glossary)

---

> _Last reviewed: 2026-11-18 (v3 reset) · Source: `MASTER-PLAN.md`, `README.md`, `CLAUDE.md`,
> [Roadmap & Status](Roadmap-and-Status),
> [Completion Master Plan](Completion-Master-Plan) · Related modules: all._
