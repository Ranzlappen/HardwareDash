package dev.ranzlappen.gadget.feature.diagnostics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import dev.ranzlappen.gadget.core.ui.module.RootActionRow
import dev.ranzlappen.gadget.core.ui.module.RootToolsSection
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

@Composable
fun DiagnosticsScreen(
    modifier: Modifier = Modifier,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val rootTools by viewModel.rootTools.collectAsState()
    var rootToolsExpanded by remember { mutableStateOf(true) }
    DiagnosticsScreenContent(
        state = state,
        moduleInfo = diagnosticsModuleInfo(state),
        modifier = modifier,
        rootTools = {
            RootToolsSection(
                title = stringResource(R.string.diagnostics_root_tools_title),
                available = state.isRootedFlavor,
                unavailableMessage = stringResource(R.string.diagnostics_root_tools_unavailable),
                expanded = rootToolsExpanded,
                onExpandedChange = { rootToolsExpanded = it },
            ) {
                RootActionRow(
                    label = stringResource(R.string.diagnostics_root_meminfo_label),
                    description = stringResource(R.string.diagnostics_root_meminfo_detail),
                    runLabel = stringResource(R.string.diagnostics_root_run),
                    onRun = viewModel::onDumpMemInfo,
                    enabled = !rootTools.memInfo.running,
                    statusMessage = rootTools.memInfo.message,
                    statusKind = rootTools.memInfo.statusKind,
                )
                RootActionRow(
                    label = stringResource(R.string.diagnostics_root_cpuinfo_label),
                    description = stringResource(R.string.diagnostics_root_cpuinfo_detail),
                    runLabel = stringResource(R.string.diagnostics_root_run),
                    onRun = viewModel::onDumpCpuInfo,
                    enabled = !rootTools.cpuInfo.running,
                    statusMessage = rootTools.cpuInfo.message,
                    statusKind = rootTools.cpuInfo.statusKind,
                )
                RootActionRow(
                    label = stringResource(R.string.diagnostics_root_procstats_label),
                    description = stringResource(R.string.diagnostics_root_procstats_detail),
                    runLabel = stringResource(R.string.diagnostics_root_run),
                    onRun = viewModel::onDumpProcstats,
                    enabled = !rootTools.procstats.running,
                    statusMessage = rootTools.procstats.message,
                    statusKind = rootTools.procstats.statusKind,
                )
            }
        },
        monitors = {
            LiveMonitorContainer(
                metricKey = MemoryMetricSource.METRIC_KEY,
                title = stringResource(R.string.diagnostics_live_monitor_memory),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "diagnostics_live_${MemoryMetricSource.METRIC_KEY}",
            )
            MonitorContainer(
                metricKey = MemoryMetricSource.METRIC_KEY,
                title = stringResource(R.string.diagnostics_monitor_memory),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "diagnostics_monitor_${MemoryMetricSource.METRIC_KEY}",
            )
        },
    )
}

@Composable
private fun diagnosticsModuleInfo(state: DiagnosticsState): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 1),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.diagnostics_cap_logcat_name),
            detail = stringResource(R.string.diagnostics_cap_logcat_detail),
            status = {
                if (state.isRootedFlavor) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.diagnostics_cap_rooted_active),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Warning,
                    message = stringResource(R.string.diagnostics_cap_rooted_required),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.diagnostics_cap_meminfo_name),
            detail = stringResource(R.string.diagnostics_cap_meminfo_detail),
            status = {
                if (state.isRootedFlavor) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.diagnostics_cap_rooted_active),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Warning,
                    message = stringResource(R.string.diagnostics_cap_rooted_required),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.diagnostics_cap_cpuinfo_name),
            detail = stringResource(R.string.diagnostics_cap_cpuinfo_detail),
            status = {
                if (state.isRootedFlavor) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.diagnostics_cap_rooted_active),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Warning,
                    message = stringResource(R.string.diagnostics_cap_rooted_required),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.diagnostics_cap_procstats_name),
            detail = stringResource(R.string.diagnostics_cap_procstats_detail),
            status = {
                if (state.isRootedFlavor) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.diagnostics_cap_rooted_active),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Warning,
                    message = stringResource(R.string.diagnostics_cap_rooted_required),
                )
            },
        ),
    ),
)

@Composable
internal fun DiagnosticsScreenContent(
    state: DiagnosticsState,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
    rootTools: @Composable () -> Unit = {},
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.diagnostics_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            DiagnosticsInfoCard()
            monitors()
            rootTools()
        },
    )
}

@Composable
private fun DiagnosticsInfoCard(modifier: Modifier = Modifier) {
    DashCard(
        modifier = modifier,
        title = stringResource(R.string.diagnostics_screen_title),
        icon = Icons.Filled.BugReport,
    ) {
        Text(
            text = stringResource(R.string.diagnostics_info),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@Composable
private fun DiagnosticsScreenPreview() = GadgetThemedPreview {
    DiagnosticsScreenContent(
        state = DiagnosticsState(isRootedFlavor = false),
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun DiagnosticsScreenRootedPreview() = GadgetThemedPreview {
    DiagnosticsScreenContent(
        state = DiagnosticsState(isRootedFlavor = true),
        moduleInfo = null,
    )
}
