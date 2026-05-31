package dev.ranzlappen.gadget.feature.vibration.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.VibrationRootAvailability
import dev.ranzlappen.gadget.feature.vibration.VibrationRootToolsConfig
import kotlin.math.roundToInt

/**
 * Privileged vibration controls for the rooted app version. Only placed when
 * [VibrationRootAvailability.rootReady]. Each of the four tools exposes its
 * tunable parameters as [GadgetSlider]s plus a "run with these settings"
 * button; the dual-actuator section is disabled (with a caption) when the
 * device lacks both LRA + ERM. Mirror of torch's `RootToolsCard` — stateless,
 * persisted via commit-on-release; the amplitude sliders' max is the live
 * [maxAmplitudePercent] ceiling.
 */
@Composable
internal fun VibrationRootToolsCard(
    config: VibrationRootToolsConfig,
    availability: VibrationRootAvailability,
    maxAmplitudePercent: Int,
    onConfigChange: (VibrationRootToolsConfig) -> Unit,
    onConfigCommit: () -> Unit,
    onExtremeAmplitude: () -> Unit,
    onDirectPwm: () -> Unit,
    onDualActuator: () -> Unit,
    onSustainedRumble: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        title = stringResource(R.string.vibration_root_tools_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth(),
        icon = Icons.Outlined.Bolt,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Text(
                text = stringResource(R.string.vibration_root_tools_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ─── Extreme amplitude ──────────────────────────────────────
            ToolSection(label = stringResource(R.string.vibration_root_section_amplitude)) {
                GadgetSlider(
                    value = config.extremeAmplitudePercent.toFloat(),
                    onValueChange = { onConfigChange(config.copy(extremeAmplitudePercent = it.roundToInt())) },
                    onValueChangeFinished = onConfigCommit,
                    valueRange = VibrationRootToolsConfig.MIN_AMPLITUDE_PERCENT.toFloat()..
                        maxAmplitudePercent.toFloat().coerceAtLeast(VibrationRootToolsConfig.MIN_AMPLITUDE_PERCENT.toFloat() + 1f),
                    label = stringResource(R.string.vibration_root_amplitude_label),
                    suffix = "%",
                )
                DurationSlider(
                    valueMs = config.extremeBurstMs,
                    onChange = { onConfigChange(config.copy(extremeBurstMs = it)) },
                    onCommit = onConfigCommit,
                    maxMs = VibrationRootToolsConfig.MAX_BURST_MS,
                )
                Text(
                    text = stringResource(R.string.vibration_root_burst_ceiling_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RunButton(onExtremeAmplitude, R.string.vibration_root_action_amplitude)
            }

            // ─── Direct PWM ─────────────────────────────────────────────
            ToolSection(label = stringResource(R.string.vibration_root_section_pwm)) {
                MicrosSlider(
                    valueMicros = config.pwmOnMicros,
                    onChange = { onConfigChange(config.copy(pwmOnMicros = it)) },
                    onCommit = onConfigCommit,
                    label = stringResource(R.string.vibration_root_pwm_on_label),
                    minMicros = VibrationRootToolsConfig.MIN_PWM_ON_MICROS,
                )
                MicrosSlider(
                    valueMicros = config.pwmOffMicros,
                    onChange = { onConfigChange(config.copy(pwmOffMicros = it)) },
                    onCommit = onConfigCommit,
                    label = stringResource(R.string.vibration_root_pwm_off_label),
                    minMicros = VibrationRootToolsConfig.MIN_PWM_OFF_MICROS,
                )
                GadgetSlider(
                    value = config.pwmPulses.toFloat(),
                    onValueChange = { onConfigChange(config.copy(pwmPulses = it.roundToInt())) },
                    onValueChangeFinished = onConfigCommit,
                    valueRange = VibrationRootToolsConfig.MIN_PWM_PULSES.toFloat()..
                        VibrationRootToolsConfig.MAX_PWM_PULSES.toFloat(),
                    label = stringResource(R.string.vibration_root_pwm_pulses_label),
                )
                RunButton(onDirectPwm, R.string.vibration_root_action_pwm)
            }

            // ─── Dual actuator ──────────────────────────────────────────
            ToolSection(label = stringResource(R.string.vibration_root_section_dual)) {
                if (availability.hasDualActuators) {
                    MicrosSlider(
                        valueMicros = config.dualPhaseOffsetMicros,
                        onChange = { onConfigChange(config.copy(dualPhaseOffsetMicros = it)) },
                        onCommit = onConfigCommit,
                        label = stringResource(R.string.vibration_root_dual_phase_label),
                        minMicros = 0L,
                    )
                    RunButton(onDualActuator, R.string.vibration_root_action_dual)
                } else {
                    Text(
                        text = stringResource(R.string.vibration_root_no_dual),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ─── Sustained rumble ───────────────────────────────────────
            ToolSection(label = stringResource(R.string.vibration_root_section_rumble)) {
                GadgetSlider(
                    value = config.rumbleAmplitudePercent.toFloat(),
                    onValueChange = { onConfigChange(config.copy(rumbleAmplitudePercent = it.roundToInt())) },
                    onValueChangeFinished = onConfigCommit,
                    valueRange = VibrationRootToolsConfig.MIN_AMPLITUDE_PERCENT.toFloat()..
                        maxAmplitudePercent.toFloat().coerceAtLeast(VibrationRootToolsConfig.MIN_AMPLITUDE_PERCENT.toFloat() + 1f),
                    label = stringResource(R.string.vibration_root_amplitude_label),
                    suffix = "%",
                )
                DurationSlider(
                    valueMs = config.rumbleDurationMs,
                    onChange = { onConfigChange(config.copy(rumbleDurationMs = it)) },
                    onCommit = onConfigCommit,
                    maxMs = VibrationRootToolsConfig.MAX_RUMBLE_MS,
                )
                Text(
                    text = stringResource(R.string.vibration_root_rumble_ceiling_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RunButton(onSustainedRumble, R.string.vibration_root_action_rumble)
            }
        }
    }
}

@Composable
private fun RunButton(onClick: () -> Unit, textRes: Int) {
    GadgetSecondaryButton(
        onClick = onClick,
        text = stringResource(textRes),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ToolSection(label: String, content: @Composable () -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        content()
    }
}

/** Duration slider rendered in **seconds** (the model stores ms). */
@Composable
private fun DurationSlider(valueMs: Long, onChange: (Long) -> Unit, onCommit: () -> Unit, maxMs: Long) {
    GadgetSlider(
        value = valueMs.toFloat(),
        onValueChange = { onChange(it.roundToInt().toLong()) },
        onValueChangeFinished = onCommit,
        valueRange = VibrationRootToolsConfig.MIN_DURATION_MS.toFloat()..maxMs.toFloat(),
        label = stringResource(R.string.vibration_root_duration_label),
        suffix = "s",
        valueFormatter = { ms -> "%.1f".format(ms / 1000f) },
        valueParser = { text -> text.trim().toFloatOrNull()?.let { it * 1000f } },
    )
}

/** Microsecond slider rendered in **milliseconds** for legibility. */
@Composable
private fun MicrosSlider(
    valueMicros: Long,
    onChange: (Long) -> Unit,
    onCommit: () -> Unit,
    label: String,
    minMicros: Long,
) {
    GadgetSlider(
        value = valueMicros.toFloat(),
        onValueChange = { onChange(it.roundToInt().toLong()) },
        onValueChangeFinished = onCommit,
        valueRange = minMicros.toFloat()..VibrationRootToolsConfig.MAX_PWM_MICROS.toFloat(),
        label = label,
        suffix = "ms",
        valueFormatter = { micros -> "%.1f".format(micros / 1000f) },
        valueParser = { text -> text.trim().toFloatOrNull()?.let { it * 1000f } },
    )
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun VibrationRootToolsCardPreview() = GadgetThemedPreview {
    VibrationRootToolsCard(
        config = VibrationRootToolsConfig(),
        availability = VibrationRootAvailability(rootedFlavor = true, rootAccess = true, nodeFound = true, hasDualActuators = true),
        maxAmplitudePercent = 100,
        onConfigChange = {},
        onConfigCommit = {},
        onExtremeAmplitude = {},
        onDirectPwm = {},
        onDualActuator = {},
        onSustainedRumble = {},
        expanded = true,
        onExpandedChange = {},
    )
}
