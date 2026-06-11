package dev.ranzlappen.gadget.core.data

import androidx.room.RoomDatabase
import dev.ranzlappen.gadget.core.data.apps.AppsDatabase
import dev.ranzlappen.gadget.core.data.automation.AutomationDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WAL-checkpoint seam for every modular Room database (issue #153).
 *
 * `BackupManager.createBackup` zips the live `databases/` dir — including
 * each DB's `-wal`/`-shm` sidecars — but a backup taken mid-write can
 * capture a **torn** copy of any database whose latest committed pages are
 * still in the WAL. The legacy `gadget_db` always got an explicit
 * `PRAGMA wal_checkpoint(FULL)` before the sweep; this class extends the
 * same guarantee to the modular DBs (`apps.db`, `monitoring.db`,
 * `automation.db`) so rule/folder/history restore fidelity doesn't depend
 * on write timing. Load-bearing for automation: rule-restore is part of
 * the engine-milestone acceptance (#145).
 *
 * Injects the same `@Singleton` database instances the repositories use,
 * so the checkpoint flushes the exact connection pool the app writes
 * through. **Add every future modular database to [databases]** — the
 * backup sweep is generic, so a DB missing here is silently backed up
 * torn-prone again.
 */
@Singleton
class DatabaseCheckpointer @Inject constructor(
    monitoringDatabase: MonitoringDatabase,
    appsDatabase: AppsDatabase,
    automationDatabase: AutomationDatabase,
) {
    private val databases: List<RoomDatabase> =
        listOf(monitoringDatabase, appsDatabase, automationDatabase)

    /**
     * Run `PRAGMA wal_checkpoint(FULL)` on every modular database. Call
     * before any file-level copy of the `databases/` dir.
     */
    suspend fun checkpointAll() = withContext(Dispatchers.IO) {
        databases.forEach { database ->
            // wal_checkpoint returns a result row, so it must run as a
            // query — execSQL rejects statements that return rows (the
            // same gotcha CLAUDE.md documents for the legacy path).
            database.openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)")
                .use { it.moveToFirst() }
        }
    }
}
