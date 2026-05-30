package dev.ranzlappen.gadget.feature.torch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.feature.torch.R

/**
 * Privileged flashlight controls for the rooted app version. Only placed
 * in the tree when [dev.ranzlappen.gadget.feature.torch.TorchRootAvailability.rootReady]
 * is true; each button routes through the rooted implementation's
 * `RootSafetyGate` and reports back via a snackbar. One-tap presets — the
 * parameter surface lives in the ViewModel constants.
 */
@Composable
internal fun RootToolsCard(
    onBoostBrightness: () -> Unit,
    onDutyStrobe: () -> Unit,
    onMultiLed: () -> Unit,
    onThermal: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        title = stringResource(R.string.torch_root_tools_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth(),
        icon = Icons.Outlined.Bolt,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(
                text = stringResource(R.string.torch_root_tools_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GadgetSecondaryButton(
                onClick = onBoostBrightness,
                text = stringResource(R.string.torch_root_action_brightness),
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetSecondaryButton(
                onClick = onDutyStrobe,
                text = stringResource(R.string.torch_root_action_strobe),
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetSecondaryButton(
                onClick = onMultiLed,
                text = stringResource(R.string.torch_root_action_multiled),
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetSecondaryButton(
                onClick = onThermal,
                text = stringResource(R.string.torch_root_action_thermal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
