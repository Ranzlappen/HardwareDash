package com.gadget.battery

import com.gadget.root.sysfs.SysfsMutationLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

internal const val WIRELESS_COIL_HARD_CEILING_UA = 1_500_000L
internal const val WIRELESS_COIL_HARD_DURATION_MILLIS = 30_000L

private const val WIRELESS_COIL_PSEUDO_PATH = "power_supply://wireless/coil_current"

private val WIRELESS_COIL_NODES = listOf(
    "/sys/class/power_supply/wireless/current_max",
    "/sys/class/power_supply/wireless/input_current_max",
    "/sys/class/power_supply/wireless/dc_icl",
    "/sys/class/power_supply/dc/current_max",
)

/**
 * Caps wireless-coil charging current. Hard-clamps the requested value at
 * 1.5 A (1 500 000 µA) and the active window at 30 s, regardless of caller
 * intent. Snapshot+restore via `power_supply://wireless/coil_current`.
 */
@Singleton
class WirelessCoilWriter @Inject constructor(
    private val psuSysfs: PowerSupplySysfs,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun apply(config: WirelessCoilCurrentConfig): BatteryControllerResult {
        val node = WIRELESS_COIL_NODES.firstOrNull { psuSysfs.readNode(it) != null }
            ?: return BatteryControllerResult.Unsupported
        val original = psuSysfs.readNode(node)
            ?: return BatteryControllerResult.Unsupported
        val priorMicroAmps = original.toLongOrNull()
        val applied = config.maxCurrentMicroAmps
            .coerceAtMost(WIRELESS_COIL_HARD_CEILING_UA)
            .coerceAtLeast(0L)
        val effectiveDuration = config.durationMillis
            .coerceAtMost(WIRELESS_COIL_HARD_DURATION_MILLIS)
        mutationLog.register(WIRELESS_COIL_PSEUDO_PATH, original)
        try {
            val ok = psuSysfs.writeNode(node, applied.toString())
            if (!ok) {
                mutationLog.unregister(WIRELESS_COIL_PSEUDO_PATH)
                return BatteryControllerResult.HardwareError(
                    "write to $node rejected by kernel",
                )
            }
            delay(effectiveDuration)
            return BatteryControllerResult.WirelessCoilSnapshot(
                appliedCoilCurrentMicroAmps = applied,
                priorCoilCurrentMicroAmps = priorMicroAmps,
            )
        } finally {
            withContext(NonCancellable) {
                if (psuSysfs.writeNode(node, original)) {
                    mutationLog.unregister(WIRELESS_COIL_PSEUDO_PATH)
                }
            }
        }
    }
}
