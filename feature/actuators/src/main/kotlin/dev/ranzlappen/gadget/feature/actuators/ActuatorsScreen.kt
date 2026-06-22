package dev.ranzlappen.gadget.feature.actuators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

@Composable
fun ActuatorsScreen(
    modifier: Modifier = Modifier,
    viewModel: ActuatorsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    ActuatorsScreenContent(
        state = state,
        moduleInfo = actuatorsModuleInfo(state),
        modifier = modifier,
    )
}

@Composable
private fun actuatorsModuleInfo(state: ActuatorsState): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 1),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.actuators_cap_vibrator_name),
            detail = stringResource(R.string.actuators_cap_vibrator_detail),
            status = {
                if (state.vibratorAvailable) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.actuators_cap_vibrator_available),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Error,
                    message = stringResource(R.string.actuators_cap_vibrator_unavailable),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.actuators_cap_extreme_name),
            detail = stringResource(R.string.actuators_cap_extreme_detail),
            status = {
                if (state.isRootedFlavor) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.actuators_cap_rooted_active),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Warning,
                    message = stringResource(R.string.actuators_cap_rooted_required),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.actuators_cap_pwm_name),
            detail = stringResource(R.string.actuators_cap_pwm_detail),
            status = {
                if (state.isRootedFlavor) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.actuators_cap_rooted_active),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Warning,
                    message = stringResource(R.string.actuators_cap_rooted_required),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.actuators_cap_dual_name),
            detail = stringResource(R.string.actuators_cap_dual_detail),
            status = {
                if (state.isRootedFlavor) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.actuators_cap_rooted_active),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Warning,
                    message = stringResource(R.string.actuators_cap_rooted_required),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.actuators_cap_rumble_name),
            detail = stringResource(R.string.actuators_cap_rumble_detail),
            status = {
                if (state.isRootedFlavor) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.actuators_cap_rooted_active),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Warning,
                    message = stringResource(R.string.actuators_cap_rooted_required),
                )
            },
        ),
    ),
)

@Composable
internal fun ActuatorsScreenContent(
    state: ActuatorsState,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.actuators_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            ActuatorsStatusCard(state = state)
        },
    )
}

@Composable
private fun ActuatorsStatusCard(
    state: ActuatorsState,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.actuators_card_title),
        icon = Icons.Filled.Vibration,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
            GadgetChip(
                selected = state.vibratorAvailable,
                onClick = {},
                label = stringResource(
                    if (state.vibratorAvailable) R.string.actuators_chip_available
                    else R.string.actuators_chip_unavailable,
                ),
                enabled = false,
            )
            if (state.vibratorAvailable) {
                GadgetChip(
                    selected = state.hasAmplitudeControl,
                    onClick = {},
                    label = stringResource(
                        if (state.hasAmplitudeControl) R.string.actuators_chip_amplitude
                        else R.string.actuators_chip_no_amplitude,
                    ),
                    enabled = false,
                )
            }
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun ActuatorsScreenPreview() = GadgetThemedPreview {
    ActuatorsScreenContent(
        state = ActuatorsState(vibratorAvailable = true, hasAmplitudeControl = true),
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun ActuatorsScreenNoVibratorPreview() = GadgetThemedPreview {
    ActuatorsScreenContent(
        state = ActuatorsState(vibratorAvailable = false),
        moduleInfo = null,
    )
}
