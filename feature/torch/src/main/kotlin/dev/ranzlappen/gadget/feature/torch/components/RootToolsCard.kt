package dev.ranzlappen.gadget.feature.torch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.TorchRootToolsConfig
import kotlin.math.roundToInt

/**
 * Privileged flashlight controls for the rooted app version. Only placed in
 * the tree when
 * [dev.ranzlappen.gadget.feature.torch.TorchRootAvailability.rootReady] is
 * true.
 *
 * Each of the four tools exposes its tunable parameters as [GadgetSlider]s (and
 * a toggle for multi-LED's screen inclusion) plus a "run with these settings"
 * [GadgetSecondaryButton] that routes through the rooted implementation's
 * `RootSafetyGate` and reports back via a snackbar. Stateless + preview-safe:
 * every slider derives from [config] and edits emit an updated
 * [TorchRootToolsConfig] via [onConfigChange] (live) / [onConfigCommit]
 * (persist on release).
 *
 * The boost-brightness slider's maximum is the live [maxBrightnessPercent]
 * ceiling (100 stock, up to 150 on a rooted device with a usable LED node) —
 * never a hardcoded boost the hardware might not deliver. The thermal-override
 * duration caps at [TorchRootToolsConfig.MAX_THERMAL_DURATION_MS] (~45s
 * hardware ceiling).
 */
@Composable
internal fun RootToolsCard(
    config: TorchRootToolsConfig,
    maxBrightnessPercent: Int,
    onConfigChange: (TorchRootToolsConfig) -> Unit,
    onConfigCommit: () -> Unit,
    onBoostBrightness: () -> Unit,
    onDutyStrobe: () -> Unit,
    onMultiLed: () -> Unit,
    onThermal: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        title = stringResource(R.string.torch_root_tools_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth(),
        icon = Icons.Outlined.Bolt,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Text(
                text = stringResource(R.string.torch_root_tools_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ─── Boost brightness ───────────────────────────────────────
            ToolSection(label = stringResource(R.string.torch_root_section_brightness)) {
                // Only show the slider when the device actually has boost
                // headroom (a usable LED node raised the ceiling above the
                // stock 100). A zero-width range (100..100) would make the M3
                // Slider divide by (end - start) == 0 → NaN thumb fraction.
                if (maxBrightnessPercent > TorchRootToolsConfig.MIN_BRIGHTNESS_PERCENT) {
                    GadgetSlider(
                        value = config.boostBrightnessPercent.toFloat(),
                        onValueChange = {
                            onConfigChange(config.copy(boostBrightnessPercent = it.roundToInt()))
                        },
                        onValueChangeFinished = onConfigCommit,
                        valueRange = TorchRootToolsConfig.MIN_BRIGHTNESS_PERCENT.toFloat()..
                            maxBrightnessPercent.toFloat(),
                        label = stringResource(R.string.torch_root_brightness_label),
                        suffix = "%",
                    )
                } else {
                    Text(
                        text = stringResource(R.string.torch_root_no_boost_headroom),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                GadgetSecondaryButton(
                    onClick = onBoostBrightness,
                    text = stringResource(R.string.torch_root_action_brightness),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ─── Duty-cycle strobe ──────────────────────────────────────
            ToolSection(label = stringResource(R.string.torch_root_section_strobe)) {
                GadgetSlider(
                    value = config.dutyFrequencyHz.toFloat(),
                    onValueChange = { onConfigChange(config.copy(dutyFrequencyHz = it.roundToInt())) },
                    onValueChangeFinished = onConfigCommit,
                    valueRange = TorchRootToolsConfig.MIN_HZ.toFloat()..TorchRootToolsConfig.MAX_HZ.toFloat(),
                    label = stringResource(R.string.torch_root_freq_label),
                    suffix = "Hz",
                )
                GadgetSlider(
                    value = config.dutyPercent.toFloat(),
                    onValueChange = { onConfigChange(config.copy(dutyPercent = it.roundToInt())) },
                    onValueChangeFinished = onConfigCommit,
                    valueRange = TorchRootToolsConfig.MIN_DUTY.toFloat()..TorchRootToolsConfig.MAX_DUTY.toFloat(),
                    label = stringResource(R.string.torch_root_duty_label),
                    suffix = "%",
                )
                DurationSlider(
                    valueMs = config.dutyDurationMs,
                    onChange = { onConfigChange(config.copy(dutyDurationMs = it)) },
                    onCommit = onConfigCommit,
                    maxMs = TorchRootToolsConfig.MAX_DURATION_MS,
                )
                GadgetSecondaryButton(
                    onClick = onDutyStrobe,
                    text = stringResource(R.string.torch_root_action_strobe),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ─── Multi-LED ──────────────────────────────────────────────
            ToolSection(label = stringResource(R.string.torch_root_section_multiled)) {
                DurationSlider(
                    valueMs = config.multiLedDurationMs,
                    onChange = { onConfigChange(config.copy(multiLedDurationMs = it)) },
                    onCommit = onConfigCommit,
                    maxMs = TorchRootToolsConfig.MAX_DURATION_MS,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    Text(
                        text = stringResource(R.string.torch_root_include_screen_label),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = config.multiLedIncludeScreen,
                        // A toggle is a discrete choice — commit immediately so
                        // it persists without waiting for a slider-style release.
                        onCheckedChange = {
                            onConfigChange(config.copy(multiLedIncludeScreen = it))
                            onConfigCommit()
                        },
                    )
                }
                GadgetSecondaryButton(
                    onClick = onMultiLed,
                    text = stringResource(R.string.torch_root_action_multiled),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ─── Thermal override ───────────────────────────────────────
            ToolSection(label = stringResource(R.string.torch_root_section_thermal)) {
                GadgetSlider(
                    value = config.thermalFrequencyHz.toFloat(),
                    onValueChange = { onConfigChange(config.copy(thermalFrequencyHz = it.roundToInt())) },
                    onValueChangeFinished = onConfigCommit,
                    valueRange = TorchRootToolsConfig.MIN_HZ.toFloat()..TorchRootToolsConfig.MAX_HZ.toFloat(),
                    label = stringResource(R.string.torch_root_freq_label),
                    suffix = "Hz",
                )
                GadgetSlider(
                    value = config.thermalDutyPercent.toFloat(),
                    onValueChange = { onConfigChange(config.copy(thermalDutyPercent = it.roundToInt())) },
                    onValueChangeFinished = onConfigCommit,
                    valueRange = TorchRootToolsConfig.MIN_DUTY.toFloat()..TorchRootToolsConfig.MAX_DUTY.toFloat(),
                    label = stringResource(R.string.torch_root_duty_label),
                    suffix = "%",
                )
                DurationSlider(
                    valueMs = config.thermalDurationMs,
                    onChange = { onConfigChange(config.copy(thermalDurationMs = it)) },
                    onCommit = onConfigCommit,
                    maxMs = TorchRootToolsConfig.MAX_THERMAL_DURATION_MS,
                )
                Text(
                    text = stringResource(R.string.torch_root_thermal_ceiling_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GadgetSecondaryButton(
                    onClick = onThermal,
                    text = stringResource(R.string.torch_root_action_thermal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** One tool's labelled block: a section header over its controls. */
@Composable
private fun ToolSection(
    label: String,
    content: @Composable () -> Unit,
) {
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

/**
 * Duration slider rendered in **seconds** (the model stores ms). The slider's
 * value domain stays in ms; [valueFormatter] renders seconds for the chip and
 * [GadgetSlider.valueParser] is overridden so the tap-to-edit field reads the
 * typed text back as **seconds** (multiplying to ms) — without it the default
 * parser would interpret the typed seconds as raw ms (1000× off). Commits the
 * ms value on release; caps at [maxMs].
 */
@Composable
private fun DurationSlider(
    valueMs: Long,
    onChange: (Long) -> Unit,
    onCommit: () -> Unit,
    maxMs: Long,
) {
    GadgetSlider(
        value = valueMs.toFloat(),
        onValueChange = { onChange(it.roundToInt().toLong()) },
        onValueChangeFinished = onCommit,
        valueRange = TorchRootToolsConfig.MIN_DURATION_MS.toFloat()..maxMs.toFloat(),
        label = stringResource(R.string.torch_root_duration_label),
        suffix = "s",
        // Slider value is ms; show + edit in seconds.
        valueFormatter = { ms -> "%.1f".format(ms / 1000f) },
        valueParser = { text -> text.trim().toFloatOrNull()?.let { it * 1000f } },
    )
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun RootToolsCardPreview() = GadgetThemedPreview {
    RootToolsCard(
        config = TorchRootToolsConfig(),
        maxBrightnessPercent = 150,
        onConfigChange = {},
        onConfigCommit = {},
        onBoostBrightness = {},
        onDutyStrobe = {},
        onMultiLed = {},
        onThermal = {},
        expanded = true,
        onExpandedChange = {},
    )
}
