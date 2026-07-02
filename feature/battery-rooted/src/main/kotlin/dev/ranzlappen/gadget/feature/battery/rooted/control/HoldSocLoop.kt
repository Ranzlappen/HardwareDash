package dev.ranzlappen.gadget.feature.battery.rooted.control

import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dev.ranzlappen.gadget.feature.battery.control.BatteryControllerResult
import dev.ranzlappen.gadget.feature.battery.control.HoldSocConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal const val HOLD_SOC_MIN_PERCENT = 20
internal const val HOLD_SOC_MAX_PERCENT = 90
internal const val HOLD_SOC_HARD_CEILING_MILLIS = 600_000L
internal const val HOLD_SOC_POLL_INTERVAL_MILLIS = 1_000L
internal const val HOLD_SOC_DEAD_BAND_PERCENT = 1

private const val HOLD_SOC_PSEUDO_PATH = "power_supply://battery/hold_soc"

private val INPUT_SUSPEND_NODES = listOf(
    "/sys/class/power_supply/battery/input_suspend",
    "/sys/class/power_supply/battery/charge_disable",
    "/sys/class/power_supply/battery/charging_enabled",
)
private const val CAPACITY_NODE = "/sys/class/power_supply/battery/capacity"

/**
 * Holds the pack at a target SOC by toggling the first available
 * `input_suspend` / `charge_disable` / `charging_enabled` node every
 * 1 Hz tick. `charging_enabled` uses inverted polarity (1 = charging on).
 */
@Singleton
class HoldSocLoop @Inject constructor(
    private val psuSysfs: PowerSupplySysfs,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun run(config: HoldSocConfig): BatteryControllerResult {
        val node = INPUT_SUSPEND_NODES.firstOrNull { psuSysfs.readNode(it) != null }
            ?: return BatteryControllerResult.Unsupported
        val invertedPolarity = node.endsWith("/charging_enabled")
        val original = psuSysfs.readNode(node)
            ?: return BatteryControllerResult.Unsupported
        val target = config.targetSocPercent
            .coerceIn(HOLD_SOC_MIN_PERCENT, HOLD_SOC_MAX_PERCENT)
        val effectiveDuration = config.durationMillis
            .coerceAtMost(HOLD_SOC_HARD_CEILING_MILLIS)
        val initialSoc = psuSysfs.readNode(CAPACITY_NODE)?.toIntOrNull()
        mutationLog.register(HOLD_SOC_PSEUDO_PATH, original)
        try {
            withTimeoutOrNull(effectiveDuration) {
                while (true) {
                    val soc = psuSysfs.readNode(CAPACITY_NODE)?.toIntOrNull() ?: break
                    val shouldSuspend = soc >= target + HOLD_SOC_DEAD_BAND_PERCENT
                    val shouldResume = soc <= target - HOLD_SOC_DEAD_BAND_PERCENT
                    val writeValue = when {
                        shouldSuspend -> if (invertedPolarity) "0" else "1"
                        shouldResume -> if (invertedPolarity) "1" else "0"
                        else -> null
                    }
                    if (writeValue != null) {
                        psuSysfs.writeNode(node, writeValue)
                    }
                    delay(HOLD_SOC_POLL_INTERVAL_MILLIS)
                }
            }
            return BatteryControllerResult.HoldSocSnapshot(
                appliedTargetSocPercent = target,
                appliedDurationMillis = effectiveDuration,
                initialSocPercent = initialSoc,
            )
        } finally {
            withContext(NonCancellable) {
                if (psuSysfs.writeNode(node, original)) {
                    mutationLog.unregister(HOLD_SOC_PSEUDO_PATH)
                }
            }
        }
    }
}
