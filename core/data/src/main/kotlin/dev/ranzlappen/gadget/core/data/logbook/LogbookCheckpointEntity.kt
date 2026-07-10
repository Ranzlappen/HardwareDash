package dev.ranzlappen.gadget.core.data.logbook

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One step of a [LogbookProcessEntity] — a name, an optional due date, an
 * optional reminder time, and a completion flag. `ON DELETE CASCADE` so
 * deleting a process cleans up its checkpoints in one statement; the
 * `process_id` index backs both the cascade and
 * [LogbookDao.observeCheckpoints]'s per-process query.
 *
 * [reminderAtMillis] is optional and independent of [dueAtMillis] — a
 * checkpoint can carry a due date with no reminder (a plain deadline) or a
 * reminder with no due date (a bare "ping me" note). When set,
 * `LogbookReminderScheduler` (in `:feature:logbook`) arms one WorkManager
 * one-shot per checkpoint keyed by [id].
 */
@Entity(
    tableName = "logbook_checkpoint",
    foreignKeys = [
        ForeignKey(
            entity = LogbookProcessEntity::class,
            parentColumns = ["id"],
            childColumns = ["process_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("process_id")],
)
data class LogbookCheckpointEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "process_id")
    val processId: Long,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "due_at_millis")
    val dueAtMillis: Long? = null,
    @ColumnInfo(name = "reminder_at_millis")
    val reminderAtMillis: Long? = null,
    @ColumnInfo(name = "completed")
    val completed: Boolean = false,
    @ColumnInfo(name = "completed_at_millis")
    val completedAtMillis: Long? = null,
)
