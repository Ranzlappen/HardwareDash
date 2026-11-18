package dev.ranzlappen.gadget.feature.dashboard

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.navigation.GadgetDestination
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.CompactCard
import dev.ranzlappen.gadget.core.ui.component.GadgetCircleControl
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Home (v3 B3, MASTER-PLAN §5.1). Brings back v1's "useful without tapping
 * anything" dashboard:
 *
 * - **Status** — battery, storage, Wi-Fi and the latest logbook entry, live;
 *   each card opens its tool.
 * - **Quick actions** — torch on/off and a short test buzz.
 * - **Pinned tools** — the user's pinned destinations, edited in
 *   [DashboardEditorSheet].
 *
 * Browsing every tool is the section hubs' job (bottom bar), so Home no
 * longer lists the whole catalog.
 */
@Composable
fun DashboardScreen(
    onNavigate: (GadgetDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    var showEditor by rememberSaveable { mutableStateOf(false) }

    DashboardScreenContent(
        status = status,
        pinned = state.pinned,
        onNavigate = onNavigate,
        onToggleTorch = viewModel::toggleTorch,
        onBuzz = viewModel::buzz,
        onEdit = { showEditor = true },
        modifier = modifier,
    )

    if (showEditor) {
        DashboardEditorSheet(
            entries = state.entries,
            onMoveUp = viewModel::moveUp,
            onMoveDown = viewModel::moveDown,
            onSetHidden = viewModel::setHidden,
            onSetPinned = viewModel::setPinned,
            onReset = viewModel::resetLayout,
            onDismiss = { showEditor = false },
        )
    }
}

@Composable
internal fun DashboardScreenContent(
    status: HomeStatus,
    pinned: List<DashboardEntry>,
    onNavigate: (GadgetDestination) -> Unit,
    onToggleTorch: () -> Unit,
    onBuzz: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    ModuleScreenScaffold(
        title = stringResource(R.string.dashboard_title),
        modifier = modifier,
        functional = {
            HomeSectionLabel(stringResource(R.string.home_status))
            Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                StatusCards(status = status, onNavigate = onNavigate)
            }

            HomeSectionLabel(stringResource(R.string.home_quick_actions))
            QuickActions(status = status, onToggleTorch = onToggleTorch, onBuzz = onBuzz)

            HomeSectionLabel(stringResource(R.string.home_pinned))
            Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                if (pinned.isEmpty()) {
                    Text(
                        text = stringResource(R.string.home_pinned_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    pinned.forEach { entry ->
                        CompactCard(
                            modifier = Modifier.fillMaxWidth(),
                            title = entry.destination.label,
                            leadingIcon = entry.destination.iconOutlined,
                            onClick = { onNavigate(entry.destination) },
                        )
                    }
                }
                GadgetSecondaryButton(
                    onClick = onEdit,
                    text = stringResource(R.string.dashboard_edit),
                    leadingIcon = Icons.Outlined.Edit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun HomeSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StatusCards(
    status: HomeStatus,
    onNavigate: (GadgetDestination) -> Unit,
) {
    val unknown = stringResource(R.string.home_unknown)

    val battery = when {
        status.batteryPercent != null && status.batteryTemperatureC != null ->
            stringResource(R.string.home_battery_value, status.batteryPercent, status.batteryTemperatureC)
        status.batteryPercent != null -> stringResource(R.string.home_battery_level_only, status.batteryPercent)
        else -> unknown
    }
    CompactCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.home_battery),
        subtitle = battery,
        leadingIcon = Icons.Outlined.BatteryFull,
        onClick = { onNavigate(GadgetDestination.Battery) },
    )

    val storage = if (status.storageUsedPercent != null && status.storageFreeGb != null) {
        stringResource(R.string.home_storage_value, status.storageUsedPercent, status.storageFreeGb)
    } else {
        unknown
    }
    CompactCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.home_storage),
        subtitle = storage,
        leadingIcon = Icons.Outlined.Storage,
        onClick = { onNavigate(GadgetDestination.Storage) },
    )

    val wifi = when {
        status.wifiEnabled == null -> unknown
        !status.wifiEnabled -> stringResource(R.string.home_wifi_off)
        status.wifiRssiDbm != null -> stringResource(R.string.home_wifi_connected, status.wifiRssiDbm)
        else -> stringResource(R.string.home_wifi_on)
    }
    CompactCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.home_wifi),
        subtitle = wifi,
        leadingIcon = if (status.wifiEnabled == false) Icons.Outlined.WifiOff else Icons.Outlined.Wifi,
        onClick = { onNavigate(GadgetDestination.RadiosWifi) },
    )

    val log = status.lastLog?.let { entry ->
        val ago = DateUtils.getRelativeTimeSpanString(
            entry.timestampMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
        stringResource(R.string.home_logbook_value, entry.text, ago)
    } ?: stringResource(R.string.home_logbook_empty)
    CompactCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.home_logbook),
        subtitle = log,
        leadingIcon = Icons.Outlined.Checklist,
        onClick = { onNavigate(GadgetDestination.Logbook) },
    )
}

@Composable
private fun QuickActions(
    status: HomeStatus,
    onToggleTorch: () -> Unit,
    onBuzz: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.large)) {
        val torchOn = status.torchOn == true
        GadgetCircleControl(
            icon = if (torchOn) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff,
            contentDescription = stringResource(
                if (torchOn) R.string.home_torch_off_cd else R.string.home_torch_on_cd,
            ),
            caption = stringResource(R.string.home_torch),
            enabled = status.torchOn != null,
            active = torchOn,
            onClick = onToggleTorch,
        )
        GadgetCircleControl(
            icon = Icons.Outlined.Vibration,
            contentDescription = stringResource(R.string.home_buzz_cd),
            caption = stringResource(R.string.home_buzz),
            enabled = status.canBuzz,
            onClick = onBuzz,
        )
    }
}

@GadgetPreviewLightDark
@Composable
private fun DashboardScreenPreview() = GadgetThemedPreview {
    DashboardScreenContent(
        status = HomeStatus(
            batteryPercent = 87,
            batteryTemperatureC = 31.4f,
            storageUsedPercent = 42,
            storageFreeGb = 120.5f,
            wifiEnabled = true,
            wifiRssiDbm = -58,
            torchOn = false,
            canBuzz = true,
            lastLog = LastLogEntry("Swapped the router", System.currentTimeMillis() - 2 * 60 * 60 * 1000L),
        ),
        pinned = listOf(DashboardEntry(GadgetDestination.Torch, hidden = false, pinned = true)),
        onNavigate = {},
        onToggleTorch = {},
        onBuzz = {},
        onEdit = {},
    )
}
