package com.gadget.vibration

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Vibration controller. Every extreme-tier method returns
 * [VibrationControllerResult.Unsupported] — the standard APK has no
 * privileged shell, so direct sysfs PWM writes are impossible regardless
 * of permissions.
 */
@Singleton
class StandardVibrationController @Inject constructor() : VibrationController {

    override suspend fun extremeAmplitude(
        amplitudePercent: Int,
        durationMillis: Long,
    ): VibrationControllerResult = VibrationControllerResult.Unsupported

    override suspend fun directPwm(pattern: List<PwmPulse>): VibrationControllerResult =
        VibrationControllerResult.Unsupported

    override suspend fun dualActuator(
        lraPattern: List<PwmPulse>,
        ermPattern: List<PwmPulse>,
        phaseOffsetMicros: Long,
    ): VibrationControllerResult = VibrationControllerResult.Unsupported

    override suspend fun sustainedRumble(
        durationMillis: Long,
        amplitudePercent: Int,
    ): VibrationControllerResult = VibrationControllerResult.Unsupported
}
