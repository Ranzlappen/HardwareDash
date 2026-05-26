package dev.ranzlappen.gadget.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single time-series datapoint for a monitored metric.
 *
 * Rows are keyed by [metricKey] (e.g. `"torch_intensity"`) so one table
 * holds the history of every module's monitored signals. The
 * `(metric_key, timestamp_ms)` index backs both the windowed chart query
 * and the prune sweep.
 */
@Entity(
    tableName = "monitor_sample",
    indices = [Index(value = ["metric_key", "timestamp_ms"])],
)
data class MonitorSample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "metric_key") val metricKey: String,
    @ColumnInfo(name = "timestamp_ms") val timestampMs: Long,
    @ColumnInfo(name = "value") val value: Float,
)
