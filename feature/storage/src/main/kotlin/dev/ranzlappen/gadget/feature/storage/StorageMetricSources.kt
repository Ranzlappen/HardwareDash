package dev.ranzlappen.gadget.feature.storage

import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storage metric sources bound `@IntoMap @StringKey(METRIC_KEY)` via [di.StorageModule]. Both
 * are poll sources — storage usage changes slowly and doesn't need a push/stream path.
 */

@Singleton
class StorageUsedPercentMetricSource @Inject constructor(
    private val monitor: StorageMonitor,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Storage used",
        unit = "%",
        min = 0f,
        max = 100f,
        category = MetricCategory.Device,
    )

    override suspend fun sample(): Float = monitor.internalUsedPercent()

    companion object {
        const val METRIC_KEY = "storage_used_percent"
    }
}

@Singleton
class StorageFreeGbMetricSource @Inject constructor(
    private val monitor: StorageMonitor,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Storage free",
        unit = "GB",
        min = 0f,
        max = 512f,
        category = MetricCategory.Device,
    )

    override suspend fun sample(): Float =
        monitor.internalFreeBytes().toFloat() / (1024f * 1024f * 1024f)

    companion object {
        const val METRIC_KEY = "storage_free_gb"
    }
}
