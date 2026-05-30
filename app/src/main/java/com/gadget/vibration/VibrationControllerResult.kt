package com.gadget.vibration

/**
 * Result returned by every [VibrationController] extreme-tier method. Same
 * shape as [dev.ranzlappen.gadget.feature.torch.legacy.LegacyTorchControllerResult] — a separate type lets
 * each surface evolve independently and gives compile-time errors if a
 * caller passes a torch result to a vibration handler by mistake.
 */
sealed class VibrationControllerResult {
    data object Ok : VibrationControllerResult()
    data object Unsupported : VibrationControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : VibrationControllerResult()
    data object OptedOut : VibrationControllerResult()
    data class HardwareError(val message: String) : VibrationControllerResult()
}

/**
 * Single on/off pulse for raw-PWM patterns. Microsecond resolution where the
 * underlying shell `usleep` supports it; the wave generator enforces a
 * minimum 5 ms off-time floor regardless of input to prevent near-DC drive
 * stalling the motor.
 */
data class PwmPulse(val onMicros: Long, val offMicros: Long)

/**
 * Logical haptic actuator. Many flagship phones expose both an LRA (linear
 * resonant actuator — fast, precise) and an ERM (eccentric rotating mass —
 * broader, heavier feel). Some devices only have one; the dual-actuator
 * controller probes for both and falls back to whichever exists.
 */
enum class HapticActuator { Primary, Lra, Erm }
