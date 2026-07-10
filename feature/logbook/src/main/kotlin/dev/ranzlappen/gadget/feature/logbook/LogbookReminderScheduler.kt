package dev.ranzlappen.gadget.feature.logbook

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.feature.logbook.worker.LogbookReminderWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arms/disarms one WorkManager one-shot per checkpoint reminder.
 *
 * **WorkManager, not `AlarmManager`.** `:core:automation`'s
 * `AutomationScheduler` deliberately chose `AlarmManager` over WorkManager
 * for its schedule triggers (ADR-0002 Decision 4) because WorkManager's
 * ~15-minute minimum-delay floor is too coarse for "fire this automation
 * rule at exactly 09:00". A Logbook checkpoint reminder is a different
 * shape of problem: it fires once, often days or weeks in the future, and
 * a few minutes of slack around the target time is an acceptable trade for
 * WorkManager's battery-friendly Doze-aware scheduling (no
 * `SCHEDULE_EXACT_ALARM` permission dance for a "remind me about this"
 * note). The legacy `com.gadget.ui.logbook.LogbookReminderWorker` also
 * used WorkManager for the same reason.
 *
 * Each checkpoint gets a **unique work name** (`enqueueUniqueWork` +
 * [androidx.work.ExistingWorkPolicy.REPLACE]) so editing a checkpoint's
 * reminder time simply re-schedules in place, and [cancel] is a precise,
 * idempotent no-op when nothing is pending.
 */
@Singleton
class LogbookReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    fun schedule(checkpointId: Long, reminderAtMillis: Long) {
        val delayMillis = (reminderAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<LogbookReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putLong(LogbookReminderWorker.KEY_CHECKPOINT_ID, checkpointId)
                    .build(),
            )
            .addTag(REMINDER_WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(uniqueWorkName(checkpointId), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(checkpointId: Long) {
        workManager.cancelUniqueWork(uniqueWorkName(checkpointId))
    }

    private fun uniqueWorkName(checkpointId: Long): String = "logbook_checkpoint_reminder_$checkpointId"

    companion object {
        /**
         * Deliberately **not** the legacy tag `"logbook_reminder"` —
         * `MainActivity.bootstrapLegacyManagers()` unconditionally calls
         * `WorkManager.cancelAllWorkByTag("logbook_reminder")` on every app
         * launch (a leftover purge of pending work that referenced the
         * now-deleted legacy `LogbookReminderWorker` class). Sharing that
         * tag here would have this module's freshly-scheduled reminders
         * cancelled the next time the user simply opens the app.
         */
        const val REMINDER_WORK_TAG = "logbook_checkpoint_reminder"
    }
}
