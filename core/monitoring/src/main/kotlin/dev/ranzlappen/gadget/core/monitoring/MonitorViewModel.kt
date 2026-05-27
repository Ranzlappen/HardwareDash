package dev.ranzlappen.gadget.core.monitoring

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.data.MonitorBucket
import dev.ranzlappen.gadget.core.data.MonitorSampleRepository
import dev.ranzlappen.gadget.core.model.MetricSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs [MonitorContainer]. One instance per metricKey (the container
 * scopes it via `hiltViewModel(key = metricKey)`), but every method is
 * metricKey-parameterised so the same VM type serves any module.
 */
@HiltViewModel
class MonitorViewModel @Inject constructor(
    private val configRepo: MonitorConfigRepository,
    private val sampleRepo: MonitorSampleRepository,
    private val controller: MonitorController,
    private val collapseRepo: CollapseStateRepository,
    private val metricSources: Map<String, @JvmSuppressWildcards MetricSource>,
) : ViewModel() {

    fun config(metricKey: String): Flow<MonitorConfig> = configRepo.config(metricKey)

    /** Persisted expanded/collapsed state for a collapsible host id. */
    fun expanded(id: String): Flow<Boolean> = collapseRepo.expanded(id)

    fun setExpanded(id: String, expanded: Boolean) {
        viewModelScope.launch { collapseRepo.setExpanded(id, expanded) }
    }

    /**
     * The metric's full-scale ceiling, from its [MetricSource] descriptor —
     * the y-axis / progress max. Capability-driven for the torch (100 on
     * standard, ~150 on the rooted boost flavor); falls back to the config
     * default for an unbound key.
     */
    fun maxValue(metricKey: String): Float =
        metricSources[metricKey]?.descriptor?.max ?: MonitorConfig().yMax

    /**
     * Downsampled, windowed sample history. The window lower-bound slides on
     * a ticker (re-querying every poll interval) so the chart scrolls even
     * when no new sample has landed. Each emission carries the bucket size so
     * the chart can map bucket indices back to elapsed time; the bucket size
     * is derived from the window + poll rate to cap the point count
     * regardless of window length (a 24h window stays a few hundred points).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun history(metricKey: String): Flow<MonitorHistory> =
        configRepo.config(metricKey).flatMapLatest { cfg ->
            val windowMs = cfg.windowSeconds.toLong() * 1_000L
            val bucketMs = MonitorDownsampling.bucketMs(
                windowMs = windowMs,
                pollIntervalMs = cfg.pollIntervalMs,
                maxPoints = MonitorDownsampling.IN_APP_MAX_POINTS,
            )
            flow {
                while (true) {
                    emit(System.currentTimeMillis() - windowMs)
                    delay(cfg.pollIntervalMs.coerceAtLeast(MIN_TICK_MS))
                }
            }.flatMapLatest { sinceMs ->
                sampleRepo.observeBucketedSince(metricKey, sinceMs, bucketMs)
                    .map { MonitorHistory(buckets = it, bucketMs = bucketMs, windowMs = windowMs) }
            }
        }

    fun update(metricKey: String, config: MonitorConfig) {
        viewModelScope.launch {
            configRepo.save(metricKey, config)
            if (config.enabled) controller.ensureStarted()
        }
    }

    private companion object {
        const val MIN_TICK_MS = 250L
    }
}

/**
 * A downsampled chart window: peak-per-bucket [buckets], the [bucketMs] each
 * bucket index spans, and the total [windowMs] the chart covers. The chart
 * pins its x-axis to the full window (`bucket index 0..windowMs/bucketMs`,
 * right edge = now) so the axis always shows the configured span with
 * "time-ago" labels anchored to now, and data flows in from the right.
 */
@Immutable
data class MonitorHistory(
    val buckets: List<MonitorBucket>,
    val bucketMs: Long,
    val windowMs: Long,
)
