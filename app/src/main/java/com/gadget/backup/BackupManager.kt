package com.gadget.backup

import android.content.Context
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import com.gadget.data.db.GadgetDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.feature.apps.LegacyAppsImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whole-app backup / restore. Produces a ZIP that captures every byte of
 * user state the app has authority over so a restore on a fresh install
 * reproduces the device's full configuration:
 *
 *  - `metadata.json` — app version, schema versions, timestamp.
 *  - `gadget_db` (+ `-wal` + `-shm`) — the Room database (every entity).
 *  - `shared_prefs/<name>.xml` — every `SharedPreferences` file the app uses.
 *  - `datastore/<name>` — every `DataStore<Preferences>` file the app uses.
 *  - `folder_covers/<id>.png` — App-Organizer folder cover photos.
 *  - `apps_favicons/<sha1>` — App-Organizer web-link favicon cache.
 *
 * Things deliberately NOT in the ZIP:
 *  - The OSMDroid map tile cache (regenerates on demand).
 *  - User exports to MediaStore (camera / voice / video) — those are public
 *    files the user manages outside the app.
 *  - Per-`appWidgetId` rows whose ids are system-assigned: the rows ride
 *    along in the Room DB, but on a new device the user has to re-pin
 *    widgets so the OS can mint fresh appWidgetIds.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GadgetDatabase,
    private val legacyAppsImporter: LegacyAppsImporter,
) {

    /**
     * (subdir under filesDir, ZIP-entry prefix). Every file directly inside
     * the subdir is added to the ZIP under the prefix; `restoreBackup`
     * mirrors the reverse mapping. Keep this list in sync with new asset
     * directories the app introduces.
     */
    private val filesDirAssetSweeps: List<Pair<String, String>> = listOf(
        "folder_covers" to "folder_covers",
        "apps_favicons" to "apps_favicons",
        // Custom widget icons imported in the torch / vibration widget
        // customizers (downscaled WEBP under filesDir/widget_icons/, keyed
        // "custom:<uuid>"). Without this sweep a restore loses every custom
        // icon and the placed widgets fall back to their default glyph.
        "widget_icons" to "widget_icons",
    )

    suspend fun createBackup(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        // PRAGMA wal_checkpoint returns rows, so it must run as a query, not execSQL.
        database.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(FULL)")
            .use { it.moveToFirst() }

        ZipOutputStream(outputStream).use { zip ->
            // 1. metadata.json
            val metadata = buildMetadata()
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write(metadata.toString(2).toByteArray())
            zip.closeEntry()

            // 2. Database file
            val dbPath = database.openHelper.writableDatabase.path
            if (dbPath != null) {
                val dbFile = File(dbPath)
                if (dbFile.exists()) {
                    addFileToZip(zip, dbFile, "gadget_db")
                }
                val walFile = File("$dbPath-wal")
                if (walFile.exists()) {
                    addFileToZip(zip, walFile, "gadget_db-wal")
                }
                val shmFile = File("$dbPath-shm")
                if (shmFile.exists()) {
                    addFileToZip(zip, shmFile, "gadget_db-shm")
                }
            }

            // 2b. Modular Room databases (apps.db = App-Organizer, monitoring.db
            //     = metric history) live in the same databases/ dir but aren't
            //     owned by this manager's GadgetDatabase reference. Sweep them
            //     (+ their -wal/-shm) under databases/ so v2 backups carry that
            //     data. gadget_db* is already captured above and is skipped here.
            val databasesDir = dbPath?.let { File(it).parentFile }
            if (databasesDir != null && databasesDir.isDirectory) {
                databasesDir.listFiles()?.forEach { file ->
                    if (file.isFile && !file.name.startsWith("gadget_db")) {
                        addFileToZip(zip, file, "databases/${file.name}")
                    }
                }
            }

            // 3. SharedPreferences XML files
            val sharedPrefsDir = File(context.filesDir.parent, "shared_prefs")
            if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                sharedPrefsDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".xml")) {
                        addFileToZip(zip, file, "shared_prefs/${file.name}")
                    }
                }
            }

            // 4. DataStore files
            val dataStoreDir = File(context.filesDir, "datastore")
            if (dataStoreDir.exists() && dataStoreDir.isDirectory) {
                dataStoreDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        addFileToZip(zip, file, "datastore/${file.name}")
                    }
                }
            }

            // 5. App-Organizer asset directories (cover photos + favicon cache).
            //    These live under filesDir but outside `datastore/`, so the
            //    sweep above doesn't reach them.
            for ((subDir, prefix) in filesDirAssetSweeps) {
                val src = File(context.filesDir, subDir)
                if (src.exists() && src.isDirectory) {
                    src.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            addFileToZip(zip, file, "$prefix/${file.name}")
                        }
                    }
                }
            }
        }
        Timber.i("Backup created successfully")
    }

    suspend fun restoreBackup(inputStream: InputStream) = withContext(Dispatchers.IO) {
        val dbPath = database.openHelper.writableDatabase.path

        // Close the database before restoring
        database.close()

        // Delete stale WAL/SHM so a leftover write-ahead log doesn't shadow the
        // restored DB on next open. The ZIP may or may not include fresh ones.
        if (dbPath != null) {
            File("$dbPath-wal").delete()
            File("$dbPath-shm").delete()
        }

        // Same for the modular Room DBs (apps.db / monitoring.db): their open
        // singletons can't be closed from here, so the restore takes effect on
        // the next app launch ("restart to apply"); clearing their stale
        // WAL/SHM up front keeps the restored main files from being shadowed.
        val databasesDir = dbPath?.let { File(it).parentFile }
        if (databasesDir != null && databasesDir.isDirectory) {
            databasesDir.listFiles()?.forEach { file ->
                if (file.isFile &&
                    !file.name.startsWith("gadget_db") &&
                    (file.name.endsWith("-wal") || file.name.endsWith("-shm"))
                ) {
                    file.delete()
                }
            }
        }

        // Pre-resolve asset prefix → target subdir for cheap O(1) restore-side
        // dispatch matching the createBackup mapping.
        val assetPrefixToSubdir: Map<String, String> = filesDirAssetSweeps
            .associate { (subDir, prefix) -> prefix to subDir }

        // Legacy-backup detection: a backup produced by the monolithic app (or
        // before the apps.db split) carries the App-Organizer data inside
        // gadget_db's apps_* tables and has NO databases/apps.db entry. We note
        // both signals during the sweep and, if it's a legacy backup, hand the
        // data off to LegacyAppsImporter on next launch (below).
        var restoredLegacyDb = false
        var restoredModularAppsDb = false

        try {
            ZipInputStream(inputStream).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == "metadata.json" -> {
                            zip.readBytes()
                            Timber.i("Backup metadata read")
                        }

                        name.startsWith("gadget_db") -> {
                            if (dbPath != null) {
                                restoredLegacyDb = true
                                val suffix = name.removePrefix("gadget_db")
                                val targetFile = File("$dbPath$suffix")
                                targetFile.outputStream().use { out -> zip.copyTo(out) }
                                Timber.i("Restored database file: $name")
                            }
                        }

                        name.startsWith("databases/") -> {
                            if (name.removePrefix("databases/").startsWith("apps.db")) {
                                restoredModularAppsDb = true
                            }
                            // Modular Room DBs (apps.db / monitoring.db). Written
                            // into the same databases/ dir as gadget_db; applied
                            // on next launch. A legacy backup (no databases/
                            // entries) instead restores gadget_db, and the
                            // LegacyAppsImporter lifts its apps_* rows into
                            // apps.db on that launch.
                            val fileName = name.removePrefix("databases/")
                            val targetDir = dbPath?.let { File(it).parentFile }
                            if (targetDir != null) {
                                targetDir.mkdirs()
                                File(targetDir, fileName).outputStream().use { out ->
                                    zip.copyTo(out)
                                }
                                Timber.i("Restored database file: $fileName")
                            }
                        }

                        name.startsWith("shared_prefs/") -> {
                            val fileName = name.removePrefix("shared_prefs/")
                            val targetDir = File(context.filesDir.parent, "shared_prefs")
                            targetDir.mkdirs()
                            val targetFile = File(targetDir, fileName)
                            targetFile.outputStream().use { out -> zip.copyTo(out) }
                            Timber.i("Restored shared pref: $fileName")
                        }

                        name.startsWith("datastore/") -> {
                            val fileName = name.removePrefix("datastore/")
                            val targetDir = File(context.filesDir, "datastore")
                            targetDir.mkdirs()
                            val targetFile = File(targetDir, fileName)
                            targetFile.outputStream().use { out -> zip.copyTo(out) }
                            Timber.i("Restored datastore file: $fileName")
                        }

                        else -> {
                            // Asset-dir restore: try every registered prefix.
                            // A single match wins; unknown entries are ignored
                            // so older clients reading newer ZIPs degrade
                            // gracefully.
                            val match = assetPrefixToSubdir.entries.firstOrNull {
                                name.startsWith(it.key + "/")
                            }
                            if (match != null) {
                                val fileName = name.removePrefix("${match.key}/")
                                val targetDir = File(context.filesDir, match.value)
                                targetDir.mkdirs()
                                val targetFile = File(targetDir, fileName)
                                targetFile.outputStream().use { out -> zip.copyTo(out) }
                                Timber.i("Restored ${match.value}: $fileName")
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            // Legacy backup (gadget_db present, no modular apps.db): the
            // App-Organizer data lives in gadget_db's apps_* tables, and the
            // restored gadget_db is an OLD schema the current GadgetDatabase
            // can't migrate. Don't leave it on the live Room path — Room would
            // crash trying to open it on reopen (below). Instead STAGE it for
            // LegacyAppsImporter (raw SQLite, schema-agnostic) and remove the
            // live file so Room recreates a fresh current-schema gadget_db.
            if (restoredLegacyDb && !restoredModularAppsDb && dbPath != null) {
                // Older backups (e.g. 1.0.117) may not have checkpointed before
                // export, leaving committed rows in the companion -wal. Merge the
                // WAL into the main file via a raw (non-Room) connection so the
                // staged copy is self-contained. TRUNCATE empties the WAL after.
                runCatching {
                    SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE).use { raw ->
                        raw.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
                    }
                }.onFailure { Timber.w(it, "legacy gadget_db WAL checkpoint failed") }

                val live = File(dbPath)
                val staging = File(
                    context.filesDir,
                    "${LegacyAppsImporter.RESTORE_STAGING_SUBDIR}/${LegacyAppsImporter.LEGACY_DB}",
                )
                staging.parentFile?.mkdirs()
                staging.delete()
                if (live.exists() && !live.renameTo(staging)) {
                    // Cross-dir rename can fail on some devices — fall back to copy.
                    live.inputStream().use { input ->
                        staging.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                live.delete()
                File("$dbPath-wal").delete()
                File("$dbPath-shm").delete()

                // Reset the importer's one-shot guard so a cold-start re-import
                // can run as a fallback if the in-process import below doesn't
                // complete. The guard is a SharedPrefs flag the legacy backup
                // doesn't overwrite, so an already-imported install would
                // otherwise skip it.
                context.getSharedPreferences(LEGACY_APPS_IMPORT_PREFS, Context.MODE_PRIVATE)
                    .edit().clear().commit()

                // Import the staged App-Organizer data IN-PROCESS through the
                // live apps.db connection so folders reappear WITHOUT a cold
                // restart (the cross-process restart proved unreliable on some
                // devices). The importer clears existing apps data first
                // (restore replaces). We deliberately DON'T delete apps.db —
                // that would orphan the open Room connection the import writes
                // through.
                legacyAppsImporter.importFromStaged()
                Timber.i("Legacy backup staged + imported in-process")
            }
        } finally {
            // Reopen the database
            database.openHelper.writableDatabase
        }
        Timber.i("Backup restored successfully")
    }

    private fun buildMetadata(): JSONObject {
        val versionName = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown"
        }

        // Read the live Room version so this stays correct across migrations
        // without a manual bump every schema change.
        val dbVersion = runCatching {
            database.openHelper.readableDatabase.version
        }.getOrDefault(0)

        return JSONObject().apply {
            put("appVersion", versionName)
            put("backupTimestamp", System.currentTimeMillis())
            put("backupDate", java.time.Instant.now().toString())
            put("androidSdk", Build.VERSION.SDK_INT)
            put("dbSchemaVersion", dbVersion)
            put("logbookSchemaVersion", 7)
            put("backupFormatVersion", BACKUP_FORMAT_VERSION)
        }
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { input ->
            input.copyTo(zip)
        }
        zip.closeEntry()
    }

    private companion object {
        /**
         * Bumped when the ZIP layout gains new entry kinds. Older clients
         * silently ignore unknown prefixes during restore, so this is purely
         * informational — useful when triaging backups from future builds.
         *  - 1: original (db + shared_prefs + datastore)
         *  - 2: + folder_covers/ + apps_favicons/ asset sweeps
         *  - 3: + databases/ sweep (modular apps.db / monitoring.db); a legacy
         *       backup without these restores gadget_db and the
         *       LegacyAppsImporter migrates its apps_* rows into apps.db.
         *  - 4: + widget_icons/ sweep (custom torch / vibration widget icons).
         */
        const val BACKUP_FORMAT_VERSION = 4

        /**
         * SharedPreferences file backing `LegacyAppsImporter`'s one-shot import
         * guard (`feature:apps`'s `LegacyAppsImporter.PREFS`). Cleared on a
         * legacy-backup restore so the importer re-runs. Hardcoded rather than
         * referenced to avoid a compile coupling to the feature module.
         */
        const val LEGACY_APPS_IMPORT_PREFS = "apps_migration"
    }
}
