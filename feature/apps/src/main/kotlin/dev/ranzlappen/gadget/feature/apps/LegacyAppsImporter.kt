package dev.ranzlappen.gadget.feature.apps

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.data.apps.AppRecord
import dev.ranzlappen.gadget.core.data.apps.AppsDao
import dev.ranzlappen.gadget.core.data.apps.Folder
import dev.ranzlappen.gadget.core.data.apps.FolderApp
import dev.ranzlappen.gadget.core.data.apps.FolderRuleEntity
import dev.ranzlappen.gadget.core.data.apps.FolderWidgetConfig
import dev.ranzlappen.gadget.core.data.apps.WebLinkApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot importer that copies App-Organizer data out of the **legacy**
 * `gadget_db` (the combined com.gadget Room database) into the modular
 * `:core:data` `AppsDatabase` (`apps.db`).
 *
 * Covers both data-continuity paths for the migration:
 *  - **in-place app upgrade** — an existing install's `gadget_db` still holds
 *    the user's folders/membership/web-links/rules/widget bindings; the modular
 *    `apps.db` starts empty until this copies them over.
 *  - **legacy backup restore** — `BackupManager` restores the legacy `gadget_db`
 *    file; on the next launch this importer lifts its `apps_*` rows into
 *    `apps.db`.
 *
 * Reads the legacy tables via **raw SQLite** (the legacy Room entities were
 * deleted in the migration) and is guarded by a one-shot SharedPreferences flag
 * so it runs exactly once. The legacy `apps_*` tables are left untouched in
 * `gadget_db` (now unused — see `GadgetDatabase.MIGRATION_4_5`).
 *
 * Eagerly instantiated for the process lifetime (the app startup path injects
 * it), mirroring `AppRepository`.
 */
@Singleton
class LegacyAppsImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AppsDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch { importIfNeeded() }
    }

    suspend fun importIfNeeded() {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // A legacy-backup restore stages the old gadget_db here (BackupManager)
        // so Room never tries to open — and fail to migrate — the old-schema
        // file. Its mere presence forces a re-import even if a prior launch
        // already marked the one-shot guard done; the guard only gates the
        // in-place-upgrade path (reading the live gadget_db). The in-process
        // restore (BackupManager.importFromStaged) usually does this already —
        // this is the cold-start fallback if that didn't complete.
        val staging = stagingFile()
        val fromStaging = staging.exists()
        if (!fromStaging && prefs.getBoolean(KEY_DONE, false)) return

        val dbFile = if (fromStaging) staging else context.getDatabasePath(LEGACY_DB)
        if (!dbFile.exists()) {
            // Fresh install with no legacy DB — nothing to import, ever.
            prefs.edit().putBoolean(KEY_DONE, true).apply()
            return
        }

        // A staged restore replaces current data; an in-place upgrade merges
        // into the (empty-on-first-launch) modular DB.
        val imported = runCatching { doImport(dbFile, clearFirst = fromStaging) }
            .onFailure { Timber.e(it, "LegacyAppsImporter: import failed") }
            .getOrDefault(false)

        // Mark done even if the source had no apps tables, so we don't reopen
        // the legacy DB on every launch. A genuine failure (caught above)
        // leaves the flag unset so the next launch retries.
        if (imported || runCatching { !hasAppsTables(dbFile) }.getOrDefault(false)) {
            prefs.edit().putBoolean(KEY_DONE, true).apply()
            if (fromStaging) staging.parentFile?.deleteRecursively()
        }
        Timber.i(
            "LegacyAppsImporter: source=${if (fromStaging) "staged-restore" else "live"} imported=$imported",
        )
    }

    /**
     * Import a staged legacy backup **in-process**, called by `BackupManager`
     * right after a restore so folders reappear **without a cold restart** (the
     * cross-process restart proved unreliable on some devices). Writes through
     * the live [AppsDao] / `apps.db` connection — restore must therefore NOT
     * delete the apps.db file — and clears existing App-Organizer data first
     * (restore replaces). On success it sets the one-shot guard and drops the
     * staged file so the cold-start [importIfNeeded] is a no-op. Returns whether
     * data was imported.
     */
    suspend fun importFromStaged(): Boolean {
        val staging = stagingFile()
        if (!staging.exists()) return false
        if (!runCatching { hasAppsTables(staging) }.getOrDefault(false)) {
            staging.parentFile?.deleteRecursively()
            return false
        }
        val ok = runCatching { doImport(staging, clearFirst = true) }
            .onFailure { Timber.e(it, "LegacyAppsImporter: in-process restore import failed") }
            .getOrDefault(false)
        if (ok) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_DONE, true).apply()
            staging.parentFile?.deleteRecursively()
            Timber.i("LegacyAppsImporter: in-process restore import complete")
        }
        return ok
    }

    private fun stagingFile(): File =
        File(context.filesDir, "$RESTORE_STAGING_SUBDIR/$LEGACY_DB")

    private suspend fun doImport(dbFile: File, clearFirst: Boolean): Boolean {
        if (clearFirst) clearAllAppsData()
        return importFrom(dbFile)
    }

    private suspend fun clearAllAppsData() {
        dao.clearAllMembership()
        dao.clearRules()
        dao.clearWidgetConfigs()
        dao.clearWebLinks()
        dao.clearFolders()
        dao.clearAppRecords()
    }

    private suspend fun importFrom(dbFile: File): Boolean {
        openReadOnly(dbFile).use { db ->
            if (!hasAppsTables(db)) return false

            db.rawQuery("SELECT * FROM apps_folder", null).use { c ->
                while (c.moveToNext()) {
                    dao.insertFolder(
                        Folder(
                            id = c.getLong(c.getColumnIndexOrThrow("id")),
                            name = c.getString(c.getColumnIndexOrThrow("name")).orEmpty(),
                            baseColorArgb = c.getInt(c.getColumnIndexOrThrow("base_color_argb")),
                            coverIcon = c.getString(c.getColumnIndexOrThrow("cover_icon")).orEmpty(),
                            sortOrder = c.getInt(c.getColumnIndexOrThrow("sort_order")),
                            locked = c.getInt(c.getColumnIndexOrThrow("locked")) != 0,
                            createdAt = c.getLong(c.getColumnIndexOrThrow("created_at")),
                        ),
                    )
                }
            }

            val memberships = mutableListOf<FolderApp>()
            db.rawQuery("SELECT * FROM apps_folder_app", null).use { c ->
                while (c.moveToNext()) {
                    memberships += FolderApp(
                        folderId = c.getLong(c.getColumnIndexOrThrow("folder_id")),
                        appKey = c.getString(c.getColumnIndexOrThrow("app_key")).orEmpty(),
                        sortOrder = c.getInt(c.getColumnIndexOrThrow("sort_order")),
                    )
                }
            }
            if (memberships.isNotEmpty()) dao.insertFolderApps(memberships)

            val records = mutableListOf<AppRecord>()
            db.rawQuery("SELECT * FROM apps_record", null).use { c ->
                while (c.moveToNext()) {
                    records += AppRecord(
                        appKey = c.getString(c.getColumnIndexOrThrow("app_key")).orEmpty(),
                        packageName = c.getString(c.getColumnIndexOrThrow("package_name")).orEmpty(),
                        activityClass = c.getStringOrNull("activity_class"),
                        label = c.getString(c.getColumnIndexOrThrow("label")).orEmpty(),
                        userSerial = c.getLong(c.getColumnIndexOrThrow("user_serial")),
                        isWebApk = c.getInt(c.getColumnIndexOrThrow("is_web_apk")) != 0,
                        isWebLink = c.getInt(c.getColumnIndexOrThrow("is_web_link")) != 0,
                        firstInstallTime = c.getLong(c.getColumnIndexOrThrow("first_install_time")),
                        lastSeen = c.getLong(c.getColumnIndexOrThrow("last_seen")),
                        isOnExternalStorage = c.getIntOrZero("is_on_external_storage") != 0,
                        isSystemApp = c.getIntOrZero("is_system_app") != 0,
                    )
                }
            }
            if (records.isNotEmpty()) dao.upsertAppRecords(records)

            db.rawQuery("SELECT * FROM apps_weblink", null).use { c ->
                while (c.moveToNext()) {
                    dao.insertWebLink(
                        WebLinkApp(
                            id = c.getLong(c.getColumnIndexOrThrow("id")),
                            url = c.getString(c.getColumnIndexOrThrow("url")).orEmpty(),
                            label = c.getString(c.getColumnIndexOrThrow("label")).orEmpty(),
                            faviconPath = c.getStringOrNull("favicon_path"),
                            createdAt = c.getLong(c.getColumnIndexOrThrow("created_at")),
                        ),
                    )
                }
            }

            db.rawQuery("SELECT * FROM apps_folder_rule", null).use { c ->
                while (c.moveToNext()) {
                    dao.upsertRule(
                        FolderRuleEntity(
                            folderId = c.getLong(c.getColumnIndexOrThrow("folder_id")),
                            ruleJson = c.getString(c.getColumnIndexOrThrow("rule_json")).orEmpty(),
                        ),
                    )
                }
            }

            // Per-appWidgetId bindings survive an in-place upgrade (same ids);
            // the FolderWidgetProvider mirrors them into the kit store lazily.
            db.rawQuery("SELECT * FROM apps_widget_config", null).use { c ->
                while (c.moveToNext()) {
                    dao.upsertWidgetConfig(
                        FolderWidgetConfig(
                            appWidgetId = c.getInt(c.getColumnIndexOrThrow("app_widget_id")),
                            folderId = c.getLong(c.getColumnIndexOrThrow("folder_id")),
                            sizeVariant = c.getString(c.getColumnIndexOrThrow("size_variant")).orEmpty(),
                            createdAt = c.getLong(c.getColumnIndexOrThrow("created_at")),
                        ),
                    )
                }
            }
        }
        Timber.i("LegacyAppsImporter: imported App-Organizer data from gadget_db")
        return true
    }

    private fun hasAppsTables(dbFile: File): Boolean =
        openReadOnly(dbFile).use { hasAppsTables(it) }

    private fun hasAppsTables(db: SQLiteDatabase): Boolean =
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='apps_folder'",
            null,
        ).use { it.moveToFirst() }

    private fun openReadOnly(dbFile: File): SQLiteDatabase =
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)

    private fun android.database.Cursor.getStringOrNull(column: String): String? {
        val idx = getColumnIndex(column)
        return if (idx < 0 || isNull(idx)) null else getString(idx)
    }

    /** Tolerates a column the source DB predates (e.g. the v3→v4 flags). */
    private fun android.database.Cursor.getIntOrZero(column: String): Int {
        val idx = getColumnIndex(column)
        return if (idx < 0 || isNull(idx)) 0 else getInt(idx)
    }

    companion object {
        const val LEGACY_DB = "gadget_db"
        const val PREFS = "apps_migration"
        const val KEY_DONE = "legacy_apps_imported"

        /**
         * Subdir under `filesDir` where `BackupManager` stages a restored legacy
         * `gadget_db` for this importer to read via raw SQLite — keeping the
         * old-schema file off the live Room path so `GadgetDatabase` never tries
         * (and fails) to migrate it.
         */
        const val RESTORE_STAGING_SUBDIR = "legacy_restore"
    }
}
