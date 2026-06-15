# Roadmap & Status

> The canonical roadmap page. Absorbs the useful parts of the former
> `MASTER-PLAN.md`. Git history, commit messages, and PR descriptions are
> the authoritative record of shipped work — this page is the readable
> summary.

## Vision

Gadget will be the definitive Android app for exploring every sensor and
actuator, with sophisticated cross-automation, widgets, notification
panels, granular permission management, and hardware-safety guardrails.
The rooted flavor safely extends functionality; the standard flavor stays
fully functional and Play-store-safe.

## Current phase — Phase 2: Accelerated Feature Migration (🚧 in progress)

Each feature migrates directly from the archived `legacy-main` branch
into a `:feature:<name>` module using the new design system (`:core:ui`),
component library, token plumbing (`LocalGadgetTheme.current`), and
Hilt-injected state — **no placeholder screens**.

**Clean-cut policy:** `legacy-main` is reference material only. New code
never imports from `com.gadget.**`; it lives entirely under
`dev.ranzlappen.gadget.feature.<name>.*`. Legacy paths are deleted once
the new module is verified.

All work lands on `main` via one PR per `claude/<topic>` batch branch;
the long-lived `claude/refactor-2026` integration branch is retired.

### Migrated & live in the shell

| Feature | Modules | Status |
|---|---|---|
| Settings (About + Appearance + Accessibility + Backup) | `:feature:settings` + `:core:datastore` | ✅ |
| Dashboard | `:feature:dashboard` | ✅ |
| Torch / Flashlight (+ QS tile + 2 home widgets + strobe service) | `:feature:torch` (+ `-rooted`/`-standard`) | ✅ |
| Rooted torch extras (DutyCycle / MultiLed / Thermal) | `:feature:torch-rooted` + `:core:root` | ✅ (closes #94) |
| Vibration (standard + rooted) | `:feature:vibration` (+ `-rooted`/`-standard`) | ✅ |
| App-Organizer + folder widgets | `:feature:apps` (+ `-rooted` skeleton) | ✅ |
| Sensors (proximity / light / acceleration) | `:feature:sensors` | ✅ (PR #158) |
| Battery (level / charging / temperature / voltage / health) | `:feature:battery` | ✅ |
| GPS / Location (map, position, speed, altitude) | `:feature:gps` | ✅ |
| Storage (volumes, used / free / total, monitoring) | `:feature:storage` | ✅ |
| IR Blaster (NEC / Pronto / RAW, saved-signal library, automation action) | `:feature:radios-ir` | ✅ |
| Barcode Scanner (CameraX + MLKit, all formats, scan history, WiFi/URL) | `:feature:camera` | ✅ |
| Motion (gyroscope / step counter / motion detect) | `:feature:motion` | ✅ |
| Audio (dB meter + WAV voice recording) | `:feature:audio` | ✅ |
| NFC (NDEF tag read + HCE emulation) | `:feature:radios-nfc` | ✅ |
| Bluetooth (adapter status + bonded devices) | `:feature:radios-bt` | ✅ |
| Cross-automation engine + rule builder | `:core:automation` + `:core:hardware` + `:feature:automation-ui` | ✅ (epics #145/#146) |

### Shared infrastructure landed

- **`:core:root`** — the root-safety seam (`RootCapabilityRegistry`,
  `RootSafetyGate`, `RootFeatureKey`).
- **`:core:widgetkit`** — the home-screen-widget framework (config store,
  pin flow, RemoteViews rendering, base providers, boot re-arm).
- **`:core:monitoring`** — the chart + persist framework (MetricSource
  consumption, monitor containers, hand-drawn charts). Enhanced: per-signal
  min/max/count stats in `MonitorService`, richer notifications with
  conditional "Stop monitoring" action button, `MonitorGlobalPrefs` DataStore
  preference, `MonitorNotificationActionReceiver`, and correct "enable hint"
  vs "collecting" chart placeholder logic.
- **Cross-automation engine** — `:core:automation` (contract + rule model
  + evaluator + runtime), `automation.db` in `:core:data`,
  `:core:hardware`'s `HardwareRegistry`, and `:feature:automation-ui`.
- **Whole-app backup format v5** — ZIP of every DB / DataStore /
  SharedPrefs / asset sweep, with legacy in-process import. Fixed: WAL data
  staged alongside `gadget_db`; `LegacyAppsImporter` uses upsert to survive
  ID collisions; manual re-import UI added to Apps screen overflow.

### Remaining legacy surface

~297 legacy `com.gadget.*` Kotlin files remain across all `:app` source
sets, migrating feature-by-feature per the
[Feature Migration Guide](Feature-Migration-Guide). Canonical metric:

```bash
find app/src -path "*com/gadget*" -name "*.kt" | wc -l
```

### Phase-2 tail (skeleton modules awaiting migration)

These modules exist as Gradle skeletons with **no Kotlin sources yet**:
`actuators`, `ambient`, `bugreport` (+ `-rooted`),
`diagnostics` (+ `-rooted`), `flipper` (+ `-rooted`),
`lock` (+ `-rooted`), `manual`, `radios-{wifi,subghz}`,
`storage-rooted`. One feature per batch, following the guide.

## Completed phases

- **Phase 0 — Future-Proof Repo Structure & Foundation (✅ May 2026).**
  Modular monorepo with `core/`, `feature/`, `build-logic/`; convention
  plugins; new applicationIds for side-by-side install.
- **Phase 1 — Light Preview / Skeleton App (✅ May 2026).** The full
  component library, `LocalGadgetTheme`, accessibility locals, the
  `WindowSizeClass`-aware shell, `:core:testing`, and the preview matrix.
- **Phase 1.1 — Hardening sweep (✅).** Full `LocalGadgetTheme` wiring
  (#90), accessibility semantics, responsiveness, glass consistency,
  shimmer width, preview matrix expansion.

## Forward plan

- **Phase 3 — Core God-App Capabilities.** Full widget/notification-panel
  coverage of the legacy surface, in-depth permission UI + rooted
  one-ups, a custom theme picker (high-contrast / amoled-true / pastel).
- **Phase 4 — Polish, Testing, CI/CD & Release.** Per-feature
  instrumented tests on `:core:testing` fixtures, emulator CI (#92),
  performance benchmarks, release-candidate flow + Play metadata.

## Open follow-up issues

- **[#89](https://github.com/Ranzlappen/HardwareDash/issues/89)** —
  ✅ `material3-adaptive` foldable posture consumer landed.
  `ModuleScreenScaffold` now reads `rememberPosture()` and stacks the
  `secondaryPane` below the primary content on `GadgetPosture.Tabletop`
  (side-by-side on Flat/Book). `DashboardScreen` is the first consumer
  (feature shortcut pane on wide/foldable screens).
- **[#95](https://github.com/Ranzlappen/HardwareDash/issues/95)** — torch
  brightness control (`WRITE_SETTINGS` UX). Feature enhancement.
- **[#107](https://github.com/Ranzlappen/HardwareDash/issues/107)** —
  delete dead legacy `com.gadget.widget` torch providers; folded into the
  per-feature clean-cut deletion step.
- **[#95](https://github.com/Ranzlappen/HardwareDash/issues/95)** torch
  brightness slider (API 33+ `turnOnTorchWithStrengthLevel`) — **done**, in
  `claude/open-issues-xktnze`.
- **[#101](https://github.com/Ranzlappen/HardwareDash/issues/101)** floating
  torch overlay button — **done**, in `claude/open-issues-xktnze`.
- Tech-debt: **[#72](https://github.com/Ranzlappen/HardwareDash/issues/72)**
  / **[#68](https://github.com/Ranzlappen/HardwareDash/issues/68)** detekt
  cleanup — **done** (PR #164).

**Resolved & closed:** #89 (foldable posture consumer — `ModuleScreenScaffold`
Tabletop layout + `DashboardScreen` secondary pane), #90 (theme wiring),
#91 (`GadgetBottomSheet` instrumented tests, covered by `ModalsTest`),
#92 (emulator CI, `instrumented-tests.yml`), #94 (rooted torch extras +
`:core:root`).

## Release readiness

CI produces `standard-debug.apk`, `standard-release.apk` + `.aab`, and
`rooted-debug.apk` on every push. The standard-APK leak gate hard-fails
any PR that lets rooted code/assets/permissions into the standard APK.
`rooted-release.apk` (signed) ships once the rooted modules are feature
complete. See [Testing & CI](Testing-and-CI).

## Historical milestones

- **`claude/refactor-2026`** carried the Phase-0 → Phase-2 migration
  (Torch blueprint hardening, `:core:widgetkit` extraction, the automation
  engine epic) and is now retired in favour of per-batch branches onto
  `main`.
- **`legacy-main`** is the full archive of the old single-module
  codebase — read-only reference, never imported from new code.

---

> _Last reviewed: 2026-06-15 · Source: `MASTER-PLAN.md`,
> `docs/refactor-2026/*`, `README.md` · Related modules: all._
