package dev.ranzlappen.gadget.core.monitoring

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Persistent per-metric monitoring settings. One record per `metricKey`,
 * stored via [MonitorConfigRepository]. Every field is user-controllable
 * from the [MonitorContainer] settings block and survives process death.
 */
@Serializable
@Immutable
data class MonitorConfig(
    val enabled: Boolean = false,
    val pollIntervalMs: Long = 1_000L,
    val chartLayout: MonitorChartLayout = MonitorChartLayout.Line,
    val windowSeconds: Int = DEFAULT_WINDOW_SECONDS,
    val yMax: Float = 100f,
    val widgetEnabled: Boolean = false,
    val notificationEnabled: Boolean = false,
) {
    companion object {
        /** 5h default window. The chart downsamples so a long window stays
         *  cheap; the user can widen it up to [MAX_WINDOW_SECONDS] (24h). */
        const val DEFAULT_WINDOW_SECONDS = 5 * 60 * 60

        /** 1-minute floor / 24-hour ceiling for the window slider. The 24h cap
         *  matches the sample-retention horizon, so the chart can never ask
         *  for data older than what's kept. */
        const val MIN_WINDOW_SECONDS = 60
        const val MAX_WINDOW_SECONDS = 24 * 60 * 60
    }
}

@Serializable
enum class MonitorChartLayout { Line, Area, Bars }
