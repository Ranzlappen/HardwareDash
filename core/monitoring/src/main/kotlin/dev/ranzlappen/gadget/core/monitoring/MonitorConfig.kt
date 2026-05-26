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
    val windowSeconds: Int = 60,
    val yMax: Float = 100f,
    val widgetEnabled: Boolean = false,
    val notificationEnabled: Boolean = false,
)

@Serializable
enum class MonitorChartLayout { Line, Area, Bars }
