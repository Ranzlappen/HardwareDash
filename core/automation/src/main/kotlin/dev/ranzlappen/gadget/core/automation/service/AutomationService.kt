package dev.ranzlappen.gadget.core.automation.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.core.automation.RuleRepository
import dev.ranzlappen.gadget.core.automation.engine.AutomationServiceResidency
import dev.ranzlappen.gadget.core.automation.engine.MetricThresholdGate
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.Trigger
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.core.notifications.NotificationChannelRegistry
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
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * The automation engine's foreground service (ADR-0002 Decision 4 /
 * `docs/automation-engine.md` § Runtime host). Mirrors the
 * [dev.ranzlappen.gadget.core.monitoring] `MonitorService` shape: one shared
 * `specialUse` FGS for the whole engine, self-stopping when not needed.
 *
 * **Residency:** resident only while ≥1 enabled rule has a
 * [Trigger.MetricThreshold] trigger (the one kind needing a continuous
 * `MetricSource` subscription — [AutomationServiceResidency]). Schedule /
 * system-event / manual rules evaluate one-shot via [AutomationAlarmReceiver]
 * / [AutomationSystemEventReceiver] / "run now" and never start this
 * service. It re-derives residency on every rule-set change and [stopSelf]s
 * when no longer required.
 *
 * **This service only detects edges**: a metric sample steps each rule's
 * [MetricThresholdGate] (edge + hysteresis; no fire-on-subscribe); a fire
 * hands off to [RuleFireExecutor], the single budget-confined pipeline every
 * trigger path shares.
 */
@AndroidEntryPoint
class AutomationService : Service() {

    @Inject lateinit var ruleRepository: RuleRepository
    @Inject lateinit var metricSources: Map<String, @JvmSuppressWildcards MetricSource>
    @Inject lateinit var fireExecutor: RuleFireExecutor
    @Inject lateinit var channelRegistry: NotificationChannelRegistry

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        channelRegistry.ensure(RuleFireExecutor.CHANNEL)
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
                if (step.fire) fireExecutor.fire(rule, preSampledTriggerValue = value)
            }
        }
    }

    private suspend fun sampleWithTimeout(source: MetricSource): Float? = try {
        withTimeout(RuleFireExecutor.SAMPLE_TIMEOUT_MS) { source.sample() }
    } catch (_: TimeoutCancellationException) {
        null
    }

    private fun ongoingNotification(): Notification =
        NotificationCompat.Builder(this, RuleFireExecutor.CHANNEL.id)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            // TODO localize (Strings.kt) — FGS notification copy.
            .setContentTitle("Automation running")
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    private companion object {
        const val NOTIFICATION_ID = 0x4155_0001 // "AU".. ongoing FGS notification
        const val POLL_INTERVAL_MS = 1_000L
    }
}
