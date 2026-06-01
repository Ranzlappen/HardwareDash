package dev.ranzlappen.gadget.feature.vibration.rooted

import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Probes the kernel for vibration sysfs nodes. Two driver eras coexist:
 *
 *   - Legacy `timed_output` (Android 4.0–8.x kernels): write a duration in
 *     ms to `/sys/class/timed_output/vibrator/enable`. No amplitude control.
 *   - Modern LED-based DRV2624 / similar (Android 9+): write `duration` then
 *     `1` to `activate`, optionally `pattern` for sequences. Some flavors
 *     expose two nodes for dual-actuator phones (LRA + ERM).
 *
 * Resolution is one-shot per process — the path table is read once from the
 * kernel via root and cached. Ported verbatim from the legacy
 * `com.gadget.vibration.VibrationSysfsPaths` (re-packaged into
 * `:feature:vibration-rooted`).
 */
@Singleton
class VibrationSysfsPaths @Inject constructor(
    private val shell: RootShell,
) {
    private var cached: VibrationNodeSet? = null
    private var probed = false

    suspend fun resolve(): VibrationNodeSet {
        if (!probed) {
            cached = probeAll()
            probed = true
        }
        return cached ?: VibrationNodeSet.EMPTY
    }

    private suspend fun probeAll(): VibrationNodeSet {
        val primaryLed = LED_PRIMARY_CANDIDATES.firstOrNull { existsAt("$it/activate") }
        val legacy = if (existsAt(LEGACY_TIMED_OUTPUT)) LEGACY_TIMED_OUTPUT else null
        val lra = LRA_CANDIDATES.firstOrNull { existsAt("$it/activate") }
        val erm = ERM_CANDIDATES.firstOrNull { existsAt("$it/activate") }
        return VibrationNodeSet(
            primaryLed = primaryLed,
            legacyTimedOutput = legacy,
            lra = lra,
            erm = erm,
        )
    }

    private suspend fun existsAt(path: String): Boolean =
        shell.exec("test -e \"$path\"").isSuccess

    private companion object {
        const val LEGACY_TIMED_OUTPUT = "/sys/class/timed_output/vibrator"

        val LED_PRIMARY_CANDIDATES = listOf(
            "/sys/class/leds/vibrator",
            "/sys/class/leds/vibrator_0",
        )

        val LRA_CANDIDATES = listOf(
            "/sys/class/leds/vibrator-l",
            "/sys/class/leds/vibrator_lra",
            "/sys/class/leds/vibrator-lra",
        )

        val ERM_CANDIDATES = listOf(
            "/sys/class/leds/vibrator-e",
            "/sys/class/leds/vibrator_erm",
            "/sys/class/leds/vibrator-erm",
        )
    }
}

data class VibrationNodeSet(
    val primaryLed: String?,
    val legacyTimedOutput: String?,
    val lra: String?,
    val erm: String?,
) {
    val anyAvailable: Boolean
        get() = primaryLed != null || legacyTimedOutput != null || lra != null || erm != null

    val hasDualActuators: Boolean get() = lra != null && erm != null

    companion object {
        val EMPTY = VibrationNodeSet(null, null, null, null)
    }
}
