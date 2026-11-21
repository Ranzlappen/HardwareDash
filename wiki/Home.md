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

> This wiki is deliberately small (v3): the pages a contributor needs.
> Direction lives in `MASTER-PLAN.md`; v2-era deep-dives are kept
> unmaintained in [`docs/wiki-archive/`](https://github.com/Ranzlappen/HardwareDash/tree/main/docs/wiki-archive).

---

## What is HardwareDash / Gadget?

- **Tech stack:** Kotlin 1.9.10 + Jetpack Compose (BOM 2024.04.01) + Hilt
  + Room. `minSdk 29`, `targetSdk 35`, Java/Kotlin target 17.
- **Shape:** a modular monorepo — `app → feature/* → core/*`, with no
  feature-to-feature dependencies.
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

## Pages

| Page | What it is for |
|---|---|
| [Roadmap & Status](Roadmap-and-Status) | Every tool, its section, what works (✅ / ⚪ / 🧪) and its v3 plan |
| [AI Collaboration](AI-Collaboration) | Workflow, branches, batches, pause-after-batch |
| [Module Authoring Contract](Module-Authoring-Contract) | v3 required core + opt-in extras for a module |
| [Design System](Design-System) | Tokens, theming, `:core:ui` components, accessibility |
| [Flavors & Root Safety](Flavors-and-Root-Safety) | standard / rooted / v3 test builds, root seam, honest-root gate |
| [Troubleshooting](Troubleshooting) | CI-only traps (no local SDK — CI is the compile gate) |

Outside the wiki: [`MASTER-PLAN.md`](https://github.com/Ranzlappen/HardwareDash/blob/main/MASTER-PLAN.md)
(direction, batches) and the
[v3 smoke test](https://github.com/Ranzlappen/HardwareDash/blob/main/docs/v3-smoke-test.md)
(the on-phone checklist that decides ✅).

## Trying a build

Every push to a `claude/v3-**` branch publishes phone-test APKs to the
rolling [`v3-dev` prerelease](https://github.com/Ranzlappen/HardwareDash/releases/tag/v3-dev):
`Gadget-v3-standard.apk` / `Gadget-v3-rooted.apk`, installed beside any
other Gadget as "Gadget v3". Play Protect blocks sideloads of apps with a
notification listener — turn its scanning off while installing.

---

> _Last reviewed: 2026-11-21 (v3 wiki slim-down) · Source: `MASTER-PLAN.md`,
> `docs/v3-smoke-test.md` · Related modules: all._
