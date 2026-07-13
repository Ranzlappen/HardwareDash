package dev.ranzlappen.gadget.feature.metricwidget

import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import kotlinx.serialization.Serializable

/**
 * Per-`appWidgetId` config for the **generic metric widget** (W4) — the first
 * cross-cutting widget that binds to *any* registered `MetricSource` the user
 * picks in its configure activity, rather than a single feature's hardwired
 * signal (battery %, folder cover). [metricKey] is the `@StringKey` of the
 * chosen source in the app-wide `Map<String, MetricSource>` multibinding;
 * [NO_METRIC] is the unbound self-heal default (renders a "pick a metric"
 * placeholder and stays inert on tap).
 *
 * A content/display widget, so it carries the kit contract + a starting-size
 * hint + the metric-specific choices ([display], [showLabel], [tintArgb]).
 * `@Serializable` — persisted per `appWidgetId` via the kit `WidgetConfigStore`.
 */
@Serializable
data class MetricWidgetConfig(
    val metricKey: String = NO_METRIC,
    val display: MetricWidgetDisplay = MetricWidgetDisplay.ValueAndBar,
    val showLabel: Boolean = true,
    val tintArgb: Long? = null,
    val sizePreset: WidgetSizePreset = WidgetSizePreset.Medium,
    override val displayName: String = "",
    override val removed: Boolean = false,
    override val schemaVersion: Int = 1,
    override val appearance: WidgetAppearance = WidgetAppearance(),
) : WidgetKitConfig {

    /** Whether this widget is bound to a metric yet (vs. a placeholder). */
    val isBound: Boolean get() = metricKey != NO_METRIC

    companion object {
        /** The unbound sentinel: a fresh tray-drop before the picker runs. */
        const val NO_METRIC: String = ""
    }
}

/** How the generic metric widget renders the chosen metric's live value. */
@Serializable
enum class MetricWidgetDisplay {
    /** Just the formatted value + unit. */
    Value,

    /** The value + unit plus a progress bar scaled to the descriptor ceiling. */
    ValueAndBar,
}
