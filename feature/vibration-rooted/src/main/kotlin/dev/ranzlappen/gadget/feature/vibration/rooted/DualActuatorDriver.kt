package dev.ranzlappen.gadget.feature.vibration.rooted

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.feature.vibration.PwmPulse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

internal const val DUAL_ACTUATOR_AMPLITUDE_CLAMP_PERCENT = 80
private const val AMPLITUDE_MAX_RAW_LOCAL = 255L
private const val PERCENT_DENOMINATOR_LOCAL = 100L
private const val MICROS_PER_MILLI_LOCAL = 1000L
private const val SHELL_TIMEOUT_MARGIN_MS_LOCAL = 2_000L

/**
 * Plays paired patterns on the LRA and ERM motors concurrently. Each pattern
 * runs in its own coroutine + shell invocation; phase alignment is achieved by
 * delaying the ERM coroutine before its shell call so the two timelines start
 * exactly `phaseOffsetMicros` apart.
 *
 * Per-actuator amplitude is hard-clamped at 80 % of raw to keep peak current
 * within either motor's spec when both fire in phase. Ported verbatim from the
 * legacy `com.gadget.vibration.DualActuatorDriver`.
 */
@Singleton
class DualActuatorDriver @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun drive(
        lraNode: String,
        ermNode: String,
        lraPattern: List<PwmPulse>,
        ermPattern: List<PwmPulse>,
        phaseOffsetMicros: Long,
    ) = coroutineScope {
        val lraJob = async { playPattern(lraNode, lraPattern) }
        val ermJob = async {
            if (phaseOffsetMicros > 0) delay(phaseOffsetMicros / MICROS_PER_MILLI_LOCAL)
            playPattern(ermNode, ermPattern)
        }
        awaitAll(lraJob, ermJob)
    }

    private suspend fun playPattern(nodeBase: String, pattern: List<PwmPulse>) {
        val rawAmplitude = (AMPLITUDE_MAX_RAW_LOCAL * DUAL_ACTUATOR_AMPLITUDE_CLAMP_PERCENT) /
            PERCENT_DENOMINATOR_LOCAL
        val totalMs = pattern.sumOf {
            (it.onMicros + it.offMicros.coerceAtLeast(DIRECT_PWM_MIN_OFF_MICROS)) /
                MICROS_PER_MILLI_LOCAL
        }
        val script = buildString {
            append("echo $rawAmplitude > \"$nodeBase/state\" 2>/dev/null;")
            pattern.forEach { pulse ->
                val on = pulse.onMicros.coerceAtLeast(0L)
                val off = pulse.offMicros.coerceAtLeast(DIRECT_PWM_MIN_OFF_MICROS)
                append("echo 1 > \"$nodeBase/activate\";")
                append("usleep $on;")
                append("echo 0 > \"$nodeBase/activate\";")
                append("usleep $off;")
            }
        }
        shell.exec(script, timeoutMillis = totalMs + SHELL_TIMEOUT_MARGIN_MS_LOCAL)
    }
}
