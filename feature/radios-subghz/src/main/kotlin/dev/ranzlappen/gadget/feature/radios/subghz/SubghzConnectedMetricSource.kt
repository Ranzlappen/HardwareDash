package dev.ranzlappen.gadget.feature.radios.subghz

import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Push source for "is a Sub-GHz bridge attached" (0/1). Event-driven off the
 * shared [SubghzMonitor], so an idle bus incurs no polling wakeups — it emits
 * only when a dongle is plugged in or removed. Feeds both monitoring history
 * and automation triggers from the one definition.
 */
@Singleton
class SubghzConnectedMetricSource @Inject constructor(
    private val monitor: SubghzMonitor,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Sub-GHz bridge",
        unit = "",
        min = 0f,
        max = 1f,
        category = MetricCategory.Network,
    )

    override suspend fun sample(): Float = if (monitor.state.value.bridgeConnected) 1f else 0f

    override fun stream(): Flow<Float> =
        monitor.state.map { if (it.bridgeConnected) 1f else 0f }

    companion object {
        const val METRIC_KEY = "subghz_bridge_connected"
    }
}
