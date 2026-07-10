package dev.ranzlappen.gadget.core.data.logbook

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Modular Logbook store — dated/tagged session-note entries plus named
 * checkpoint "processes". Kept separate from [dev.ranzlappen.gadget.core.data.MonitoringDatabase]
 * / [dev.ranzlappen.gadget.core.data.apps.AppsDatabase] / the automation
 * database for the same reason those are split from each other: one
 * feature-area concern per Room database file (`logbook.db`), never
 * recombined into one do-everything schema.
 *
 * A fresh schema starting at version 1 — this module deliberately does
 * **not** attempt to match the legacy `com.gadget.ui.logbook` "Ticked"
 * DataStore-JSON schema (`LOGBOOK_SCHEMA_VERSION = 7` in the deleted
 * `LogbookModels.kt`) or its migration chain; there is no legacy data to
 * import (the legacy feature was already deleted from the app before this
 * module existed) and no companion "Ticked" web app to stay
 * cross-compatible with.
 */
@Database(
    entities = [
        LogbookEntryEntity::class,
        LogbookProcessEntity::class,
        LogbookCheckpointEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(LogbookConverters::class)
abstract class LogbookDatabase : RoomDatabase() {
    abstract fun logbookDao(): LogbookDao
}
