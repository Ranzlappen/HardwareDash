package dev.ranzlappen.gadget.feature.microphone

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.GadgetDialog
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewSizeClasses
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.microphone.components.MicrophoneToolsCard

/**
 * Builds this module's [ModuleInfo]: root required for every one of the six
 * extreme-tier rows (there is genuinely zero standard-tier functionality —
 * `StandardMicrophoneController` returns `Unsupported` for all six methods),
 * plus the OS-compatibility floor. Mirrors `cameraModuleInfo` / `irModuleInfo`
 * — one [ModuleCapability] per controller method so
 * [dev.ranzlappen.gadget.core.ui.module.ModuleCapabilitiesSection] renders a
 * live green/amber row per function.
 */
@Composable
internal fun microphoneModuleInfo(isRootedFlavor: Boolean): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 1),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.microphone_cap_gain_boost_name),
            detail = stringResource(R.string.microphone_cap_gain_boost_detail),
            status = { rootedCapabilityStatus(isRootedFlavor) },
        ),
        ModuleCapability(
            name = stringResource(R.string.microphone_cap_direct_pcm_name),
            detail = stringResource(R.string.microphone_cap_direct_pcm_detail),
            status = { rootedCapabilityStatus(isRootedFlavor) },
        ),
        ModuleCapability(
            name = stringResource(R.string.microphone_cap_custom_rate_name),
            detail = stringResource(R.string.microphone_cap_custom_rate_detail),
            status = { rootedCapabilityStatus(isRootedFlavor) },
        ),
        ModuleCapability(
            name = stringResource(R.string.microphone_cap_multi_mic_name),
            detail = stringResource(R.string.microphone_cap_multi_mic_detail),
            status = { rootedCapabilityStatus(isRootedFlavor) },
        ),
        ModuleCapability(
            name = stringResource(R.string.microphone_cap_disable_effects_name),
            detail = stringResource(R.string.microphone_cap_disable_effects_detail),
            status = { rootedCapabilityStatus(isRootedFlavor) },
        ),
        ModuleCapability(
            name = stringResource(R.string.microphone_cap_system_audio_name),
            detail = stringResource(R.string.microphone_cap_system_audio_detail),
            status = { rootedCapabilityStatus(isRootedFlavor) },
        ),
    ),
)

@Composable
private fun rootedCapabilityStatus(isRootedFlavor: Boolean): CapabilityStatus = if (isRootedFlavor) {
    CapabilityStatus(kind = GadgetStatusKind.Success, message = stringResource(R.string.microphone_cap_rooted_active))
} else {
    CapabilityStatus(kind = GadgetStatusKind.Warning, message = stringResource(R.string.microphone_cap_rooted_required))
}

/**
 * Stateless screen content — a single [MicrophoneScreenState] snapshot plus a
 * flat [MicrophoneUiEvent] dispatcher. Hilt-free so it's directly usable from
 * previews and instrumented tests. Mirror of `VibrationScreenContent`.
 *
 * Owns the two mandatory confirm dialogs inline (rather than inside
 * [MicrophoneToolsCard]) since they gate the whole screen, not just their
 * originating row: the custom-sample-rate kernel-lockup-risk warning and the
 * system-audio-capture call-recording-legality disclaimer.
 */
@Composable
internal fun MicrophoneScreenContent(
    state: MicrophoneScreenState,
    onEvent: (MicrophoneUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    rootTools: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.microphone_screen_title),
        modifier = modifier,
        moduleInfo = microphoneModuleInfo(state.isRootedFlavor),
        functional = {
            MicrophoneToolsCard(
                state = state,
                onGainBoostDbChange = { onEvent(MicrophoneUiEvent.GainBoostDbChange(it)) },
                onGainBoostDurationChange = { onEvent(MicrophoneUiEvent.GainBoostDurationChange(it)) },
                onGainBoostRun = { onEvent(MicrophoneUiEvent.GainBoostRun) },
                onDirectPcmDurationChange = { onEvent(MicrophoneUiEvent.DirectPcmDurationChange(it)) },
                onDirectPcmRun = { onEvent(MicrophoneUiEvent.DirectPcmRun) },
                onCustomSampleRateHzChange = { onEvent(MicrophoneUiEvent.CustomSampleRateHzChange(it)) },
                onCustomSampleRateDurationChange = { onEvent(MicrophoneUiEvent.CustomSampleRateDurationChange(it)) },
                onCustomSampleRateRequest = { onEvent(MicrophoneUiEvent.CustomSampleRateRequest) },
                onMultiMicDurationChange = { onEvent(MicrophoneUiEvent.MultiMicDurationChange(it)) },
                onMultiMicStreamsChange = { onEvent(MicrophoneUiEvent.MultiMicStreamsChange(it)) },
                onMultiMicRun = { onEvent(MicrophoneUiEvent.MultiMicRun) },
                onDisableEffectsToggle = { onEvent(MicrophoneUiEvent.DisableEffectsToggle) },
                onSystemAudioCaptureDurationChange = { onEvent(MicrophoneUiEvent.SystemAudioCaptureDurationChange(it)) },
                onSystemAudioCaptureRequest = { onEvent(MicrophoneUiEvent.SystemAudioCaptureRequest) },
            )
            rootTools()
        },
    )

    if (state.showCustomSampleRateConfirm) {
        GadgetDialog(
            onDismissRequest = { onEvent(MicrophoneUiEvent.CustomSampleRateDismiss) },
            icon = Icons.Outlined.WarningAmber,
            title = stringResource(R.string.microphone_custom_rate_confirm_title),
            text = stringResource(R.string.microphone_custom_rate_confirm_body),
            confirmButton = {
                GadgetPrimaryButton(
                    onClick = { onEvent(MicrophoneUiEvent.CustomSampleRateConfirm) },
                    text = stringResource(R.string.microphone_custom_rate_confirm_action),
                )
            },
            dismissButton = {
                GadgetTertiaryButton(
                    onClick = { onEvent(MicrophoneUiEvent.CustomSampleRateDismiss) },
                    text = stringResource(R.string.microphone_confirm_cancel),
                )
            },
        )
    }

    if (state.showSystemAudioCaptureConfirm) {
        GadgetDialog(
            onDismissRequest = { onEvent(MicrophoneUiEvent.SystemAudioCaptureDismiss) },
            icon = Icons.Outlined.WarningAmber,
            title = stringResource(R.string.microphone_system_audio_confirm_title),
            text = stringResource(R.string.microphone_system_audio_confirm_body),
            confirmButton = {
                GadgetPrimaryButton(
                    onClick = { onEvent(MicrophoneUiEvent.SystemAudioCaptureConfirm) },
                    text = stringResource(R.string.microphone_system_audio_confirm_action),
                )
            },
            dismissButton = {
                GadgetTertiaryButton(
                    onClick = { onEvent(MicrophoneUiEvent.SystemAudioCaptureDismiss) },
                    text = stringResource(R.string.microphone_confirm_cancel),
                )
            },
        )
    }
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@GadgetPreviewSizeClasses
@Composable
private fun MicrophoneScreenRootedPreview() = GadgetThemedPreview {
    MicrophoneScreenContent(
        state = MicrophoneScreenState(isRootedFlavor = true),
        onEvent = {},
    )
}

@GadgetPreviewLightDark
@Composable
private fun MicrophoneScreenStandardPreview() = GadgetThemedPreview {
    MicrophoneScreenContent(
        state = MicrophoneScreenState(isRootedFlavor = false),
        onEvent = {},
    )
}

@GadgetPreviewLightDark
@Composable
private fun MicrophoneScreenCustomRateConfirmPreview() = GadgetThemedPreview {
    MicrophoneScreenContent(
        state = MicrophoneScreenState(isRootedFlavor = true, showCustomSampleRateConfirm = true),
        onEvent = {},
    )
}

@GadgetPreviewLightDark
@Composable
private fun MicrophoneScreenSystemAudioConfirmPreview() = GadgetThemedPreview {
    MicrophoneScreenContent(
        state = MicrophoneScreenState(isRootedFlavor = true, showSystemAudioCaptureConfirm = true),
        onEvent = {},
    )
}
