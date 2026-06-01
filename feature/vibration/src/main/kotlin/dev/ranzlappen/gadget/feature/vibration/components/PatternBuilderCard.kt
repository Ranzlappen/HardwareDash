package dev.ranzlappen.gadget.feature.vibration.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.ui.PatternCanvas

/**
 * Draw-canvas pattern builder: the freehand [PatternCanvas] + a loop toggle +
 * play / clear / save controls. The save name is captured in a local field and
 * passed up via [onSave]. Stateless w.r.t. the draft (hoisted as [samples]).
 */
@Composable
internal fun PatternBuilderCard(
    samples: List<Float>,
    loop: Boolean,
    enabled: Boolean,
    onSamplesChange: (List<Float>) -> Unit,
    onLoopChange: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onClear: () -> Unit,
    onSave: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    var name by remember { mutableStateOf("") }
    GadgetExpandableCard(
        title = stringResource(R.string.vibration_builder_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth(),
        icon = Icons.Outlined.Gesture,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Text(
                text = stringResource(R.string.vibration_builder_instructions),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PatternCanvas(
                samples = samples,
                onSamplesChange = onSamplesChange,
                lineColor = MaterialTheme.colorScheme.primary,
                fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                gridColor = MaterialTheme.colorScheme.outline,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Text(
                    text = stringResource(R.string.vibration_builder_loop),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = loop, onCheckedChange = onLoopChange)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                GadgetPrimaryButton(
                    onClick = onPlay,
                    text = stringResource(R.string.vibration_builder_play),
                    enabled = enabled && samples.any { it > 0f },
                    modifier = Modifier.weight(1f),
                )
                GadgetTertiaryButton(
                    onClick = onClear,
                    text = stringResource(R.string.vibration_builder_clear),
                    modifier = Modifier.weight(1f),
                )
            }
            GadgetTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.vibration_builder_name),
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetSecondaryButton(
                onClick = {
                    onSave(name)
                    name = ""
                },
                text = stringResource(R.string.vibration_builder_save),
                enabled = name.isNotBlank() && samples.any { it > 0f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun PatternBuilderCardPreview() = GadgetThemedPreview {
    PatternBuilderCard(
        samples = List(40) { (it % 10) / 10f },
        loop = false,
        enabled = true,
        onSamplesChange = {},
        onLoopChange = {},
        onPlay = {},
        onClear = {},
        onSave = {},
        expanded = true,
        onExpandedChange = {},
    )
}
