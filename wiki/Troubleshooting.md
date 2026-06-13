# Troubleshooting

The practical failure guide — the engineering pitfalls that **don't show up
in a local syntax check**. There is no Android SDK in the agent dev
container, so CI catches build errors; this page is the pre-flight
checklist. Grouped by symptom.

## Kotlin visibility — `internal` leaking through public API

`'public' function exposes its 'internal' return type containing
declaration X`. Common causes: a `public` function returns/accepts an
`internal` type (or a type nested in an `internal object`); a `public
class` has a `public` member touching an `internal` type in its signature.

**Rules:** if a type is `internal`, every public function/property
mentioning it **in its signature** must also be `internal` (bodies are
fine). Prefer `internal` for module-private helpers (codecs, framing, wire
format). After adding files, grep `^internal (object|class|interface)` and
confirm every consumer is `internal` too, or widen the type.

## Compose / SDK-guard mistakes

- **`compileSdk = 35` constants** (`Context.RECEIVER_NOT_EXPORTED`, API
  33+ APIs) must be guarded with `Build.VERSION.SDK_INT >=
  Build.VERSION_CODES.TIRAMISU`. Don't use `VERSION_CODES.S` (=31) to guard
  an API-33-only call.
- **`Modifier.blur(…)` requires API 31+** — a no-op below. Glass components
  need a higher-opacity solid fallback under `SDK_INT < S`.
- **`return@OutlinedButton` / `return@Button` for named-argument lambdas**
  (`onClick = { … }`) is unreliable — the label resolves to the parameter
  name, not the function. Restructure with `if/else`.
- **`@Composable` accessor reads inside non-composable callbacks** — `val
  foo @Composable get()` must be evaluated inside a `@Composable` function.
  Capture the resolved object into a local once; read its plain `val`s from
  callbacks.
- **`usb-serial-for-android`** maps `setDTR/getDTR/setRTS/getRTS` to
  property `DTR`/`RTS` (uppercase). Call the methods explicitly.

## Compose annotations in non-Compose modules

`:core:datastore` (and other foundation modules) don't pull in
`androidx.compose.runtime`, so `@Immutable` / `@Stable` won't resolve.
Compose treats `data class`es and enums of stable members as stable
automatically — **drop the annotation** rather than adding the Compose dep
to a foundation module.

## `collectAsStateWithLifecycle` not resolving

It lives in `androidx.lifecycle:lifecycle-runtime-compose`, **not** the
`lifecycle-runtime-ktx` the feature convention plugin brings in by default.
Either add `androidx-lifecycle-runtime-compose` to the plugin's default
`implementation` set, or use `collectAsState()` (fine when the flow is
already `stateIn(…)`-backed, which describes most feature ViewModels).

## Library `BuildConfig.VERSION_NAME` missing

Only `:app`'s `defaultConfig` generates `VERSION_NAME` / `VERSION_CODE`. A
feature module's `BuildConfig` carries `BUILD_TYPE` + `DEBUG` only. When a
feature card needs the version (Settings About), add `buildConfigField`
entries re-deriving both from the same `CI_VERSION_NAME` / `CI_VERSION_CODE`
gradle properties `:app` reads.

## Non-exhaustive `when` after adding a sealed/enum value

After adding a `GadgetDestination` value (or any sealed/enum used in
non-exhaustive `when`), grep `when (destination)` / `when (it:
GadgetDestination)` across `:core:navigation`, `:app`, and every feature
and add the missing branch. `ComingSoonScreen` was the first site.

## `init`-block forward-reference

Kotlin runs `init` blocks and property initializers in **declaration
order**. An `init` block reading/registering a property declared *below* it
sees an uninitialized field. Move the property above the `init` block, or
move the registration into the property's initializer. Tripped
`StandardTorchController`'s `TorchCallback` registration.

## Hilt errors

- **Entry-point method name collisions.** Hilt synthesises one `SingletonC`
  implementing every `@EntryPoint`. Two entry points with a same-named
  getter but different return types (legacy `com.gadget.torch.TorchController`
  vs new modular `…feature.torch.TorchController`) fail with `Found
  conflicting entry point declarations`. Rename the legacy getter
  (`legacyXController()`) until the legacy impl is deleted. (Distinct FQNs
  per flavor avoid this — see [Flavors & Root Safety](Flavors-and-Root-Safety).)
- **Duplicate bindings.** A second **bare** `@Binds @Singleton X` for a
  type already bound bare is a `[Dagger/DuplicateBindings]` clash in
  `SingletonC`. Use a `Map<String, X>` multibinding keyed by `featureId`
  (the widgetkit renderer/dispatcher pattern — see [Widgets, Tiles &
  Surfaces](Widgets-Tiles-and-Surfaces)).
- **Companion-object `@Provides` on an abstract `@Module`** is fragile
  across Hilt/KSP versions — some configs skip the `@Provides`, leaving the
  binding unresolved. Convention: a top-level `object` module for
  `@Provides` + a separate abstract class for `@Binds` (see
  `feature/torch/.../di/TorchProvidesModule.kt`).

## Flavor source-set mismatch / rooted code leakage

See [Flavors & Root Safety](Flavors-and-Root-Safety). Never `rm -rf
src/rooted/` from CI; never import su under `src/main/`; the leak gate
catches the rest. **Same-package symbol resolution across a module-move:**
Kotlin references a same-package type without an import — after lifting an
interface into a new module, every impl that stayed at the old package
loses access (`error.NonExistentClass` at KSP). Audit before/after with
`grep -rn "interface X\|: X\b"` and inject an explicit import.

## Foreground service failures

Required typed `foregroundServiceType` on API 34+. Use `shortService` (3-min
OS cap) for brief user-initiated tasks like `setTorchMode` — no
camera-typed FGS permission needed. Wrap every `startForegroundService` in
try/catch for `IllegalStateException`
(`ForegroundServiceStartNotAllowedException`, API 31+) and degrade
gracefully — never crash on a stray broadcast.

## Widget pinning failures (pins blank + inert)

The pin success-callback `PendingIntent` **must** be `FLAG_MUTABLE` +
explicit `ComponentName`, **and** the provider **must** override
`reconcilePendingConfig` with `claimSolePending` over a config carried
through `PendingWidgetConfigs`. A `FLAG_IMMUTABLE` callback silently drops
the assigned `appWidgetId` fill-in. CI-invisible — only fails inside the
launcher round-trip. Full detail: [Widgets, Tiles &
Surfaces](Widgets-Tiles-and-Surfaces).

## RemoteViews limitations

Widget layouts inflate only `@RemoteView` classes — a bare
`<View>`/`<Space>` throws `InflateException` → "Couldn't add widget". Use
`ImageView`/`FrameLayout` for backgrounds. With `nonTransitiveRClass=true`,
**Kotlin** refs to a dep module's id must qualify the R class (`WidgetKitR.id.…`);
XML refs merge fine. Bitmaps must be sized to the widget (Binder
transaction limit).

## DataStore corruption

`FeaturePreferences` uses a `ReplaceFileCorruptionHandler` so one bad write
can't permanently brick a feature's storage. New per-feature DataStores
should do the same.

## Room migration failures

- **0-byte schema file.** A 0-byte `.gitkeep` (or any unparseable file) in
  `room.schemaLocation` makes the KSP processor throw
  `IllegalStateException("Empty schema file")` — it iterates every file in
  the dir. Delete it and let Room create the dir on first export, or commit
  a real schema. (Fixed on PR #123.)
- **Legacy backup restore.** A backup with `gadget_db` but no
  `databases/apps.db` is a legacy backup — its `gadget_db` is an old schema
  Room can't migrate (`migration from 1 to 5 not found`). Restore **stages
  it off the live Room path** (`filesDir/legacy_restore/`), deletes the live
  `gadget_db` (Room recreates fresh), clears the `apps_migration` guard, and
  calls `LegacyAppsImporter.importFromStaged()` **in-process** (not a
  restart-dependent import — `exit(0)` restart proved unreliable). Restore
  must **not** delete `apps.db` (the import writes through the live, open
  connection). WAL must be checkpointed via `query("PRAGMA
  wal_checkpoint(FULL)")`, **not** `execSQL` (which rejects row-returning
  statements). Every new modular DB joins `DatabaseCheckpointer.checkpointAll()`
  (#153) and bumps `BACKUP_FORMAT_VERSION`. Full design: [Asset
  Catalog](Asset-Catalog) + the backup notes below.
- **WAL data loss on legacy restore.** After `PRAGMA wal_checkpoint(TRUNCATE)`
  the WAL may still exist if the checkpoint only partially succeeded (e.g.,
  another process held a read lock). `BackupManager` now copies the companion
  `-wal` file alongside the staged `gadget_db` before deleting the originals.
  `LegacyAppsImporter.openReadOnly` uses `OPEN_READONLY`, which makes SQLite
  apply the WAL at read time, recovering any committed pages not yet in the
  main file. Without this, committed folders/apps written only to the WAL are
  silently lost on a legacy-backup restore.
- **`LegacyAppsImporter` silent-failure on ID collision.** `insertFolder` /
  `insertWebLink` used `OnConflictStrategy.ABORT`; any ID collision (e.g.,
  user created a folder in the new app before import ran) threw and was
  swallowed by `runCatching`, leaving `KEY_DONE = false` so every subsequent
  launch retried and kept failing. Fixed: `AppsDao` exposes `upsertFolder` /
  `upsertWebLink` with `OnConflictStrategy.REPLACE`; `LegacyAppsImporter`
  uses them and returns a typed `ImportResult(folderCount, appCount,
  webLinkCount)` for UI feedback. A manual "Import from legacy app" action in
  the Apps screen's overflow menu calls `forceReimport()` to re-run without
  a cold restart.

## R8 / serialization issues

- **Minified release strips synthetic serializers** → runtime
  `SerializationException`. Each module with `@Serializable` types ships a
  `consumer-rules.pro` keeping them.
- **Polymorphic discriminator drift on package moves.** A `@Serializable
  sealed` subtype encodes its discriminator as its FQN. Moving a sealed
  root/subtype silently breaks decoding of every persisted user record. Fix:
  pin every subtype with explicit `@SerialName("<legacy.FQN>")` (a
  regression test guards it — `WidgetAppearanceSerializationTest`,
  `RuleSerializationTest`), OR bump `schemaVersion` + write a `Migrator<T>`.

## Emulator CI failures

`instrumented-tests.yml` pre-installs the API 30 system image with a
retry-on-corrupt-download step and enables KVM; Compose UI tests run with
animations off. A flaky emulator run is usually a corrupt image download —
re-run the job. See [Testing & CI](Testing-and-CI).

## Backup/restore design (reference)

Whole-app ZIP (format v5): legacy `gadget_db`, modular
`databases/{apps,monitoring,automation}.db`, every `shared_prefs/*.xml` +
`datastore/*`, and asset sweeps (`folder_covers/`, `apps_favicons/`,
`widget_icons/`). `BackupManager` lives in `:app` (it depends on the legacy
DB) and is dropped into `:feature:settings` via a `backupSection` slot,
reaching the singleton through `BackupManagerEntryPoint`. Not backed up:
OSMDroid tile cache, MediaStore exports, per-`appWidgetId` pending-pin
bridges.

---

> _Last reviewed: 2026-06-13 · Source: `CLAUDE.md` (engineering pitfalls),
> `docs/backup.md` · Related modules: all._
