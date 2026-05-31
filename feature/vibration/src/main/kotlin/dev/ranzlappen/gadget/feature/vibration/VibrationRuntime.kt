package dev.ranzlappen.gadget.feature.vibration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide source of truth for the **modelled** vibration signal.
 *
 * Vibration is fire-and-forget: unlike the torch (whose `CameraManager`
 * exposes a readable on/off state), the OS gives no "currently vibrating at
 * X%" query. So we model it. Every command publishes its commanded amplitude
 * here; a timed command schedules a **decay** that resets the amplitude to 0
 * when its duration elapses, and a new command **replaces** any in-flight
 * decay (replace-on-new). A `loop = true` / sustained command sets
 * [setActive] with no decay and stays until [clear].
 *
 * This is the single signal [VibrationMetricSource] polls — so the monitoring
 * chart shows a filled plateau at the commanded amplitude that drops to 0 the
 * moment the command ends. It's the deliberate generalization test of the
 * `MetricSource` poll contract for a non-pollable actuator.
 *
 * Writers: [StandardVibrationController] (standard haptics) and the rooted
 * [VibrationRootCapabilities] impl (extreme-tier) both fold their commanded
 * amplitude in here. Readers: [VibrationMetricSource] (`.value`) and the
 * widgets' state observer.
 */
@Singleton
class VibrationRuntime @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(VibrationState())

    /** Hot, conflated signal of the current modelled vibration state. */
    val state: StateFlow<VibrationState> = _state.asStateFlow()

    /** Most-recent scheduled decay; replaced (cancelled) on every new command. */
    private var decayJob: Job? = null

    /**
     * Publish device availability + amplitude-control capability once known
     * (from the standard controller's init). Leaves the live amplitude/active
     * fields untouched.
     */
    fun setCapabilities(isAvailable: Boolean, hasAmplitudeControl: Boolean) {
        _state.value = _state.value.copy(
            isAvailable = isAvailable,
            hasAmplitudeControl = hasAmplitudeControl,
        )
    }

    /**
     * Record a **timed** command at [amplitudePercent] that auto-resets to
     * idle after [durationMillis]. Cancels any prior in-flight decay so the
     * chart tracks the newest command (replace-on-new). [amplitudePercent] is
     * clamped to 0..[ceiling] — pass the live capability ceiling so a rooted
     * boost above 100 reads correctly.
     */
    fun setCommand(amplitudePercent: Int, durationMillis: Long, ceiling: Int = MAX_PERCENT) {
        val clamped = amplitudePercent.coerceIn(0, ceiling)
        decayJob?.cancel()
        _state.value = _state.value.copy(amplitudePercent = clamped, isActive = clamped > 0)
        if (clamped > 0 && durationMillis > 0) {
            decayJob = scope.launch {
                delay(durationMillis)
                _state.value = _state.value.copy(amplitudePercent = 0, isActive = false)
            }
        }
    }

    /**
     * Record a **sustained** command at [amplitudePercent] that holds until an
     * explicit [clear] (looping waveform / sustained rumble). No decay.
     */
    fun setSustained(amplitudePercent: Int, ceiling: Int = MAX_PERCENT) {
        val clamped = amplitudePercent.coerceIn(0, ceiling)
        decayJob?.cancel()
        decayJob = null
        _state.value = _state.value.copy(amplitudePercent = clamped, isActive = clamped > 0)
    }

    /** Mark whether *something* is actively playing without changing the
     *  commanded amplitude (used by playback bookkeeping). */
    fun setActive(active: Boolean) {
        _state.value = _state.value.copy(isActive = active)
    }

    /** Reset to idle (amplitude 0, inactive) and cancel any pending decay. */
    fun clear() {
        decayJob?.cancel()
        decayJob = null
        _state.value = _state.value.copy(amplitudePercent = 0, isActive = false)
    }

    companion object {
        const val MAX_PERCENT = 100
    }
}
