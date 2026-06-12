package dev.ranzlappen.gadget.feature.automation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.automation.model.ComparisonOp
import dev.ranzlappen.gadget.core.automation.model.DayOfWeek
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.SystemEventKind
import dev.ranzlappen.gadget.core.automation.model.Trigger
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import java.text.DecimalFormat

/** Fixed one-decimal readout for metric bounds (matches the sensors screen). */
private val VALUE_FORMAT = DecimalFormat("0.#")

/** "09:05" from minutes-after-midnight. */
internal fun formatTimeOfDay(totalMinutes: Int): String {
    val clamped = totalMinutes.coerceIn(0, MINUTES_PER_DAY - 1)
    return "%02d:%02d".format(clamped / 60, clamped % 60)
}

/** Inverse of [formatTimeOfDay] for the slider's editable-text path. */
internal fun parseTimeOfDay(text: String): Float? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    if (hours !in 0..23 || minutes !in 0..59) return null
    return (hours * 60 + minutes).toFloat()
}

internal const val MINUTES_PER_DAY = 1440

/** The mathematical glyph for [this] — universal, deliberately unlocalized. */
internal val ComparisonOp.symbol: String
    get() = when (this) {
        ComparisonOp.Lt -> "<"
        ComparisonOp.Lte -> "≤"
        ComparisonOp.Gt -> ">"
        ComparisonOp.Gte -> "≥"
        ComparisonOp.Eq -> "="
        ComparisonOp.Neq -> "≠"
    }

/** "4.5 cm" — value + the signal's unit when one is declared. */
internal fun formatMetricValue(value: Float, descriptor: MetricDescriptor?): String {
    val number = VALUE_FORMAT.format(value)
    val unit = descriptor?.unit.orEmpty()
    return if (unit.isEmpty()) number else "$number $unit"
}

/** Short localized label for [day]. */
@Composable
internal fun dayLabel(day: DayOfWeek): String = stringResource(
    when (day) {
        DayOfWeek.Monday -> R.string.automation_day_mon
        DayOfWeek.Tuesday -> R.string.automation_day_tue
        DayOfWeek.Wednesday -> R.string.automation_day_wed
        DayOfWeek.Thursday -> R.string.automation_day_thu
        DayOfWeek.Friday -> R.string.automation_day_fri
        DayOfWeek.Saturday -> R.string.automation_day_sat
        DayOfWeek.Sunday -> R.string.automation_day_sun
    },
)

/**
 * One-line human summary of a trigger for the rules list ("When Proximity
 * < 5 cm", "Daily at 09:00 · exact", "On power connected", …).
 * [signalsByKey] resolves metric keys to display names/units; an
 * unregistered key falls back to the raw key so restored rules from
 * another build still render.
 */
@Composable
internal fun triggerSummary(
    trigger: Trigger,
    signalsByKey: Map<String, MetricDescriptor>,
): String = when (trigger) {
    is Trigger.MetricThreshold -> {
        val descriptor = signalsByKey[trigger.metricKey]
        stringResource(
            R.string.automation_summary_metric,
            descriptor?.displayName ?: trigger.metricKey,
            trigger.op.symbol,
            formatMetricValue(trigger.value, descriptor),
        )
    }
    is Trigger.Schedule -> {
        val time = formatTimeOfDay(trigger.timeOfDayMinutes)
        val base = if (trigger.daysOfWeek == DayOfWeek.everyDay()) {
            stringResource(R.string.automation_summary_schedule_daily, time)
        } else {
            // Stable Monday-first ordering regardless of set iteration order.
            // map (inline) resolves the composable labels; joinToString takes
            // no lambda — a composable call inside its non-inline transform
            // doesn't compile.
            val days = DayOfWeek.values()
                .filter { it in trigger.daysOfWeek }
                .map { dayLabel(it) }
                .joinToString(", ")
            stringResource(R.string.automation_summary_schedule_days, days, time)
        }
        if (trigger.exact) {
            "$base ${stringResource(R.string.automation_summary_schedule_exact_suffix)}"
        } else {
            base
        }
    }
    is Trigger.SystemEvent -> stringResource(
        when (trigger.event) {
            SystemEventKind.BootCompleted -> R.string.automation_summary_boot
            SystemEventKind.PowerConnected -> R.string.automation_summary_power_connected
            SystemEventKind.PowerDisconnected -> R.string.automation_summary_power_disconnected
            SystemEventKind.Connectivity -> R.string.automation_summary_connectivity
        },
    )
    is Trigger.Manual -> stringResource(R.string.automation_summary_manual)
}

/**
 * "Runs: Torch · Off, Vibration · Pulse" — the rule's action labels,
 * resolved through the registry choices; unknown (e.g. root-only actions
 * on a standard build) fall back to `featureId · actionKey`.
 */
@Composable
internal fun actionsSummary(rule: Rule, choices: List<ActionChoice>): String {
    if (rule.actions.isEmpty()) return stringResource(R.string.automation_rule_actions_none)
    val labels = rule.actions.map { action ->
        val choice = choices.firstOrNull {
            it.featureId == action.featureId && it.action.key == action.actionKey
        }
        actionChoiceLabel(action.featureId, choice?.action?.label ?: action.actionKey)
    }
    return stringResource(R.string.automation_rule_actions_summary, labels.joinToString(", "))
}

/** "Torch · Off" — feature id (title-cased) + the action's own label. */
@Composable
internal fun actionChoiceLabel(featureId: String, actionLabel: String): String = stringResource(
    R.string.automation_action_label,
    featureId.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
    actionLabel,
)
