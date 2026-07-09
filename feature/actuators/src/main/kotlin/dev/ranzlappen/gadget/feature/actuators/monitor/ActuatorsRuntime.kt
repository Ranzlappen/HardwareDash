package dev.ranzlappen.gadget.feature.actuators.monitor

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
 * Process-wide source of truth for the **modelled** actuator-pulse signal.
 *
 * `ActuatorsActionHandler`'s haptic actions are fire-and-forget one-shots
 * (`Vibrator.vibrate`): the OS gives no "currently vibrating" query, so —
 * same idea as `:feature:vibration`'s `VibrationRuntime` for its own
 * non-continuously-readable actuator — the signal is modelled instead of
 * polled from hardware. Every dispatched haptic action calls [notifyTriggered]
 * with the effect's approximate duration; [pulsePercent] jumps to 100 and
 * decays back to 0 when that duration elapses. A new trigger replaces any
 * in-flight decay (replace-on-new).
 *
 * This is the single signal [ActuatorsMetricSource] polls, producing a
 * filled plateau on the monitoring chart for the duration of each haptic
 * pulse.
 */
@Singleton
class ActuatorsRuntime @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _pulsePercent = MutableStateFlow(0f)

    /** Hot, conflated 0..100 signal of the current modelled pulse state. */
    val pulsePercent: StateFlow<Float> = _pulsePercent.asStateFlow()

    /** Most-recent scheduled decay; replaced (cancelled) on every new trigger. */
    private var decayJob: Job? = null

    /**
     * Record a haptic action having just been dispatched to the vibrator,
     * approximated to last [durationMillis]. Cancels any prior in-flight
     * decay so the chart tracks the newest trigger (replace-on-new).
     */
    fun notifyTriggered(durationMillis: Long) {
        decayJob?.cancel()
        _pulsePercent.value = PULSE_PERCENT
        decayJob = scope.launch {
            delay(durationMillis)
            _pulsePercent.value = 0f
        }
    }

    companion object {
        private const val PULSE_PERCENT = 100f
    }
}
