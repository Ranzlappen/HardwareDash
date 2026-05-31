package dev.ranzlappen.gadget.feature.vibration.rooted

import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.feature.vibration.PwmPulse
import dev.ranzlappen.gadget.feature.vibration.VibrationRootAvailability
import dev.ranzlappen.gadget.feature.vibration.VibrationRootCapabilities
import dev.ranzlappen.gadget.feature.vibration.VibrationRootResult
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
private const val NORMAL_MAX_PERCENT = 100
// Direct PWM can over-drive the motor briefly; advertise a modest headroom
// ceiling so the monitoring chart scales above the 100% standard cap. The
// per-command hardware clamps (raw 0..255) still bound the actual drive.
private const val EXTREME_MAX_PERCENT = 100

/**
 * Rooted-flavor Vibration capabilities — the modular
 * [VibrationRootCapabilities] adapter, implemented directly over the privileged
 * sysfs surface (libsu `RootShell`). Ports the legacy
 * `com.gadget.vibration.RootedVibrationController` verbatim (direct sysfs PWM
 * via legacy `timed_output` or modern `/sys/class/leds/vibrator/`, phased
 * dual-actuator drive, sustained rumble guarded by [RumbleMonitor]) and folds
 * in the modular seam's probe + live amplitude/ceiling flows.
 *
 * Every privileged call routes through [RootSafetyGate] (capability + opt-out +
 * rate-limit) keyed by a `RootFeatureKey.Vibration*`, keeps its hard ceilings
 * (3 s burst, 5 ms PWM off-floor, 80 % dual clamp, 5 min rumble), and restores
 * device state in a `NonCancellable finally`.
 */
@Singleton
class RootedVibrationRootCapabilities @Inject constructor(
    private val registry: RootCapabilityRegistry,
    private val safetyGate: RootSafetyGate,
    private val shell: RootShell,
    private val paths: VibrationSysfsPaths,
    private val dualActuatorDriver: DualActuatorDriver,
    private val rumbleMonitor: RumbleMonitor,
) : VibrationRootCapabilities {

    override val isRootedFlavor: Boolean get() = registry.isRootedFlavor

    private val _maxAmplitudePercent = MutableStateFlow(NORMAL_MAX_PERCENT)
    override val maxAmplitudePercentFlow: StateFlow<Int> = _maxAmplitudePercent.asStateFlow()

    private val _commandedAmplitudePercent = MutableStateFlow(0)
    override val commandedAmplitudePercent: StateFlow<Int> = _commandedAmplitudePercent.asStateFlow()

    override fun hasRootAccess(): Boolean = registry.hasRootAccess()

    override suspend fun probe(): VibrationRootAvailability {
        registry.probe()
        val rootAccess = registry.hasRootAccess()
        val nodes = if (rootAccess) paths.resolve() else VibrationNodeSet.EMPTY
        val nodeFound = rootAccess && nodes.anyAvailable
        _maxAmplitudePercent.value = if (nodeFound) EXTREME_MAX_PERCENT else NORMAL_MAX_PERCENT
        return VibrationRootAvailability(
            rootedFlavor = registry.isRootedFlavor,
            rootAccess = rootAccess,
            nodeFound = nodeFound,
            hasDualActuators = nodes.hasDualActuators,
        )
    }

    override suspend fun extremeAmplitude(
        amplitudePercent: Int,
        durationMillis: Long,
    ): VibrationRootResult = runGated(RootFeatureKey.VibrationExtremeAmplitude) {
        val nodes = paths.resolve()
        val cappedDuration = durationMillis.coerceAtMost(EXTREME_AMPLITUDE_BURST_CAP_MS)
        val rawAmplitude = ((amplitudePercent.toLong() * AMPLITUDE_MAX_RAW) /
            AMPLITUDE_PERCENT_DENOMINATOR).coerceIn(0L, AMPLITUDE_MAX_RAW)
        val result = when {
            nodes.primaryLed != null -> driveModernLed(nodes.primaryLed, rawAmplitude, cappedDuration)
            nodes.legacyTimedOutput != null -> driveLegacyTimedOutput(nodes.legacyTimedOutput, cappedDuration)
            else -> VibrationRootResult.Unsupported
        }
        if (result is VibrationRootResult.Ok) _commandedAmplitudePercent.value =
            amplitudePercent.coerceIn(0, EXTREME_MAX_PERCENT)
        result
    }

    override suspend fun directPwm(pattern: List<PwmPulse>): VibrationRootResult =
        runGated(RootFeatureKey.VibrationDirectPwm) {
            val nodes = paths.resolve()
            val node = nodes.primaryLed
                ?: return@runGated VibrationRootResult.Unsupported
            val totalMicros = pattern.sumOf { it.onMicros + it.offMicros.coerceAtLeast(DIRECT_PWM_MIN_OFF_MICROS) }
            val totalMs = totalMicros / MICROS_PER_MILLI
            if (totalMs > DIRECT_PWM_MAX_DURATION_MS) {
                return@runGated VibrationRootResult.Error(
                    "pattern total ${totalMs}ms exceeds cap ${DIRECT_PWM_MAX_DURATION_MS}ms",
                )
            }
            executePwmPattern(node, pattern, totalMs)
        }

    override suspend fun dualActuator(
        lraPattern: List<PwmPulse>,
        ermPattern: List<PwmPulse>,
        phaseOffsetMicros: Long,
    ): VibrationRootResult = runGated(RootFeatureKey.VibrationDualActuator) {
        val nodes = paths.resolve()
        val lra = nodes.lra ?: return@runGated VibrationRootResult.Unsupported
        val erm = nodes.erm ?: return@runGated VibrationRootResult.Unsupported
        dualActuatorDriver.drive(lra, erm, lraPattern, ermPattern, phaseOffsetMicros)
        VibrationRootResult.Ok
    }

    override suspend fun sustainedRumble(
        durationMillis: Long,
        amplitudePercent: Int,
    ): VibrationRootResult = runGated(RootFeatureKey.VibrationSustainedRumble) {
        val nodes = paths.resolve()
        val node = nodes.primaryLed ?: return@runGated VibrationRootResult.Unsupported
        val cappedDuration = durationMillis.coerceAtMost(SUSTAINED_RUMBLE_HARD_CAP_MS)
        val rawAmplitude = ((amplitudePercent.toLong() * AMPLITUDE_MAX_RAW) /
            AMPLITUDE_PERCENT_DENOMINATOR).coerceIn(0L, AMPLITUDE_MAX_RAW)

        coroutineScope {
            val abortReason = AtomicReference<String?>(null)
            val monitorJob = launch {
                rumbleMonitor.monitor(cappedDuration) { reason -> abortReason.set(reason) }
            }
            _commandedAmplitudePercent.value = amplitudePercent.coerceIn(0, EXTREME_MAX_PERCENT)
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
                abortReason.get()?.let { VibrationRootResult.Error(it) }
                    ?: VibrationRootResult.Ok
            } finally {
                withContext(NonCancellable) {
                    monitorJob.cancel()
                    shell.exec("echo 0 > \"$node/activate\"")
                    _commandedAmplitudePercent.value = 0
                }
            }
        }
    }

    private suspend fun driveModernLed(
        nodeBase: String,
        rawAmplitude: Long,
        durationMs: Long,
    ): VibrationRootResult {
        val script = buildString {
            append("echo $rawAmplitude > \"$nodeBase/state\" 2>/dev/null;")
            append("echo $durationMs > \"$nodeBase/duration\";")
            append("echo 1 > \"$nodeBase/activate\"")
        }
        val result = shell.exec(script)
        return if (result.isSuccess) {
            VibrationRootResult.Ok
        } else {
            VibrationRootResult.Error("modern-led drive failed: ${result.stderr.firstOrNull().orEmpty()}")
        }
    }

    private suspend fun driveLegacyTimedOutput(
        nodeBase: String,
        durationMs: Long,
    ): VibrationRootResult {
        val result = shell.exec("echo $durationMs > \"$nodeBase/enable\"")
        return if (result.isSuccess) {
            VibrationRootResult.Ok
        } else {
            VibrationRootResult.Error("legacy timed_output failed: ${result.stderr.firstOrNull().orEmpty()}")
        }
    }

    private suspend fun executePwmPattern(
        nodeBase: String,
        pattern: List<PwmPulse>,
        totalMs: Long,
    ): VibrationRootResult {
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
                return VibrationRootResult.Error("pwm drive failed: ${result.stderr.firstOrNull().orEmpty()}")
            }
            return VibrationRootResult.Ok
        } finally {
            withContext(NonCancellable) {
                shell.exec("echo 0 > \"$nodeBase/activate\"")
            }
        }
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> VibrationRootResult,
    ): VibrationRootResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is VibrationRootResult.Ok) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> VibrationRootResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            VibrationRootResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> VibrationRootResult.Unsupported
    }
}
