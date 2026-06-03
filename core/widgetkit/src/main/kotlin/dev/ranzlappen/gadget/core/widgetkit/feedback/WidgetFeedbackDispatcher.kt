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
 *  - `{state}` — resolved from the tap's [WidgetFeedbackState]: `"on"`/`"off"`
 *    for a toggle, `"triggered"` for a momentary function, or the failure
 *    reason when the action failed.
 *
 * The dispatcher's notification path needs `POST_NOTIFICATIONS` on
 * API 33+. The caller (UI configuration sheet) is responsible for
 * gating the Notification variant behind a permission request; if
 * the runtime permission is missing we still build the notification
 * but [NotificationManagerCompat.notify] silently drops it (the
 * permission check happens inside the compat layer).
 *
 * One app-wide singleton serves every widget-bearing feature: each
 * feature contributes its [WidgetFeedbackConfig] into a
 * `Map<String, WidgetFeedbackConfig>` multibinding keyed by its stable
 * feature id, and the provider passes that id to [dispatch] so the
 * right channel / small icon / notification-id base is used.
 */
@Singleton
class WidgetFeedbackDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configs: Map<String, @JvmSuppressWildcards WidgetFeedbackConfig>,
    private val channelRegistry: NotificationChannelRegistry,
) {
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private fun channelSpec(config: WidgetFeedbackConfig) =
        ChannelSpec(
            id = config.channelId,
            displayName = config.channelName,
            description = config.channelDescription,
            importance = ChannelSpec.Importance.Low,
            silent = true,
        )

    /**
     * Fire the feedback for a widget tap.
     *
     * @param displayName the widget's user-facing label (for `{name}`).
     * @param state the tap outcome (for `{state}` — see [WidgetFeedbackState]).
     * @param feedback the configured feedback variant.
     * @param featureId selects the calling feature's [WidgetFeedbackConfig].
     */
    fun dispatch(displayName: String, state: WidgetFeedbackState, feedback: ToggleFeedback, featureId: String) {
        when (feedback) {
            ToggleFeedback.None -> Unit
            is ToggleFeedback.Toast ->
                Toast.makeText(
                    context,
                    render(feedback.template, displayName, state),
                    Toast.LENGTH_SHORT,
                ).show()
            is ToggleFeedback.Notification -> postNotification(
                title = render(feedback.titleTemplate, displayName, state),
                body = render(feedback.bodyTemplate, displayName, state),
                config = requireNotNull(configs[featureId]) {
                    "No WidgetFeedbackConfig bound for feature id '$featureId' — bind one " +
                        "@IntoMap @StringKey(\"$featureId\") in the feature's Hilt module."
                },
            )
        }
    }

    private fun render(template: String, name: String, state: WidgetFeedbackState): String =
        template
            .replace("{name}", name)
            .replace("{state}", state.literal())

    private fun WidgetFeedbackState.literal(): String = when (this) {
        is WidgetFeedbackState.Toggle -> if (active) ON_LITERAL else OFF_LITERAL
        WidgetFeedbackState.Triggered -> TRIGGERED_LITERAL
        is WidgetFeedbackState.Failed -> reason
    }

    private fun postNotification(title: String, body: String, config: WidgetFeedbackConfig) {
        // Idempotent — safe to ensure the channel on every post (the
        // registry no-ops if it already exists, preserving user overrides).
        channelRegistry.ensure(channelSpec(config))
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
        private const val TRIGGERED_LITERAL = "triggered"
    }
}
