package dev.ranzlappen.gadget.feature.vibration.standard

import dev.ranzlappen.gadget.feature.vibration.PwmPulse
import dev.ranzlappen.gadget.feature.vibration.VibrationRootAvailability
import dev.ranzlappen.gadget.feature.vibration.VibrationRootCapabilities
import dev.ranzlappen.gadget.feature.vibration.VibrationRootResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor no-op for the modular Vibration root seam. The extreme-tier
 * capabilities never exist without the rooted build, so availability is always
 * [VibrationRootAvailability.Unavailable] and every action reports
 * [VibrationRootResult.Unsupported]. Shared UI hides the root controls and the
 * per-function badges render red ("requires the rooted app version").
 *
 * Amplitude is standard-capped on this flavor: [maxAmplitudePercentFlow] never
 * leaves 100 and [commandedAmplitudePercent] stays a constant 0 (the metric
 * falls back to the standard controller's modelled reading).
 *
 * Mirror of `:feature:vibration-rooted`'s `RootedVibrationRootCapabilities`, so
 * neither flavor's Vibration impls live in `:app`.
 */
@Singleton
class StandardVibrationRootCapabilities @Inject constructor() : VibrationRootCapabilities {
    override val isRootedFlavor: Boolean = false
    override val maxAmplitudePercentFlow: StateFlow<Int> = MutableStateFlow(100).asStateFlow()
    override val commandedAmplitudePercent: StateFlow<Int> = MutableStateFlow(0)
    override fun hasRootAccess(): Boolean = false
    override suspend fun probe(): VibrationRootAvailability = VibrationRootAvailability.Unavailable
    override suspend fun extremeAmplitude(
        amplitudePercent: Int,
        durationMillis: Long,
    ): VibrationRootResult = VibrationRootResult.Unsupported
    override suspend fun directPwm(pattern: List<PwmPulse>): VibrationRootResult =
        VibrationRootResult.Unsupported
    override suspend fun dualActuator(
        lraPattern: List<PwmPulse>,
        ermPattern: List<PwmPulse>,
        phaseOffsetMicros: Long,
    ): VibrationRootResult = VibrationRootResult.Unsupported
    override suspend fun sustainedRumble(
        durationMillis: Long,
        amplitudePercent: Int,
    ): VibrationRootResult = VibrationRootResult.Unsupported
}
