package dev.ranzlappen.gadget.core.automation.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.automation.ModuleActionRegistry
import dev.ranzlappen.gadget.core.automation.RuleFireHistoryRepository
import dev.ranzlappen.gadget.core.automation.RuleFireOutcome
import dev.ranzlappen.gadget.core.automation.RuleFireRecord
import dev.ranzlappen.gadget.core.automation.RuleRepository
import dev.ranzlappen.gadget.core.automation.engine.AutomationBudget
import dev.ranzlappen.gadget.core.automation.engine.RuleEvaluator
import dev.ranzlappen.gadget.core.automation.engine.referencedMetricKeys
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.Trigger
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.core.notifications.ChannelSpec
import dev.ranzlappen.gadget.core.notifications.NotificationChannelRegistry
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single fire→evaluate→budget→dispatch pipeline behind **every** trigger
 * path — the resident [AutomationService] (metric streams), the one-shot
 * paths ([AutomationAlarmReceiver] schedules, [AutomationSystemEventReceiver]
 * broadcasts), and the future manual "run now". One executor means **one
 * global [AutomationBudget]** across all paths, which is what ADR-0002
 * Decision 8 budgets: total dispatch pressure, not per-path.
 *
 * **Budget confinement:** [budget] is mutable, non-thread-safe state. Every
 * [fire] hops onto [dispatcher] — `Dispatchers.Default.limitedParallelism(1)`,
 * a single-lane queue — so all budget access (and the whole evaluate→dispatch
 * critical section) is serialized regardless of which receiver/service thread
 * called in. Never touch [budget] outside [dispatcher].
 *
 * Pipeline per [fire]: readings snapshot (the pre-sampled trigger value when
 * the caller has one + each condition metric) → [RuleEvaluator] (enabled /
 * cooldown with the Manual bypass / trigger match / conditions / root
 * filter) → [AutomationBudget.admit] → [ModuleActionRegistry.dispatch] each
 * admitted action → [RuleRepository.markFired]. A budget breach drops the
 * overflow and posts a single "Automation throttled" notification.
 */
@Singleton
class RuleFireExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ruleRepository: RuleRepository,
    private val metricSources: Map<String, @JvmSuppressWildcards MetricSource>,
    private val actionRegistry: ModuleActionRegistry,
    private val rootRegistry: RootCapabilityRegistry,
    private val evaluator: RuleEvaluator,
    private val channelRegistry: NotificationChannelRegistry,
    private val fireHistory: RuleFireHistoryRepository,
) {
    // Single-lane execution: serializes the budget + dispatch critical
    // section across the service and every receiver (see class KDoc).
    // limitedParallelism is the supported way to carve a serial lane out of
    // Default; still marked experimental in coroutines 1.7.x.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = Dispatchers.Default.limitedParallelism(1)

    private val budget = AutomationBudget()

    /**
     * Run the pipeline for [rule], whose own trigger just fired.
     * [preSampledTriggerValue] carries the metric sample that caused a
     * [Trigger.MetricThreshold] fire so it isn't re-sampled; one-shot paths
     * pass `null`.
     *
     * @return the number of actions actually dispatched — `0` when the
     *   evaluator returned empty (disabled / cooldown / conditions failed /
     *   root-filtered) or the budget dropped everything. Automated callers
     *   ignore it; the manual "run now" surface uses it for honest feedback.
     */
    suspend fun fire(rule: Rule, preSampledTriggerValue: Float? = null): Int =
        withContext(dispatcher) {
            val readings = gatherReadings(rule, preSampledTriggerValue)
            val now = System.currentTimeMillis()
            val sinceLastFired = ruleRepository.lastFiredAt(rule.id)?.let { now - it }
            val actions = evaluator.evaluate(
                rule = rule,
                firedTrigger = rule.trigger,
                readings = readings,
                now = LocalTime.now(),
                rootAvailable = rootRegistry.hasRootAccess(),
                sinceLastFiredMillis = sinceLastFired,
            )
            if (actions.isEmpty()) {
                fireHistory.record(record(rule, now, RuleFireOutcome.Skipped, dispatched = 0))
                return@withContext 0
            }

            val admission = budget.admit(now, actions.size)
            val dispatched = actions.take(admission.allowed)
            dispatched.forEach { actionRegistry.dispatch(it.featureId, it.actionKey, it.params) }
            if (dispatched.isNotEmpty()) ruleRepository.markFired(rule.id, now)
            if (admission.throttled) postThrottleNotification()
            val outcome = when {
                dispatched.isEmpty() && admission.throttled -> RuleFireOutcome.Throttled
                dispatched.isEmpty() -> RuleFireOutcome.Skipped
                else -> RuleFireOutcome.Fired
            }
            fireHistory.record(
                record(rule, now, outcome, dispatched.size, throttled = admission.throttled),
            )
            dispatched.size
        }

    /**
     * Evaluate [rule] exactly as [fire] would, but **dispatch nothing** and
     * **do not** update the cooldown clock — a "test fire" that reports what
     * the rule *would* do (how many actions would run, or why it's a no-op)
     * and records a dry-run entry in the firing history. The Manual "run
     * now" bypasses cooldown, so the dry-run models a Manual evaluation to
     * report the true action set regardless of the rule's automated cooldown.
     */
    suspend fun dryRun(rule: Rule): DryRunResult =
        withContext(dispatcher) {
            val readings = gatherReadings(rule, preSampledTriggerValue = null)
            val actions = evaluator.evaluate(
                rule = rule,
                firedTrigger = rule.trigger,
                readings = readings,
                now = LocalTime.now(),
                rootAvailable = rootRegistry.hasRootAccess(),
                // Manual consent bypasses cooldown — report the real action set.
                sinceLastFiredMillis = null,
            )
            val now = System.currentTimeMillis()
            val detail = if (actions.isEmpty()) {
                "Conditions not met — nothing would run"
            } else {
                "${actions.size} action(s) would run"
            }
            fireHistory.record(
                record(
                    rule = rule,
                    firedAtMs = now,
                    outcome = if (actions.isEmpty()) RuleFireOutcome.Skipped else RuleFireOutcome.Fired,
                    dispatched = 0,
                    dryRun = true,
                    detail = detail,
                ),
            )
            DryRunResult(wouldDispatch = actions.size, actions = actions)
        }

    private fun record(
        rule: Rule,
        firedAtMs: Long,
        outcome: RuleFireOutcome,
        dispatched: Int,
        throttled: Boolean = false,
        dryRun: Boolean = false,
        detail: String? = null,
    ): RuleFireRecord = RuleFireRecord(
        ruleId = rule.id,
        ruleName = rule.name,
        firedAtMs = firedAtMs,
        outcome = outcome,
        dispatched = dispatched,
        throttled = throttled,
        dryRun = dryRun,
        detail = detail,
    )

    /** Outcome of [dryRun]: what the rule would do, without doing it. */
    data class DryRunResult(
        val wouldDispatch: Int,
        val actions: List<dev.ranzlappen.gadget.core.automation.model.RuleAction>,
    )

    /** The trigger metric (pre-sampled when available) + each condition metric. */
    private suspend fun gatherReadings(rule: Rule, triggerValue: Float?): Map<String, Float> {
        val readings = HashMap<String, Float>()
        val metricTrigger = rule.trigger as? Trigger.MetricThreshold
        if (metricTrigger != null) {
            val value = triggerValue ?: metricSources[metricTrigger.metricKey]
                ?.let { sampleWithTimeout(it) }
            value?.let { readings[metricTrigger.metricKey] = it }
        }
        // Every metric referenced anywhere in the condition tree, including
        // keys nested inside Condition.Group nodes (referencedMetricKeys
        // recurses), so a grouped condition still has its reading sampled.
        val conditionKeys = rule.conditions.flatMap { it.referencedMetricKeys() }.distinct()
        for (metricKey in conditionKeys) {
            if (metricKey !in readings) {
                val source = metricSources[metricKey] ?: continue
                sampleWithTimeout(source)?.let { readings[metricKey] = it }
            }
        }
        return readings
    }

    private suspend fun sampleWithTimeout(source: MetricSource): Float? = try {
        withTimeout(SAMPLE_TIMEOUT_MS) { source.sample() }
    } catch (_: TimeoutCancellationException) {
        null
    }

    private fun postThrottleNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        channelRegistry.ensure(CHANNEL)
        val notification = NotificationCompat.Builder(context, CHANNEL.id)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            // TODO localize (Strings.kt) — throttle copy.
            .setContentTitle("Automation throttled")
            .setContentText("Too many automation actions fired at once — some were dropped.")
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(THROTTLE_NOTIFICATION_ID, notification)
    }

    internal companion object {
        /** Shared with [AutomationService]'s ongoing FGS notification. */
        val CHANNEL = ChannelSpec(
            id = "automation",
            displayName = "Automation",
            description = "Automation rule engine",
            importance = ChannelSpec.Importance.Low,
            silent = true,
        )
        const val THROTTLE_NOTIFICATION_ID = 0x4155_0002
        const val SAMPLE_TIMEOUT_MS = 2_000L
    }
}
