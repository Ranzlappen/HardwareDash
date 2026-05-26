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

    fun observeSince(metricKey: String, sinceMs: Long): Flow<List<MonitorSample>> =
        dao.observeSince(metricKey, sinceMs)

    fun observeLatest(metricKey: String): Flow<MonitorSample?> =
        dao.observeLatest(metricKey)

    suspend fun prune(metricKey: String, cutoffMs: Long) =
        dao.pruneOlderThan(metricKey, cutoffMs)
}
