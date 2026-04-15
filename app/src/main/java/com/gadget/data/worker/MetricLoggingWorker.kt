package com.gadget.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gadget.data.repository.MetricRepository
import com.gadget.widget.WidgetMetric
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Periodically logs enabled metrics to the Room database.
 * Reads "widget_settings" SharedPreferences to determine which metrics are enabled
 * (key pattern: metric_log_${metricKey}).
 */
@HiltWorker
class MetricLoggingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val metricRepository: MetricRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences(
                "widget_settings",
                Context.MODE_PRIVATE,
            )

            val enabledMetrics = WidgetMetric.entries.filter { metric ->
                prefs.getBoolean("metric_log_${metric.key}", false)
            }

            if (enabledMetrics.isEmpty()) {
                Timber.d("No metrics enabled for logging")
                return Result.success()
            }

            metricRepository.recordReadings(enabledMetrics)
            Timber.d("Logged %d enabled metrics", enabledMetrics.size)

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "MetricLoggingWorker failed")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "metric_logging"
    }
}
