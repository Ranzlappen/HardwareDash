package com.gadget.data.repository

import android.content.Context
import com.gadget.data.db.MetricDao
import com.gadget.data.db.MetricReading
import com.gadget.data.db.MetricSession
import com.gadget.widget.WidgetMetric
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetricRepository @Inject constructor(
    private val metricDao: MetricDao,
    @ApplicationContext private val context: Context,
) {
    suspend fun recordReading(metric: WidgetMetric) {
        try {
            val formatted = metric.fetch(context)
            val numeric = extractNumeric(formatted) ?: return
            metricDao.insertReading(
                MetricReading(
                    metricKey = metric.key,
                    rawValue = numeric,
                    formattedValue = formatted,
                    timestamp = System.currentTimeMillis(),
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to record reading for %s", metric.key)
        }
    }

    suspend fun recordReadings(metrics: List<WidgetMetric>) {
        val now = System.currentTimeMillis()
        val readings = metrics.mapNotNull { metric ->
            try {
                val formatted = metric.fetch(context)
                val numeric = extractNumeric(formatted) ?: return@mapNotNull null
                MetricReading(
                    metricKey = metric.key,
                    rawValue = numeric,
                    formattedValue = formatted,
                    timestamp = now,
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch metric %s", metric.key)
                null
            }
        }
        if (readings.isNotEmpty()) {
            metricDao.insertReadings(readings)
        }
    }

    fun getReadingsForMetric(key: String, limit: Int = 100): Flow<List<MetricReading>> =
        metricDao.getReadingsForMetric(key, limit)

    fun getReadingsInRange(key: String, start: Long, end: Long): Flow<List<MetricReading>> =
        metricDao.getReadingsInRange(key, start, end)

    suspend fun getRecentReadings(key: String, count: Int): List<MetricReading> =
        metricDao.getRecentReadings(key, count)

    fun getReadingsForSession(sessionId: Long): Flow<List<MetricReading>> =
        metricDao.getReadingsForSession(sessionId)

    suspend fun deleteOlderThan(before: Long) = metricDao.deleteOlderThan(before)

    suspend fun getReadingCount(): Int = metricDao.getReadingCount()

    suspend fun getRecordedMetricKeys(): List<String> = metricDao.getRecordedMetricKeys()

    // Session operations
    suspend fun startSession(name: String, metricKeys: List<String>): Long =
        metricDao.insertSession(
            MetricSession(
                name = name,
                startTime = System.currentTimeMillis(),
                metricKeys = metricKeys.joinToString(","),
            )
        )

    suspend fun endSession(sessionId: Long) {
        metricDao.getSession(sessionId)?.let { session ->
            metricDao.updateSession(session.copy(endTime = System.currentTimeMillis()))
        }
    }

    fun getAllSessions(): Flow<List<MetricSession>> = metricDao.getAllSessions()

    companion object {
        private val numericRegex = Regex("(-?\\d+\\.?\\d*)")

        fun extractNumeric(formatted: String): Double? {
            return numericRegex.find(formatted)?.value?.toDoubleOrNull()
        }
    }
}
