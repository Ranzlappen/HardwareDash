package com.gadget.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricDao {

    @Insert
    suspend fun insertReading(reading: MetricReading)

    @Insert
    suspend fun insertReadings(readings: List<MetricReading>)

    @Query("SELECT * FROM metric_readings WHERE metric_key = :key ORDER BY timestamp DESC LIMIT :limit")
    fun getReadingsForMetric(key: String, limit: Int = 100): Flow<List<MetricReading>>

    @Query("SELECT * FROM metric_readings WHERE metric_key = :key AND timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    fun getReadingsInRange(key: String, start: Long, end: Long): Flow<List<MetricReading>>

    @Query("SELECT * FROM metric_readings WHERE metric_key = :key ORDER BY timestamp DESC LIMIT :count")
    suspend fun getRecentReadings(key: String, count: Int): List<MetricReading>

    @Query("SELECT * FROM metric_readings WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun getReadingsForSession(sessionId: Long): Flow<List<MetricReading>>

    @Query("DELETE FROM metric_readings WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM metric_readings")
    suspend fun getReadingCount(): Int

    @Query("SELECT DISTINCT metric_key FROM metric_readings")
    suspend fun getRecordedMetricKeys(): List<String>

    // Session operations
    @Insert
    suspend fun insertSession(session: MetricSession): Long

    @Update
    suspend fun updateSession(session: MetricSession)

    @Query("SELECT * FROM metric_sessions ORDER BY start_time DESC")
    fun getAllSessions(): Flow<List<MetricSession>>

    @Query("SELECT * FROM metric_sessions WHERE id = :id")
    suspend fun getSession(id: Long): MetricSession?
}
