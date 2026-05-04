package com.gadget.backup

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.gadget.data.db.GadgetDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
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

        // Pre-resolve asset prefix → target subdir for cheap O(1) restore-side
        // dispatch matching the createBackup mapping.
        val assetPrefixToSubdir: Map<String, String> = filesDirAssetSweeps
            .associate { (subDir, prefix) -> prefix to subDir }

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
                                val suffix = name.removePrefix("gadget_db")
                                val targetFile = File("$dbPath$suffix")
                                targetFile.outputStream().use { out -> zip.copyTo(out) }
                                Timber.i("Restored database file: $name")
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
         */
        const val BACKUP_FORMAT_VERSION = 2
    }
}
