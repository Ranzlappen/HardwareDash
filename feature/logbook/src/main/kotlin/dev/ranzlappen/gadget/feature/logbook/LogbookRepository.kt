package dev.ranzlappen.gadget.feature.logbook

import dev.ranzlappen.gadget.core.data.logbook.LogbookCheckpointEntity
import dev.ranzlappen.gadget.core.data.logbook.LogbookDao
import dev.ranzlappen.gadget.core.data.logbook.LogbookEntryEntity
import dev.ranzlappen.gadget.core.data.logbook.LogbookProcessEntity
import dev.ranzlappen.gadget.core.data.logbook.LogbookTagColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the Logbook module — wraps [LogbookDao]
 * (mirrors `AppRepository`'s "feature-layer repository wraps a
 * `:core:data` DAO" shape) and owns the one side effect the DAO can't:
 * arming/disarming [LogbookReminderScheduler] work whenever a checkpoint's
 * reminder time is set, changed, or no longer relevant.
 */
@Singleton
class LogbookRepository @Inject constructor(
    private val dao: LogbookDao,
    private val reminderScheduler: LogbookReminderScheduler,
) {

    val entries: Flow<List<LogbookEntryEntity>> = dao.observeEntries()

    val processes: Flow<List<LogbookProcessWithCheckpoints>> = combine(
        dao.observeProcesses(),
        dao.observeAllCheckpoints(),
    ) { processes, checkpoints ->
        val byProcess = checkpoints.groupBy { it.processId }
        processes.map { process ->
            LogbookProcessWithCheckpoints(process, byProcess[process.id].orEmpty())
        }
    }

    /** The `logbook_open_checkpoints` [dev.ranzlappen.gadget.core.model.MetricSource]
     *  signal: incomplete checkpoints across every process, right now. */
    val openCheckpointCount: Flow<Int> = dao.observeOpenCheckpointCount()

    suspend fun addEntry(text: String, tag: LogbookTagColor) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.insertEntry(
            LogbookEntryEntity(timestampMillis = System.currentTimeMillis(), text = trimmed, tag = tag),
        )
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteEntry(id)
    }

    /** Creates a process with its checkpoints in order, arming a reminder
     *  for each draft that requested one. Blank checkpoint names are
     *  dropped; a process with zero surviving checkpoints is not created. */
    suspend fun addProcess(name: String, checkpoints: List<LogbookCheckpointDraft>) {
        val processName = name.trim()
        val validCheckpoints = checkpoints.filter { it.name.isNotBlank() }
        if (processName.isEmpty() || validCheckpoints.isEmpty()) return

        val processId = dao.insertProcess(
            LogbookProcessEntity(name = processName, createdAtMillis = System.currentTimeMillis()),
        )
        val ids = dao.insertCheckpoints(
            validCheckpoints.mapIndexed { index, draft ->
                LogbookCheckpointEntity(
                    processId = processId,
                    orderIndex = index,
                    name = draft.name.trim(),
                    dueAtMillis = draft.dueAtMillis,
                    reminderAtMillis = draft.reminderAtMillis,
                )
            },
        )
        ids.zip(validCheckpoints).forEach { (id, draft) ->
            draft.reminderAtMillis?.let { reminderScheduler.schedule(id, it) }
        }
    }

    suspend fun deleteProcess(id: Long) {
        val checkpoints = dao.getCheckpointsForProcess(id)
        dao.deleteProcess(id) // ON DELETE CASCADE removes the checkpoint rows.
        checkpoints.forEach { reminderScheduler.cancel(it.id) }
    }

    /** Marks one checkpoint (by its position within [processId]'s ordered
     *  checkpoint list) as [completed] and cancels/leaves-cancelled its
     *  reminder — a completed checkpoint never needs to notify. */
    suspend fun setCheckpointCompleted(processId: Long, checkpointIndex: Int, completed: Boolean) {
        val checkpoint = dao.getCheckpointsForProcess(processId).getOrNull(checkpointIndex) ?: return
        dao.updateCheckpoint(
            checkpoint.copy(
                completed = completed,
                completedAtMillis = if (completed) System.currentTimeMillis() else null,
            ),
        )
        if (completed) reminderScheduler.cancel(checkpoint.id)
    }
}
