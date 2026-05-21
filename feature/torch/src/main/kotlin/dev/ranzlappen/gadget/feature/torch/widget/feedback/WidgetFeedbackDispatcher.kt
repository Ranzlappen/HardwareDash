package dev.ranzlappen.gadget.feature.torch.widget.feedback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.widget.customization.ToggleFeedback
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispatches the optional confirmation surface configured on a
 * widget's [dev.ranzlappen.gadget.feature.torch.widget.customization
 * .WidgetAppearance.feedback] when the widget's toggle action fires.
 *
 * Variants:
 * - [ToggleFeedback.None] — no-op.
 * - [ToggleFeedback.Toast] — short toast on the home screen.
 * - [ToggleFeedback.Notification] — posted on a low-importance
 *   "Widget feedback" channel; auto-cancels after 3 s via
 *   [NotificationCompat.Builder.setTimeoutAfter].
 *
 * Placeholder grammar inside templates (documented in
 * [ToggleFeedback]):
 *  - `{name}` — the widget's display name.
 *  - `{state}` — `"on"` or `"off"` after the toggle.
 *
 * The dispatcher's notification path needs `POST_NOTIFICATIONS` on
 * API 33+. The caller (UI configuration sheet) is responsible for
 * gating the Notification variant behind a permission request; if
 * the runtime permission is missing we still build the notification
 * but [NotificationManagerCompat.notify] silently drops it (the
 * permission check happens inside the compat layer).
 */
@Singleton
class WidgetFeedbackDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context).also { ensureChannel() }
    }

    /**
     * Fire the feedback for a widget toggle.
     *
     * @param displayName the widget's user-facing label (for `{name}`).
     * @param newState the post-toggle state (for `{state}` → on/off).
     * @param feedback the configured feedback variant.
     */
    fun dispatch(displayName: String, newState: Boolean, feedback: ToggleFeedback) {
        when (feedback) {
            ToggleFeedback.None -> Unit
            is ToggleFeedback.Toast ->
                Toast.makeText(
                    context,
                    render(feedback.template, displayName, newState),
                    Toast.LENGTH_SHORT,
                ).show()
            is ToggleFeedback.Notification -> postNotification(
                title = render(feedback.titleTemplate, displayName, newState),
                body = render(feedback.bodyTemplate, displayName, newState),
            )
        }
    }

    private fun render(template: String, name: String, state: Boolean): String =
        template
            .replace("{name}", name)
            .replace(
                "{state}",
                if (state) ON_LITERAL else OFF_LITERAL,
            )

    private fun postNotification(title: String, body: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_strobe)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setSilent(true)
            .setAutoCancel(true)
            .setTimeoutAfter(AUTO_CANCEL_MS)
            .build()
        // Permission gate is internal to NotificationManagerCompat —
        // pre-API-33 always allowed, API 33+ gated on
        // POST_NOTIFICATIONS. A silent drop when denied is the
        // correct behaviour for an optional feedback surface.
        runCatching {
            notificationManager.notify(NOTIFICATION_ID_BASE + (title.hashCode() and 0xFFFF), notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val sysManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (sysManager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.widget_feedback_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.widget_feedback_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
        sysManager.createNotificationChannel(channel)
    }

    companion object {
        /** Channel ID — stable across app versions. Created lazily
         *  the first time the dispatcher posts a notification. */
        const val CHANNEL_ID = "widget_feedback"

        /** Hash-derived notification IDs all sit in this range. */
        private const val NOTIFICATION_ID_BASE = 0x57_46_00_00 // "WF" prefix

        /** Auto-cancel after 3 s. */
        private const val AUTO_CANCEL_MS: Long = 3_000L

        private const val ON_LITERAL = "on"
        private const val OFF_LITERAL = "off"
    }
}
