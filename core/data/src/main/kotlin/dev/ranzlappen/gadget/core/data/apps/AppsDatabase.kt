package dev.ranzlappen.gadget.core.data.apps

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Modular App-Organizer store. Kept separate from [dev.ranzlappen.gadget.core.data.MonitoringDatabase]
 * so the two concerns don't recombine (the legacy `GadgetDatabase` mixed the
 * apps tables with the monitoring time-series — this split is deliberate).
 *
 * Starts at version 1 with the *final* legacy schema (legacy `gadget_db` v4
 * apps tables), table- and column-compatible so legacy data imports row-for-row.
 * Legacy data ingestion (in-place upgrade + backup restore) is handled by the
 * `:core:data` import path rather than by Room auto-migration, because the
 * source rows live in a different (combined) database file.
 */
@Database(
    entities = [
        Folder::class,
        FolderApp::class,
        AppRecord::class,
        WebLinkApp::class,
        FolderRuleEntity::class,
        FolderWidgetConfig::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppsDatabase : RoomDatabase() {
    abstract fun appsDao(): AppsDao
}
