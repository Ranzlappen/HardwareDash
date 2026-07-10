package dev.ranzlappen.gadget.feature.logbook.worker

import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.ranzlappen.gadget.core.data.logbook.LogbookDao
import dev.ranzlappen.gadget.core.navigation.GadgetDestination
import dev.ranzlappen.gadget.core.notifications.ChannelSpec
import dev.ranzlappen.gadget.core.notifications.NotificationChannelRegistry
import dev.ranzlappen.gadget.feature.logbook.R

/**
 * Fires once per checkpoint reminder, scheduled by [dev.ranzlappen.gadget.feature.logbook.LogbookReminderScheduler].
 *
 * Re-reads the checkpoint from [dao] at fire time rather than trusting the
 * input data's snapshot — the checkpoint may have been completed or the
 * whole process deleted since the reminder was armed (WorkManager makes no
 * promise the request gets cancelled *before* an in-flight execution
 * starts), so a stale checkpoint or a since-completed one is a silent
 * no-op [Result.success], not a spurious notification.
 *
 * `@HiltWorker` requires [androidx.hilt.work.HiltWorkerFactory] to be wired
 * as `GadgetApplication`'s `Configuration.workerFactory` — already true at
 * the `:app` level (no changes needed there); this is simply the first
 * class in the modular codebase that actually implements a
 * [CoroutineWorker].
 */
@HiltWorker
class LogbookReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: LogbookDao,
    private val channels: NotificationChannelRegistry,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val checkpointId = inputData.getLong(KEY_CHECKPOINT_ID, NO_CHECKPOINT_ID)
        if (checkpointId == NO_CHECKPOINT_ID) return Result.failure()

        val checkpoint = dao.getCheckpoint(checkpointId) ?: return Result.success()
        if (checkpoint.completed) return Result.success()
        val processName = dao.getProcess(checkpoint.processId)?.name
            ?: applicationContext.getString(R.string.logbook_notification_process_fallback)

        channels.ensure(reminderChannelSpec())

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logbook_reminder)
            .setContentTitle(
                applicationContext.getString(R.string.logbook_notification_title, checkpoint.name),
            )
            .setContentText(
                applicationContext.getString(R.string.logbook_notification_body, processName),
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent())
            .build()

        // NotificationManagerCompat.notify() is a documented safe no-op when
        // POST_NOTIFICATIONS isn't granted on API 33+ (unlike most runtime
        // permissions it does not throw) — the checkpoint itself still shows
        // as due/overdue in-app either way, so a missing grant degrades
        // gracefully rather than failing the work.
        NotificationManagerCompat.from(applicationContext).notify(checkpointId.toInt(), notification)
        return Result.success()
    }

    /** Tap target: the launcher activity (resolved by package rather than
     *  by class — feature modules never reference `:app`'s `MainActivity`
     *  directly) with the shared [GadgetDestination.EXTRA_ROUTE] extra so
     *  `MainActivity` navigates straight to the Logbook screen. */
    private fun tapPendingIntent(): PendingIntent {
        val launchIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(GadgetDestination.EXTRA_ROUTE, GadgetDestination.Logbook.route)
            }
            ?: Intent(Intent.ACTION_MAIN).setPackage(applicationContext.packageName)
        return PendingIntent.getActivity(
            applicationContext,
            REMINDER_REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun reminderChannelSpec() = ChannelSpec(
        id = CHANNEL_ID,
        displayName = applicationContext.getString(R.string.logbook_notification_channel_name),
        description = applicationContext.getString(R.string.logbook_notification_channel_description),
        importance = ChannelSpec.Importance.Default,
        silent = false,
    )

    companion object {
        const val KEY_CHECKPOINT_ID: String = "checkpoint_id"
        const val CHANNEL_ID: String = "logbook_checkpoint_reminder"
        private const val NO_CHECKPOINT_ID: Long = -1L
        private const val REMINDER_REQUEST_CODE: Int = 0
    }
}
