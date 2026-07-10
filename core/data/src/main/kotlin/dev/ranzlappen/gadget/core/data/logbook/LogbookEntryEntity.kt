package dev.ranzlappen.gadget.core.data.logbook

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single dated/tagged session-note log entry — the "session notes" half
 * of the Logbook module's brief.
 *
 * Deliberately simpler than the legacy `com.gadget.ui.logbook.LogbookEntry`:
 * no `custom`/auto-logged distinction, no free-form `tags: List<String>`,
 * no `metrics: Map<String, String>` snapshot (the legacy field existed for
 * a "attach the current sensor readout to this note" flow that never
 * shipped a UI on the modular side — adding it back would be schema
 * complexity with no consumer). One [tag] color picked from the fixed
 * [LogbookTagColor] enum covers the "tag this note" need this module scopes
 * to.
 */
@Entity(tableName = "logbook_entry")
data class LogbookEntryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long,
    @ColumnInfo(name = "text")
    val text: String,
    @ColumnInfo(name = "tag")
    val tag: LogbookTagColor = LogbookTagColor.None,
)
