# Backup / restore — design + format

Whole-app export/import so a restore on a fresh install reproduces the device's
full configuration. `app/src/main/java/com/gadget/backup/BackupManager.kt` owns
the archive; `BackupCard` (Settings) is the UI.

## Archive layout (ZIP, format v4)

```
metadata.json                      app version, schema versions, timestamp, backupFormatVersion
gadget_db (+ -wal, -shm)           legacy Room DB (metrics + pre-split apps_* tables)
databases/apps.db (+ -wal/-shm)    modular App-Organizer DB
databases/monitoring.db (+ …)      modular metric-history DB
shared_prefs/<name>.xml            every SharedPreferences file
datastore/<name>                   every DataStore<Preferences> file (widget configs, monitor config, …)
folder_covers/<id>.png             App-Organizer folder cover photos
apps_favicons/<sha1>               web-link favicon cache
widget_icons/custom:<uuid>.webp    custom torch / vibration widget icons   ← added in v4
```

`filesDirAssetSweeps` drives the `filesDir/<dir>` ↔ `<prefix>/` mapping for both
export and restore. **Adding a new asset dir**: append a `(subdir, prefix)` pair
and bump `BACKUP_FORMAT_VERSION`. Unknown prefixes are ignored on restore, so old
clients read new ZIPs gracefully.

### Format version history
- v1: db + shared_prefs + datastore
- v2: + folder_covers/ + apps_favicons/
- v3: + databases/ (modular apps.db / monitoring.db)
- v4: + widget_icons/ (custom widget icons)

## Export

`createBackup(OutputStream)` — checkpoints the WAL
(`query("PRAGMA wal_checkpoint(FULL)")`, **not** `execSQL` — it returns rows),
then streams each category into the ZIP. Triggered from `BackupCard` via
`ACTION_CREATE_DOCUMENT` (`application/zip`, default name
`gadget-backup-<yyyyMMdd-HHmm>.zip`).

## Restore

`restoreBackup(InputStream)` — closes the legacy DB, clears stale WAL/SHM (legacy
+ modular), then writes each entry back to its target. The modular DB singletons
can't be reopened in-process, so the restored `databases/*.db` apply on the
**next launch** — `BackupCard` shows a "restart required" dialog and offers a
cold restart. Triggered via `ACTION_OPEN_DOCUMENT` behind a destructive-action
confirm dialog.

## Legacy backwards-compatibility

A backup produced by the monolithic app (or before the apps.db split) carries
App-Organizer data inside `gadget_db`'s `apps_*` tables and has **no**
`databases/apps.db` entry. The restored `gadget_db` is also an **old schema** the
current `GadgetDatabase` (v5) can't migrate — leaving it on the live Room path
crashes Room on reopen (`"A migration from 1 to 5 was required but not found"`).
`LegacyAppsImporter` reads the legacy tables via **raw SQLite** (schema-agnostic),
so the file should never reach Room at all.

Restore detects the legacy shape (`restoredLegacyDb && !restoredModularAppsDb`)
and:

1. **Stages** the restored `gadget_db` to `filesDir/legacy_restore/gadget_db`
   (off the live Room path) and **deletes the live `gadget_db`** — so Room
   recreates a fresh current-schema db on reopen instead of choking on the old
   one.
2. Deletes the current `apps.db*` so the import rebuilds cleanly rather than
   merging into stale data.
3. Clears `LegacyAppsImporter`'s one-shot guard (the `apps_migration`
   SharedPreferences file).

On next launch `LegacyAppsImporter` finds the staged file, lifts its `apps_*`
rows into `apps.db` (raw SQLite), and deletes the staging dir. **Why the guard
reset matters**: the guard is a SharedPrefs flag a legacy backup doesn't contain,
so an install that already ran the importer once would otherwise keep the flag
set, skip the import, and silently drop the backup's folders. The legacy
`gadget_db`'s **metric** data is not recovered — Room can't migrate it — but it's
vestigial (metrics moved to `monitoring.db`); the folders, which users care
about, are.

## Wiring (leaf-module seam)

`BackupManager` depends on the legacy `GadgetDatabase`, which the modular
`:feature:settings` can't see. So the card lives in `:app`
(`com.gadget.backup.ui.BackupCard`) and is dropped into `SettingsScreen` /
`settingsScreen()` through a `backupSection: @Composable () -> Unit = {}` slot
that `:app`'s `MainActivity` fills — the same pattern as the rooted toggles card.
The card reaches the singleton via `BackupManagerEntryPoint.get(context)`.

## Not in the backup

OSMDroid tile cache (regenerates), MediaStore exports (user-managed, public), and
the per-`appWidgetId` pending-pin bridges (system-assigned ids — the user re-pins
widgets on a new device so the OS mints fresh ids).

## Testing

Manual on-device (no automated coverage yet — `BackupManager` is tightly coupled
to `Context` + Room):
1. Export → file lands; re-import on a fresh install reproduces folders, widget
   configs, custom icons, monitor history.
2. Legacy: restore an old monolithic backup → folders appear after the prompted
   restart (the importer re-runs).
