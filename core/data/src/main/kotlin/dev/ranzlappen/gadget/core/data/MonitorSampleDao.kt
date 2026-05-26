package dev.ranzlappen.gadget.core.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitorSampleDao {

    @Insert
    suspend fun insert(sample: MonitorSample)

    /**
     * Downsampled windowed history: collapses raw samples into fixed
     * `bucketMs`-wide time buckets (relative to [sinceMs]) and returns the
     * **peak** value per non-empty bucket, oldest-first. Caps the row count at
     * `window / bucketMs` regardless of poll rate, so a 24h window stays a few
     * hundred points instead of tens of thousands — bounding chart memory and
     * dodging Vico's x-range blow-up. `MAX(value)` preserves activity peaks
     * (the right readout for on/off actuators like the torch reference); a
     * single-sample bucket reports that sample unchanged. [bucketMs] must be
     * `>= 1` (the caller derives it from the window + a point cap).
     */
    @Query(
        "SELECT (timestamp_ms - :sinceMs) / :bucketMs AS bucket, " +
            "MAX(value) AS max_value " +
            "FROM monitor_sample " +
            "WHERE metric_key = :metricKey AND timestamp_ms >= :sinceMs " +
            "GROUP BY bucket ORDER BY bucket ASC",
    )
    fun observeBucketedSince(
        metricKey: String,
        sinceMs: Long,
        bucketMs: Long,
    ): Flow<List<MonitorBucket>>

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

/**
 * One downsampled time bucket — the [bucket] index (0-based, counting from
 * the query's `sinceMs`) and the peak value within it. The index is a small
 * monotonic integer suitable for direct use as a chart x-coordinate (its
 * range is bounded by the bucket count, sidestepping Vico's float-precision
 * and x-range limits). Multiply [bucket] by the query's `bucketMs` to recover
 * the elapsed time for axis labels.
 */
data class MonitorBucket(
    @ColumnInfo(name = "bucket") val bucket: Long,
    @ColumnInfo(name = "max_value") val maxValue: Float,
)
