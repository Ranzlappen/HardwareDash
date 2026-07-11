package dev.ranzlappen.gadget.feature.logbook

import dev.ranzlappen.gadget.core.data.logbook.LogbookCheckpointEntity
import dev.ranzlappen.gadget.core.data.logbook.LogbookProcessEntity

/**
 * A [LogbookProcessEntity] joined with its ordered [LogbookCheckpointEntity]
 * rows — the shape [LogbookRepository.processes] emits and the screen
 * renders. Built client-side (`groupBy` over
 * [dev.ranzlappen.gadget.core.data.logbook.LogbookDao.observeAllCheckpoints])
 * rather than via a Room `@Relation`, since both source flows are already
 * small, whole-table reads.
 */
data class LogbookProcessWithCheckpoints(
    val process: LogbookProcessEntity,
    val checkpoints: List<LogbookCheckpointEntity>,
) {
    /** Count of not-yet-completed checkpoints in this process. */
    val openCount: Int get() = checkpoints.count { !it.completed }

    /** True when any incomplete checkpoint's due date has passed. */
    fun isOverdue(nowMillis: Long): Boolean =
        // dueAtMillis is a Long? from :core:data (another module) so it can't
        // smart-cast after a null check — resolve it through ?.let.
        checkpoints.any { cp -> !cp.completed && cp.dueAtMillis?.let { it < nowMillis } == true }
}

/** A user-authored checkpoint, not yet persisted — the process-builder's
 *  draft shape before [LogbookRepository.addProcess] assigns real ids. */
data class LogbookCheckpointDraft(
    val name: String,
    val dueAtMillis: Long? = null,
    val reminderAtMillis: Long? = null,
)
