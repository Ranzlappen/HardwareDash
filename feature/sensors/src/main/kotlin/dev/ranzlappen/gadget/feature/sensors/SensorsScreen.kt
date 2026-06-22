package dev.ranzlappen.gadget.feature.sensors

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

/**
 * Hilt entry point for the Sensors feature screen: collects the live row
 * state, builds the [ModuleInfo] (per-sensor capability rows), and supplies
 * the monitor containers as a slot — keeping [SensorsScreenContent] (and its
 * previews/tests) Hilt-free, per the module blueprint (torch reference).
 */
@Composable
fun SensorsScreen(
    modifier: Modifier = Modifier,
    viewModel: SensorsViewModel = hiltViewModel(),
) {
    // stateIn-backed flow -> collectAsState (lifecycle-runtime-compose isn't
    // in the feature plugin's default set; see the CLAUDE.md pitfall).
    val rows by viewModel.rows.collectAsState()
    val isRootedFlavor = viewModel.isRootedFlavor
    SensorsScreenContent(
        rows = rows,
        moduleInfo = sensorsModuleInfo(rows, isRootedFlavor),
        modifier = modifier,
        liveMonitors = {
            rows.filter { it.available }.forEach { row ->
                LiveMonitorContainer(
                    metricKey = row.metricKey,
                    title = stringResource(R.string.sensors_live_monitor_title, row.name),
                    modifier = Modifier.fillMaxWidth(),
                    collapseId = "sensors_live_monitor_${row.metricKey}",
                )
            }
        },
        monitors = {
            rows.filter { it.available }.forEach { row ->
                MonitorContainer(
                    metricKey = row.metricKey,
                    title = stringResource(R.string.sensors_monitor_title, row.name),
                    modifier = Modifier.fillMaxWidth(),
                    collapseId = "sensors_monitor_${row.metricKey}",
                )
            }
        },
    )
}

/**
 * Per-sensor tri-state capability rows: green when the hardware is present
 * and streaming, red when this device simply lacks the sensor. No runtime
 * permissions — proximity / light / accelerometer are normal-protection.
 */
@Composable
private fun sensorsModuleInfo(rows: List<SensorRowUi>, isRootedFlavor: Boolean): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 29),
    capabilities = rows.map { row ->
        ModuleCapability(
            name = row.name,
            detail = stringResource(R.string.sensors_capability_detail_signal, row.metricKey),
            status = {
                if (row.available) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.sensors_capability_available),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Error,
                        message = stringResource(R.string.sensors_capability_missing),
                    )
                }
            },
        )
    } + listOf(
        ModuleCapability(
            name = stringResource(R.string.sensors_capability_high_polling),
            detail = stringResource(R.string.sensors_capability_high_polling_detail),
            status = {
                if (isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.sensors_capability_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.sensors_capability_rooted_required),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.sensors_capability_raw_unfiltered),
            detail = stringResource(R.string.sensors_capability_raw_unfiltered_detail),
            status = {
                if (isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.sensors_capability_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.sensors_capability_rooted_required),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.sensors_capability_sysfs_read),
            detail = stringResource(R.string.sensors_capability_sysfs_read_detail),
            status = {
                if (isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.sensors_capability_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.sensors_capability_rooted_required),
                    )
                }
            },
        ),
    ),
)

/**
 * Stateless Sensors screen content: one live-readout [DashCard] per signal
 * plus the [monitors] slot (persisted history charts, supplied Hilt-free by
 * the route — the torch `monitor`-slot pattern).
 */
@Composable
internal fun SensorsScreenContent(
    rows: List<SensorRowUi>,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
    liveMonitors: @Composable () -> Unit = {},
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.sensors_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            rows.forEach { row -> SensorReadingCard(row) }
            liveMonitors()
            monitors()
        },
    )
}

@Composable
private fun SensorReadingCard(row: SensorRowUi, modifier: Modifier = Modifier) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = row.name,
    ) {
        val text = when {
            !row.available -> stringResource(R.string.sensors_not_present)
            row.value == null -> stringResource(R.string.sensors_value_pending)
            else -> "${FORMAT.format(row.value)} ${row.unit}"
        }
        val style =
            if (row.available && row.value != null) MaterialTheme.typography.headlineMedium
            else MaterialTheme.typography.bodyMedium
        Text(text = text, style = style)
    }
}

/** Fixed one-decimal readout; sensor magnitudes don't need more. */
private val FORMAT = java.text.DecimalFormat("0.0")

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun SensorsScreenPreview() = GadgetThemedPreview {
    SensorsScreenContent(
        rows = listOf(
            SensorRowUi("proximity", "Proximity", "cm", available = true, value = 4.2f),
            SensorRowUi("light", "Ambient light", "lx", available = true, value = null),
            SensorRowUi("acceleration", "Acceleration", "m/s²", available = false, value = null),
        ),
        moduleInfo = null,
    )
}
