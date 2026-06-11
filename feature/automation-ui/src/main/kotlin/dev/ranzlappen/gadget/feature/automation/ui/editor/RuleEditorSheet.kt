// GadgetBottomSheet's signature carries M3's experimental SheetState, and
// LabeledChipRow's carries FlowRowScope — both opt-in requirements
// propagate to every call site in this file (annotating a declaration
// covers its body, not its consumers), so opt in file-wide.
@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package dev.ranzlappen.gadget.feature.automation.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.ranzlappen.gadget.core.automation.model.ComparisonOp
import dev.ranzlappen.gadget.core.automation.model.DayOfWeek
import dev.ranzlappen.gadget.core.automation.model.Edge
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.SystemEventKind
import dev.ranzlappen.gadget.core.automation.model.Trigger
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.currentMax
import dev.ranzlappen.gadget.core.ui.component.GadgetBottomSheet
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.feature.automation.ui.ActionChoice
import dev.ranzlappen.gadget.feature.automation.ui.MINUTES_PER_DAY
import dev.ranzlappen.gadget.feature.automation.ui.R
import dev.ranzlappen.gadget.feature.automation.ui.dayLabel
import dev.ranzlappen.gadget.feature.automation.ui.formatTimeOfDay
import dev.ranzlappen.gadget.feature.automation.ui.parseTimeOfDay
import dev.ranzlappen.gadget.feature.automation.ui.symbol
import kotlin.math.roundToInt

/**
 * The rule builder (`docs/automation-engine.md` batch 3.4): one bottom
 * sheet editing a whole [Rule] draft — name → trigger (kind picker +
 * per-kind params) → conditions → actions (picked from the registry,
 * params auto-generated from each action's `ActionParam` schema) →
 * cooldown. Mirrors the `WidgetCustomizationSheet` shape, the repo's
 * established "configure a thing in a sheet" pattern.
 *
 * Stateless toward persistence: the draft lives in local compose state
 * and leaves only through [onSave] (the caller persists + re-arms the
 * engine). Validation is structural — save enables once the rule has a
 * name and at least one action; value-level safety (wrong-side
 * hysteresis) is normalized by the repository on save.
 */
@Composable
internal fun RuleEditorSheet(
    initial: Rule,
    isNew: Boolean,
    signals: List<MetricDescriptor>,
    actionChoices: List<ActionChoice>,
    exactAlarmAllowed: Boolean,
    onRequestExactAlarm: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (Rule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    var draft by remember(initial) { mutableStateOf(initial) }

    GadgetBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        // A rule is a long form (trigger + conditions + actions + options):
        // open fully expanded instead of M3's half state, where everything
        // below the trigger section starts under the fold (also what the
        // instrumented badge test scrolls to).
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = stringResource(
            if (isNew) R.string.automation_editor_title_new else R.string.automation_editor_title_edit,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            GadgetTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = stringResource(R.string.automation_editor_name_label),
                modifier = Modifier.fillMaxWidth(),
            )

            // ─── When (trigger) ───────────────────────────────────────
            SheetSectionHeader(stringResource(R.string.automation_editor_section_trigger))
            TriggerSection(
                trigger = draft.trigger,
                signals = signals,
                exactAlarmAllowed = exactAlarmAllowed,
                onRequestExactAlarm = onRequestExactAlarm,
                onTriggerChange = { draft = draft.copy(trigger = it) },
            )

            // ─── Only if (conditions) ─────────────────────────────────
            SheetSectionHeader(stringResource(R.string.automation_editor_section_conditions))
            ConditionsSection(
                conditions = draft.conditions,
                conditionLogic = draft.conditionLogic,
                signals = signals,
                onConditionsChange = { draft = draft.copy(conditions = it) },
                onLogicChange = { draft = draft.copy(conditionLogic = it) },
            )

            // ─── Then (actions) ───────────────────────────────────────
            SheetSectionHeader(stringResource(R.string.automation_editor_section_actions))
            ActionsSection(
                actions = draft.actions,
                actionChoices = actionChoices,
                onActionsChange = { draft = draft.copy(actions = it) },
            )

            // ─── Options ──────────────────────────────────────────────
            SheetSectionHeader(stringResource(R.string.automation_editor_section_options))
            GadgetSlider(
                value = draft.cooldownSeconds.toFloat(),
                onValueChange = { draft = draft.copy(cooldownSeconds = it.roundToInt()) },
                valueRange = 0f..MAX_COOLDOWN_SECONDS,
                label = stringResource(R.string.automation_editor_cooldown),
                suffix = stringResource(R.string.automation_editor_cooldown_suffix),
                modifier = Modifier.fillMaxWidth(),
            )
            HintText(stringResource(R.string.automation_editor_cooldown_hint))

            // ─── Footer ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                GadgetTertiaryButton(
                    onClick = onDismiss,
                    text = stringResource(R.string.automation_cancel),
                    modifier = Modifier.weight(1f),
                )
                GadgetPrimaryButton(
                    onClick = { onSave(draft) },
                    text = stringResource(R.string.automation_editor_save),
                    enabled = draft.name.isNotBlank() && draft.actions.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ─── Trigger section ────────────────────────────────────────────────────

/** The four authorable trigger kinds; chips switch the draft between them. */
private enum class TriggerKind { Metric, Schedule, Event, Manual }

private fun Trigger.kind(): TriggerKind = when (this) {
    is Trigger.MetricThreshold -> TriggerKind.Metric
    is Trigger.Schedule -> TriggerKind.Schedule
    is Trigger.SystemEvent -> TriggerKind.Event
    is Trigger.Manual -> TriggerKind.Manual
}

@Composable
private fun TriggerKind.label(): String = stringResource(
    when (this) {
        TriggerKind.Metric -> R.string.automation_trigger_kind_metric
        TriggerKind.Schedule -> R.string.automation_trigger_kind_schedule
        TriggerKind.Event -> R.string.automation_trigger_kind_event
        TriggerKind.Manual -> R.string.automation_trigger_kind_manual
    },
)

/** Sensible per-kind defaults when the user switches trigger kind. */
private fun defaultTriggerFor(kind: TriggerKind, signals: List<MetricDescriptor>): Trigger {
    return when (kind) {
        TriggerKind.Metric -> {
            // The Metric chip is disabled when no signals exist; the Manual
            // fallback is unreachable belt-and-braces.
            val first = signals.firstOrNull() ?: return Trigger.Manual
            Trigger.MetricThreshold(
                metricKey = first.metricKey,
                op = ComparisonOp.Lt,
                value = (first.min + first.currentMax()) / 2f,
            )
        }
        TriggerKind.Schedule -> Trigger.Schedule(timeOfDayMinutes = DEFAULT_SCHEDULE_MINUTES)
        TriggerKind.Event -> Trigger.SystemEvent(SystemEventKind.PowerConnected)
        TriggerKind.Manual -> Trigger.Manual
    }
}

@Composable
private fun TriggerSection(
    trigger: Trigger,
    signals: List<MetricDescriptor>,
    exactAlarmAllowed: Boolean,
    onRequestExactAlarm: () -> Unit,
    onTriggerChange: (Trigger) -> Unit,
) {
    LabeledChipRow(label = stringResource(R.string.automation_editor_trigger_kind)) {
        TriggerKind.values().forEach { kind ->
            GadgetChip(
                selected = trigger.kind() == kind,
                onClick = {
                    if (trigger.kind() != kind) {
                        onTriggerChange(defaultTriggerFor(kind, signals))
                    }
                },
                label = kind.label(),
                enabled = kind != TriggerKind.Metric || signals.isNotEmpty(),
            )
        }
    }
    when (trigger) {
        is Trigger.MetricThreshold -> MetricTriggerEditor(trigger, signals, onTriggerChange)
        is Trigger.Schedule -> ScheduleTriggerEditor(
            trigger = trigger,
            exactAlarmAllowed = exactAlarmAllowed,
            onRequestExactAlarm = onRequestExactAlarm,
            onTriggerChange = onTriggerChange,
        )
        is Trigger.SystemEvent -> SystemEventTriggerEditor(trigger, onTriggerChange)
        is Trigger.Manual -> HintText(stringResource(R.string.automation_editor_manual_hint))
    }
}

@Composable
private fun MetricTriggerEditor(
    trigger: Trigger.MetricThreshold,
    signals: List<MetricDescriptor>,
    onTriggerChange: (Trigger) -> Unit,
) {
    val descriptor = signals.firstOrNull { it.metricKey == trigger.metricKey }
    val min = descriptor?.min ?: 0f
    val max = descriptor?.currentMax() ?: DEFAULT_METRIC_CEILING

    LabeledChipRow(label = stringResource(R.string.automation_editor_signal)) {
        signals.forEach { signal ->
            GadgetChip(
                selected = signal.metricKey == trigger.metricKey,
                onClick = {
                    if (signal.metricKey != trigger.metricKey) {
                        // Ranges differ wildly between signals — re-seed the
                        // threshold mid-range and drop the old hysteresis.
                        onTriggerChange(
                            trigger.copy(
                                metricKey = signal.metricKey,
                                value = (signal.min + signal.currentMax()) / 2f,
                                clearValue = null,
                            ),
                        )
                    }
                },
                label = signal.displayName,
            )
        }
    }

    LabeledChipRow(label = stringResource(R.string.automation_editor_operator)) {
        ComparisonOp.values().forEach { op ->
            GadgetChip(
                selected = trigger.op == op,
                onClick = { onTriggerChange(trigger.copy(op = op, clearValue = null)) },
                label = op.symbol,
            )
        }
    }

    GadgetSlider(
        value = trigger.value,
        onValueChange = { onTriggerChange(trigger.copy(value = it)) },
        valueRange = min..max,
        label = stringResource(R.string.automation_editor_threshold),
        suffix = descriptor?.unit?.takeIf { it.isNotEmpty() },
        valueFormatter = ::formatOneDecimal,
        modifier = Modifier.fillMaxWidth(),
    )

    LabeledChipRow(label = stringResource(R.string.automation_editor_edge)) {
        GadgetChip(
            selected = trigger.edge == Edge.Rising,
            onClick = { onTriggerChange(trigger.copy(edge = Edge.Rising)) },
            label = stringResource(R.string.automation_edge_rising),
        )
        GadgetChip(
            selected = trigger.edge == Edge.Falling,
            onClick = { onTriggerChange(trigger.copy(edge = Edge.Falling)) },
            label = stringResource(R.string.automation_edge_falling),
        )
    }

    // Hysteresis only makes sense for the ordered comparisons; the
    // repository's save-normalization guards the degenerate combos.
    if (trigger.op in ORDERED_OPS) {
        SwitchRow(
            label = stringResource(R.string.automation_editor_hysteresis),
            checked = trigger.clearValue != null,
            onCheckedChange = { enabled ->
                val clear = if (enabled) defaultClearValue(trigger, min, max) else null
                onTriggerChange(trigger.copy(clearValue = clear))
            },
        )
        trigger.clearValue?.let { clear ->
            GadgetSlider(
                value = clear,
                onValueChange = { onTriggerChange(trigger.copy(clearValue = it)) },
                valueRange = min..max,
                label = stringResource(R.string.automation_editor_rearm),
                suffix = descriptor?.unit?.takeIf { it.isNotEmpty() },
                valueFormatter = ::formatOneDecimal,
                modifier = Modifier.fillMaxWidth(),
            )
            HintText(stringResource(R.string.automation_editor_hysteresis_hint))
        }
    }
}

/** A starting re-arm bound on the re-arm side of the threshold. */
private fun defaultClearValue(trigger: Trigger.MetricThreshold, min: Float, max: Float): Float =
    when (trigger.op) {
        ComparisonOp.Lt, ComparisonOp.Lte -> (trigger.value + max) / 2f
        ComparisonOp.Gt, ComparisonOp.Gte -> (min + trigger.value) / 2f
        ComparisonOp.Eq, ComparisonOp.Neq -> trigger.value
    }

@Composable
private fun ScheduleTriggerEditor(
    trigger: Trigger.Schedule,
    exactAlarmAllowed: Boolean,
    onRequestExactAlarm: () -> Unit,
    onTriggerChange: (Trigger) -> Unit,
) {
    GadgetSlider(
        value = trigger.timeOfDayMinutes.toFloat(),
        onValueChange = { raw ->
            val snapped = (raw / TIME_STEP_MINUTES).roundToInt() * TIME_STEP_MINUTES
            onTriggerChange(
                trigger.copy(timeOfDayMinutes = snapped.coerceIn(0, MINUTES_PER_DAY - 1)),
            )
        },
        valueRange = 0f..(MINUTES_PER_DAY - TIME_STEP_MINUTES).toFloat(),
        label = stringResource(R.string.automation_editor_time),
        valueFormatter = { formatTimeOfDay(it.roundToInt()) },
        valueParser = ::parseTimeOfDay,
        modifier = Modifier.fillMaxWidth(),
    )

    LabeledChipRow(label = stringResource(R.string.automation_editor_days)) {
        DayOfWeek.values().forEach { day ->
            val selected = day in trigger.daysOfWeek
            GadgetChip(
                selected = selected,
                onClick = {
                    val days =
                        if (selected) trigger.daysOfWeek - day else trigger.daysOfWeek + day
                    // An empty day set would never fire — keep the last one.
                    if (days.isNotEmpty()) onTriggerChange(trigger.copy(daysOfWeek = days))
                },
                label = dayLabel(day),
            )
        }
    }

    SwitchRow(
        label = stringResource(R.string.automation_editor_exact),
        checked = trigger.exact,
        onCheckedChange = { onTriggerChange(trigger.copy(exact = it)) },
    )
    HintText(
        stringResource(
            if (trigger.exact) {
                R.string.automation_editor_exact_hint_exact
            } else {
                R.string.automation_editor_exact_hint_inexact
            },
        ),
    )
    if (trigger.exact && !exactAlarmAllowed) {
        // The design doc's third degradation state: exact requested but the
        // special permission is denied — badge + deep link.
        Text(
            text = stringResource(R.string.automation_editor_exact_denied),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        GadgetTertiaryButton(
            onClick = onRequestExactAlarm,
            text = stringResource(R.string.automation_editor_exact_allow),
        )
    }
}

@Composable
private fun SystemEventTriggerEditor(
    trigger: Trigger.SystemEvent,
    onTriggerChange: (Trigger) -> Unit,
) {
    LabeledChipRow(label = stringResource(R.string.automation_editor_event)) {
        // Connectivity is modeled but not yet armed (no resident
        // NetworkCallback) — hidden from authoring per the design doc's
        // batch-3.3 amendment.
        AUTHORABLE_EVENTS.forEach { event ->
            GadgetChip(
                selected = trigger.event == event,
                onClick = { onTriggerChange(trigger.copy(event = event)) },
                label = stringResource(
                    when (event) {
                        SystemEventKind.BootCompleted -> R.string.automation_event_boot
                        SystemEventKind.PowerConnected -> R.string.automation_event_power_connected
                        SystemEventKind.PowerDisconnected ->
                            R.string.automation_event_power_disconnected
                        SystemEventKind.Connectivity -> R.string.automation_summary_connectivity
                    },
                ),
            )
        }
    }
}

// ─── Shared sheet primitives (also used by RuleEditorSections) ──────────

@Composable
internal fun SheetSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            // The visual label is a sibling — carry it on the switch so a
            // screen reader announces what's being toggled.
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

@Composable
internal fun LabeledChipRow(label: String, content: @Composable FlowRowScope.() -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
            verticalArrangement = Arrangement.spacedBy(spacing.tiny),
            content = content,
        )
    }
}

/** "4.5" — one decimal, locale-aware, trailing zeros dropped by the format. */
internal fun formatOneDecimal(value: Float): String = ONE_DECIMAL.format(value)

private val ONE_DECIMAL = java.text.DecimalFormat("0.#")

internal val ORDERED_OPS =
    setOf(ComparisonOp.Lt, ComparisonOp.Lte, ComparisonOp.Gt, ComparisonOp.Gte)

private val AUTHORABLE_EVENTS = listOf(
    SystemEventKind.BootCompleted,
    SystemEventKind.PowerConnected,
    SystemEventKind.PowerDisconnected,
)

/** 09:00 — the design doc's worked schedule example. */
private const val DEFAULT_SCHEDULE_MINUTES = 9 * 60

/** Time slider snap step: 5-minute granularity, editable to the minute. */
private const val TIME_STEP_MINUTES = 5

/** Fallback ceiling when a saved rule references an unregistered signal. */
private const val DEFAULT_METRIC_CEILING = 100f

/** 0..1 h — beyond an hour, a Schedule trigger is the better tool. */
private const val MAX_COOLDOWN_SECONDS = 3_600f
