package dev.ranzlappen.gadget.feature.notification.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusDot
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.feature.notification.NotificationChannelSummary
import dev.ranzlappen.gadget.feature.notification.NotificationImportance
import dev.ranzlappen.gadget.feature.notification.R
import dev.ranzlappen.gadget.feature.notification.toImportanceLabel

/**
 * Standard-flavor builder card — title / body / importance chips + post /
 * cancel. No root required; posts through plain `NotificationManager`.
 */
@Composable
internal fun NotificationBuilderCard(
    title: String,
    body: String,
    importance: NotificationImportance,
    hasPostedNotification: Boolean,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onImportanceChange: (NotificationImportance) -> Unit,
    onPost: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.notification_builder_card_title),
        icon = Icons.Outlined.Notifications,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            GadgetTextField(
                value = title,
                onValueChange = onTitleChange,
                label = stringResource(R.string.notification_builder_title_label),
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetTextField(
                value = body,
                onValueChange = onBodyChange,
                label = stringResource(R.string.notification_builder_body_label),
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.notification_builder_importance_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                GadgetChip(
                    selected = importance == NotificationImportance.Low,
                    onClick = { onImportanceChange(NotificationImportance.Low) },
                    label = stringResource(R.string.notification_builder_importance_low),
                )
                GadgetChip(
                    selected = importance == NotificationImportance.Default,
                    onClick = { onImportanceChange(NotificationImportance.Default) },
                    label = stringResource(R.string.notification_builder_importance_default),
                )
                GadgetChip(
                    selected = importance == NotificationImportance.High,
                    onClick = { onImportanceChange(NotificationImportance.High) },
                    label = stringResource(R.string.notification_builder_importance_high),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                GadgetPrimaryButton(
                    onClick = onPost,
                    text = stringResource(R.string.notification_builder_post),
                    modifier = Modifier.weight(1f),
                )
                GadgetSecondaryButton(
                    onClick = onCancel,
                    text = stringResource(R.string.notification_builder_cancel),
                    enabled = hasPostedNotification,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Standard-flavor channel inspector — every channel this app owns, read
 *  live via `NotificationManager.getNotificationChannels()`. */
@Composable
internal fun ChannelInspectorCard(
    channels: List<NotificationChannelSummary>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.notification_channel_inspector_card_title),
        icon = Icons.Outlined.Tune,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            if (channels.isEmpty()) {
                GadgetEmptyState(
                    title = stringResource(R.string.notification_channel_inspector_empty),
                    icon = Icons.Outlined.NotificationsNone,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                channels.forEach { channel ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.notification_channel_importance_row,
                                channel.name.ifBlank { channel.id },
                                channel.importance.toImportanceLabel(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            GadgetTertiaryButton(
                onClick = onRefresh,
                text = stringResource(R.string.notification_channel_inspector_refresh),
            )
        }
    }
}

/** Rooted-only sticky-channel-importance override picker. */
@Composable
internal fun StickyOverrideCard(
    channelId: String,
    onChannelIdChange: (String) -> Unit,
    onOverrideRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.notification_sticky_override_card_title),
        icon = Icons.Outlined.SettingsBackupRestore,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                text = stringResource(R.string.notification_sticky_override_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GadgetTextField(
                value = channelId,
                onValueChange = onChannelIdChange,
                label = stringResource(R.string.notification_sticky_override_channel_id_label),
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetPrimaryButton(
                onClick = onOverrideRequest,
                text = stringResource(R.string.notification_sticky_override_button),
                enabled = channelId.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Rooted-only programmatic listener-access grant. */
@Composable
internal fun ListenerAccessCard(
    listenerConnected: Boolean,
    onGrantRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val statusLabel = stringResource(
        if (listenerConnected) {
            R.string.notification_listener_access_status_granted
        } else {
            R.string.notification_listener_access_status_not_granted
        },
    )
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.notification_listener_access_card_title),
        icon = Icons.Outlined.VisibilityOff,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                text = stringResource(R.string.notification_listener_access_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                GadgetStatusDot(
                    contentDescription = statusLabel,
                    color = if (listenerConnected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Text(text = statusLabel, style = MaterialTheme.typography.bodyMedium)
            }
            GadgetPrimaryButton(
                onClick = onGrantRequest,
                text = stringResource(R.string.notification_listener_access_button),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Rooted-only lock-screen overlay test panel: message + duration slider. */
@Composable
internal fun LockScreenOverlayCard(
    message: String,
    durationMillis: Long,
    onMessageChange: (String) -> Unit,
    onDurationChange: (Long) -> Unit,
    onShowOverlayRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.notification_overlay_card_title),
        icon = Icons.Outlined.Lock,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                text = stringResource(R.string.notification_overlay_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GadgetTextField(
                value = message,
                onValueChange = onMessageChange,
                label = stringResource(R.string.notification_overlay_message_label),
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetSlider(
                value = durationMillis / MILLIS_PER_SECOND,
                onValueChange = { onDurationChange((it * MILLIS_PER_SECOND).toLong()) },
                valueRange = MIN_OVERLAY_SECONDS..MAX_OVERLAY_SECONDS,
                label = stringResource(R.string.notification_overlay_duration_label),
                suffix = "s",
                steps = MAX_OVERLAY_SECONDS.toInt() - MIN_OVERLAY_SECONDS.toInt() - 1,
            )
            GadgetPrimaryButton(
                onClick = onShowOverlayRequest,
                text = stringResource(R.string.notification_overlay_button),
                enabled = message.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Rooted-only "revert everything" button. */
@Composable
internal fun ResetAllCard(
    onResetAllRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.notification_reset_card_title),
        icon = Icons.Outlined.RestartAlt,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                text = stringResource(R.string.notification_reset_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GadgetSecondaryButton(
                onClick = onResetAllRequest,
                text = stringResource(R.string.notification_reset_button),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Overlay duration slider bounds — mirrors the rooted helper's hard 60 s
 *  active-window ceiling (`LOCK_SCREEN_OVERLAY_HARD_CEILING_MILLIS` in
 *  `:feature:notification-rooted`'s `LockScreenOverlayHelper`); the helper is
 *  the actual enforcement point, this is only the picker's range. */
private const val MIN_OVERLAY_SECONDS = 1f
private const val MAX_OVERLAY_SECONDS = 60f
private const val MILLIS_PER_SECOND = 1000f
