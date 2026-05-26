package dev.ranzlappen.gadget.core.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only Room surface other modules touch for monitoring history.
 *
 * Wraps [MonitorSampleDao] so consumers (`:core:monitoring`) depend on a
 * repository, not on Room directly — per the repo's "Room → :core:data"
 * convention.
 */
@Singleton
class MonitorSampleRepository @Inject constructor(
    private val dao: MonitorSampleDao,
) {
    suspend fun insert(metricKey: String, timestampMs: Long, value: Float) =
        dao.insert(MonitorSample(metricKey = metricKey, timestampMs = timestampMs, value = value))

    /**
     * Downsampled windowed history — at most `window / bucketMs` peak-per-
     * bucket points. Feeds the chart + chart widget so a long window (up to
     * 24h) stays bounded. [bucketMs] is coerced to `>= 1` to keep the SQL
     * bucket divisor safe.
     */
    fun observeBucketedSince(
        metricKey: String,
        sinceMs: Long,
        bucketMs: Long,
    ): Flow<List<MonitorBucket>> =
        dao.observeBucketedSince(metricKey, sinceMs, bucketMs.coerceAtLeast(1L))

    fun observeLatest(metricKey: String): Flow<MonitorSample?> =
        dao.observeLatest(metricKey)

    suspend fun prune(metricKey: String, cutoffMs: Long) =
        dao.pruneOlderThan(metricKey, cutoffMs)
}
