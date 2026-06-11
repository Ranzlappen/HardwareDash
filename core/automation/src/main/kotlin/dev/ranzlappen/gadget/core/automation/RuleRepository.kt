package dev.ranzlappen.gadget.core.automation

import dev.ranzlappen.gadget.core.automation.model.Rule
import kotlinx.coroutines.flow.Flow

/**
 * Persistence contract for automation rules (`automation.db`'s `rules`
 * table). The Room implementation lives in `:core:data`
 * (`RoomRuleRepository`), bound via Hilt — consumers (the engine runtime,
 * `:feature:automation-ui`) inject this interface and never touch Room,
 * per the repo's "Room → :core:data" convention.
 *
 * [save] **normalizes** the rule via `Rule.normalized()` before writing —
 * a `MetricThreshold.clearValue` on the wrong side of `value` for the
 * rule's (op, edge) silently degenerates hysteresis to ~none, so the
 * repository nulls it out rather than persisting a footgun (see
 * `RuleNormalization`).
 */
interface RuleRepository {

    /** All rules, hot — drives the rules list and the engine's re-arm. */
    fun observeRules(): Flow<List<Rule>>

    suspend fun rule(id: String): Rule?

    /** Upsert; stamps `updated_at`, preserves `created_at` + `last_fired_at`. */
    suspend fun save(rule: Rule)

    suspend fun delete(id: String)

    suspend fun setEnabled(id: String, enabled: Boolean)

    /**
     * Record a firing at [firedAtMs] (epoch ms). Persisted as
     * `last_fired_at` so per-rule cooldowns survive process death and
     * reboot (ADR-0002 Decision 8). The runtime derives the evaluator's
     * `sinceLastFiredMillis` from [lastFiredAt].
     */
    suspend fun markFired(id: String, firedAtMs: Long)

    suspend fun lastFiredAt(id: String): Long?
}
