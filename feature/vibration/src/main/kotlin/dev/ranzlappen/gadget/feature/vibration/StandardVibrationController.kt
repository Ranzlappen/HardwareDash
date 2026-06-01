package dev.ranzlappen.gadget.feature.vibration

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor [VibrationController] backed by `VibratorManager` (API 31+)
 * or the legacy `Vibrator` system service below that.
 *
 * Resolves the default vibrator + its `hasAmplitudeControl()` capability at
 * construction and publishes both into the shared [VibrationRuntime] so the
 * screen + the monitoring chart see one signal. Each command also folds its
 * commanded amplitude into the runtime (timed commands decay; looping stays).
 *
 * Privileged extreme-tier control (direct sysfs PWM, dual-actuator, sustained
 * rumble) is **not** here — it's the separate [VibrationRootCapabilities] seam,
 * bound to a real impl only on the rooted flavor (`:feature:vibration-rooted`).
 */
@Singleton
class StandardVibrationController @Inject constructor(
    @ApplicationContext context: Context,
    private val runtime: VibrationRuntime,
) : VibrationController {

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val available: Boolean = vibrator?.hasVibrator() == true
    private val amplitudeControl: Boolean = available && vibrator?.hasAmplitudeControl() == true

    override val state: StateFlow<VibrationState> = runtime.state

    init {
        runtime.setCapabilities(isAvailable = available, hasAmplitudeControl = amplitudeControl)
    }

    override fun oneShot(amplitudePercent: Int, durationMillis: Long) {
        val vib = vibrator ?: return
        if (!available || durationMillis <= 0) return
        val percent = amplitudePercent.coerceIn(1, MAX_PERCENT)
        val amplitude = if (amplitudeControl) percentToRaw(percent) else VibrationEffect.DEFAULT_AMPLITUDE
        vib.vibrate(VibrationEffect.createOneShot(durationMillis, amplitude))
        // Model the signal at the *commanded* percent so the chart plateaus
        // even when the device ignores amplitude (DEFAULT_AMPLITUDE).
        runtime.setCommand(percent, durationMillis)
    }

    override fun playPattern(timingsMillis: LongArray, amplitudes: IntArray, loop: Boolean) {
        val vib = vibrator ?: return
        if (!available || timingsMillis.isEmpty()) return
        val repeatIndex = if (loop) 0 else -1
        val effect = if (amplitudeControl && amplitudes.size == timingsMillis.size) {
            VibrationEffect.createWaveform(timingsMillis, amplitudes.map(::clampRaw).toIntArray(), repeatIndex)
        } else {
            VibrationEffect.createWaveform(timingsMillis, repeatIndex)
        }
        vib.vibrate(effect)
        // Use the peak commanded amplitude for the modelled plateau; total
        // duration = sum of the on/off segments (a looping pattern stays
        // sustained until stop()).
        val peakPercent = amplitudes.maxOrNull()?.let { rawToPercent(it) } ?: PATTERN_FALLBACK_PERCENT
        if (loop) {
            runtime.setSustained(peakPercent)
        } else {
            runtime.setCommand(peakPercent, timingsMillis.sum())
        }
    }

    override fun playPredefined(effect: VibrationPredefinedEffect) {
        val vib = vibrator ?: return
        if (!available || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val (effectId, approxDurationMs, approxPercent) = when (effect) {
            VibrationPredefinedEffect.Click -> Triple(VibrationEffect.EFFECT_CLICK, 40L, 80)
            VibrationPredefinedEffect.DoubleClick -> Triple(VibrationEffect.EFFECT_DOUBLE_CLICK, 120L, 80)
            VibrationPredefinedEffect.Tick -> Triple(VibrationEffect.EFFECT_TICK, 20L, 40)
            VibrationPredefinedEffect.HeavyClick -> Triple(VibrationEffect.EFFECT_HEAVY_CLICK, 60L, 100)
        }
        vib.vibrate(VibrationEffect.createPredefined(effectId))
        // Predefined effects have no queryable duration; model a short plateau
        // so the primitive still registers on the chart.
        runtime.setCommand(approxPercent, approxDurationMs)
    }

    override fun stop() {
        vibrator?.cancel()
        runtime.clear()
    }

    private companion object {
        const val MAX_PERCENT = 100
        const val RAW_MAX = 255
        const val PATTERN_FALLBACK_PERCENT = 100

        fun percentToRaw(percent: Int): Int = (percent * RAW_MAX / MAX_PERCENT).coerceIn(1, RAW_MAX)
        fun rawToPercent(raw: Int): Int =
            if (raw <= 0) PATTERN_FALLBACK_PERCENT else (raw * MAX_PERCENT / RAW_MAX).coerceIn(1, MAX_PERCENT)
        fun clampRaw(raw: Int): Int =
            if (raw == VibrationController.AMPLITUDE_DEFAULT) VibrationEffect.DEFAULT_AMPLITUDE
            else raw.coerceIn(0, RAW_MAX)
    }
}
