package com.gadget.torch

import com.gadget.root.core.RootShell
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val MILLIS_PER_SECOND = 1000L
private const val MICROS_PER_MILLI = 1000L
private const val MIN_PHASE_TIMING_MS = 1L
private const val DUTY_PERCENT_DENOMINATOR = 100L
private const val SHELL_TIMEOUT_MARGIN_MS = 1000L

/**
 * Strobes a single LED node with arbitrary on/off durations using a single
 * privileged shell invocation rather than per-cycle round-trips — at high
 * frequency (≥30 Hz) per-call latency would dominate timing. The shell
 * `usleep` builtin (toybox / busybox) provides microsecond resolution.
 *
 * [phaseOffsetMillis] adds a leading delay before the loop starts so a
 * caller can run two strobes 180° out of phase by issuing them
 * concurrently with one offset by `period / 2`.
 */
@Singleton
class DutyCycleStrobe @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun run(
        node: TorchLedNode,
        frequencyHz: Int,
        dutyPercent: Int,
        durationMillis: Long,
        phaseOffsetMillis: Long,
    ) {
        if (frequencyHz <= 0 || durationMillis <= 0) return
        val periodMs = MILLIS_PER_SECOND / frequencyHz
        val onMs = (periodMs * dutyPercent / DUTY_PERCENT_DENOMINATOR).coerceAtLeast(MIN_PHASE_TIMING_MS)
        val offMs = (periodMs - onMs).coerceAtLeast(MIN_PHASE_TIMING_MS)
        val cycles = (durationMillis * frequencyHz / MILLIS_PER_SECOND).coerceAtLeast(1L)

        if (phaseOffsetMillis > 0) delay(phaseOffsetMillis)

        val script = buildString {
            append("max=\$(cat \"").append(node.maxBrightnessPath).append("\");")
            append("for i in \$(seq 1 ").append(cycles).append(");do ")
            append("echo \$max > \"").append(node.brightnessPath).append("\";")
            append("usleep ").append(onMs * MICROS_PER_MILLI).append(';')
            append("echo 0 > \"").append(node.brightnessPath).append("\";")
            append("usleep ").append(offMs * MICROS_PER_MILLI).append(';')
            append("done;")
            append("echo 0 > \"").append(node.brightnessPath).append('"')
        }

        try {
            shell.exec(script, timeoutMillis = durationMillis + SHELL_TIMEOUT_MARGIN_MS)
        } finally {
            withContext(NonCancellable) {
                shell.exec("echo 0 > \"${node.brightnessPath}\"")
            }
        }
    }
}
