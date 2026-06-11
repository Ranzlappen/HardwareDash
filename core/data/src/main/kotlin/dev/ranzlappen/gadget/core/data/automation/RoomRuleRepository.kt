package dev.ranzlappen.gadget.core.data.automation

import dev.ranzlappen.gadget.core.automation.RuleRepository
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.normalized
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [RuleRepository] over `automation.db` — the only Room surface
 * automation consumers touch, per the "Room → :core:data" convention.
 *
 * [save] runs the rule through [normalized] first (engine-core review P2):
 * a `MetricThreshold.clearValue` on the wrong side of `value` for the
 * rule's (op, edge) is nulled rather than persisted, protecting every write
 * path (builder UI, restored backups, future APIs) from the degenerate-
 * hysteresis footgun. It also stamps `updated_at` and preserves
 * `created_at` / `last_fired_at` across upserts.
 */
@Singleton
class RoomRuleRepository @Inject constructor(
    private val dao: RuleDao,
) : RuleRepository {

    override fun observeRules(): Flow<List<Rule>> =
        dao.observeAll().map { entities -> entities.map(RuleMapper::toModel) }

    override suspend fun rule(id: String): Rule? =
        dao.getById(id)?.let(RuleMapper::toModel)

    override suspend fun save(rule: Rule) {
        val normalized = rule.normalized()
        val now = System.currentTimeMillis()
        val existing = dao.getById(normalized.id)
        dao.upsert(
            RuleMapper.toEntity(
                rule = normalized,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                lastFiredAt = existing?.lastFiredAt,
            ),
        )
    }

    override suspend fun delete(id: String) = dao.deleteById(id)

    override suspend fun setEnabled(id: String, enabled: Boolean) =
        dao.setEnabled(id, enabled, updatedAt = System.currentTimeMillis())

    override suspend fun markFired(id: String, firedAtMs: Long) =
        dao.markFired(id, firedAtMs)

    override suspend fun lastFiredAt(id: String): Long? = dao.lastFiredAt(id)
}
