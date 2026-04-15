package com.gadget.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "metric_readings",
    indices = [
        Index(value = ["metric_key", "timestamp"]),
        Index(value = ["session_id"]),
    ]
)
data class MetricReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "metric_key")
    val metricKey: String,
    @ColumnInfo(name = "raw_value")
    val rawValue: Double,
    @ColumnInfo(name = "formatted_value")
    val formattedValue: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "session_id")
    val sessionId: Long? = null,
)
