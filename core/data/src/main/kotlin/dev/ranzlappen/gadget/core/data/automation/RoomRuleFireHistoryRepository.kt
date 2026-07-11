package dev.ranzlappen.gadget.core.data.automation

import dev.ranzlappen.gadget.core.automation.RuleFireHistoryRepository
import dev.ranzlappen.gadget.core.automation.RuleFireOutcome
import dev.ranzlappen.gadget.core.automation.RuleFireRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [RuleFireHistoryRepository] over `automation.db`'s
 * `rule_fire_history` table (W7). Each [record] insert trims the trail to
 * [RuleFireHistoryRepository.DEFAULT_HISTORY_LIMIT] rows so the audit log
 * stays bounded. The outcome enum is mapped by name (a lenient decode falls
 * back to [RuleFireOutcome.Skipped] for an unknown legacy value).
 */
@Singleton
class RoomRuleFireHistoryRepository @Inject constructor(
    private val dao: RuleFireDao,
) : RuleFireHistoryRepository {

    override fun observeRecent(limit: Int): Flow<List<RuleFireRecord>> =
        dao.observeRecent(limit).map { rows -> rows.map(::toModel) }

    override suspend fun record(record: RuleFireRecord) {
        dao.insert(toEntity(record))
        dao.trimTo(RuleFireHistoryRepository.DEFAULT_HISTORY_LIMIT)
    }

    override suspend fun clear() = dao.clear()

    private fun toEntity(record: RuleFireRecord): RuleFireEntity = RuleFireEntity(
        ruleId = record.ruleId,
        ruleName = record.ruleName,
        firedAt = record.firedAtMs,
        outcome = record.outcome.name,
        dispatched = record.dispatched,
        throttled = record.throttled,
        dryRun = record.dryRun,
        detail = record.detail,
    )

    private fun toModel(entity: RuleFireEntity): RuleFireRecord = RuleFireRecord(
        id = entity.id,
        ruleId = entity.ruleId,
        ruleName = entity.ruleName,
        firedAtMs = entity.firedAt,
        outcome = runCatching { RuleFireOutcome.valueOf(entity.outcome) }
            .getOrDefault(RuleFireOutcome.Skipped),
        dispatched = entity.dispatched,
        throttled = entity.throttled,
        dryRun = entity.dryRun,
        detail = entity.detail,
    )
}
