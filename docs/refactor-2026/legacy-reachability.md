# Legacy reachability audit — `com.gadget.*` in `:app`

> **Batch D of the engine-milestone plan. Closes the audit half of
> [#147](https://github.com/Ranzlappen/HardwareDash/issues/147); the
> *decision* half is Matthias's** (see § Recommendation). Audited at
> parity 310 (`find app/src -path "*com/gadget*" -name "*.kt" | wc -l`:
> 195 `src/main`, 86 `src/rooted`, 20 `src/standard`, 9 test sets),
> June 2026, post-PR-#148.

## Method

Reachability was determined by tracing, from every **live entry point**,
what legacy code can actually execute:

- **UI shell:** `MainActivity` hosts `GadgetApp` with exactly these routes:
  Dashboard, Torch, Vibration, Apps, Settings (+ Sensors/Actuators/
  Automation coming-soon placeholders). No other Activity is reachable
  from a launcher intent.
- **Manifest registrations** (`app/src/main/AndroidManifest.xml`): one
  extra activity (`CallerScreenActivity`), 9 legacy services
  (`com.gadget.services.*` + `gps.spoof.LocationSpoofService`), 2
  receivers (`AdminReceiver`, `ScheduleActionReceiver`). Registration
  keeps a component *startable*, but each was traced to its starters.
- **Process bootstraps:** `GadgetApplication.onCreate`
  (`notifications.ensureAllChannels`) and
  `MainActivity.bootstrapLegacyManagers()` (localization/theme/
  accessibility managers, osmdroid config, `WidgetUpdateWorker.schedule`,
  logbook reminder channel).
- **Settings slots:** `BackupCard` (`com.gadget.backup`) and
  `RootedFeatureTogglesCard` (`com.gadget.root.ui`) are deliberately
  slotted into the modular Settings screen — reachable by design.
- Cross-package imports were swept per capability package to find who,
  outside the package, references it.

## Headline findings

1. **The legacy screens are already gone.** There is no
   `com.gadget.ui.screens` package — the clean-cut deleted the per-feature
   screens (Radios, Battery, Camera, GPS, …) when the new shell landed.
   What survives under `com.gadget.ui` is support code (`components`,
   `theme`, `charts`) plus four **orphaned** screens (`LogbookScreen`,
   `LinkScreen`, `OnboardingScreen`, `MetricHistoryScreen`) that *nothing
   references*.
2. **Every manifest-registered legacy service is inert.** Their only
   starters are (a) the **unregistered** legacy widget providers
   (`com.gadget.widget`, dead code per #107), (b) the orphaned legacy
   screens above, or (c) each other. No reachable code path starts any of
   them. The legacy `StrobeService` / `VibrationService` are superseded by
   the `:feature:torch` / `:feature:vibration` ones.
3. **Every legacy capability package is an orphaned engine room.** The
   only external consumers of `com.gadget.{sensors,battery,audio,camera,
   gps,ir,nfc,wifi,bluetooth,cell,storage,display,microphone,diagnostics,
   automation,usbdebug,adbdebug,…}` are `RootFeaturesEntryPoint` (the
   legacy Hilt entry point being retired per #94's roadmap) and the
   `root/ui` `Rooted*ExtrasSections` cards — **which are themselves
   orphaned** (their host screens were deleted; zero references). The
   flavor source sets (86 rooted / 20 standard files) keep the 22 legacy
   controllers compiled and Hilt-bound per flavor, but no UI can invoke
   them.
4. **One piece of live legacy work is pure waste:**
   `WidgetUpdateWorker.schedule()` runs periodically from every app launch
   to call `GadgetWidgetProvider.updateAll(...)` — a widget provider that
   is **no longer registered** in any manifest. It burns a WorkManager
   slot doing nothing observable.
5. **Live-and-intentional legacy code** (the small set that should stay
   until its replacement ships): `backup` + `backup/ui` (Settings slot),
   `root/ui`'s `FatalLaunchScreen` + `RootedFeatureTogglesCard` (+ its
   nested dialog/legal/disclaimer/reset cards), `localization`,
   `ui/theme` managers, `notifications` channels, `data/db` (the legacy
   `gadget_db` the backup path stages), `permissions` (referenced by the
   toggles card flow).

**Net: the app is already living under a de-facto strict clean-cut.** For
a daily-driver install, every non-migrated feature is *gone*, not hidden —
and has been since the shell landed. The regression this audit was
commissioned to assess already happened, silently, and nobody could have
bridged to the old screens anyway: **they no longer exist in the tree.**

## Per-package disposition

Counts are `src/main` Kotlin files; ✚N marks additional rooted/standard
flavor files. "Queue" is the WS5 migration order from the get-back-on-track
plan, subject to this batch's decision.

| Package | Files | Reachable today? | Kept alive by | Queue | Interim disposition |
|---|---|---|---|---|---|
| `gps` + `gps/spoof` | 19 ✚5 | ❌ (service registered, never started) | manifest reg., `RootFeaturesEntryPoint` | WS5 #3 (`feature/gps`; spoof → rooted sibling) | Keep until migration; delete with it |
| `flipper` + `flipper/rpc` | 12 | ❌ | nothing external | WS5 #4 (`feature/flipper`) | Keep until migration |
| `ir`, `nfc` | 8 | ❌ (`NfcEmulationService` registered, never started) | `RootFeaturesEntryPoint`, orphaned radios card | WS5 #2 (`radios-nfc` / `radios-ir`) | Keep until migration |
| `subghz` | 1 | ❌ | nothing | WS5 #6 | Keep until migration |
| `sensors` | 3 ✚5 | ❌ | `RootFeaturesEntryPoint`, orphaned monitor card | **WS5 #1 / Batch G (active)** | Delete in Batch G |
| `battery`, `audio`, `camera`, `storage`, `wifi`, `bluetooth`, `cell`, `display`, `microphone`, `diagnostics`, `usbdebug`, `adbdebug` | ~35 ✚~60 | ❌ | `RootFeaturesEntryPoint`, orphaned extras cards | WS5 #5–#6 | Keep until each migration |
| `automation` | 3 ✚2 | ❌ | `RootFeaturesEntryPoint` | superseded by `:core:automation` engine (epic #145) | Delete with engine Batch F, after confirming no binding consumers |
| `services` (9 files) | 9 | ❌ **inert** — registered, zero starters | manifest reg. only | dies with its feature's migration | Candidates for early deletion per feature (see cleanups) |
| `widget` | 16 | ⚠️ only `WidgetUpdateWorker` runs (uselessly) | `MainActivity` bootstrap | #107 (already filed) | **Cleanup C1+C2 below** |
| `ui/logbook`, `ui/link`, `ui/charts`, `ui/onboarding` | 12 | ❌ orphaned screens | nothing | Logbook/Link superseded by monitoring + automation engine; onboarding TBD | Delete when their successor ships (Link → Batch H) |
| `ui/components`, `ui/theme` | 20 | ⚠️ partially (theme managers bootstrapped; components serve orphaned screens) | `MainActivity`, `FatalLaunchScreen` | dies with last legacy screen | Keep |
| `root` + `root/ui` | 19 | ⚠️ `FatalLaunchScreen` + `RootedFeatureTogglesCard` reachable; **11 extras-section cards orphaned** | Settings slot, launch gate | #94 roadmap (per-feature retirement) | Keep the 2 live surfaces; orphaned cards die with their feature's migration |
| `backup` + `backup/ui` | 3 | ✅ Settings slot | intentional | replaced when backup goes modular | Keep |
| `localization`, `notifications`, `notification`, `keepalive`, `permissions`, `receivers`, `data/*`, `di`, `export`, root-level | ~30 | ⚠️ mixed (channels/managers live; `AdminReceiver`+`ScheduleActionReceiver`+`CallerScreenActivity` traced only to dead code) | bootstraps, manifest | infrastructure — dies last | Keep; see cleanup C3 |

## The decision: strict clean-cut vs. bridge-until-migrated

### Option 1 — Strict clean-cut (recommended)

Accept the (already-in-effect) regression; prioritise the WS5 migration
order by daily-use value. Each migration batch ends by deleting its
`com.gadget.<feature>` package + flavor controllers + its inert service —
the parity metric is the progress bar.

**Cost, honestly stated:** the daily driver stays without GPS-spoof,
Flipper, IR/NFC, SubGHz, logbook, etc. until each module ships. At
roughly one feature per batch, the high-value tail (sensors → NFC/IR →
GPS → Flipper) spans the next several batches.

### Option 2 — Bridge-until-migrated

**Materially more expensive than the original plan assumed.** The plan's
framing ("host the old screens behind a Legacy rail section") presumed the
screens still existed. They don't — bridging means **resurrecting each
screen from the `legacy-main` archive**, reconciling it against deleted
helpers (`ui/screens` siblings, old nav, old theme), re-registering its
services, and maintaining that Frankenstein surface until the real module
ships — then deleting it again. That is migration-scale work per feature,
spent on code with a planned death date, and it re-introduces exactly the
`com.gadget.*` coupling the CI leak gate exists to kill.

**Recommendation: Option 1, strict clean-cut.** The bridge's premise
(cheaply re-linking existing screens) is factually void; its real cost
approaches doing the migration twice. The honest lever for the daily
driver is migration *order*, which Matthias should set (the WS5 default is
sensors → NFC/IR → GPS → Flipper → the rest; reorder by what you actually
miss day-to-day).

## Interim cleanups (independent of the decision, all reversible)

- **C1:** stop calling `WidgetUpdateWorker.schedule()` from
  `MainActivity` and cancel its unique work on next launch — it services
  an unregistered provider. (Fold into #107.)
- **C2:** execute #107 (delete the dead `com.gadget.widget` providers);
  this audit confirms the package's only live member is the worker from C1.
- **C3 (optional):** unregister `CallerScreenActivity`,
  `ScheduleActionReceiver`, and the inert `com.gadget.services.*`
  entries from the manifest — they are unreachable yet still part of the
  app's attack/permission surface (e.g. `SYSTEM_ALERT_WINDOW`-style
  affordances, foreground-service types). Verify per component before
  removal; `AdminReceiver` needs a device-admin-state check first.

## Decision record

- **2026-06-11 — audit complete; recommendation: strict clean-cut.**
- **Decision (Matthias):** _pending — recorded here when made._
- **WS5 order confirmed/amended:** _pending._
