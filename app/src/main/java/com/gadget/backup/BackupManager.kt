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

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GadgetDatabase,
) {

    /**
     * Creates a ZIP backup containing:
     * - metadata.json (app version, timestamp, schema versions)
     * - Room database file
     * - SharedPreferences XML files
     * - DataStore files
     */
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
                // Also backup WAL and SHM if they exist
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
        }
        Timber.i("Backup created successfully")
    }

    /**
     * Restores a ZIP backup by:
     * 1. Closing the database
     * 2. Restoring all files from the ZIP
     * 3. Reopening the database
     */
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

        try {
            ZipInputStream(inputStream).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "metadata.json" -> {
                            // Read metadata but don't need to store it
                            zip.readBytes()
                            Timber.i("Backup metadata read")
                        }

                        entry.name.startsWith("gadget_db") -> {
                            // Restore database file(s)
                            if (dbPath != null) {
                                val suffix = entry.name.removePrefix("gadget_db")
                                val targetFile = File("$dbPath$suffix")
                                targetFile.outputStream().use { out ->
                                    zip.copyTo(out)
                                }
                                Timber.i("Restored database file: ${entry.name}")
                            }
                        }

                        entry.name.startsWith("shared_prefs/") -> {
                            // Restore SharedPreferences
                            val fileName = entry.name.removePrefix("shared_prefs/")
                            val targetDir = File(context.filesDir.parent, "shared_prefs")
                            targetDir.mkdirs()
                            val targetFile = File(targetDir, fileName)
                            targetFile.outputStream().use { out ->
                                zip.copyTo(out)
                            }
                            Timber.i("Restored shared pref: $fileName")
                        }

                        entry.name.startsWith("datastore/") -> {
                            // Restore DataStore files
                            val fileName = entry.name.removePrefix("datastore/")
                            val targetDir = File(context.filesDir, "datastore")
                            targetDir.mkdirs()
                            val targetFile = File(targetDir, fileName)
                            targetFile.outputStream().use { out ->
                                zip.copyTo(out)
                            }
                            Timber.i("Restored datastore file: $fileName")
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

        return JSONObject().apply {
            put("appVersion", versionName)
            put("backupTimestamp", System.currentTimeMillis())
            put("backupDate", java.time.Instant.now().toString())
            put("androidSdk", Build.VERSION.SDK_INT)
            put("dbSchemaVersion", 2) // Room database version
            put("logbookSchemaVersion", 7) // Logbook schema version
        }
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { input ->
            input.copyTo(zip)
        }
        zip.closeEntry()
    }
}
