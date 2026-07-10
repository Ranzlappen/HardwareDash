package dev.ranzlappen.gadget.feature.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.Immutable
import dev.ranzlappen.gadget.core.notifications.ChannelSpec

/**
 * The three importance tiers the in-app test-notification builder offers.
 *
 * Android 8+ fixes a channel's importance at creation time — a single
 * notification cannot carry its own importance independent of the channel it
 * posts to — so "pick an importance" in the builder really means "post to
 * the matching pre-created channel". [channelSpec] describes that channel
 * for [dev.ranzlappen.gadget.core.notifications.NotificationChannelRegistry.ensure],
 * the app-wide idempotent-creation seam — never hand-rolled
 * `createNotificationChannel` calls here.
 */
@Immutable
enum class NotificationImportance(
    val channelId: String,
    val osImportance: Int,
) {
    Low(channelId = "notification_builder_low", osImportance = NotificationManager.IMPORTANCE_LOW),
    Default(channelId = "notification_builder_default", osImportance = NotificationManager.IMPORTANCE_DEFAULT),
    High(channelId = "notification_builder_high", osImportance = NotificationManager.IMPORTANCE_HIGH);

    private fun toChannelSpecImportance(): ChannelSpec.Importance = when (this) {
        Low -> ChannelSpec.Importance.Low
        Default -> ChannelSpec.Importance.Default
        High -> ChannelSpec.Importance.High
    }

    /** The [ChannelSpec] for this tier's test channel, ready for
     *  [dev.ranzlappen.gadget.core.notifications.NotificationChannelRegistry.ensure]. Not silent —
     *  the whole point of the picker is to demonstrate each importance
     *  tier's real heads-up / sound behaviour. */
    fun channelSpec(context: Context): ChannelSpec = ChannelSpec(
        id = channelId,
        displayName = context.getString(
            when (this) {
                Low -> R.string.notification_channel_low_name
                Default -> R.string.notification_channel_default_name
                High -> R.string.notification_channel_high_name
            },
        ),
        description = context.getString(R.string.notification_channel_description),
        importance = toChannelSpecImportance(),
        silent = false,
    )
}

/** A single row in the channel-inspector card — the live-read projection of
 *  an [NotificationChannel] (or the app's own, for pre-O devices where the
 *  channel API doesn't exist and the standard flavor has nothing to list). */
@Immutable
data class NotificationChannelSummary(
    val id: String,
    val name: String,
    val importance: Int,
)

/** Human-readable label for a raw `NotificationManager.IMPORTANCE_*` int —
 *  shared by the channel inspector and the sticky-override result copy. */
fun Int.toImportanceLabel(): String = when (this) {
    NotificationManager.IMPORTANCE_NONE -> "None"
    NotificationManager.IMPORTANCE_MIN -> "Min"
    NotificationManager.IMPORTANCE_LOW -> "Low"
    NotificationManager.IMPORTANCE_DEFAULT -> "Default"
    NotificationManager.IMPORTANCE_HIGH -> "High"
    NotificationManager.IMPORTANCE_MAX -> "Max"
    else -> "Unspecified"
}

/** Read every channel the app owns, for the channel-inspector card. */
fun NotificationManager.readChannelSummaries(): List<NotificationChannelSummary> =
    notificationChannels.map { NotificationChannelSummary(it.id, it.name.toString(), it.importance) }
