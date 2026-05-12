# Flavors: standard vs rooted

HardwareDash ships as two product flavors from a single repo:

| Flavor     | applicationId                  | Display name | Intent                                    |
|------------|--------------------------------|--------------|-------------------------------------------|
| `standard` | `dev.ranzlappen.gadget`        | Gadget       | Non-rooted; ships on Play.                |
| `rooted`   | `dev.ranzlappen.gadget.rooted` | Gadget Root  | Adds rooted-only capabilities; side-load. |

Both APKs install side-by-side on the same device because the
`applicationId` differs.

> **Migration state.** The legacy `app/build.gradle` (Groovy) still ships
> `com.gadget` / `com.gadget.root` from before the refactor. The
> applicationId migration to `dev.ranzlappen.gadget` /
> `dev.ranzlappen.gadget.rooted` lands in **Batch 2** alongside the
> `app/build.gradle` → Kotlin DSL conversion. Until that batch lands,
> the table above describes the target — not the live install on a
> phone built from `main` or from intermediate `claude/refactor-2026` revisions.

## Namespaces (per module)

Namespace is set per-module in each module's `build.gradle.kts`. It is
unrelated to `applicationId`; namespace controls where `R`, `BuildConfig`,
and code packages live.

| Surface                  | Namespace                                | Notes                                                       |
|--------------------------|------------------------------------------|-------------------------------------------------------------|
| Legacy `app/src/main/`   | `com.gadget`                             | Stays in place until each screen is migrated to `feature/`. |
| New `core/*` modules     | `dev.ranzlappen.gadget.core.<name>`      | Applied by `gadget.android.library` consumers.              |
| New `feature/*` modules  | `dev.ranzlappen.gadget.feature.<name>`   | Applied by `gadget.android.feature` consumers.              |
| New `feature/*-rooted/`  | `dev.ranzlappen.gadget.feature.<name>.rooted` | Parallel rooted-only modules.                          |

The shift to `dev.ranzlappen.*` namespaces happens module-by-module as
each feature migrates out of `app/src/main/`. Two namespaces coexist on
the branch during the refactor and that is expected.

## Where code goes

- `app/src/main/` — shared code, used by **both** APKs. Legacy. New
  features go straight into `feature/<name>/`, never here.
- `app/src/standard/` — files that ship **only** in the standard APK.
  Today this is the no-op implementations of the rooted-features safety
  framework.
- `app/src/rooted/` — files that ship **only** in the rooted APK. Today
  this holds the root-detection / Hilt-binding stubs; real su calls and
  root-only feature modules live in `feature/*-rooted/`.
- `core/<name>/` — reusable infrastructure (data, ui, hardware,
  automation, …). Consumed by every feature; never consumes a feature.
- `feature/<name>/` — one user-facing capability per module. Standard
  build only.
- `feature/<name>-rooted/` — root-only sibling module for that feature.
  Pulled in by the rooted flavor of `:app` via `rootedImplementation`
  only. `:app`'s standard dependency list never names a `*-rooted`
  module, so the standard APK is physically incapable of compiling
  against root code.

The two source-set directories under `app/src/` (`standard/` and
`rooted/`) MUST contain files with **matching** fully-qualified class
names (e.g. `com.gadget.root.RootBindings` exists in both). Gradle picks
one at build time based on the active flavor — never both — so a name
mismatch would produce duplicate Hilt bindings on one side and missing
classes on the other.

## Don't do this

- ❌ Don't gate UI on `BuildConfig.IS_ROOTED` directly. Always go through
  `RootCapabilityRegistry` / `RootSafetyGate` (currently in
  `com.gadget.root`, eventually under
  `dev.ranzlappen.gadget.rooted`). This keeps flavor differences flowing
  through one Hilt seam, not scattered `if` branches.
- ❌ Don't put rooted-specific code in `src/main/` or in a `feature/<name>/`
  (non-rooted) module. If it imports `Shell.SU` (or any future root
  library), it lives in `src/rooted/` or `feature/<name>-rooted/` and
  the standard flavor gets a no-op stub with the same FQN.
- ❌ Don't `rm -rf src/rooted/` or `rm -rf feature/*-rooted/` from CI to
  "exclude rooted code from the standard build". AGP source-set scoping
  + the `rootedImplementation`-only dependency on `*-rooted/` already
  does this; deletion would race with the Gradle build cache and break
  the rooted leg.

## Build outputs

CI produces three artifacts on every push:

- `standard-debug.apk`
- `standard-release.apk` + `standard-release.aab`
- `rooted-debug.apk`

`rooted-release.apk` (signed) is added in a later batch once the rooted
modules actually do something.

## versionCode strategy

`versionCode = CI_VERSION_CODE * 10 + flavorOffset` where standard=+0 and
rooted=+1. Lets the two APKs ship side-by-side without colliding on Play
and keeps upgrades monotonic within each flavor.

## The safety/opt-out framework

Every rooted operation must call `RootSafetyGate.check(feature)` before
running. The gate combines:

1. `RootCapabilityRegistry` — is the rooted code path even available?
2. `RootSafetyPreferences` — has the user opted out of this specific feature?
3. `RootSoftLimiter` — are we within the soft rate-limit window for this
   feature?

The gate returns a `RootGateDecision` (`Allowed` / `BlockedByUser` /
`BlockedByLimiter` / `Unsupported`). UI translates each branch into a
user-visible explanation rather than silently failing.

Adding a new rooted feature means: declare a new `RootFeatureKey` data
object, add a `RootFeatureDescriptor` (with optional `RootLimitPolicy`),
land the implementation under `feature/<name>-rooted/`, and wire the UI
through the gate. No flavor-specific code changes needed in shared
modules.
