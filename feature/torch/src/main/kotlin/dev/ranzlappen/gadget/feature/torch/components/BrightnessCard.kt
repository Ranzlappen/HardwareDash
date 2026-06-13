package dev.ranzlappen.gadget.feature.torch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.feature.torch.R

/**
 * Collapsible card housing the flash-intensity slider.
 *
 * When [brightnessSupported] is `false` (API < 33 or single-level flash)
 * the slider is replaced with an informational note so the card remains
 * visible — the user knows the control exists but learns their device
 * doesn't support it.
 */
@Composable
internal fun BrightnessCard(
    brightness: Float,
    brightnessSupported: Boolean,
    onBrightnessChange: (Float) -> Unit,
    onBrightnessCommit: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        title = stringResource(R.string.torch_section_brightness_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth(),
        icon = Icons.Outlined.Brightness6,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            if (brightnessSupported) {
                GadgetSlider(
                    value = brightness,
                    onValueChange = onBrightnessChange,
                    onValueChangeFinished = onBrightnessCommit,
                    valueRange = 0f..1f,
                    label = stringResource(R.string.torch_brightness_label),
                    suffix = "%",
                    // Normalised 0..1 shown as 0..100 %; user types "75" → parsed as 0.75
                    valueFormatter = { (it * 100).toInt().toString() },
                    valueParser = { it.trim().toFloatOrNull()?.div(100f)?.coerceIn(0f, 1f) },
                )
            } else {
                Text(
                    text = stringResource(R.string.torch_brightness_unsupported),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
