package dev.ranzlappen.gadget.feature.vibration.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.VibrationPredefinedEffect
import dev.ranzlappen.gadget.feature.vibration.VibrationState
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationWidgetConfig
import kotlin.math.roundToInt

/**
 * Standard-tier haptic controls: predefined-effect buttons + amplitude/duration
 * sliders feeding a one-shot test buzz, plus a stop. The amplitude slider is
 * disabled (with a caption) when the device lacks `hasAmplitudeControl`.
 * Stateless — values + callbacks hoisted to the ViewModel.
 */
@Composable
internal fun VibrationControlsCard(
    state: VibrationState,
    amplitudePercent: Int,
    durationMs: Long,
    onPredefined: (VibrationPredefinedEffect) -> Unit,
    onAmplitudeChange: (Int) -> Unit,
    onDurationChange: (Long) -> Unit,
    onCommit: () -> Unit,
    onOneShot: () -> Unit,
    onStop: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        title = stringResource(R.string.vibration_controls_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth(),
        icon = Icons.Outlined.Vibration,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            if (!state.isAvailable) {
                Text(
                    text = stringResource(R.string.vibration_controls_no_motor),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Predefined effects.
            Text(
                text = stringResource(R.string.vibration_controls_predefined),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                GadgetSecondaryButton(
                    onClick = { onPredefined(VibrationPredefinedEffect.Click) },
                    text = stringResource(R.string.vibration_effect_click),
                    enabled = state.isAvailable,
                    modifier = Modifier.weight(1f),
                )
                GadgetSecondaryButton(
                    onClick = { onPredefined(VibrationPredefinedEffect.Tick) },
                    text = stringResource(R.string.vibration_effect_tick),
                    enabled = state.isAvailable,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                GadgetSecondaryButton(
                    onClick = { onPredefined(VibrationPredefinedEffect.DoubleClick) },
                    text = stringResource(R.string.vibration_effect_double_click),
                    enabled = state.isAvailable,
                    modifier = Modifier.weight(1f),
                )
                GadgetSecondaryButton(
                    onClick = { onPredefined(VibrationPredefinedEffect.HeavyClick) },
                    text = stringResource(R.string.vibration_effect_heavy_click),
                    enabled = state.isAvailable,
                    modifier = Modifier.weight(1f),
                )
            }

            // One-shot amplitude + duration.
            GadgetSlider(
                value = amplitudePercent.toFloat(),
                onValueChange = { onAmplitudeChange(it.roundToInt()) },
                onValueChangeFinished = onCommit,
                valueRange = VibrationWidgetConfig.MIN_AMPLITUDE_PERCENT.toFloat()..
                    VibrationWidgetConfig.MAX_AMPLITUDE_PERCENT.toFloat(),
                label = stringResource(R.string.vibration_controls_amplitude),
                suffix = "%",
                enabled = state.isAvailable && state.hasAmplitudeControl,
            )
            if (state.isAvailable && !state.hasAmplitudeControl) {
                Text(
                    text = stringResource(R.string.vibration_controls_no_amplitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GadgetSlider(
                value = durationMs.toFloat(),
                onValueChange = { onDurationChange(it.roundToInt().toLong()) },
                onValueChangeFinished = onCommit,
                valueRange = VibrationWidgetConfig.MIN_DURATION_MS.toFloat()..
                    VibrationWidgetConfig.MAX_DURATION_MS.toFloat(),
                label = stringResource(R.string.vibration_controls_duration),
                suffix = "ms",
                enabled = state.isAvailable,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                GadgetPrimaryButton(
                    onClick = onOneShot,
                    text = stringResource(R.string.vibration_controls_play),
                    enabled = state.isAvailable,
                    modifier = Modifier.weight(1f),
                )
                GadgetSecondaryButton(
                    onClick = onStop,
                    text = stringResource(R.string.vibration_controls_stop),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun VibrationControlsCardPreview() = GadgetThemedPreview {
    VibrationControlsCard(
        state = VibrationState(isAvailable = true, hasAmplitudeControl = true),
        amplitudePercent = 60,
        durationMs = 300,
        onPredefined = {},
        onAmplitudeChange = {},
        onDurationChange = {},
        onCommit = {},
        onOneShot = {},
        onStop = {},
        expanded = true,
        onExpandedChange = {},
    )
}
