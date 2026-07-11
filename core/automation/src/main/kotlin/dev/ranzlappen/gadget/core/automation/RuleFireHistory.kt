package dev.ranzlappen.gadget.core.automation

import kotlinx.coroutines.flow.Flow

/**
 * One audit-trail entry for a rule evaluation (W7 firing history). Written
 * by `RuleFireExecutor` on every fire — automated, manual, **and** dry-run —
 * so the user can see what the engine actually did and why. [dispatched] is
 * the count of actions that reached hardware (0 for a no-op or a dry-run);
 * [outcome] classifies the evaluation.
 */
data class RuleFireRecord(
    val id: Long = 0L,
    val ruleId: String,
    val ruleName: String,
    val firedAtMs: Long,
    val outcome: RuleFireOutcome,
    val dispatched: Int,
    val throttled: Boolean = false,
    /** `true` for a test-fire that evaluated but deliberately dispatched nothing. */
    val dryRun: Boolean = false,
    val detail: String? = null,
)

/** How a single rule evaluation resolved. */
enum class RuleFireOutcome {
    /** Conditions held and at least one action dispatched. */
    Fired,

    /** Trigger matched but the evaluator returned no actions
     *  (disabled / cooldown / conditions failed / root-filtered). */
    Skipped,

    /** The budget dropped every action (storm throttle). */
    Throttled,
}

/**
 * Persistence + read contract for the rule firing history. The Room
 * implementation lives in `:core:data` (`RoomRuleFireHistoryRepository`),
 * keeping the "Room → :core:data" convention; the engine and
 * `:feature:automation-ui` inject this interface.
 */
interface RuleFireHistoryRepository {

    /** The most-recent firings first, hot — drives the history UI. */
    fun observeRecent(limit: Int = DEFAULT_HISTORY_LIMIT): Flow<List<RuleFireRecord>>

    /** Append one record; the store trims to [DEFAULT_HISTORY_LIMIT] rows. */
    suspend fun record(record: RuleFireRecord)

    /** Clear the whole audit trail. */
    suspend fun clear()

    companion object {
        const val DEFAULT_HISTORY_LIMIT: Int = 100
    }
}
