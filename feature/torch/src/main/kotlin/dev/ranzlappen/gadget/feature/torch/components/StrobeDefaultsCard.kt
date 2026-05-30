package dev.ranzlappen.gadget.feature.torch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig

@Composable
internal fun StrobeDefaultsCard(
    rateHz: Float,
    onRateChange: (Float) -> Unit,
    onRateCommit: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        title = stringResource(R.string.torch_section_defaults_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth(),
        icon = Icons.Outlined.FlashlightOn,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(
                text = stringResource(R.string.torch_section_defaults_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GadgetSlider(
                value = rateHz,
                onValueChange = onRateChange,
                onValueChangeFinished = onRateCommit,
                valueRange = TorchWidgetConfig.MIN_RATE_HZ..TorchWidgetConfig.MAX_RATE_HZ,
                steps = (TorchWidgetConfig.MAX_RATE_HZ - TorchWidgetConfig.MIN_RATE_HZ).toInt() - 1,
                label = stringResource(R.string.torch_strobe_rate_label),
                suffix = "Hz",
            )
        }
    }
}
