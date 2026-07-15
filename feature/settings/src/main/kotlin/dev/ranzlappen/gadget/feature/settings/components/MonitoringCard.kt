package dev.ranzlappen.gadget.feature.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.feature.settings.R

/**
 * Settings card for global monitoring behaviour.
 *
 * Currently exposes one toggle: whether per-metric notifications include a
 * "Stop monitoring" action button. Backed by [MonitorGlobalPrefs] via the
 * parent [SettingsViewModel].
 */
@Composable
internal fun MonitoringCard(
    notificationActionsEnabled: Boolean,
    onNotificationActionsEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.settings_monitoring_title),
        icon = Icons.Outlined.Notifications,
    ) {
        SettingsToggleRow(
            title = stringResource(R.string.settings_notification_controls),
            subtitle = stringResource(R.string.settings_notification_controls_subtitle),
            checked = notificationActionsEnabled,
            onCheckedChange = onNotificationActionsEnabledChange,
        )
    }
}
