package dev.ranzlappen.gadget.core.widgetkit.feedback

import android.content.Context
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.notifications.ChannelSpec
import dev.ranzlappen.gadget.core.notifications.NotificationChannelRegistry
import dev.ranzlappen.gadget.core.widgetkit.config.ToggleFeedback
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispatches the optional confirmation surface configured on a
 * widget's [dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance.feedback]
 * when the widget's toggle action fires.
 *
 * Variants:
 * - [ToggleFeedback.None] — no-op.
 * - [ToggleFeedback.Toast] — short toast on the home screen.
 * - [ToggleFeedback.Notification] — posted on a low-importance feedback
 *   channel (id / display name / description / small icon all come from
 *   the per-feature [WidgetFeedbackConfig]); auto-cancels after 3 s via
 *   [NotificationCompat.Builder.setTimeoutAfter].
 *
 * Placeholder grammar inside templates (documented in [ToggleFeedback]):
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
    private val config: WidgetFeedbackConfig,
    private val channelRegistry: NotificationChannelRegistry,
) {
    private val channelSpec by lazy {
        ChannelSpec(
            id = config.channelId,
            displayName = config.channelName,
            description = config.channelDescription,
            importance = ChannelSpec.Importance.Low,
            silent = true,
        )
    }
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context).also { channelRegistry.ensure(channelSpec) }
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
        val notification = NotificationCompat.Builder(context, config.channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(config.smallIcon)
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
            notificationManager.notify(config.notificationIdBase + (title.hashCode() and 0xFFFF), notification)
        }
    }

    companion object {
        /** Auto-cancel after 3 s. */
        private const val AUTO_CANCEL_MS: Long = 3_000L

        private const val ON_LITERAL = "on"
        private const val OFF_LITERAL = "off"
    }
}
