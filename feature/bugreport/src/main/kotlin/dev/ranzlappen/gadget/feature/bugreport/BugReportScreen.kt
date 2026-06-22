package dev.ranzlappen.gadget.feature.bugreport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

@Composable
fun BugReportScreen(
    modifier: Modifier = Modifier,
    viewModel: BugReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    BugReportScreenContent(
        state = state,
        moduleInfo = bugReportModuleInfo(state),
        modifier = modifier,
    )
}

@Composable
private fun bugReportModuleInfo(state: BugReportState): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 1),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.bugreport_cap_health_name),
            detail = stringResource(R.string.bugreport_cap_health_detail),
            status = {
                if (state.allGranted) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.bugreport_cap_health_ok),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Warning,
                    message = stringResource(R.string.bugreport_cap_health_missing, state.missingCount),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.bugreport_cap_adb_name),
            detail = stringResource(R.string.bugreport_cap_adb_detail),
            status = {
                if (state.isRootedFlavor) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.bugreport_cap_rooted_active),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Warning,
                    message = stringResource(R.string.bugreport_cap_rooted_required),
                )
            },
        ),
    ),
)

@Composable
internal fun BugReportScreenContent(
    state: BugReportState,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.bugreport_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            BugReportPermissionsCard(state = state)
        },
    )
}

@Composable
private fun BugReportPermissionsCard(
    state: BugReportState,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.bugreport_card_title),
        icon = Icons.Filled.HealthAndSafety,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
            state.permissions.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(entry.label),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    GadgetChip(
                        selected = entry.granted,
                        onClick = {},
                        label = stringResource(
                            if (entry.granted) R.string.bugreport_perm_granted
                            else R.string.bugreport_perm_denied,
                        ),
                        enabled = false,
                    )
                }
            }
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@Composable
private fun BugReportScreenAllGrantedPreview() = GadgetThemedPreview {
    BugReportScreenContent(
        state = BugReportState(
            permissions = listOf(
                PermissionEntry(R.string.bugreport_perm_camera, "android.permission.CAMERA", true),
                PermissionEntry(R.string.bugreport_perm_microphone, "android.permission.RECORD_AUDIO", true),
                PermissionEntry(R.string.bugreport_perm_location, "android.permission.ACCESS_FINE_LOCATION", false),
            ),
            isRootedFlavor = false,
        ),
        moduleInfo = null,
    )
}
