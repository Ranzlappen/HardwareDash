package dev.ranzlappen.gadget.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitorSampleDao {

    @Insert
    suspend fun insert(sample: MonitorSample)

    /** Windowed history for a metric, oldest-first — feeds the chart. */
    @Query(
        "SELECT * FROM monitor_sample WHERE metric_key = :metricKey " +
            "AND timestamp_ms >= :sinceMs ORDER BY timestamp_ms ASC",
    )
    fun observeSince(metricKey: String, sinceMs: Long): Flow<List<MonitorSample>>

    /** Most recent sample for a metric — feeds the widget/notification readout. */
    @Query(
        "SELECT * FROM monitor_sample WHERE metric_key = :metricKey " +
            "ORDER BY timestamp_ms DESC LIMIT 1",
    )
    fun observeLatest(metricKey: String): Flow<MonitorSample?>

    /** Drop samples older than [cutoffMs] for a metric (retention sweep). */
    @Query("DELETE FROM monitor_sample WHERE metric_key = :metricKey AND timestamp_ms < :cutoffMs")
    suspend fun pruneOlderThan(metricKey: String, cutoffMs: Long)
}
