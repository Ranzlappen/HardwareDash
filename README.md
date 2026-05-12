# HardwareDash (Gadget)

A modular Android app for monitoring and controlling device sensors,
radios, cameras, audio, storage, and (on the rooted flavor) deeper system
surfaces.

> **Status (May 2026):** mid-refactor. The legacy `com.gadget.*` codebase
> in `app/src/main/` is being migrated onto a modular monorepo layout on
> branch `refactor-2026`. See [`MASTER-PLAN.md`](MASTER-PLAN.md),
> [`AI-COLLABORATION.md`](AI-COLLABORATION.md), and
> [`docs/adr/0001-monorepo-refactor.md`](docs/adr/0001-monorepo-refactor.md).

## Repo layout

```
HardwareDash/
├── app/                     # single application module
│                            # (still uses legacy Groovy build script —
│                            #  migration is a later Phase-0 batch)
├── build-logic/convention/  # Gradle convention plugins (Batch 1+)
├── core/                    # reusable infrastructure (no UI features)
│   ├── common/              # pure-Kotlin utilities
│   ├── model/               # cross-feature data classes
│   ├── domain/              # use-cases / policy
│   ├── data/                # repositories (Room, network)
│   ├── datastore/           # DataStore wrappers
│   ├── designsystem/        # Compose theme, colors, typography
│   ├── ui/                  # higher-level Compose composables
│   ├── navigation/          # NavGraph + destination types
│   ├── permissions/         # permission state objects
│   ├── surfaces/            # widget, Tile, Wear surface registry
│   ├── automation/          # rule engine
│   ├── hardware/            # Sensor / Actuator registries
│   └── testing/             # Hilt-aware test helpers, fakes
├── feature/                 # one user-facing capability per module
│   ├── automation-ui/
│   ├── settings/
│   ├── diagnostics/
│   ├── diagnostics-rooted/  # rooted-only diagnostics surface
│   ├── manual/
│   ├── sensors/
│   ├── actuators/
│   ├── battery/
│   ├── audio/
│   ├── camera/
│   ├── torch/
│   ├── vibration/
│   ├── gps/
│   ├── motion/
│   ├── ambient/
│   ├── radios-wifi/
│   ├── radios-bt/
│   ├── radios-nfc/
│   ├── radios-subghz/
│   ├── radios-ir/
│   ├── flipper/
│   ├── flipper-rooted/
│   ├── storage/
│   ├── storage-rooted/
│   ├── apps/
│   ├── apps-rooted/
│   ├── lock/
│   ├── lock-rooted/
│   ├── bugreport/
│   └── bugreport-rooted/
├── benchmark/               # macrobenchmark host (Batch 0 skeleton)
├── lsposed-module/          # bundled Xposed module (rooted flavor only)
├── docs/                    # architecture docs + ADRs
└── scripts/                 # local helpers (verify-standard-apk.sh, …)
```

## Flavors

The app ships as two product flavors built from one repo:

| Flavor     | applicationId                  | Intent                          |
|------------|--------------------------------|---------------------------------|
| `standard` | `dev.ranzlappen.gadget`        | Non-rooted; ships on Play.      |
| `rooted`   | `dev.ranzlappen.gadget.rooted` | Rooted-only capabilities; side-load. |

> **Note (transitional):** the applicationId change to `dev.ranzlappen.*`
> lands in a later Phase-0 batch alongside the `app/build.gradle` →
> Kotlin DSL migration. Until then the legacy `com.gadget` /
> `com.gadget.root` IDs remain.

Full flavor rules live in [`docs/flavors.md`](docs/flavors.md). The
standard-APK leak gate in `.github/workflows/build-apk.yml` continues to
hard-fail any PR that lets rooted code, assets, or permissions leak into
the standard APK.

## Building locally

```bash
# Standard debug APK
./gradlew :app:assembleStandardDebug

# Rooted debug APK (note the -P flag — required to include :lsposed-module)
./gradlew -PenableLsposedModule=true :app:assembleRootedDebug

# Lint / format
./gradlew detekt ktlintCheck
```

No Android SDK is required in the Claude Code dev container; CI catches
build errors. See [`CLAUDE.md`](CLAUDE.md) for the things that won't show
up in local syntax checks (Kotlin visibility, SDK guards, Compose
@Composable evaluation, …).

## Targeting

- `minSdk = 29` (Android 10 — covers Huawei P30 and the rest of the
  Android-10 fleet).
- `targetSdk = 35` (Android 15 — targets Xiaomi 14T Pro and the current
  Pixel line).
- `compileSdk = 35`. Java/Kotlin target 17.

## Contributing

1. Find the right module. Cross-feature contracts go through `core/`;
   single-screen capabilities go in `feature/<name>/`. Root-only surface
   lives in `feature/<name>-rooted/`.
2. Run `scripts/new-feature.sh <name>` (lands Batch 1) to scaffold.
3. Open a PR against `refactor-2026`. Do **not** target `main` until the
   refactor is complete.
