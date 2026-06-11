package dev.ranzlappen.gadget.core.automation.service

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.core.automation.ModuleActionRegistry
import dev.ranzlappen.gadget.core.automation.RuleRepository
import dev.ranzlappen.gadget.core.automation.engine.AutomationBudget
import dev.ranzlappen.gadget.core.automation.engine.AutomationServiceResidency
import dev.ranzlappen.gadget.core.automation.engine.MetricThresholdGate
import dev.ranzlappen.gadget.core.automation.engine.RuleEvaluator
import dev.ranzlappen.gadget.core.automation.model.Condition
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.Trigger
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.core.notifications.ChannelSpec
import dev.ranzlappen.gadget.core.notifications.NotificationChannelRegistry
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalTime
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * The automation engine's foreground service (ADR-0002 Decision 4 /
 * `docs/automation-engine.md` § Runtime host). Mirrors the
 * [dev.ranzlappen.gadget.core.monitoring] `MonitorService` shape: one shared
 * `specialUse` FGS for the whole engine, self-stopping when no metric-stream
 * rule needs it.
 *
 * **Residency:** resident only while ≥1 enabled rule has a
 * [Trigger.MetricThreshold] trigger (the one kind needing a continuous
 * `MetricSource` subscription — [AutomationServiceResidency]). Schedule /
 * system-event / manual rules evaluate one-shot via alarms / broadcasts /
 * "run now" and never start this service. It re-derives residency on every
 * rule-set change and [stopSelf]s when it's no longer required.
 *
 * **Per-fire pipeline:** a metric sample → per-rule [MetricThresholdGate]
 * edge/hysteresis → on fire, gather a readings snapshot → [RuleEvaluator]
 * (cooldown / conditions / root filter) → [AutomationBudget] storm cap →
 * dispatch each admitted action through [ModuleActionRegistry] →
 * [RuleRepository.markFired].
 *
 * **Threading / budget confinement:** the whole evaluation pipeline runs on
 * this service's single [scope] (`Dispatchers.Default`). [budget] is mutable
 * and **not thread-safe** — every `admit` happens on [scope], so it needs no
 * synchronisation. Never touch [budget] off [scope].
 */
@AndroidEntryPoint
class AutomationService : Service() {

    @Inject lateinit var ruleRepository: RuleRepository
    @Inject lateinit var metricSources: Map<String, @JvmSuppressWildcards MetricSource>
    @Inject lateinit var actionRegistry: ModuleActionRegistry
    @Inject lateinit var rootRegistry: RootCapabilityRegistry
    @Inject lateinit var evaluator: RuleEvaluator
    @Inject lateinit var channelRegistry: NotificationChannelRegistry

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Storm budget — confined to [scope] (see class KDoc). */
    private val budget = AutomationBudget()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForeground(NOTIFICATION_ID, ongoingNotification())
        scope.launch {
            ruleRepository.observeRules().collectLatest { rules ->
                val streaming = AutomationServiceResidency.streamingRules(rules)
                if (streaming.isEmpty()) {
                    stopSelf()
                    return@collectLatest
                }
                // Suspends here (subscriptions stay live) until the rule set
                // changes, at which point collectLatest cancels + re-invokes.
                subscribeStreamingRules(streaming)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        scope.cancel()
        super.onDestroy()
    }

    /** One collector per watched metric; each rule carries its own gate. */
    private suspend fun subscribeStreamingRules(rules: List<Rule>) = coroutineScope {
        rules.groupBy { (it.trigger as Trigger.MetricThreshold).metricKey }
            .forEach { (metricKey, metricRules) ->
                val source = metricSources[metricKey] ?: return@forEach
                launch { watchMetric(source, metricRules) }
            }
    }

    private suspend fun watchMetric(source: MetricSource, rules: List<Rule>) {
        // Per-rule arm/fire state, keyed by rule id, across samples.
        val gates = HashMap<String, MetricThresholdGate.State>()
        val stream = source.stream()
        if (stream != null) {
            stream.collect { value -> onSample(rules, gates, value) }
        } else {
            while (coroutineContext.isActive) {
                sampleWithTimeout(source)?.let { onSample(rules, gates, it) }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun onSample(
        rules: List<Rule>,
        gates: HashMap<String, MetricThresholdGate.State>,
        value: Float,
    ) {
        for (rule in rules) {
            val trigger = rule.trigger as? Trigger.MetricThreshold ?: continue
            val existing = gates[rule.id]
            if (existing == null) {
                // First sample sets the arm baseline only — no fire-on-subscribe.
                gates[rule.id] = MetricThresholdGate.initialState(trigger, value)
            } else {
                val step = MetricThresholdGate.step(trigger, existing, value)
                gates[rule.id] = step.state
                if (step.fire) fireRule(rule, value)
            }
        }
    }

    private suspend fun fireRule(rule: Rule, triggerValue: Float) {
        val readings = gatherReadings(rule, triggerValue)
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
        if (actions.isEmpty()) return

        val admission = budget.admit(now, actions.size)
        val dispatched = actions.take(admission.allowed)
        dispatched.forEach { actionRegistry.dispatch(it.featureId, it.actionKey, it.params) }
        if (dispatched.isNotEmpty()) ruleRepository.markFired(rule.id, now)
        if (admission.throttled) postThrottleNotification()
    }

    /** The trigger metric (already sampled) + each condition metric. */
    private suspend fun gatherReadings(rule: Rule, triggerValue: Float): Map<String, Float> {
        val readings = HashMap<String, Float>()
        (rule.trigger as? Trigger.MetricThreshold)?.let { readings[it.metricKey] = triggerValue }
        for (condition in rule.conditions) {
            if (condition is Condition.MetricCompare && condition.metricKey !in readings) {
                val source = metricSources[condition.metricKey] ?: continue
                sampleWithTimeout(source)?.let { readings[condition.metricKey] = it }
            }
        }
        return readings
    }

    private suspend fun sampleWithTimeout(source: MetricSource): Float? = try {
        withTimeout(SAMPLE_TIMEOUT_MS) { source.sample() }
    } catch (_: TimeoutCancellationException) {
        null
    }

    // -- notifications ----------------------------------------------------

    private fun ensureChannel() {
        channelRegistry.ensure(
            ChannelSpec(
                id = CHANNEL_ID,
                // TODO localize (Strings.kt) — FGS channel + notification copy.
                displayName = "Automation",
                description = "Automation rule engine",
                importance = ChannelSpec.Importance.Low,
                silent = true,
            ),
        )
    }

    private fun ongoingNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("Automation running")
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    private fun postThrottleNotification() {
        if (!canPostNotifications()) return
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Automation throttled")
            .setContentText("Too many automation actions fired at once — some were dropped.")
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(THROTTLE_NOTIFICATION_ID, notification)
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val CHANNEL_ID = "automation"
        const val NOTIFICATION_ID = 0x4155_0001 // "AU".. ongoing FGS notification
        const val THROTTLE_NOTIFICATION_ID = 0x4155_0002
        const val POLL_INTERVAL_MS = 1_000L
        const val SAMPLE_TIMEOUT_MS = 2_000L // a slow sample can't stall the dispatcher
    }
}
