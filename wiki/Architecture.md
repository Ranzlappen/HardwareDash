# Architecture

The on-ramp for new contributors. Absorbs the former
`docs/architecture/overview.md`.

## Modular monorepo

Gadget is a modular monorepo standardised on the Now-in-Android layout:

```
app/                     single application module (aggregates feature/*)
core/<name>/             reusable infrastructure (data, ui, hardware, …)
feature/<name>/          one user-facing capability per module
feature/<name>-rooted/   root-only capability surface (rooted flavor only)
feature/<name>-standard/ standard no-op twin for a rooted-bearing feature
benchmark/               macrobenchmark host
build-logic/             Gradle convention plugins (DRY module build files)
lsposed-module/          bundled Xposed module (rooted flavor, opt-in)
```

## Dependency direction: `app → feature → core`

```
            ┌─────────┐
            │  :app   │  depends on every standard feature; rooted flavor
            └────┬────┘  adds feature:*-rooted via rootedImplementation
                 ▼
        ┌────────────────────┐
        │  feature:<name>    │  consumes core/* only — never another feature
        └──────────┬─────────┘
                   ▼
        ┌────────────────────┐
        │  core:<name>       │
        └────────────────────┘
```

**Invariants (enforced by review and the convention plugins):**

1. `core/*` modules never depend on `feature/*` modules.
2. A `feature:<name>` module never depends on another `feature:<name>`
   directly. **Cross-feature contracts go through `core:*`** — e.g.
   `core:navigation` (`GadgetDestination`), `core:model` (`MetricSource`),
   `core:automation` (`ActionHandler`), `core:hardware`
   (`HardwareRegistry`).
3. The standard APK never compiles against a `*-rooted` module. The
   rooted flavor adds them via `rootedImplementation` only — so the
   standard APK is physically incapable of compiling against root code.
4. Each module declares its own `namespace`; cross-module flags move to
   `core/common` configuration objects rather than a shared `BuildConfig`.

See **[Decision Records → ADR-0001](Decision-Records)** for why this
layout was chosen over staying monolithic, multi-repo, dynamic feature
modules, or layer-only modularisation.

## Core module taxonomy

| Module | Purpose |
|---|---|
| `core:common` | Pure-Kotlin utilities — `Result` types, dispatchers, time helpers, log tags. |
| `core:model` | Cross-feature data classes; **`MetricSource`** (readable-signal seam). No Android. |
| `core:domain` | Use-cases / policy that don't touch Android APIs. |
| `core:data` | Repositories backed by Room / Files / DataStore; the modular DBs (`apps.db`, `monitoring.db`, `automation.db`) + `DatabaseCheckpointer`. |
| `core:datastore` | Preferences DataStore wrappers (`UserPreferences`, generic `FeaturePreferences<T>`). |
| `core:designsystem` | Compose theme, colour, typography, spacing/motion/glass tokens, `LocalGadgetTheme`. |
| `core:ui` | Higher-level Compose components built on `core:designsystem`. |
| `core:navigation` | `GadgetApp` shell + typed `GadgetDestination` contracts. |
| `core:permissions` | Permission state objects + resume advancers. |
| `core:surfaces` | Widget / QS-tile / Wear surface registry. |
| `core:notifications` | Shared notification channels / helpers. |
| `core:automation` | The `ActionHandler` contract + rule model + pure evaluator + runtime (FGS/AlarmManager). |
| `core:hardware` | `HardwareRegistry` — read-side enumeration over the `MetricSource` map. |
| `core:monitoring` | Chart + persist framework: monitor containers, charts, `MonitorService`. |
| `core:widgetkit` | Home-screen-widget framework: config store, pin flow, base providers, boot re-arm. |
| `core:root` | Root-safety seam: `RootCapabilityRegistry`, `RootSafetyGate`, `RootFeatureKey`. |
| `core:testing` | Hilt-aware test helpers, fakes, `GadgetTestTheme`, JUnit rules. |

Live source counts (2026-06): `ui` 35, `widgetkit` 35, `automation` 30,
`root` 25, `data` 22, `monitoring` 18, `designsystem` 9, `datastore` 6,
`navigation` 4. `common`, `domain`, `permissions`, `surfaces` are
seam-only skeletons today. Full detail: [Module Catalog](Module-Catalog).

## Feature module taxonomy

Each `feature:<name>` module typically contains:

- `<Name>Screen.kt` — thin Hilt-wrapped Compose entry point.
- `<Name>ScreenContent.kt` — **stateless** inner composable (Hilt-free, so
  previews + instrumented tests stay simple).
- `<Name>ViewModel.kt` — Hilt-injected state container.
- `<Name>Navigation.kt` — `NavGraphBuilder.<name>Screen()` registration.
- A `Controller`/repository layer + a Hilt `@Binds`/`@Provides` module.
- Module-private helpers at `internal` visibility (see
  [Troubleshooting → Kotlin visibility](Troubleshooting)).

Rooted-only counterparts live in `feature:<name>-rooted/` and expose only
Hilt entry points; the standard APK does not link against them. See
[Flavors & Root Safety](Flavors-and-Root-Safety).

## What lives in `:app`

- `GadgetApplication` (`@HiltAndroidApp`).
- `MainActivity` (single-activity host) + the top-level `GadgetApp { … }`
  nav wiring.
- Flavor / `applicationId` / signing configuration.
- The flavor `RootBindings.kt` (Hilt seam binding the right per-flavor
  root impl), plus leaf-module-can't-see-`:app` glue (e.g. `BackupCard`,
  `RootedFeatureTogglesCard`) dropped into feature screens via
  `@Composable () -> Unit` slots.
- Whatever legacy `com.gadget.*` capability hasn't migrated yet.

Everything else — every screen, ViewModel, and non-`App`-scoped Hilt
module — lives in `feature/*` or `core/*`.

## Legacy-main as read-only reference

Until a feature's migration batch lands, its legacy implementation stays
under `app/src/main/java/com/gadget/`. The empty `feature/<name>/`
skeleton is a **destination, not a source** — depending on it from `:app`
is a no-op until the migration batch moves the code. New code never
imports `com.gadget.**`; the full archive is the `legacy-main` branch.

## How a new feature is wired

1. `scripts/new-feature.sh <name> [--rooted]` scaffolds the module
   (build file, manifest, Screen/ViewModel/Navigation, the
   standard/rooted sibling pair with `--rooted`) and appends the
   `:feature:<name>` include to `settings.gradle.kts`.
2. Register the route in `GadgetDestination` (`:core:navigation`).
3. Add `implementation(project(":feature:<name>"))` to `:app`'s build
   file (+ `rootedImplementation(project(":feature:<name>-rooted"))` for
   the rooted sibling).
4. Wire the `NavGraphBuilder` extension into `GadgetApp { … }`.

Full procedure: **[Feature Migration Guide](Feature-Migration-Guide)**.

---

> _Last reviewed: 2026-06-12 · Source: `docs/architecture/overview.md`,
> `settings.gradle.kts`, `docs/adr/0001-monorepo-refactor.md` · Related
> modules: all `core/*` and `feature/*`._
