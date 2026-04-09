package com.gadget.ui.logbook

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.gadget.MainActivity
import com.gadget.R
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that fires a native notification for a Logbook checkpoint reminder.
 * Scheduled as a OneTimeWorkRequest with an initial delay matching the reminder time.
 */
class LogbookReminderWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    override fun doWork(): Result {
        val procId = inputData.getString(KEY_PROC_ID) ?: return Result.failure()
        val cpIdx = inputData.getInt(KEY_CP_IDX, -1)
        val procName = inputData.getString(KEY_PROC_NAME) ?: "Process"
        val cpName = inputData.getString(KEY_CP_NAME) ?: "Checkpoint"

        if (cpIdx < 0) return Result.failure()

        ensureChannel(applicationContext)

        // Tap opens the app
        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            applicationContext, (procId.hashCode() + cpIdx),
            tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val lang = LocalizationManager.loadLanguage(applicationContext)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(S.Services.checkpointTitle(lang, cpName))
            .setContentText(S.Services.checkpointDue(lang, procName, cpName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tapPending)
            .build()

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId(procId, cpIdx), notification)

        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "logbook_reminders"
        private const val CHANNEL_NAME = "Logbook Reminders"

        private const val KEY_PROC_ID = "proc_id"
        private const val KEY_CP_IDX = "cp_idx"
        private const val KEY_PROC_NAME = "proc_name"
        private const val KEY_CP_NAME = "cp_name"

        private fun workName(procId: String, cpIdx: Int) = "logbook_reminder_${procId}_$cpIdx"
        private fun notificationId(procId: String, cpIdx: Int) = (procId.hashCode() + cpIdx * 31)

        /** Create the notification channel (idempotent). */
        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Reminders for Logbook checkpoint due dates"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        /**
         * Schedule a one-shot reminder at [remindAtIso].
         * If the time is in the past or > 30 days away, it is skipped.
         */
        fun schedule(
            context: Context,
            procId: String,
            cpIdx: Int,
            procName: String,
            cpName: String,
            remindAtIso: String,
        ) {
            val targetInstant = try {
                // Try ISO instant first
                Instant.parse(remindAtIso)
            } catch (_: Exception) {
                try {
                    // Try datetime-local format "2024-01-15T14:30"
                    val ldt = java.time.LocalDateTime.parse(remindAtIso)
                    ldt.atZone(java.time.ZoneId.systemDefault()).toInstant()
                } catch (_: Exception) {
                    return
                }
            }

            val delayMillis = Duration.between(Instant.now(), targetInstant).toMillis()
            if (delayMillis <= 0) return // Past due, skip
            if (delayMillis > TimeUnit.DAYS.toMillis(30)) return // Too far out

            val data = Data.Builder()
                .putString(KEY_PROC_ID, procId)
                .putInt(KEY_CP_IDX, cpIdx)
                .putString(KEY_PROC_NAME, procName)
                .putString(KEY_CP_NAME, cpName)
                .build()

            val request = OneTimeWorkRequestBuilder<LogbookReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("logbook_reminder")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                workName(procId, cpIdx),
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        /** Cancel a pending reminder. */
        fun cancel(context: Context, procId: String, cpIdx: Int) {
            WorkManager.getInstance(context).cancelUniqueWork(workName(procId, cpIdx))
            // Also dismiss the notification if it's already showing
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notificationId(procId, cpIdx))
        }
    }
}
