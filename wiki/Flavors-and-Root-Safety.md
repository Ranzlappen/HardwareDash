# Flavors & Root Safety

HardwareDash ships as two product flavors from one repo. Absorbs
`docs/flavors.md`.

| Flavor | applicationId | Display name | Intent |
|---|---|---|---|
| `standard` | `dev.ranzlappen.gadget` | Gadget | Non-rooted; ships on Play. |
| `rooted` | `dev.ranzlappen.gadget.rooted` (`applicationIdSuffix = ".rooted"`) | Gadget Root | Adds root-only capabilities; side-load. |

Both APKs install side-by-side because the `applicationId` differs — from
Android's perspective they're separate apps (and they coexist with any
legacy `com.gadget` / `com.gadget.root` build still on the device).

## Namespaces vs. applicationId

`applicationId` identifies the installed app; **namespace** (set
per-module) controls where `R`, `BuildConfig`, and code packages live —
unrelated concepts.

| Surface | Namespace |
|---|---|
| Legacy `app/src/main/` | `com.gadget` (until each screen migrates out) |
| `core/*` | `dev.ranzlappen.gadget.core.<name>` |
| `feature/*` | `dev.ranzlappen.gadget.feature.<name>` |
| `feature/*-rooted/` | `dev.ranzlappen.gadget.feature.<name>.rooted` |

Two namespaces coexist during the refactor — expected.

## Where code goes

- `app/src/main/` — shared code, both APKs. Legacy. New features go
  straight into `feature/<name>/`, never here.
- `app/src/standard/` — standard-only (today: no-op root-framework
  stubs). `app/src/rooted/` — rooted-only stubs / Hilt bindings.
- `core/<name>/` — reusable infrastructure; never consumes a feature.
- `feature/<name>/` — one capability; standard build.
- `feature/<name>-rooted/` — root-only sibling, pulled in by the rooted
  flavor of `:app` via `rootedImplementation` **only**. `:app`'s standard
  dependency list never names a `*-rooted` module → the standard APK is
  physically incapable of compiling against root code.
- `feature/<name>-standard/` — the no-op twin (via `standardImplementation`)
  so each interface has exactly one `@Binds` on each variant's classpath.

### Source-set symmetry — the old vs new rule

The **older** guideline required `app/src/standard/` and `app/src/rooted/`
to share fully-qualified class names so Gradle picks one per flavor. The
refactor-2026 batches **relaxed** this: modular flavor impls now live under
distinct per-feature packages
(`dev.ranzlappen.gadget.feature.{standard,rooted}.<feature>.*`) with
**distinct FQNs**. Hilt still picks the right impl because each flavor's
`RootBindings.kt` (and each `*-rooted`/`-standard` module's Hilt module) is
itself flavor-scoped and contributes only its impl to the variant. Distinct
FQNs also avoid the entry-point-getter-rename problem (see
[Troubleshooting → Hilt collisions](Troubleshooting)).

## Prohibited patterns

- ❌ **Gate UI on `BuildConfig.IS_ROOTED`.** Always inject
  `RootCapabilityRegistry` / `RootSafetyGate` (`:core:root`,
  `dev.ranzlappen.gadget.core.root.*`) and let the Hilt seam pick the impl.
- ❌ **Put rooted-specific code (su / libsu / `com.gadget.root.*`) under
  `src/main/` or a non-rooted `feature/<name>/`.** It lives in
  `src/rooted/` or `feature/<name>-rooted/`, with a no-op standard twin.
- ❌ **`rm -rf src/rooted/` / `feature/*-rooted/` from CI** to "exclude"
  root code — source-set scoping + `rootedImplementation`-only deps
  already do this; deletion races the build cache and breaks the rooted
  leg.

## The root safety / opt-out framework (`:core:root`)

Every rooted operation calls `RootSafetyGate.check(feature)` before
running. The gate combines:

1. `RootCapabilityRegistry` — is the rooted code path even available?
2. `RootSafetyPreferences` — has the user opted out of this feature?
3. `RootSoftLimiter` — within the soft rate-limit window?

It returns a `RootGateDecision` (`Allowed` / `BlockedByUser` /
`BlockedByLimiter` / `Unsupported`); UI translates each branch into a
user-visible explanation rather than failing silently.

**Two-stage opt-in UI** (`RootedFeatureTogglesCard`): a global **Safety
mode** master switch (default ON, blocks every `isWriteCapable` feature)
**and** each feature's own toggle (default OFF, `requiresExplicitConfirm`).
Because the card depends on legacy entry points it lives in `:app/src/main`
and is dropped into the modular `SettingsScreen` via a `rootFeatureToggles:
@Composable () -> Unit = {}` slot — the leaf-module-can't-see-`:app`
workaround. The card self-hides on standard/no-root via its
`hasRootAccess()` guard.

**Hardware-safe by construction:** clamp to a hard ceiling (torch's 150 %
brightness cap), bound any override with an absolute time ceiling (torch's
45 s thermal), and always restore device state in a `NonCancellable
finally` so a cancelled coroutine can't leave the device latched.

### Adding a new rooted feature

Declare a `RootFeatureKey` data object, add a `RootFeatureDescriptor`
(with optional `RootLimitPolicy`), land the impl under
`feature/<name>-rooted/`, and wire the UI through the gate. No
flavor-specific changes in shared modules. See [Module Authoring
Contract](Module-Authoring-Contract) item 3 and [Torch
Blueprint](Torch-Blueprint).

## Build outputs & versionCode

CI produces `standard-debug.apk`, `standard-release.apk` +
`standard-release.aab`, and `rooted-debug.apk` on every push.
`versionCode = CI_VERSION_CODE * 10 + flavorOffset` (standard +0, rooted
+1) — keeps the two APKs side-by-side and upgrades monotonic per flavor.

## The standard-APK leak gate

CI's `Assert standard APK has no rooted leakage` step (in
`build-apk.yml`) runs on both `assembleStandardDebug` and
`assembleStandardRelease` and hard-fails if the APK contains:

- su strings: `topjohnwu`, `libsu`, `/system/bin/su`, `/system/xbin/su`,
  `chainfire`, `hiddenapibypass`;
- rooted assets: `lsposed`, `magisk`, `spoofer`, `.magisk.`, `/su/`;
- root-tier permissions: `WRITE_SECURE_SETTINGS`, `MOUNT_UNMOUNT*`,
  `INSTALL_PACKAGES`, `DELETE_PACKAGES`, `READ_LOGS`, `MANAGE_USERS`,
  `CHANGE_CONFIGURATION`, `MASTER_CLEAR`, `REBOOT`, `ACCESS_SUPERUSER`.

The dex pattern is deliberately precise — bare `magisk`/`superuser` would
trip on shared `RootProvider` sealed-variant names and cosmetic
localization strings. If you add a new rooted-only library/asset/permission,
scope it to `rootedImplementation` / `app/src/rooted/assets/` /
`app/src/rooted/AndroidManifest.xml` — the gate catches the mistake on PR.

See also: [Testing & CI](Testing-and-CI), [Decision Records →
ADR-0001](Decision-Records).

---

> _Last reviewed: 2026-06-12 · Source: `docs/flavors.md`, `CLAUDE.md`
> (flavors + leak gate), `.github/workflows/build-apk.yml` · Related
> modules: `:core:root`, every `feature/*-rooted`/`-standard`, `:app`._
