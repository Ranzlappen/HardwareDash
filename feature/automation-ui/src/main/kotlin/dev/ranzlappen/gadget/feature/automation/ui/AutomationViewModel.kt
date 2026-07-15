package dev.ranzlappen.gadget.feature.automation.ui

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.core.automation.ModuleActionRegistry
import dev.ranzlappen.gadget.core.automation.RuleFireHistoryRepository
import dev.ranzlappen.gadget.core.automation.RuleFireRecord
import dev.ranzlappen.gadget.core.automation.RuleRepository
import dev.ranzlappen.gadget.core.automation.engine.AutomationServiceResidency
import dev.ranzlappen.gadget.core.automation.model.AutomationTransfer
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.RuleTemplate
import dev.ranzlappen.gadget.core.automation.model.RuleTemplates
import dev.ranzlappen.gadget.core.automation.model.Trigger
import dev.ranzlappen.gadget.core.automation.service.AutomationController
import dev.ranzlappen.gadget.core.automation.service.AutomationScheduler
import dev.ranzlappen.gadget.core.automation.service.GeofenceRegistrar
import dev.ranzlappen.gadget.core.automation.service.RuleFireExecutor
import dev.ranzlappen.gadget.core.hardware.HardwareRegistry
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One pickable action for the rule builder: a feature's [ModuleAction]
 * with its registry key, pre-filtered for the current flavor (gating
 * layer 1 — `requiresRoot` actions are absent on a standard build, the
 * same filter the widget function picker applies).
 */
@Immutable
data class ActionChoice(
    val featureId: String,
    val action: ModuleAction,
)

/** One-shot UI events surfaced as snackbars by the route. */
sealed interface AutomationUiEvent {
    /** A manual "run now" finished; [dispatched] actions were sent. */
    data class RanNow(val ruleName: String, val dispatched: Int) : AutomationUiEvent

    /** A template was added as a new (disabled-until-saved) rule. */
    data class TemplateAdded(val templateName: String) : AutomationUiEvent

    /** A dry-run "test fire" finished; [wouldDispatch] actions would have run. */
    data class TestFired(val ruleName: String, val wouldDispatch: Int) : AutomationUiEvent

    /** Rules were exported to the clipboard. */
    data class Exported(val count: Int) : AutomationUiEvent

    /** An import finished. [imported] is null on a parse failure ([reason] set). */
    data class Imported(val imported: Int?, val reason: String? = null) : AutomationUiEvent
}

/**
 * ViewModel backing the Automation screen — the consumer of both
 * enumeration seams: [HardwareRegistry] (read side — trigger/condition
 * signal pickers) and [ModuleActionRegistry] (write side — action picker),
 * neither of which imports a feature module.
 *
 * Owns the **rule-save → engine** wiring the runtime batches left open:
 * after every save / enable-toggle the scheduler (re)arms or cancels the
 * rule's alarm ([AutomationScheduler.scheduleNext] handles the
 * not-Schedule / disabled cases) and [AutomationController.ensureStarted]
 * spins the resident service up iff the rule needs a live subscription —
 * metric-stream or Connectivity, the shared
 * `AutomationServiceResidency.requiresResidency` predicate (the service
 * itself re-derives residency from the repository and self-stops when no
 * longer required, so disabling needs no stop call).
 */
@HiltViewModel
class AutomationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ruleRepository: RuleRepository,
    private val scheduler: AutomationScheduler,
    private val geofenceRegistrar: GeofenceRegistrar,
    private val controller: AutomationController,
    private val fireExecutor: RuleFireExecutor,
    private val fireHistory: RuleFireHistoryRepository,
    hardwareRegistry: HardwareRegistry,
    actionRegistry: ModuleActionRegistry,
    rootRegistry: RootCapabilityRegistry,
) : ViewModel() {

    /** All persisted rules, hot while the screen is subscribed. */
    val rules: StateFlow<List<Rule>> = ruleRepository.observeRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_FLOW_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    /** The built-in rule templates surfaced in the "Templates" picker. */
    val templates: List<RuleTemplate> = RuleTemplates.all

    /** The rule firing-history audit trail, most-recent first. */
    val fireHistoryRecords: StateFlow<List<RuleFireRecord>> = fireHistory.observeRecent()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_FLOW_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    /**
     * Every registered readable signal, for the trigger/condition pickers.
     * Static per process — features register sources at graph build.
     */
    val signals: List<MetricDescriptor> = hardwareRegistry.signals()

    /**
     * Every dispatchable action, flavor-filtered (builder filter — root
     * gating layer 1; the evaluator re-filters at fire time as layer 2).
     */
    val actionChoices: List<ActionChoice> = actionRegistry.actions()
        .filter { (_, action) -> !action.requiresRoot || rootRegistry.hasRootAccess() }
        .map { (featureId, action) -> ActionChoice(featureId, action) }

    private val _exactAlarmAllowed = MutableStateFlow(readExactAlarmAllowed())

    /**
     * Live `canScheduleExactAlarms()` state — drives the "needs Alarms &
     * reminders" badge (the design doc's degradation contract, third row).
     * Refreshed on ON_RESUME by the route so it updates after the
     * settings round-trip.
     */
    val exactAlarmAllowed: StateFlow<Boolean> = _exactAlarmAllowed.asStateFlow()

    private val _events = Channel<AutomationUiEvent>(Channel.BUFFERED)
    val events: Flow<AutomationUiEvent> = _events.receiveAsFlow()

    fun refreshExactAlarmStatus() {
        _exactAlarmAllowed.value = readExactAlarmAllowed()
    }

    /** Upsert from the rule builder, then re-wire the engine. */
    fun saveRule(rule: Rule) {
        viewModelScope.launch {
            ruleRepository.save(rule)
            // Re-read so the engine arms the *persisted* (normalized) rule.
            afterRuleChange(ruleRepository.rule(rule.id) ?: rule)
        }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            ruleRepository.setEnabled(id, enabled)
            ruleRepository.rule(id)?.let { afterRuleChange(it) }
        }
    }

    fun deleteRule(id: String) {
        viewModelScope.launch {
            ruleRepository.delete(id)
            scheduler.cancel(id)
            geofenceRegistrar.unregister(id)
        }
    }

    /**
     * The manual "run now" surface: hand the rule to the shared
     * fire→evaluate→budget→dispatch pipeline (one global budget across
     * every trigger path). The evaluator receives the rule's own trigger,
     * so this works for any rule kind; cooldown still gates non-Manual
     * rules and conditions are re-checked — the dispatched count comes
     * back for honest feedback.
     */
    fun runNow(rule: Rule) {
        viewModelScope.launch {
            val dispatched = fireExecutor.fire(rule)
            _events.send(AutomationUiEvent.RanNow(rule.name, dispatched))
        }
    }

    /**
     * Dry-run "test fire" — evaluate the rule and report what it *would* do
     * without dispatching anything or touching the cooldown clock. Records a
     * dry-run entry in the firing history.
     */
    fun testFire(rule: Rule) {
        viewModelScope.launch {
            val result = fireExecutor.dryRun(rule)
            _events.send(AutomationUiEvent.TestFired(rule.name, result.wouldDispatch))
        }
    }

    /** Materialize a [RuleTemplate] as a fresh persisted rule, then re-arm. */
    fun applyTemplate(template: RuleTemplate) {
        viewModelScope.launch {
            val rule = template.create(java.util.UUID.randomUUID().toString())
            ruleRepository.save(rule)
            afterRuleChange(ruleRepository.rule(rule.id) ?: rule)
            _events.send(AutomationUiEvent.TemplateAdded(template.name))
        }
    }

    /** Serialize all current rules to a JSON document on the clipboard. */
    fun exportRules() {
        viewModelScope.launch {
            val current = rules.value
            if (current.isEmpty()) {
                _events.send(AutomationUiEvent.Exported(0))
                return@launch
            }
            val json = AutomationTransfer.export(current)
            copyToClipboard(json)
            _events.send(AutomationUiEvent.Exported(current.size))
        }
    }

    /**
     * Parse a pasted JSON document and persist each rule with a **fresh id**
     * (an import never clobbers an existing rule), then re-arm the engine.
     */
    fun importRules(json: String) {
        viewModelScope.launch {
            when (val result = AutomationTransfer.import(json)) {
                is AutomationTransfer.ImportResult.Success -> {
                    result.rules.forEach { imported ->
                        val fresh = imported.copy(id = java.util.UUID.randomUUID().toString())
                        ruleRepository.save(fresh)
                        afterRuleChange(ruleRepository.rule(fresh.id) ?: fresh)
                    }
                    _events.send(AutomationUiEvent.Imported(result.rules.size))
                }
                is AutomationTransfer.ImportResult.Failure ->
                    _events.send(AutomationUiEvent.Imported(imported = null, reason = result.reason))
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { fireHistory.clear() }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
        clipboard?.setPrimaryClip(
            android.content.ClipData.newPlainText("Gadget automation rules", text),
        )
    }

    private suspend fun afterRuleChange(rule: Rule) {
        if (rule.trigger is Trigger.Schedule) {
            // Handles disabled → cancel internally; same-id PendingIntent
            // slot means re-arming replaces rather than stacks.
            scheduler.scheduleNext(rule)
        } else {
            // The trigger may have *changed away* from Schedule — clear any
            // stale alarm armed for the old shape.
            scheduler.cancel(rule.id)
        }
        // register() no-ops / unregisters for non-geofence or disabled rules,
        // so an unconditional call keeps the OS fence in sync with the edit.
        geofenceRegistrar.register(rule)
        // Same predicate the service + boot re-arm use: metric-stream and
        // connectivity rules both need the resident service.
        if (AutomationServiceResidency.requiresResidency(rule)) {
            controller.ensureStarted()
        }
        refreshExactAlarmStatus()
    }

    private fun readExactAlarmAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(AlarmManager::class.java) ?: return false
        return manager.canScheduleExactAlarms()
    }

    private companion object {
        const val STATE_FLOW_TIMEOUT_MS = 5_000L
    }
}
