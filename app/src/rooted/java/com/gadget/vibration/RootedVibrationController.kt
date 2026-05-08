package com.gadget.vibration

import com.gadget.root.RootFeatureKey
import com.gadget.root.RootGateDecision
import com.gadget.root.RootSafetyGate
import com.gadget.root.core.RootShell
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

internal const val EXTREME_AMPLITUDE_BURST_CAP_MS = 3_000L
internal const val DIRECT_PWM_MAX_DURATION_MS = 30_000L
internal const val DIRECT_PWM_MIN_OFF_MICROS = 5_000L
internal const val AMPLITUDE_MAX_RAW = 255L
internal const val SUSTAINED_RUMBLE_HARD_CAP_MS = 5L * 60L * 1000L
private const val AMPLITUDE_PERCENT_DENOMINATOR = 100L
private const val MICROS_PER_MILLI = 1000L
private const val SHELL_TIMEOUT_MARGIN_MS = 2_000L
private const val RUMBLE_POLL_INTERVAL_MS = 250L

/**
 * Rooted-flavor Vibration controller. Direct sysfs PWM via legacy
 * `timed_output` or modern `/sys/class/leds/vibrator/`, with phased
 * dual-actuator drive and sustained-rumble mode backed by [RumbleMonitor]
 * for battery + temp thresholds.
 */
@Singleton
class RootedVibrationController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val shell: RootShell,
    private val paths: VibrationSysfsPaths,
    private val dualActuator: DualActuatorDriver,
    private val rumbleMonitor: RumbleMonitor,
) : VibrationController {

    override suspend fun extremeAmplitude(
        amplitudePercent: Int,
        durationMillis: Long,
    ): VibrationControllerResult = runGated(RootFeatureKey.VibrationExtremeAmplitude) {
        val nodes = paths.resolve()
        val cappedDuration = durationMillis.coerceAtMost(EXTREME_AMPLITUDE_BURST_CAP_MS)
        val rawAmplitude = ((amplitudePercent.toLong() * AMPLITUDE_MAX_RAW) /
            AMPLITUDE_PERCENT_DENOMINATOR).coerceIn(0L, AMPLITUDE_MAX_RAW)
        when {
            nodes.primaryLed != null -> driveModernLed(nodes.primaryLed, rawAmplitude, cappedDuration)
            nodes.legacyTimedOutput != null -> driveLegacyTimedOutput(nodes.legacyTimedOutput, cappedDuration)
            else -> VibrationControllerResult.Unsupported
        }
    }

    override suspend fun directPwm(pattern: List<PwmPulse>): VibrationControllerResult =
        runGated(RootFeatureKey.VibrationDirectPwm) {
            val nodes = paths.resolve()
            val node = nodes.primaryLed
                ?: return@runGated VibrationControllerResult.Unsupported
            val totalMicros = pattern.sumOf { it.onMicros + it.offMicros.coerceAtLeast(DIRECT_PWM_MIN_OFF_MICROS) }
            val totalMs = totalMicros / MICROS_PER_MILLI
            if (totalMs > DIRECT_PWM_MAX_DURATION_MS) {
                return@runGated VibrationControllerResult.HardwareError(
                    "pattern total ${totalMs}ms exceeds cap ${DIRECT_PWM_MAX_DURATION_MS}ms",
                )
            }
            executePwmPattern(node, pattern, totalMs)
        }

    override suspend fun dualActuator(
        lraPattern: List<PwmPulse>,
        ermPattern: List<PwmPulse>,
        phaseOffsetMicros: Long,
    ): VibrationControllerResult = runGated(RootFeatureKey.VibrationDualActuator) {
        val nodes = paths.resolve()
        val lra = nodes.lra ?: return@runGated VibrationControllerResult.Unsupported
        val erm = nodes.erm ?: return@runGated VibrationControllerResult.Unsupported
        dualActuator.drive(lra, erm, lraPattern, ermPattern, phaseOffsetMicros)
        VibrationControllerResult.Ok
    }

    override suspend fun sustainedRumble(
        durationMillis: Long,
        amplitudePercent: Int,
    ): VibrationControllerResult = runGated(RootFeatureKey.VibrationSustainedRumble) {
        val nodes = paths.resolve()
        val node = nodes.primaryLed ?: return@runGated VibrationControllerResult.Unsupported
        val cappedDuration = durationMillis.coerceAtMost(SUSTAINED_RUMBLE_HARD_CAP_MS)
        val rawAmplitude = ((amplitudePercent.toLong() * AMPLITUDE_MAX_RAW) /
            AMPLITUDE_PERCENT_DENOMINATOR).coerceIn(0L, AMPLITUDE_MAX_RAW)

        coroutineScope {
            val abortReason = AtomicReference<String?>(null)
            val monitorJob = launch {
                rumbleMonitor.monitor(cappedDuration) { reason -> abortReason.set(reason) }
            }
            try {
                shell.exec(
                    buildString {
                        append("echo $rawAmplitude > \"$node/state\" 2>/dev/null;")
                        append("echo $cappedDuration > \"$node/duration\";")
                        append("echo 1 > \"$node/activate\"")
                    },
                )
                val deadline = System.currentTimeMillis() + cappedDuration
                while (coroutineContext.isActive &&
                    System.currentTimeMillis() < deadline &&
                    abortReason.get() == null
                ) {
                    delay(RUMBLE_POLL_INTERVAL_MS)
                }
                abortReason.get()?.let { VibrationControllerResult.HardwareError(it) }
                    ?: VibrationControllerResult.Ok
            } finally {
                withContext(NonCancellable) {
                    monitorJob.cancel()
                    shell.exec("echo 0 > \"$node/activate\"")
                }
            }
        }
    }

    private suspend fun driveModernLed(
        nodeBase: String,
        rawAmplitude: Long,
        durationMs: Long,
    ): VibrationControllerResult {
        val script = buildString {
            append("echo $rawAmplitude > \"$nodeBase/state\" 2>/dev/null;")
            append("echo $durationMs > \"$nodeBase/duration\";")
            append("echo 1 > \"$nodeBase/activate\"")
        }
        val result = shell.exec(script)
        return if (result.isSuccess) {
            VibrationControllerResult.Ok
        } else {
            VibrationControllerResult.HardwareError(
                "modern-led drive failed: ${result.stderr.firstOrNull().orEmpty()}",
            )
        }
    }

    private suspend fun driveLegacyTimedOutput(
        nodeBase: String,
        durationMs: Long,
    ): VibrationControllerResult {
        val result = shell.exec("echo $durationMs > \"$nodeBase/enable\"")
        return if (result.isSuccess) {
            VibrationControllerResult.Ok
        } else {
            VibrationControllerResult.HardwareError(
                "legacy timed_output failed: ${result.stderr.firstOrNull().orEmpty()}",
            )
        }
    }

    private suspend fun executePwmPattern(
        nodeBase: String,
        pattern: List<PwmPulse>,
        totalMs: Long,
    ): VibrationControllerResult {
        val script = buildString {
            pattern.forEach { pulse ->
                val on = pulse.onMicros.coerceAtLeast(0L)
                val off = pulse.offMicros.coerceAtLeast(DIRECT_PWM_MIN_OFF_MICROS)
                append("echo 1 > \"$nodeBase/activate\";")
                append("usleep $on;")
                append("echo 0 > \"$nodeBase/activate\";")
                append("usleep $off;")
            }
        }
        try {
            val result = shell.exec(script, timeoutMillis = totalMs + SHELL_TIMEOUT_MARGIN_MS)
            if (!result.isSuccess) {
                return VibrationControllerResult.HardwareError(
                    "pwm drive failed: ${result.stderr.firstOrNull().orEmpty()}",
                )
            }
            return VibrationControllerResult.Ok
        } finally {
            withContext(NonCancellable) {
                shell.exec("echo 0 > \"$nodeBase/activate\"")
            }
        }
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> VibrationControllerResult,
    ): VibrationControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is VibrationControllerResult.Ok) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> VibrationControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            VibrationControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> VibrationControllerResult.Unsupported
    }
}
