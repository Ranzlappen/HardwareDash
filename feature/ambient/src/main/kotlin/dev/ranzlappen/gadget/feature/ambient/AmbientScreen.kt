package dev.ranzlappen.gadget.feature.ambient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
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
fun AmbientScreen(
    modifier: Modifier = Modifier,
    viewModel: AmbientViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    AmbientScreenContent(
        state = state,
        isRootedFlavor = viewModel.isRootedFlavor,
        moduleInfo = ambientModuleInfo(state, viewModel.isRootedFlavor),
        modifier = modifier,
        monitors = {
            LiveMonitorContainer(
                metricKey = AmbientLightMetricSource.METRIC_KEY,
                title = stringResource(R.string.ambient_live_monitor_lux),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "ambient_live_${AmbientLightMetricSource.METRIC_KEY}",
            )
            MonitorContainer(
                metricKey = AmbientLightMetricSource.METRIC_KEY,
                title = stringResource(R.string.ambient_monitor_lux),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "ambient_monitor_${AmbientLightMetricSource.METRIC_KEY}",
            )
        },
    )
}

@Composable
private fun ambientModuleInfo(state: AmbientState, isRootedFlavor: Boolean): ModuleInfo =
    ModuleInfo(
        compatibility = OsCompatibility(minSdk = 1),
        capabilities = listOf(
            ModuleCapability(
                name = stringResource(R.string.ambient_cap_sensor_name),
                detail = stringResource(R.string.ambient_cap_sensor_detail),
                status = {
                    if (state.sensorAvailable) CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.ambient_cap_sensor_available),
                    ) else CapabilityStatus(
                        kind = GadgetStatusKind.Error,
                        message = stringResource(R.string.ambient_cap_sensor_unavailable),
                    )
                },
            ),
            ModuleCapability(
                name = stringResource(R.string.ambient_cap_brightness_name),
                detail = stringResource(R.string.ambient_cap_brightness_detail),
                status = {
                    if (isRootedFlavor) CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.ambient_cap_rooted_active),
                    ) else CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.ambient_cap_rooted_required),
                    )
                },
            ),
            ModuleCapability(
                name = stringResource(R.string.ambient_cap_refresh_name),
                detail = stringResource(R.string.ambient_cap_refresh_detail),
                status = {
                    if (isRootedFlavor) CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.ambient_cap_rooted_active),
                    ) else CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.ambient_cap_rooted_required),
                    )
                },
            ),
            ModuleCapability(
                name = stringResource(R.string.ambient_cap_density_name),
                detail = stringResource(R.string.ambient_cap_density_detail),
                status = {
                    if (isRootedFlavor) CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.ambient_cap_rooted_active),
                    ) else CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.ambient_cap_rooted_required),
                    )
                },
            ),
        ),
    )

@Composable
internal fun AmbientScreenContent(
    state: AmbientState,
    isRootedFlavor: Boolean = false,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.ambient_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            AmbientLightCard(state = state)
            monitors()
        },
    )
}

@Composable
private fun AmbientLightCard(
    state: AmbientState,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.ambient_card_title),
        icon = Icons.Filled.LightMode,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            if (!state.sensorAvailable) {
                Text(
                    text = stringResource(R.string.ambient_sensor_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Text(
                        text = stringResource(R.string.ambient_label_lux),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = state.luxLevel?.let {
                            stringResource(R.string.ambient_lux_format, it)
                        } ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                state.luxLevel?.let { lux ->
                    val level = when {
                        lux < 10f -> stringResource(R.string.ambient_level_dark)
                        lux < 100f -> stringResource(R.string.ambient_level_dim)
                        lux < 1000f -> stringResource(R.string.ambient_level_indoor)
                        lux < 10000f -> stringResource(R.string.ambient_level_bright)
                        else -> stringResource(R.string.ambient_level_sunlight)
                    }
                    Text(
                        text = level,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun AmbientScreenPreview() = GadgetThemedPreview {
    AmbientScreenContent(
        state = AmbientState(luxLevel = 342.5f, sensorAvailable = true),
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun AmbientScreenNoSensorPreview() = GadgetThemedPreview {
    AmbientScreenContent(
        state = AmbientState(sensorAvailable = false),
        moduleInfo = null,
    )
}
