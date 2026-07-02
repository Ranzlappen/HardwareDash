package dev.ranzlappen.gadget.feature.radios.ir.rooted.control

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

internal const val IR_RAW_BURST_HARD_CEILING_MILLIS = 5_000L
internal const val IR_RAW_DUTY_CYCLE_MAX_PERCENT = 50
private const val IR_LED_BRIGHTNESS_PATH_GLOB = "ls -1d /sys/class/leds/*ir*/brightness 2>/dev/null"

/**
 * Direct GPIO toggling of the IR LED via the IR-LED brightness sysfs node.
 * Enforces ≤ 50 % duty cycle and 5-second hard burst ceiling regardless
 * of caller input. Snapshot+restore via the shared mutation log so
 * "Reset" cleans up if the on/off loop gets interrupted mid-cycle.
 */
@Singleton
class IrLedSysfsHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun pulse(config: IrRawPatternConfig): IrControllerResult {
        val targetPath = locateBrightnessNode() ?: return IrControllerResult.Unsupported

        val effectiveDuration = config.totalDurationMillis.coerceAtMost(IR_RAW_BURST_HARD_CEILING_MILLIS)
        val onClamp = config.onMillis.coerceAtLeast(1L)
        val offClamp = config.offMillis.coerceAtLeast(1L)
        val period = onClamp + offClamp
        val dutyPct = (onClamp * 100 / period).toInt()
        val safeDutyPct = dutyPct.coerceAtMost(IR_RAW_DUTY_CYCLE_MAX_PERCENT)
        val safeOnMs = (period * safeDutyPct / 100).coerceAtLeast(1L)
        val safeOffMs = (period - safeOnMs).coerceAtLeast(1L)

        val original = readNode(targetPath) ?: return IrControllerResult.HardwareError(
            "could not read original brightness from $targetPath",
        )
        mutationLog.register(targetPath, original)

        val deadline = System.currentTimeMillis() + effectiveDuration
        return try {
            while (System.currentTimeMillis() < deadline) {
                shell.exec("echo 255 > \"$targetPath\"")
                delay(safeOnMs)
                shell.exec("echo 0 > \"$targetPath\"")
                delay(safeOffMs)
            }
            val note = if (safeDutyPct < dutyPct) {
                "Duty cycle clamped from ${dutyPct}% to ${safeDutyPct}%"
            } else null
            IrControllerResult.Ok(statusNote = note)
        } finally {
            withContext(NonCancellable) {
                val ok = shell.exec("echo \"$original\" > \"$targetPath\"").isSuccess
                if (ok) mutationLog.unregister(targetPath)
            }
        }
    }

    private suspend fun locateBrightnessNode(): String? {
        val ls = shell.exec(IR_LED_BRIGHTNESS_PATH_GLOB)
        if (!ls.isSuccess) return null
        return ls.stdout
            .flatMap { it.trim().split(Regex("\\s+")) }
            .firstOrNull { it.startsWith("/sys/class/leds/") && it.endsWith("/brightness") }
    }

    private suspend fun readNode(path: String): String? {
        val r = shell.exec("cat \"$path\" 2>/dev/null")
        if (!r.isSuccess) return null
        return r.stdout.firstOrNull()?.trim()
    }
}
