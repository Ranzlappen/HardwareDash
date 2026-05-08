package com.gadget.vibration

/**
 * Rooted-only Vibration capability surface. Bypasses `android.os.Vibrator`
 * entirely on the rooted flavor — writes are direct to
 * `/sys/class/timed_output/vibrator/` (legacy) or
 * `/sys/class/leds/vibrator/` (modern Qualcomm DRV2624).
 *
 * Standard flavor returns [VibrationControllerResult.Unsupported] for every
 * call so shared UI uses one code path.
 */
interface VibrationController {

    /**
     * Drives the primary motor at [amplitudePercent] (0–100) for
     * [durationMillis]. Higher than the API's clamp because we write the
     * PWM duty cycle directly. Hard-capped at 3 s burst with forced
     * cooldown by the implementation.
     */
    suspend fun extremeAmplitude(
        amplitudePercent: Int,
        durationMillis: Long,
    ): VibrationControllerResult

    /**
     * Plays an arbitrary PWM micro-pattern. The implementation enforces a
     * minimum 5 ms off-time per pulse; values lower than that are clamped.
     */
    suspend fun directPwm(pattern: List<PwmPulse>): VibrationControllerResult

    /**
     * Drives LRA + ERM motors with independent patterns aligned at
     * [phaseOffsetMicros]. Per-actuator amplitude clamp at 80 % is enforced
     * inside the driver.
     */
    suspend fun dualActuator(
        lraPattern: List<PwmPulse>,
        ermPattern: List<PwmPulse>,
        phaseOffsetMicros: Long = 0L,
    ): VibrationControllerResult

    /**
     * Continuous low-amplitude rumble for [durationMillis], with battery-
     * drain and motor-temperature monitoring that aborts early if either
     * threshold is breached. Hard-capped at 5 minutes.
     */
    suspend fun sustainedRumble(
        durationMillis: Long,
        amplitudePercent: Int,
    ): VibrationControllerResult
}
