package dev.ranzlappen.gadget.feature.microphone.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusDot
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.component.color
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewSizeClasses
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.microphone.MicrophoneScreenState
import dev.ranzlappen.gadget.feature.microphone.R
import kotlin.math.roundToInt

/**
 * The six extreme-tier [dev.ranzlappen.gadget.feature.microphone.control.MicrophoneController]
 * rows: gain-boost slider, direct-PCM diagnostic capture, custom-sample-rate
 * input (confirm-gated by the caller), multi-mic simultaneous capture,
 * disable-hardware-noise-suppression toggle, and system-audio-capture toggle
 * (also confirm-gated by the caller). Every row always renders — on the
 * standard flavor every control is disabled and a [GadgetStatusKind.Warning]
 * badge explains why, rather than hiding the whole card (there is genuinely
 * zero standard-tier functionality here per `StandardMicrophoneController`,
 * but the rows still document what root would unlock).
 *
 * The two rows with a mandatory confirm dialog (custom sample rate, system
 * audio capture) call [onCustomSampleRateRequest] / [onSystemAudioCaptureRequest]
 * rather than dispatching directly — [dev.ranzlappen.gadget.feature.microphone.MicrophoneScreenContent]
 * owns the actual [androidx.compose.material3.AlertDialog]s and only fires
 * the real controller call once the user confirms.
 */
@Composable
internal fun MicrophoneToolsCard(
    state: MicrophoneScreenState,
    onGainBoostDbChange: (Int) -> Unit,
    onGainBoostDurationChange: (Long) -> Unit,
    onGainBoostRun: () -> Unit,
    onDirectPcmDurationChange: (Long) -> Unit,
    onDirectPcmRun: () -> Unit,
    onCustomSampleRateHzChange: (Int) -> Unit,
    onCustomSampleRateDurationChange: (Long) -> Unit,
    onCustomSampleRateRequest: () -> Unit,
    onMultiMicDurationChange: (Long) -> Unit,
    onMultiMicStreamsChange: (Int) -> Unit,
    onMultiMicRun: () -> Unit,
    onDisableEffectsToggle: () -> Unit,
    onSystemAudioCaptureDurationChange: (Long) -> Unit,
    onSystemAudioCaptureRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val enabled = state.isRootedFlavor

    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.microphone_tools_title),
        icon = Icons.Outlined.GraphicEq,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Text(
                text = stringResource(R.string.microphone_tools_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!enabled) {
                RootRequiredBadge()
            }

            // ─── Gain boost ─────────────────────────────────────────────
            ToolSection(label = stringResource(R.string.microphone_section_gain_boost)) {
                GadgetSlider(
                    value = state.gainBoostDb.toFloat(),
                    onValueChange = { onGainBoostDbChange(it.roundToInt()) },
                    valueRange = MicrophoneScreenState.MIN_GAIN_BOOST_DB.toFloat()..
                        MicrophoneScreenState.MAX_GAIN_BOOST_DB.toFloat(),
                    label = stringResource(R.string.microphone_gain_boost_db_label),
                    suffix = "dB",
                    enabled = enabled,
                )
                DurationSecondsSlider(
                    valueMs = state.gainBoostDurationMs,
                    onChange = onGainBoostDurationChange,
                    maxMs = MicrophoneScreenState.MAX_GAIN_BOOST_DURATION_MS,
                    enabled = enabled,
                )
                Text(
                    text = stringResource(R.string.microphone_gain_boost_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RunButton(onGainBoostRun, R.string.microphone_action_gain_boost, enabled, state.gainBoostInFlight)
            }

            // ─── Direct PCM ─────────────────────────────────────────────
            ToolSection(label = stringResource(R.string.microphone_section_direct_pcm)) {
                Text(
                    text = stringResource(
                        R.string.microphone_direct_pcm_format,
                        MicrophoneScreenState.DIRECT_PCM_SAMPLE_RATE_HZ / 1000,
                        MicrophoneScreenState.DIRECT_PCM_BITS_PER_SAMPLE,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DurationSecondsSlider(
                    valueMs = state.directPcmDurationMs,
                    onChange = onDirectPcmDurationChange,
                    maxMs = MicrophoneScreenState.MAX_DIRECT_PCM_DURATION_MS,
                    enabled = enabled,
                )
                RunButton(onDirectPcmRun, R.string.microphone_action_direct_pcm, enabled, state.directPcmInFlight)
            }

            // ─── Custom sample rate ─────────────────────────────────────
            ToolSection(label = stringResource(R.string.microphone_section_custom_rate)) {
                GadgetSlider(
                    value = state.customSampleRateHz.toFloat(),
                    onValueChange = { onCustomSampleRateHzChange(it.roundToInt()) },
                    valueRange = MicrophoneScreenState.MIN_CUSTOM_SAMPLE_RATE_HZ.toFloat()..
                        MicrophoneScreenState.MAX_CUSTOM_SAMPLE_RATE_HZ.toFloat(),
                    label = stringResource(R.string.microphone_custom_rate_hz_label),
                    suffix = "kHz",
                    valueFormatter = { hz -> "%.1f".format(hz / 1000f) },
                    valueParser = { text -> text.trim().toFloatOrNull()?.times(1000f) },
                    enabled = enabled,
                )
                DurationSecondsSlider(
                    valueMs = state.customSampleRateDurationMs,
                    onChange = onCustomSampleRateDurationChange,
                    maxMs = MicrophoneScreenState.MAX_CUSTOM_RATE_DURATION_MS,
                    enabled = enabled,
                )
                Text(
                    text = stringResource(R.string.microphone_custom_rate_risk_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                RunButton(onCustomSampleRateRequest, R.string.microphone_action_custom_rate, enabled, state.customSampleRateInFlight)
            }

            // ─── Multi-mic raw ──────────────────────────────────────────
            ToolSection(label = stringResource(R.string.microphone_section_multi_mic)) {
                GadgetSlider(
                    value = state.multiMicStreams.toFloat(),
                    onValueChange = { onMultiMicStreamsChange(it.roundToInt()) },
                    valueRange = MicrophoneScreenState.MIN_MULTI_MIC_STREAMS.toFloat()..
                        MicrophoneScreenState.MAX_MULTI_MIC_STREAMS.toFloat(),
                    label = stringResource(R.string.microphone_multi_mic_streams_label),
                    steps = MicrophoneScreenState.MAX_MULTI_MIC_STREAMS -
                        MicrophoneScreenState.MIN_MULTI_MIC_STREAMS - 1,
                    enabled = enabled,
                )
                DurationSecondsSlider(
                    valueMs = state.multiMicDurationMs,
                    onChange = onMultiMicDurationChange,
                    maxMs = MicrophoneScreenState.MAX_MULTI_MIC_DURATION_MS,
                    enabled = enabled,
                )
                RunButton(onMultiMicRun, R.string.microphone_action_multi_mic, enabled, state.multiMicInFlight)
            }

            // ─── Disable hardware noise suppression ─────────────────────
            ToolSection(label = stringResource(R.string.microphone_section_disable_effects)) {
                Text(
                    text = stringResource(R.string.microphone_disable_effects_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ToggleRow(
                    label = stringResource(R.string.microphone_disable_effects_toggle_label),
                    checked = state.disableEffectsInFlight,
                    onCheckedChange = { onDisableEffectsToggle() },
                    enabled = enabled,
                )
            }

            // ─── System audio capture ────────────────────────────────────
            ToolSection(label = stringResource(R.string.microphone_section_system_audio)) {
                DurationSecondsSlider(
                    valueMs = state.systemAudioCaptureDurationMs,
                    onChange = onSystemAudioCaptureDurationChange,
                    minMs = MicrophoneScreenState.MIN_SYSTEM_AUDIO_DURATION_MS,
                    maxMs = MicrophoneScreenState.MAX_SYSTEM_AUDIO_DURATION_MS,
                    enabled = enabled,
                )
                Text(
                    text = stringResource(R.string.microphone_system_audio_legal_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                ToggleRow(
                    label = stringResource(R.string.microphone_system_audio_toggle_label),
                    checked = state.systemAudioCaptureInFlight,
                    onCheckedChange = { checked -> if (checked) onSystemAudioCaptureRequest() },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun RootRequiredBadge(modifier: Modifier = Modifier) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GadgetStatusDot(contentDescription = null, color = GadgetStatusKind.Warning.color())
        Text(
            text = stringResource(R.string.microphone_root_required_badge),
            style = MaterialTheme.typography.bodySmall,
            color = GadgetStatusKind.Warning.color(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
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

@Composable
private fun RunButton(onClick: () -> Unit, textRes: Int, enabled: Boolean, loading: Boolean) {
    GadgetSecondaryButton(
        onClick = onClick,
        text = stringResource(textRes),
        enabled = enabled && !loading,
        loading = loading,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(end = spacing.small),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/** Duration slider rendered in **seconds**; the model stores milliseconds. */
@Composable
private fun DurationSecondsSlider(
    valueMs: Long,
    onChange: (Long) -> Unit,
    maxMs: Long,
    enabled: Boolean,
    minMs: Long = 0L,
) {
    GadgetSlider(
        value = valueMs.toFloat(),
        onValueChange = { onChange(it.roundToInt().toLong()) },
        valueRange = minMs.toFloat()..maxMs.toFloat(),
        label = stringResource(R.string.microphone_duration_label),
        suffix = "s",
        valueFormatter = { ms -> "%.1f".format(ms / 1000f) },
        valueParser = { text -> text.trim().toFloatOrNull()?.let { it * 1000f } },
        enabled = enabled,
    )
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@GadgetPreviewSizeClasses
@Composable
private fun MicrophoneToolsCardRootedPreview() = GadgetThemedPreview {
    MicrophoneToolsCard(
        state = MicrophoneScreenState(isRootedFlavor = true),
        onGainBoostDbChange = {}, onGainBoostDurationChange = {}, onGainBoostRun = {},
        onDirectPcmDurationChange = {}, onDirectPcmRun = {},
        onCustomSampleRateHzChange = {}, onCustomSampleRateDurationChange = {}, onCustomSampleRateRequest = {},
        onMultiMicDurationChange = {}, onMultiMicStreamsChange = {}, onMultiMicRun = {},
        onDisableEffectsToggle = {},
        onSystemAudioCaptureDurationChange = {}, onSystemAudioCaptureRequest = {},
    )
}

@GadgetPreviewLightDark
@Composable
private fun MicrophoneToolsCardStandardPreview() = GadgetThemedPreview {
    MicrophoneToolsCard(
        state = MicrophoneScreenState(isRootedFlavor = false),
        onGainBoostDbChange = {}, onGainBoostDurationChange = {}, onGainBoostRun = {},
        onDirectPcmDurationChange = {}, onDirectPcmRun = {},
        onCustomSampleRateHzChange = {}, onCustomSampleRateDurationChange = {}, onCustomSampleRateRequest = {},
        onMultiMicDurationChange = {}, onMultiMicStreamsChange = {}, onMultiMicRun = {},
        onDisableEffectsToggle = {},
        onSystemAudioCaptureDurationChange = {}, onSystemAudioCaptureRequest = {},
    )
}
