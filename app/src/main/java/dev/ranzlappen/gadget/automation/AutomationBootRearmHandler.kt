package dev.ranzlappen.gadget.automation

import android.content.Context
import dev.ranzlappen.gadget.core.automation.RuleRepository
import dev.ranzlappen.gadget.core.automation.engine.AutomationServiceResidency
import dev.ranzlappen.gadget.core.automation.service.AutomationController
import dev.ranzlappen.gadget.core.automation.service.AutomationScheduler
import dev.ranzlappen.gadget.core.automation.service.RuleFireExecutor
import dev.ranzlappen.gadget.core.automation.model.SystemEventKind
import dev.ranzlappen.gadget.core.automation.model.Trigger
import dev.ranzlappen.gadget.core.widgetkit.boot.BootRearmHandler
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The automation engine's boot re-arm (ADR-0002 Decision 4: reuse the
 * `:core:widgetkit` `BootCompletedReceiver` + [BootRearmHandler]
 * multibinding — never a second boot receiver). Lives in `:app` because
 * `:core:automation` is deliberately Compose-free and must not pull
 * `:core:widgetkit` (which carries Compose UI); `:app` is the assembly
 * point that sees both.
 *
 * On boot, in cheap-first order:
 *  1. Fire any enabled [SystemEventKind.BootCompleted] rules through the
 *     shared [RuleFireExecutor] pipeline.
 *  2. Re-arm every enabled Schedule rule's alarm ([AutomationScheduler]) —
 *     alarms don't survive reboot.
 *  3. Start [dev.ranzlappen.gadget.core.automation.service.AutomationService]
 *     iff residency requires it (≥1 enabled metric-stream rule).
 *
 * Short-circuits on an empty rule set so a no-rules install adds nothing to
 * first-unlock latency (the kit's stated cost rule).
 */
@Singleton
class AutomationBootRearmHandler @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val scheduler: AutomationScheduler,
    private val controller: AutomationController,
    private val fireExecutor: RuleFireExecutor,
) : BootRearmHandler {

    override suspend fun onBootCompleted(context: Context) {
        val rules = ruleRepository.observeRules().first()
        if (rules.isEmpty()) return

        rules
            .filter { it.enabled && it.trigger == Trigger.SystemEvent(SystemEventKind.BootCompleted) }
            .forEach { fireExecutor.fire(it) }

        rules.forEach(scheduler::scheduleNext)

        if (AutomationServiceResidency.isServiceRequired(rules)) {
            controller.ensureStarted()
        }
    }

    companion object {
        /** Multibinding key — the engine's stable feature id. */
        const val FEATURE_ID: String = "automation"
    }
}
