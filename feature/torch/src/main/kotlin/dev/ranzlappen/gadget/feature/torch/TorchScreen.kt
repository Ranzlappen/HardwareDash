package dev.ranzlappen.gadget.feature.torch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetFab

/**
 * Torch / flashlight screen — v1 (standard flavor).
 *
 * Single big on/off [GadgetFab] in the centre of a hero card,
 * status text below. State + click come from [TorchViewModel] →
 * [TorchController]. On flashless devices the FAB is disabled and
 * the status reads "Unavailable".
 *
 * No second screen — the QS tile and home widgets share the same
 * `@Singleton TorchController`, so toggling from any surface
 * flows through `TorchCallback` and into this screen's state.
 *
 * Rooted-flavor extras (brightness boost, multi-LED, thermal
 * override, configurable strobe rate) ship in a later batch when
 * RootCapabilityRegistry is ported per
 * [docs/migration-guide.md](../../../../docs/migration-guide.md).
 */
@Composable
fun TorchScreen(
    modifier: Modifier = Modifier,
    viewModel: TorchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = LocalGadgetTheme.current.spacing
    ModuleScreenScaffold(
        title = "Torch",
        modifier = modifier,
        functional = {
            DashCard(
                modifier = Modifier.fillMaxWidth(),
                title = if (state.isOn) "On" else "Off",
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.medium),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HeroBoxHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        GadgetFab(
                            onClick = viewModel::onToggleClick,
                            icon = if (state.isOn) {
                                Icons.Filled.FlashlightOn
                            } else {
                                Icons.Filled.FlashlightOff
                            },
                            contentDescription = if (state.isOn) {
                                "Turn off torch"
                            } else {
                                "Turn on torch"
                            },
                            enabled = state.isAvailable,
                        )
                    }
                    Text(
                        text = state.statusMessage(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    )
}

/**
 * Compose-readable status string. Pulled out as an extension so
 * the screen body stays declarative and the message logic is
 * unit-testable later.
 */
private fun TorchState.statusMessage(): String = when {
    !isAvailable && error == TorchError.NoFlashUnit ->
        "This device doesn't have a flash unit."
    error == TorchError.HardwareError ->
        "Hardware error — try toggling again."
    error == TorchError.PermissionDenied ->
        "Camera access denied — grant in system settings."
    isOn -> "Torch is on."
    else -> "Tap the button to turn the torch on."
}

/** Hero box height — generous tap target for the central FAB. */
private val HeroBoxHeight: Dp = 160.dp
