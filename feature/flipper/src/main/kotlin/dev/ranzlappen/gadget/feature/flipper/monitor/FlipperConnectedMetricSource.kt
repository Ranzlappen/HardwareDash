package dev.ranzlappen.gadget.feature.flipper.monitor

import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.flipper.FlipperConnectionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Push source for "is a Flipper Zero connected" (0/1). Event-driven off the
 * shared [FlipperConnectionManager] state — emits only on connect/disconnect.
 */
@Singleton
class FlipperConnectedMetricSource @Inject constructor(
    private val manager: FlipperConnectionManager,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Flipper connected",
        unit = "",
        min = 0f,
        max = 1f,
        category = MetricCategory.Device,
    )

    override suspend fun sample(): Float = manager.state.value.toConnectedFloat()

    override fun stream(): Flow<Float> = manager.state.map { it.toConnectedFloat() }

    private fun FlipperConnectionManager.State.toConnectedFloat(): Float =
        if (this is FlipperConnectionManager.State.Connected) 1f else 0f

    companion object {
        const val METRIC_KEY = "flipper_connected"
    }
}
