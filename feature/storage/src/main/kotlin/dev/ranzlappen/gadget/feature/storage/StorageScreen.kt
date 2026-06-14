package dev.ranzlappen.gadget.feature.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.monitoring.LiveMonitorContainer
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetLinearProgress
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
fun StorageScreen(
    modifier: Modifier = Modifier,
    viewModel: StorageViewModel = hiltViewModel(),
) {
    val volumes by viewModel.volumes.collectAsStateWithLifecycle()
    StorageScreenContent(
        volumes = volumes,
        moduleInfo = storageModuleInfo(volumes),
        modifier = modifier,
        liveMonitors = {
            LiveMonitorContainer(
                metricKey = StorageUsedPercentMetricSource.METRIC_KEY,
                title = stringResource(R.string.storage_live_monitor_used),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "storage_live_used",
            )
        },
        monitors = {
            MonitorContainer(
                metricKey = StorageUsedPercentMetricSource.METRIC_KEY,
                title = stringResource(R.string.storage_monitor_used),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "storage_history_used",
            )
            MonitorContainer(
                metricKey = StorageFreeGbMetricSource.METRIC_KEY,
                title = stringResource(R.string.storage_monitor_free),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "storage_history_free",
            )
        },
    )
}

@Composable
private fun storageModuleInfo(volumes: List<StorageVolumeInfo>): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 24),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.storage_cap_name),
            detail = stringResource(R.string.storage_cap_detail),
            status = {
                CapabilityStatus(
                    kind = if (volumes.isNotEmpty()) GadgetStatusKind.Success else GadgetStatusKind.Warning,
                    message = if (volumes.isNotEmpty())
                        stringResource(R.string.storage_cap_available, volumes.size)
                    else
                        stringResource(R.string.storage_cap_unavailable),
                )
            },
        ),
    ),
)

@Composable
internal fun StorageScreenContent(
    volumes: List<StorageVolumeInfo>,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
    liveMonitors: @Composable () -> Unit = {},
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.storage_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            if (volumes.isEmpty()) {
                StorageLoadingCard()
            } else {
                volumes.forEach { volume -> StorageVolumeCard(volume) }
                liveMonitors()
                monitors()
            }
        },
    )
}

@Composable
private fun StorageLoadingCard(modifier: Modifier = Modifier) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.storage_card_title_internal),
    ) {
        GadgetLinearProgress(modifier = Modifier.fillMaxWidth())
        Text(
            text = stringResource(R.string.storage_reading),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StorageVolumeCard(
    volume: StorageVolumeInfo,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val cardTitle = if (volume.isRemovable)
        stringResource(R.string.storage_card_title_removable, volume.label)
    else
        stringResource(R.string.storage_card_title_internal)

    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = cardTitle,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            GadgetLinearProgress(
                progress = volume.usedPercent / 100f,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(
                        R.string.storage_used_label,
                        volume.usedBytes.toDisplayGb(),
                        volume.totalBytes.toDisplayGb(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(
                        R.string.storage_used_percent,
                        volume.usedPercent.toInt(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        volume.usedPercent >= 90f -> MaterialTheme.colorScheme.error
                        volume.usedPercent >= 75f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            Text(
                text = stringResource(R.string.storage_free_label, volume.freeBytes.toDisplayGb()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun StorageScreenPreview() = GadgetThemedPreview {
    StorageScreenContent(
        volumes = listOf(
            StorageVolumeInfo(
                label = "Internal shared storage",
                totalBytes = 128L * 1024 * 1024 * 1024,
                usedBytes = 87L * 1024 * 1024 * 1024,
                freeBytes = 41L * 1024 * 1024 * 1024,
                isRemovable = false,
            ),
            StorageVolumeInfo(
                label = "SD card",
                totalBytes = 64L * 1024 * 1024 * 1024,
                usedBytes = 12L * 1024 * 1024 * 1024,
                freeBytes = 52L * 1024 * 1024 * 1024,
                isRemovable = true,
            ),
        ),
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun StorageScreenLoadingPreview() = GadgetThemedPreview {
    StorageScreenContent(volumes = emptyList(), moduleInfo = null)
}
