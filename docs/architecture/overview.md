# Architecture overview

> **Status: draft.** Expand as features migrate into modules. This is the
> on-ramp for new contributors after the Phase-0 skeleton lands.

## Module dependency direction

```
            ┌─────────┐
            │  :app   │
            └────┬────┘
                 │ depends on every standard feature; rooted flavor
                 │ adds feature:*-rooted via rootedImplementation
                 ▼
        ┌────────────────────┐
        │  feature:<name>    │
        └──────────┬─────────┘
                   │ consumes core/* only — never another feature
                   ▼
        ┌────────────────────┐
        │  core:<name>       │
        └────────────────────┘
```

**Allowed direction of dependency:** `app → feature → core`. No
back-edges. The convention plugins (Batch 1) enforce this by failing
the build if a `core` module declares a `feature` dependency or a
`feature` module declares another `feature` as a direct dependency.

## Core module taxonomy

| Module              | Purpose                                                                              |
|---------------------|--------------------------------------------------------------------------------------|
| `core:common`       | Pure-Kotlin utilities — `Result` types, dispatchers, time helpers, log tags.         |
| `core:model`        | Cross-feature data classes. No Android dependencies. Serializable where useful.      |
| `core:domain`       | Use-cases and policy that doesn't touch Android APIs.                                |
| `core:data`         | Repositories backed by Room / network / Files / DataStore.                           |
| `core:datastore`    | Preferences DataStore wrappers (Kotlin-serialization-backed protos preferred).       |
| `core:designsystem` | Compose theme, colors, typography, base components (button, card, scaffold, …).      |
| `core:ui`           | Higher-level Compose composables built on `core:designsystem`.                       |
| `core:navigation`   | Centralised `NavGraph` + typed `NavDestination` contracts.                           |
| `core:permissions`  | Permission state objects, Accompanist wrappers, `PermissionsResumeAdvancer`.         |
| `core:surfaces`     | Widget, Quick Settings Tile, Wear-OS surface registry. Consumed by `core:automation`.|
| `core:automation`   | Rule engine consumed by `feature:automation-ui`. See `docs/automation-engine.md`.    |
| `core:hardware`     | `Sensor` / `Actuator` registries. See `docs/sensor-actuator-api.md`.                 |
| `core:testing`      | Hilt-aware test helpers, fakes, JUnit rules.                                         |

## Feature module taxonomy

Each `feature:<name>` module typically contains:

- `<Name>Screen.kt` — Compose entry point.
- `<Name>ViewModel.kt` — Hilt-injected state container.
- `<Name>NavDestination.kt` — typed route + arguments contract.
- Module-private use-cases / mappers — `internal` visibility (see
  `CLAUDE.md` § Kotlin visibility — internal must not leak through
  public API).

Rooted-only counterparts live in `feature:<name>-rooted/`. They expose
only Hilt entry points; the standard APK does not link against them.
The `RootCapabilityRegistry` Hilt seam in `app/src/rooted/` is what
makes feature code work transparently across the two flavors.

## How a new feature is wired

1. `scripts/new-feature.sh <name>` (lands Batch 1) scaffolds the
   module: `build.gradle.kts` (applying `gadget.android.feature`),
   `AndroidManifest.xml`, source-set directories, and a minimal
   Screen + ViewModel pair.
2. The scaffold appends `:feature:<name>` to `settings.gradle.kts`'s
   alphabetised feature block.
3. The scaffold adds the feature's `NavDestination` registration to
   `core:navigation`'s aggregation point.
4. `:app/build.gradle` gains `implementation(project(":feature:<name>"))`.
5. (rooted-only) `:app/build.gradle` also gains
   `rootedImplementation(project(":feature:<name>-rooted"))`.

## What lives in `:app`

After the refactor:

- `GadgetApplication` (`@HiltAndroidApp`).
- `MainActivity` (single-activity host).
- Top-level `NavHost` wiring (delegated to `core:navigation`).
- Build-config and signing configuration.
- `applicationId` / flavor wiring.

Everything else — every screen, every ViewModel, every Hilt module
that isn't `App`-scope — lives in `feature/*` or `core/*`.

## Where things still live during migration

Until each feature's migration batch lands, the legacy implementation
stays under `app/src/main/java/com/gadget/`. The empty `feature/<name>/`
module skeleton is a destination, not a source — depending on it from
`:app` is a no-op until the corresponding migration batch moves the
code.
