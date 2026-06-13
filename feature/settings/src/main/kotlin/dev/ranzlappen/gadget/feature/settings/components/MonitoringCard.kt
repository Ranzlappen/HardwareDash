package dev.ranzlappen.gadget.feature.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ranzlappen.gadget.core.ui.component.DashCard

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
        title = "Monitoring",
        icon = Icons.Outlined.Notifications,
    ) {
        SettingsToggleRow(
            title = "Notification controls",
            subtitle = "Show a 'Stop' action on each monitoring notification",
            checked = notificationActionsEnabled,
            onCheckedChange = onNotificationActionsEnabledChange,
        )
    }
}
