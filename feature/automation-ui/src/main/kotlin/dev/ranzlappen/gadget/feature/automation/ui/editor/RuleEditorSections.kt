package dev.ranzlappen.gadget.feature.automation.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.model.ComparisonOp
import dev.ranzlappen.gadget.core.automation.model.Condition
import dev.ranzlappen.gadget.core.automation.model.ConditionLogic
import dev.ranzlappen.gadget.core.automation.model.RuleAction
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.currentMax
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetIconButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.ui.component.GlassIntensity
import dev.ranzlappen.gadget.core.ui.component.GlassSurface
import dev.ranzlappen.gadget.feature.automation.ui.ActionChoice
import dev.ranzlappen.gadget.feature.automation.ui.MINUTES_PER_DAY
import dev.ranzlappen.gadget.feature.automation.ui.R
import dev.ranzlappen.gadget.feature.automation.ui.actionChoiceLabel
import dev.ranzlappen.gadget.feature.automation.ui.formatTimeOfDay
import dev.ranzlappen.gadget.feature.automation.ui.parseTimeOfDay
import dev.ranzlappen.gadget.feature.automation.ui.symbol
import kotlin.math.roundToInt

// The conditions + actions halves of the rule builder, split out of
// RuleEditorSheet.kt to keep both files readable. Same package-internal
// contract: pure value-in/value-out editing of the Rule draft's lists.

// ─── Conditions ─────────────────────────────────────────────────────────

@Composable
internal fun ConditionsSection(
    conditions: List<Condition>,
    conditionLogic: ConditionLogic,
    signals: List<MetricDescriptor>,
    onConditionsChange: (List<Condition>) -> Unit,
    onLogicChange: (ConditionLogic) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        if (conditions.isEmpty()) {
            HintText(stringResource(R.string.automation_editor_conditions_empty))
        }
        if (conditions.size > 1) {
            LabeledChipRow(label = stringResource(R.string.automation_editor_condition_logic)) {
                GadgetChip(
                    selected = conditionLogic == ConditionLogic.All,
                    onClick = { onLogicChange(ConditionLogic.All) },
                    label = stringResource(R.string.automation_logic_all),
                )
                GadgetChip(
                    selected = conditionLogic == ConditionLogic.Any,
                    onClick = { onLogicChange(ConditionLogic.Any) },
                    label = stringResource(R.string.automation_logic_any),
                )
            }
        }
        conditions.forEachIndexed { index, condition ->
            ConditionBlock(
                condition = condition,
                signals = signals,
                onConditionChange = { updated ->
                    onConditionsChange(conditions.replaceAt(index, updated))
                },
                onRemove = { onConditionsChange(conditions.withoutAt(index)) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            GadgetTertiaryButton(
                onClick = {
                    // if/else, not return@ — the labeled-return-in-named-
                    // lambda pitfall in CLAUDE.md.
                    val first = signals.firstOrNull()
                    if (first != null) {
                        onConditionsChange(
                            conditions + Condition.MetricCompare(
                                metricKey = first.metricKey,
                                op = ComparisonOp.Lt,
                                value = (first.min + first.currentMax()) / 2f,
                            ),
                        )
                    }
                },
                text = stringResource(R.string.automation_editor_condition_add_metric),
                enabled = signals.isNotEmpty(),
            )
            GadgetTertiaryButton(
                onClick = {
                    onConditionsChange(
                        conditions + Condition.TimeWindow(
                            startMinutes = DEFAULT_WINDOW_START,
                            endMinutes = DEFAULT_WINDOW_END,
                        ),
                    )
                },
                text = stringResource(R.string.automation_editor_condition_add_time),
            )
        }
    }
}

@Composable
private fun ConditionBlock(
    condition: Condition,
    signals: List<MetricDescriptor>,
    onConditionChange: (Condition) -> Unit,
    onRemove: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        intensity = GlassIntensity.Subtle,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        when (condition) {
                            is Condition.MetricCompare -> R.string.automation_condition_metric_title
                            is Condition.TimeWindow -> R.string.automation_condition_time_title
                        },
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                GadgetIconButton(
                    onClick = onRemove,
                    icon = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.automation_editor_condition_remove),
                )
            }
            when (condition) {
                is Condition.MetricCompare ->
                    MetricConditionEditor(condition, signals, onConditionChange)
                is Condition.TimeWindow ->
                    TimeWindowConditionEditor(condition, onConditionChange)
            }
        }
    }
}

@Composable
private fun MetricConditionEditor(
    condition: Condition.MetricCompare,
    signals: List<MetricDescriptor>,
    onConditionChange: (Condition) -> Unit,
) {
    val descriptor = signals.firstOrNull { it.metricKey == condition.metricKey }
    LabeledChipRow(label = stringResource(R.string.automation_editor_signal)) {
        signals.forEach { signal ->
            GadgetChip(
                selected = signal.metricKey == condition.metricKey,
                onClick = {
                    if (signal.metricKey != condition.metricKey) {
                        onConditionChange(
                            condition.copy(
                                metricKey = signal.metricKey,
                                value = (signal.min + signal.currentMax()) / 2f,
                            ),
                        )
                    }
                },
                label = signal.displayName,
            )
        }
    }
    LabeledChipRow(label = stringResource(R.string.automation_editor_condition_value_is)) {
        ComparisonOp.values().forEach { op ->
            GadgetChip(
                selected = condition.op == op,
                onClick = { onConditionChange(condition.copy(op = op)) },
                label = op.symbol,
            )
        }
    }
    GadgetSlider(
        value = condition.value,
        onValueChange = { onConditionChange(condition.copy(value = it)) },
        valueRange = (descriptor?.min ?: 0f)..(descriptor?.currentMax() ?: FALLBACK_CEILING),
        label = stringResource(R.string.automation_editor_threshold),
        suffix = descriptor?.unit?.takeIf { it.isNotEmpty() },
        valueFormatter = ::formatOneDecimal,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TimeWindowConditionEditor(
    condition: Condition.TimeWindow,
    onConditionChange: (Condition) -> Unit,
) {
    TimeOfDaySlider(
        label = stringResource(R.string.automation_editor_window_start),
        minutes = condition.startMinutes,
        onMinutesChange = { onConditionChange(condition.copy(startMinutes = it)) },
    )
    TimeOfDaySlider(
        label = stringResource(R.string.automation_editor_window_end),
        minutes = condition.endMinutes,
        onMinutesChange = { onConditionChange(condition.copy(endMinutes = it)) },
    )
    HintText(stringResource(R.string.automation_editor_window_wrap_hint))
}

@Composable
private fun TimeOfDaySlider(label: String, minutes: Int, onMinutesChange: (Int) -> Unit) {
    GadgetSlider(
        value = minutes.toFloat(),
        onValueChange = { raw ->
            val snapped = (raw / WINDOW_STEP_MINUTES).roundToInt() * WINDOW_STEP_MINUTES
            onMinutesChange(snapped.coerceIn(0, MINUTES_PER_DAY - 1))
        },
        valueRange = 0f..(MINUTES_PER_DAY - WINDOW_STEP_MINUTES).toFloat(),
        label = label,
        valueFormatter = { formatTimeOfDay(it.roundToInt()) },
        valueParser = ::parseTimeOfDay,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ─── Actions ────────────────────────────────────────────────────────────

@Composable
internal fun ActionsSection(
    actions: List<RuleAction>,
    actionChoices: List<ActionChoice>,
    onActionsChange: (List<RuleAction>) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        if (actions.isEmpty()) {
            Text(
                text = stringResource(R.string.automation_editor_actions_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        actions.forEachIndexed { index, action ->
            ActionBlock(
                action = action,
                actionChoices = actionChoices,
                onActionChange = { updated -> onActionsChange(actions.replaceAt(index, updated)) },
                onRemove = { onActionsChange(actions.withoutAt(index)) },
            )
        }
        GadgetTertiaryButton(
            onClick = {
                val first = actionChoices.firstOrNull()
                if (first != null) onActionsChange(actions + first.toRuleAction())
            },
            text = stringResource(R.string.automation_editor_action_add),
            enabled = actionChoices.isNotEmpty(),
        )
    }
}

/** Seed a [RuleAction] from a picked registry entry (defaults for params). */
private fun ActionChoice.toRuleAction(): RuleAction = RuleAction(
    featureId = featureId,
    actionKey = action.key,
    params = action.params.associate { it.name to it.default },
    requiresRoot = action.requiresRoot,
)

@Composable
private fun ActionBlock(
    action: RuleAction,
    actionChoices: List<ActionChoice>,
    onActionChange: (RuleAction) -> Unit,
    onRemove: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val selected = actionChoices.firstOrNull {
        it.featureId == action.featureId && it.action.key == action.actionKey
    }
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        intensity = GlassIntensity.Subtle,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.automation_editor_action_pick),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                GadgetIconButton(
                    onClick = onRemove,
                    icon = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.automation_editor_action_remove),
                )
            }
            LabeledChipRow(label = stringResource(R.string.automation_editor_action_pick)) {
                actionChoices.forEach { choice ->
                    GadgetChip(
                        selected = choice == selected,
                        onClick = { onActionChange(choice.toRuleAction()) },
                        label = actionChoiceLabel(choice.featureId, choice.action.label),
                    )
                }
            }
            if (selected == null) {
                // A restored rule can reference an action this build doesn't
                // register (e.g. a root action on standard) — keep it
                // visible + editable rather than silently dropping it.
                HintText(
                    stringResource(
                        R.string.automation_editor_action_unavailable,
                        actionChoiceLabel(action.featureId, action.actionKey),
                    ),
                )
            } else {
                selected.action.params.forEach { param ->
                    ActionParamEditor(
                        param = param,
                        value = action.params[param.name] ?: param.default,
                        onValueChange = { newValue ->
                            onActionChange(
                                action.copy(params = action.params + (param.name to newValue)),
                            )
                        },
                    )
                }
            }
        }
    }
}

/**
 * Renders one [ActionParam] editor from its schema — Int/Float →
 * [GadgetSlider], Bool → switch row, Text → [GadgetTextField]. The
 * feature-local twin of the widgetkit sheet's private `ParamEditor` (kept
 * private there; the schema type is shared via `:core:automation`).
 */
@Composable
private fun ActionParamEditor(
    param: ActionParam,
    value: String,
    onValueChange: (String) -> Unit,
) {
    when (param.type) {
        ActionParamType.Int, ActionParamType.Float -> {
            val isInt = param.type == ActionParamType.Int
            val min = param.min ?: 0f
            val max = param.max ?: FALLBACK_CEILING
            val current = value.toFloatOrNull() ?: param.default.toFloatOrNull() ?: min
            GadgetSlider(
                value = current.coerceIn(min, max),
                onValueChange = { raw ->
                    onValueChange(if (isInt) raw.roundToInt().toString() else raw.toString())
                },
                valueRange = min..max,
                label = param.name.toLabel(),
                valueFormatter = {
                    if (isInt) it.roundToInt().toString() else formatOneDecimal(it)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ActionParamType.Bool -> SwitchRow(
            label = param.name.toLabel(),
            checked = value.toBooleanStrictOrNull()
                ?: param.default.toBooleanStrictOrNull()
                ?: false,
            onCheckedChange = { onValueChange(it.toString()) },
        )
        ActionParamType.Text -> GadgetTextField(
            value = value,
            onValueChange = onValueChange,
            label = param.name.toLabel(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─── Small shared helpers ───────────────────────────────────────────────

private fun <T> List<T>.replaceAt(index: Int, item: T): List<T> =
    toMutableList().also { it[index] = item }

private fun <T> List<T>.withoutAt(index: Int): List<T> =
    toMutableList().also { it.removeAt(index) }

/** "rate_hz" → "Rate hz" — the widgetkit sheet's developer-key prettifier. */
private fun String.toLabel(): String =
    replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

/** Ceiling when an action param / unregistered signal declares no max. */
private const val FALLBACK_CEILING = 100f

/** 22:00–06:00 — a default window that demonstrates the midnight wrap. */
private const val DEFAULT_WINDOW_START = 22 * 60
private const val DEFAULT_WINDOW_END = 6 * 60

/** Window sliders snap to 5-minute granularity (editable to the minute). */
private const val WINDOW_STEP_MINUTES = 5
