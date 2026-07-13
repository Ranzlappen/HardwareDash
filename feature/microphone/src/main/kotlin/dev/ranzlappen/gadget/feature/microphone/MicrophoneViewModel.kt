package dev.ranzlappen.gadget.feature.microphone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.feature.microphone.control.CustomRateConfig
import dev.ranzlappen.gadget.feature.microphone.control.DirectPcmConfig
import dev.ranzlappen.gadget.feature.microphone.control.GainBoostConfig
import dev.ranzlappen.gadget.feature.microphone.control.MicrophoneController
import dev.ranzlappen.gadget.feature.microphone.control.MicrophoneControllerResult
import dev.ranzlappen.gadget.feature.microphone.control.MultiMicConfig
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The rooted-tools panel state for the microphone screen (W6 in-screen write-tier surface). */
data class MicrophoneRootToolsState(
    val disableEffects: RootActionState = RootActionState(),
)

/**
 * ViewModel for [MicrophoneScreen]. Wires the single [MicrophoneController]
 * seam (standard-flavor no-op / rooted-flavor ALSA+tinycap impl, resolved by
 * the app-level Hilt binding — never branches on `BuildConfig.IS_ROOTED`),
 * owns the pending (session-only) input for each of the six tool rows, and
 * owns the two confirm-dialog flows (custom-sample-rate kernel-lockup-risk
 * ack, system-audio-capture call-recording-legality ack).
 *
 * Every dispatch result is emitted once on [resultEvents] for the screen to
 * surface as a snackbar — mirrors `VibrationViewModel.rootToolEvents`.
 */
@HiltViewModel
class MicrophoneViewModel @Inject constructor(
    private val controller: MicrophoneController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor

    private val _state = MutableStateFlow(MicrophoneScreenState(isRootedFlavor = isRootedFlavor))
    val state: StateFlow<MicrophoneScreenState> = _state.asStateFlow()

    private val _resultEvents = MutableSharedFlow<MicrophoneControllerResult>(extraBufferCapacity = 1)
    val resultEvents: SharedFlow<MicrophoneControllerResult> = _resultEvents.asSharedFlow()

    private val _rootTools = MutableStateFlow(MicrophoneRootToolsState())

    /** Live status of the rooted, confirm-gated write-tier microphone actions (W6 in-screen surface). */
    val rootTools: StateFlow<MicrophoneRootToolsState> = _rootTools.asStateFlow()

    /** Confirm-gated write action: disables the system mic effects (AGC / NS / AEC) via the root seam. */
    fun onDisableEffects() {
        viewModelScope.launch {
            _rootTools.update { it.copy(disableEffects = it.disableEffects.copy(running = true)) }
            val result = controller.disableEffects()
            _rootTools.update { it.copy(disableEffects = result.toActionState()) }
        }
    }

    private fun MicrophoneControllerResult.toActionState(): RootActionState = when (this) {
        MicrophoneControllerResult.Ok ->
            RootActionState(message = "Effects disabled")
        MicrophoneControllerResult.Unsupported ->
            RootActionState(message = "Requires the rooted app version", isError = true)
        is MicrophoneControllerResult.RateLimited ->
            RootActionState(message = "Rate limited — retry in ${retryAfterMillis}ms", isError = true)
        MicrophoneControllerResult.OptedOut ->
            RootActionState(message = "Blocked by your root-safety opt-out", isError = true)
        is MicrophoneControllerResult.HardwareError ->
            RootActionState(message = message, isError = true)
    }

    fun onEvent(event: MicrophoneUiEvent) {
        when (event) {
            // ─── Gain boost ─────────────────────────────────────────────
            is MicrophoneUiEvent.GainBoostDbChange -> _state.update {
                it.copy(
                    gainBoostDb = event.db.coerceIn(
                        MicrophoneScreenState.MIN_GAIN_BOOST_DB,
                        MicrophoneScreenState.MAX_GAIN_BOOST_DB,
                    ),
                )
            }
            is MicrophoneUiEvent.GainBoostDurationChange -> _state.update {
                it.copy(gainBoostDurationMs = event.durationMs.coerceIn(0L, MicrophoneScreenState.MAX_GAIN_BOOST_DURATION_MS))
            }
            MicrophoneUiEvent.GainBoostRun -> runTool(
                inFlight = { it.copy(gainBoostInFlight = true) },
                idle = { it.copy(gainBoostInFlight = false) },
            ) {
                val s = _state.value
                controller.gainBoost(GainBoostConfig(boostDb = s.gainBoostDb, durationMillis = s.gainBoostDurationMs))
            }

            // ─── Direct PCM ─────────────────────────────────────────────
            is MicrophoneUiEvent.DirectPcmDurationChange -> _state.update {
                it.copy(
                    directPcmDurationMs = event.durationMs.coerceIn(
                        MicrophoneScreenState.MIN_DIRECT_PCM_DURATION_MS,
                        MicrophoneScreenState.MAX_DIRECT_PCM_DURATION_MS,
                    ),
                )
            }
            MicrophoneUiEvent.DirectPcmRun -> runTool(
                inFlight = { it.copy(directPcmInFlight = true) },
                idle = { it.copy(directPcmInFlight = false) },
            ) {
                val s = _state.value
                controller.directPcm(
                    DirectPcmConfig(
                        sampleRate = MicrophoneScreenState.DIRECT_PCM_SAMPLE_RATE_HZ,
                        channelCount = MicrophoneScreenState.DIRECT_PCM_CHANNEL_COUNT,
                        bitsPerSample = MicrophoneScreenState.DIRECT_PCM_BITS_PER_SAMPLE,
                        durationMillis = s.directPcmDurationMs,
                    ),
                )
            }

            // ─── Custom sample rate ───────────────────────────────────────
            is MicrophoneUiEvent.CustomSampleRateHzChange -> _state.update {
                it.copy(
                    customSampleRateHz = event.hz.coerceIn(
                        MicrophoneScreenState.MIN_CUSTOM_SAMPLE_RATE_HZ,
                        MicrophoneScreenState.MAX_CUSTOM_SAMPLE_RATE_HZ,
                    ),
                )
            }
            is MicrophoneUiEvent.CustomSampleRateDurationChange -> _state.update {
                it.copy(
                    customSampleRateDurationMs = event.durationMs.coerceIn(
                        0L,
                        MicrophoneScreenState.MAX_CUSTOM_RATE_DURATION_MS,
                    ),
                )
            }
            MicrophoneUiEvent.CustomSampleRateRequest -> _state.update { it.copy(showCustomSampleRateConfirm = true) }
            MicrophoneUiEvent.CustomSampleRateDismiss -> _state.update { it.copy(showCustomSampleRateConfirm = false) }
            MicrophoneUiEvent.CustomSampleRateConfirm -> {
                _state.update { it.copy(showCustomSampleRateConfirm = false) }
                runTool(
                    inFlight = { it.copy(customSampleRateInFlight = true) },
                    idle = { it.copy(customSampleRateInFlight = false) },
                ) {
                    val s = _state.value
                    controller.customSampleRate(
                        CustomRateConfig(
                            targetSampleRate = s.customSampleRateHz,
                            durationMillis = s.customSampleRateDurationMs,
                        ),
                    )
                }
            }

            // ─── Multi-mic raw ────────────────────────────────────────────
            is MicrophoneUiEvent.MultiMicDurationChange -> _state.update {
                it.copy(
                    multiMicDurationMs = event.durationMs.coerceIn(0L, MicrophoneScreenState.MAX_MULTI_MIC_DURATION_MS),
                )
            }
            is MicrophoneUiEvent.MultiMicStreamsChange -> _state.update {
                it.copy(
                    multiMicStreams = event.streams.coerceIn(
                        MicrophoneScreenState.MIN_MULTI_MIC_STREAMS,
                        MicrophoneScreenState.MAX_MULTI_MIC_STREAMS,
                    ),
                )
            }
            MicrophoneUiEvent.MultiMicRun -> runTool(
                inFlight = { it.copy(multiMicInFlight = true) },
                idle = { it.copy(multiMicInFlight = false) },
            ) {
                val s = _state.value
                controller.multiMicRaw(MultiMicConfig(durationMillis = s.multiMicDurationMs, maxStreams = s.multiMicStreams))
            }

            // ─── Disable hardware noise suppression ───────────────────────
            MicrophoneUiEvent.DisableEffectsToggle -> {
                if (_state.value.disableEffectsInFlight) return
                runTool(
                    inFlight = { it.copy(disableEffectsInFlight = true) },
                    idle = { it.copy(disableEffectsInFlight = false) },
                ) {
                    controller.disableEffects()
                }
            }

            // ─── System audio capture ──────────────────────────────────────
            is MicrophoneUiEvent.SystemAudioCaptureDurationChange -> _state.update {
                it.copy(
                    systemAudioCaptureDurationMs = event.durationMs.coerceIn(
                        MicrophoneScreenState.MIN_SYSTEM_AUDIO_DURATION_MS,
                        MicrophoneScreenState.MAX_SYSTEM_AUDIO_DURATION_MS,
                    ),
                )
            }
            MicrophoneUiEvent.SystemAudioCaptureRequest -> {
                if (_state.value.systemAudioCaptureInFlight) return
                _state.update { it.copy(showSystemAudioCaptureConfirm = true) }
            }
            MicrophoneUiEvent.SystemAudioCaptureDismiss -> _state.update { it.copy(showSystemAudioCaptureConfirm = false) }
            MicrophoneUiEvent.SystemAudioCaptureConfirm -> {
                _state.update { it.copy(showSystemAudioCaptureConfirm = false) }
                runTool(
                    inFlight = { it.copy(systemAudioCaptureInFlight = true) },
                    idle = { it.copy(systemAudioCaptureInFlight = false) },
                ) {
                    controller.systemAudioCapture(_state.value.systemAudioCaptureDurationMs)
                }
            }
        }
    }

    /**
     * Runs a suspend controller call: flips the row's `*InFlight` flag on,
     * dispatches [action] in [viewModelScope], flips it back off, and emits
     * the [MicrophoneControllerResult] once on [resultEvents]. Every one of
     * the six controller methods self-restores hardware state internally
     * (snapshot/finally), so there's nothing else for the ViewModel to undo.
     */
    private fun runTool(
        inFlight: (MicrophoneScreenState) -> MicrophoneScreenState,
        idle: (MicrophoneScreenState) -> MicrophoneScreenState,
        action: suspend () -> MicrophoneControllerResult,
    ) {
        _state.update(inFlight)
        viewModelScope.launch {
            val result = action()
            _state.update(idle)
            _resultEvents.tryEmit(result)
        }
    }
}
