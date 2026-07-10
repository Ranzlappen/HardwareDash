package dev.ranzlappen.gadget.core.data.logbook

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A named checkpoint sequence — the "process" / reminders half of the
 * Logbook module's brief. Simplified from the legacy
 * `com.gadget.ui.logbook.LogbookProcess`: no `currentCheckpoint` cursor (a
 * process is just "a bag of checkpoints", each independently completable —
 * see [LogbookCheckpointEntity.completed] — rather than a single-file
 * pipeline the legacy model advanced through one stage at a time), no
 * `tags`/`bgColor`/`borderColor` (the same dropped customization surface as
 * [LogbookEntryEntity]), no built-in process *templates* (the legacy
 * `ProcessTemplate` enum's three canned checklists — dropped per the
 * module's scope guidance; a user types their own checkpoint names).
 */
@Entity(tableName = "logbook_process")
data class LogbookProcessEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long,
)
