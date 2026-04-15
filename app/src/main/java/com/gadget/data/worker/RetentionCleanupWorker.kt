package com.gadget.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gadget.data.repository.MetricRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Periodically deletes metric readings older than the retention period.
 * Default retention is 30 days.
 */
@HiltWorker
class RetentionCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val metricRepository: MetricRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val retentionDays = inputData.getLong(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS)
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays)

            metricRepository.deleteOlderThan(cutoff)
            Timber.d("Cleaned up metric readings older than %d days", retentionDays)

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "RetentionCleanupWorker failed")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "retention_cleanup"
        const val KEY_RETENTION_DAYS = "retention_days"
        const val DEFAULT_RETENTION_DAYS = 30L
    }
}
