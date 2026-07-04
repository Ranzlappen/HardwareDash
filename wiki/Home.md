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

- **Phase 2 — Accelerated Feature Migration — ✅ feature-complete;
  Phase 3 (Core God-App Capabilities) 🚧 started.** What remains of
  Phase 2 is the per-feature clean-cut deletion of the legacy sources.
- **Live in the shell:** Dashboard, Settings, Torch, Vibration,
  App-Organizer, Sensors, Battery, GPS, Storage, IR, Camera/Barcode,
  Motion, Ambient, Audio, NFC, Bluetooth, WiFi, Sub-GHz, Flipper Zero,
  Lock, Actuators, Diagnostics, Health/Permissions, Manual, Automation
  (rules list + builder), YouTube Downloader.
- **Shared infrastructure landed:** `:core:root`, `:core:widgetkit`,
  `:core:monitoring`, the full cross-automation engine
  (`:core:automation` + `:core:hardware` + `automation.db` +
  `:feature:automation-ui`), and whole-app backup format v5.
- **Remaining legacy surface:** 96 `com.gadget.*` Kotlin files still in
  `:app` source sets, migrating feature-by-feature.
- **The road to done:** the gap analysis and phased completion roadmap
  live in the **[Completion Master Plan](Completion-Master-Plan)**.

Full detail: **[Roadmap & Status](Roadmap-and-Status)**.

---

## For users

- **[Feature Catalog](Feature-Catalog)** — what the app can do today,
  per-feature standard/rooted support, widgets, automation hooks,
  permissions, and known gaps.

## For contributors

Start here, in order:

1. **[Architecture](Architecture)** — the module graph and dependency
   rules.
2. **[Design System](Design-System)** — tokens, theming, accessibility,
   responsiveness.
3. **[Feature Migration Guide](Feature-Migration-Guide)** — the 8-step
   playbook for bringing a legacy feature into a module.
4. **[Module Authoring Contract](Module-Authoring-Contract)** — the
   acceptance checklist a new module must satisfy.
5. **[Torch Blueprint](Torch-Blueprint)** — the canonical advanced
   example exercising every seam.
6. **[Testing & CI](Testing-and-CI)** and
   **[Troubleshooting](Troubleshooting)** — how to verify, and the
   CI-only traps that don't show up in a local syntax check.

## For maintainers

- **[Roadmap & Status](Roadmap-and-Status)** — phases, migrated features,
  open follow-up issues, release readiness.
- **[Completion Master Plan](Completion-Master-Plan)** — the measured
  gap analysis and the phased plan to a finished app.
- **[Decision Records](Decision-Records)** — the ADRs.
- **[Module Catalog](Module-Catalog)** / **[Asset Catalog](Asset-Catalog)**
  — the parts inventory.

## For AI collaborators

- **[AI Collaboration](AI-Collaboration)** — roles, planning/coding/review
  modes, branch + PR conventions, the source-of-truth hierarchy, and the
  "pause after batch" protocol.
- **[AI Prompt Library](AI-Prompt-Library)** — reusable prompts for
  migrations, reviews, and CI diagnosis.

The repo keeps a tiny `CLAUDE.md` bootstrap that points an agent at the
AI Collaboration, Module Authoring Contract, Design System, Migration
Guide, and Roadmap pages before any planning or coding begins.

---

## Important links

- **Main repo:** <https://github.com/Ranzlappen/HardwareDash>
- **Subsystem deep-dives:** [Widgets, Tiles & Surfaces](Widgets-Tiles-and-Surfaces)
  · [Automation Engine](Automation-Engine) ·
  [Monitoring Framework](Monitoring-Framework) ·
  [Flavors & Root Safety](Flavors-and-Root-Safety)
- **Vocabulary:** [Glossary](Glossary)

---

> _Last reviewed: 2026-07-03 · Source: `README.md`, `CLAUDE.md`,
> [Roadmap & Status](Roadmap-and-Status),
> [Completion Master Plan](Completion-Master-Plan) · Related modules: all._
