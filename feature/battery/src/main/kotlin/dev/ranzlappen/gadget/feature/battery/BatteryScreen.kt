package dev.ranzlappen.gadget.feature.battery

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.monitoring.LiveMonitorContainer
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

@Composable
fun BatteryScreen(
    modifier: Modifier = Modifier,
    viewModel: BatteryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    BatteryScreenContent(
        state = state,
        moduleInfo = batteryModuleInfo(state),
        modifier = modifier,
        liveMonitors = {
            LiveMonitorContainer(
                metricKey = BatteryLevelMetricSource.METRIC_KEY,
                title = stringResource(R.string.battery_live_monitor_level),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "battery_live_level",
            )
            LiveMonitorContainer(
                metricKey = BatteryTemperatureMetricSource.METRIC_KEY,
                title = stringResource(R.string.battery_live_monitor_temp),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "battery_live_temp",
            )
        },
        monitors = {
            MonitorContainer(
                metricKey = BatteryLevelMetricSource.METRIC_KEY,
                title = stringResource(R.string.battery_monitor_level),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "battery_history_level",
            )
            MonitorContainer(
                metricKey = BatteryTemperatureMetricSource.METRIC_KEY,
                title = stringResource(R.string.battery_monitor_temp),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "battery_history_temp",
            )
        },
    )
}

@Composable
private fun batteryModuleInfo(state: BatteryState): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 21),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.battery_cap_level_name),
            detail = stringResource(R.string.battery_cap_level_detail),
            status = {
                CapabilityStatus(
                    kind = if (state.isAvailable) GadgetStatusKind.Success else GadgetStatusKind.Warning,
                    message = if (state.isAvailable)
                        stringResource(R.string.battery_cap_available)
                    else
                        stringResource(R.string.battery_cap_unavailable),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.battery_cap_temp_name),
            detail = stringResource(R.string.battery_cap_temp_detail),
            status = {
                CapabilityStatus(
                    kind = when {
                        !state.isAvailable -> GadgetStatusKind.Warning
                        state.temperatureCelsius > 45f -> GadgetStatusKind.Error
                        state.temperatureCelsius > 35f -> GadgetStatusKind.Warning
                        else -> GadgetStatusKind.Success
                    },
                    message = if (state.isAvailable)
                        stringResource(R.string.battery_cap_temp_ok, state.temperatureCelsius)
                    else
                        stringResource(R.string.battery_cap_unavailable),
                )
            },
        ),
    ),
)

@Composable
internal fun BatteryScreenContent(
    state: BatteryState,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
    liveMonitors: @Composable () -> Unit = {},
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.battery_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            BatteryStatusCard(state)
            BatteryChargingCard(state)
            liveMonitors()
            monitors()
        },
    )
}

@Composable
private fun BatteryStatusCard(state: BatteryState, modifier: Modifier = Modifier) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.battery_card_status_title),
    ) {
        if (!state.isAvailable) {
            Text(
                text = stringResource(R.string.battery_unavailable),
                style = MaterialTheme.typography.bodyMedium,
            )
            return@DashCard
        }
        val levelText = if (state.level >= 0)
            stringResource(R.string.battery_level_value, state.level)
        else
            stringResource(R.string.battery_level_unknown)
        Text(text = levelText, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = stringResource(
                R.string.battery_detail_line,
                stringResource(state.health.labelRes()),
                state.temperatureCelsius,
                state.voltageMv / 1000f,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BatteryChargingCard(state: BatteryState, modifier: Modifier = Modifier) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.battery_card_charging_title),
    ) {
        val statusLabel = stringResource(state.chargingStatus.labelRes())
        Text(text = statusLabel, style = MaterialTheme.typography.bodyLarge)
        if (state.pluggedType != BatteryPlugType.None) {
            Text(
                text = stringResource(
                    R.string.battery_plugged_via,
                    stringResource(state.pluggedType.labelRes()),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BatteryChargingStatus.labelRes() = when (this) {
    BatteryChargingStatus.Charging -> R.string.battery_status_charging
    BatteryChargingStatus.Discharging -> R.string.battery_status_discharging
    BatteryChargingStatus.NotCharging -> R.string.battery_status_not_charging
    BatteryChargingStatus.Full -> R.string.battery_status_full
    BatteryChargingStatus.Unknown -> R.string.battery_status_unknown
}

@Composable
private fun BatteryPlugType.labelRes() = when (this) {
    BatteryPlugType.AC -> R.string.battery_plug_ac
    BatteryPlugType.USB -> R.string.battery_plug_usb
    BatteryPlugType.Wireless -> R.string.battery_plug_wireless
    BatteryPlugType.None -> R.string.battery_plug_none
}

@Composable
private fun BatteryHealth.labelRes() = when (this) {
    BatteryHealth.Good -> R.string.battery_health_good
    BatteryHealth.Overheat -> R.string.battery_health_overheat
    BatteryHealth.Dead -> R.string.battery_health_dead
    BatteryHealth.OverVoltage -> R.string.battery_health_overvoltage
    BatteryHealth.UnspecifiedFailure -> R.string.battery_health_failure
    BatteryHealth.Cold -> R.string.battery_health_cold
    BatteryHealth.Unknown -> R.string.battery_health_unknown
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun BatteryScreenPreview() = GadgetThemedPreview {
    BatteryScreenContent(
        state = BatteryState(
            level = 78,
            isCharging = true,
            chargingStatus = BatteryChargingStatus.Charging,
            pluggedType = BatteryPlugType.USB,
            health = BatteryHealth.Good,
            temperatureCelsius = 28.5f,
            voltageMv = 4120,
            isAvailable = true,
        ),
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun BatteryScreenEmptyPreview() = GadgetThemedPreview {
    BatteryScreenContent(state = BatteryState(), moduleInfo = null)
}
