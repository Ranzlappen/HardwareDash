# Flavors: standard vs rooted

HardwareDash ships as two product flavors from a single repo:

| Flavor     | applicationId    | Display name  | Intent                          |
|------------|------------------|---------------|---------------------------------|
| `standard` | `com.gadget`     | Gadget        | Existing non-rooted behavior.   |
| `rooted`   | `com.gadget.root`| Gadget Root   | Adds rooted-only capabilities.  |

Both APKs install side-by-side on the same device because the `applicationId`
differs. The Kotlin `namespace` stays `com.gadget` for both — only the install
ID changes.

## Where code goes

- `app/src/main/` — shared code, used by **both** APKs. New features default
  here. Touch one file, both flavors get the change.
- `app/src/standard/` — files that ship **only** in the standard APK. Today
  this is the no-op implementations of the rooted-features safety framework.
- `app/src/rooted/` — files that ship **only** in the rooted APK. Today this
  holds Batch-1 stubs; real su calls and root-only modules land in later
  batches.

The two flavor directories MUST contain files with **matching** fully-qualified
class names (e.g. `com.gadget.root.RootBindings` exists in both). Gradle picks
one at build time based on the active flavor — never both — so a name mismatch
would produce duplicate Hilt bindings on one side and missing classes on the
other.

## Don't do this

- ❌ Don't gate UI on `BuildConfig.IS_ROOTED` directly. Always go through
  `RootCapabilityRegistry` / `RootSafetyGate` (in `com.gadget.root`).
  This keeps flavor differences flowing through one Hilt seam, not scattered
  `if` branches.
- ❌ Don't put rooted-specific code in `src/main/`. If it imports `Shell.SU`
  (or any future root library), it lives in `src/rooted/` and the standard
  flavor gets a no-op stub with the same FQN.
- ❌ Don't `rm -rf src/rooted/` from CI to "exclude rooted code from the
  standard build". AGP source-set scoping already does this; deletion would
  race with the Gradle build cache.

## Build outputs

CI produces three artifacts on every push:

- `standard-debug.apk`
- `standard-release.apk` + `standard-release.aab`
- `rooted-debug.apk`

`rooted-release.apk` (signed) is added in a later batch once the rooted
modules actually do something.

## versionCode strategy

`versionCode = CI_VERSION_CODE * 10 + flavorOffset` where standard=+0 and
rooted=+1. Lets the two APKs ship side-by-side without colliding on Play and
keeps upgrades monotonic within each flavor.

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

Adding a new rooted feature means: declare a new `RootFeatureKey` data object,
add a `RootFeatureDescriptor` (with optional `RootLimitPolicy`), wire the UI
through the gate. No flavor-specific code changes needed in shared modules.
