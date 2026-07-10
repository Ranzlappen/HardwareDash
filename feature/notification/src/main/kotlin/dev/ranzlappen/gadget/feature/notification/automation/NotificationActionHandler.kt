package dev.ranzlappen.gadget.feature.notification.automation

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.core.notifications.NotificationChannelRegistry
import dev.ranzlappen.gadget.feature.notification.NotificationImportance
import dev.ranzlappen.gadget.feature.notification.R
import dev.ranzlappen.gadget.feature.notification.control.LockScreenOverlayConfig
import dev.ranzlappen.gadget.feature.notification.control.NotificationController
import dev.ranzlappen.gadget.feature.notification.control.NotificationControllerResult
import dev.ranzlappen.gadget.feature.notification.control.StickyOverrideConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification's invocable-action surface for automation — the reference
 * shape is `TorchActionHandler` / `GpsActionHandler`.
 *
 * `post_test_notification` and `cancel_test_notification` are standard-flavor,
 * no-root actions that post through plain `NotificationManager` directly
 * (mirrors the screen's builder card). `assert_channel_importance` is a
 * read-only check against the live channel state (mirrors `LockActionHandler`'s
 * assert pattern) — it works on both flavors, it just only ever *passes* after
 * a successful override. The remaining four actions dispatch through the
 * injected [NotificationController] — the standard/rooted seam bound
 * per-flavor in `:app` (`RootBindings`); standard flavor's controller returns
 * [NotificationControllerResult.Unsupported] for all of them, so those four
 * carry `requiresRoot = true`. Never branches on `BuildConfig.IS_ROOTED`.
 */
@Singleton
class NotificationActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: NotificationController,
    private val channelRegistry: NotificationChannelRegistry,
) : ActionHandler {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_POST_TEST_NOTIFICATION,
            label = context.getString(R.string.notification_action_post_test),
            params = listOf(
                ActionParam(PARAM_TITLE, ActionParamType.Text, DEFAULT_TITLE),
                ActionParam(PARAM_BODY, ActionParamType.Text, DEFAULT_BODY),
                ActionParam(PARAM_IMPORTANCE, ActionParamType.Text, NotificationImportance.Default.name),
            ),
        ),
        ModuleAction(
            key = ACTION_CANCEL_TEST_NOTIFICATION,
            label = context.getString(R.string.notification_action_cancel_test),
        ),
        ModuleAction(
            key = ACTION_ASSERT_CHANNEL_IMPORTANCE,
            label = context.getString(R.string.notification_action_assert_importance),
            params = listOf(
                ActionParam(PARAM_CHANNEL_ID, ActionParamType.Text, ""),
                ActionParam(PARAM_IMPORTANCE, ActionParamType.Text, NotificationImportance.High.name),
            ),
        ),
        ModuleAction(
            key = ACTION_STICKY_OVERRIDE,
            label = context.getString(R.string.notification_action_sticky_override),
            requiresRoot = true,
            params = listOf(ActionParam(PARAM_CHANNEL_ID, ActionParamType.Text, "")),
        ),
        ModuleAction(
            key = ACTION_GRANT_LISTENER,
            label = context.getString(R.string.notification_action_grant_listener),
            requiresRoot = true,
        ),
        ModuleAction(
            key = ACTION_SHOW_OVERLAY,
            label = context.getString(R.string.notification_action_show_overlay),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_MESSAGE, ActionParamType.Text, DEFAULT_OVERLAY_MESSAGE),
                ActionParam(
                    name = PARAM_DURATION_MS,
                    type = ActionParamType.Int,
                    default = DEFAULT_OVERLAY_DURATION_MS.toString(),
                    min = MIN_OVERLAY_DURATION_MS.toFloat(),
                    max = MAX_OVERLAY_DURATION_MS.toFloat(),
                ),
            ),
        ),
        ModuleAction(
            key = ACTION_RESET_ALL,
            label = context.getString(R.string.notification_action_reset_all),
            requiresRoot = true,
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_POST_TEST_NOTIFICATION -> postTestNotification(params)
            ACTION_CANCEL_TEST_NOTIFICATION -> {
                notificationManager.cancel(TEST_NOTIFICATION_ID)
                ActionResult.Success
            }
            ACTION_ASSERT_CHANNEL_IMPORTANCE -> assertChannelImportance(params)
            ACTION_STICKY_OVERRIDE -> {
                val channelId = params[PARAM_CHANNEL_ID]?.trim()
                if (channelId.isNullOrEmpty()) {
                    ActionResult.Failure("channel id required")
                } else {
                    controller.overrideStickyChannel(StickyOverrideConfig(channelId)).toActionResult()
                }
            }
            ACTION_GRANT_LISTENER -> controller.grantListenerAccess().toActionResult()
            ACTION_SHOW_OVERLAY -> {
                val message = params[PARAM_MESSAGE]?.takeIf { it.isNotBlank() } ?: DEFAULT_OVERLAY_MESSAGE
                val duration = params[PARAM_DURATION_MS]?.toLongOrNull() ?: DEFAULT_OVERLAY_DURATION_MS
                controller.showLockScreenOverlay(
                    LockScreenOverlayConfig(message = message, durationMillis = duration),
                ).toActionResult()
            }
            ACTION_RESET_ALL -> controller.resetAllNotificationMutations().toActionResult()
            else -> ActionResult.Unsupported
        }

    private fun postTestNotification(params: Map<String, String>): ActionResult {
        val importance = params[PARAM_IMPORTANCE]
            ?.let { name -> NotificationImportance.entries.firstOrNull { it.name == name } }
            ?: NotificationImportance.Default
        channelRegistry.ensure(importance.channelSpec(context))
        val notification = NotificationCompat.Builder(context, importance.channelId)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(params[PARAM_TITLE]?.ifBlank { DEFAULT_TITLE } ?: DEFAULT_TITLE)
            .setContentText(params[PARAM_BODY]?.ifBlank { DEFAULT_BODY } ?: DEFAULT_BODY)
            .setAutoCancel(true)
            .build()
        return runCatching { notificationManager.notify(TEST_NOTIFICATION_ID, notification) }
            .fold(
                onSuccess = { ActionResult.Success },
                onFailure = { ActionResult.Failure(it.message ?: "notify() rejected") },
            )
    }

    /** Read-only assert: does the given channel's *live* importance match the
     *  expected tier? Mirrors `LockActionHandler`'s assert pattern — a plain
     *  system-service read, not a controller call, so it works identically on
     *  both flavors (it's only ever expected to *pass* after a rooted
     *  sticky-override run, but nothing about the check itself needs root). */
    private fun assertChannelImportance(params: Map<String, String>): ActionResult {
        val channelId = params[PARAM_CHANNEL_ID]?.trim()
        if (channelId.isNullOrEmpty()) return ActionResult.Failure("channel id required")
        val expected = params[PARAM_IMPORTANCE]
            ?.let { name -> NotificationImportance.entries.firstOrNull { it.name == name } }
            ?: NotificationImportance.High
        val channel = notificationManager.getNotificationChannel(channelId)
            ?: return ActionResult.Failure("no such channel: $channelId")
        return if (channel.importance == expected.osImportance) {
            ActionResult.Success
        } else {
            ActionResult.Failure(
                "channel importance is ${channel.importance}, expected ${expected.osImportance}",
            )
        }
    }

    private fun NotificationControllerResult.toActionResult(): ActionResult = when (this) {
        is NotificationControllerResult.Ok -> ActionResult.Success
        NotificationControllerResult.Unsupported -> ActionResult.Failure("requires the rooted app version")
        is NotificationControllerResult.RateLimited -> ActionResult.Failure("rate-limited; retry in ${retryAfterMillis}ms")
        NotificationControllerResult.OptedOut -> ActionResult.Failure("turned off in Settings")
        is NotificationControllerResult.HardwareError -> ActionResult.Failure(message)
        is NotificationControllerResult.ResetCompleted -> ActionResult.Success
        is NotificationControllerResult.ChannelImportanceSnapshot -> ActionResult.Success
    }

    companion object {
        const val FEATURE_ID = "notification"
        const val ACTION_POST_TEST_NOTIFICATION = "notification_post_test"
        const val ACTION_CANCEL_TEST_NOTIFICATION = "notification_cancel_test"
        const val ACTION_ASSERT_CHANNEL_IMPORTANCE = "notification_assert_channel_importance"
        const val ACTION_STICKY_OVERRIDE = "notification_sticky_override"
        const val ACTION_GRANT_LISTENER = "notification_grant_listener"
        const val ACTION_SHOW_OVERLAY = "notification_show_overlay"
        const val ACTION_RESET_ALL = "notification_reset_all"

        const val PARAM_TITLE = "title"
        const val PARAM_BODY = "body"
        const val PARAM_IMPORTANCE = "importance"
        const val PARAM_CHANNEL_ID = "channel_id"
        const val PARAM_MESSAGE = "message"
        const val PARAM_DURATION_MS = "duration_ms"

        const val DEFAULT_TITLE = "Test notification"
        const val DEFAULT_BODY = "Posted from the Notifications module automation action."
        const val DEFAULT_OVERLAY_MESSAGE = "Rooted lock-screen overlay test"
        const val DEFAULT_OVERLAY_DURATION_MS = 5_000L
        const val MIN_OVERLAY_DURATION_MS = 1_000L
        const val MAX_OVERLAY_DURATION_MS = 60_000L

        /** Stable id for the builder's own test notification — matches
         *  `NotificationViewModel`'s constant so an automation-posted test
         *  notification and an in-screen-posted one share the same slot. */
        const val TEST_NOTIFICATION_ID = 0x4E4F5449 // "NOTI"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface NotificationActionModule {

    @Binds
    @IntoMap
    @StringKey(NotificationActionHandler.FEATURE_ID)
    fun bindNotificationActionHandler(handler: NotificationActionHandler): ActionHandler
}
