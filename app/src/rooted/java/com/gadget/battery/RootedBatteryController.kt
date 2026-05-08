package com.gadget.battery

import com.gadget.root.RootFeatureKey
import com.gadget.root.RootGateDecision
import com.gadget.root.RootSafetyGate
import com.gadget.root.sysfs.SysfsMutationLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

internal const val CHARGING_TYPE_OVERRIDE_HARD_CEILING_MILLIS = 30_000L

private val BATTERY_RESET_PREFIXES = listOf(
    "/sys/class/power_supply/",
    "/sys/class/thermal/",
)
private val PSU_USB_TYPE_NODES = listOf(
    "/sys/class/power_supply/usb/type",
    "/sys/class/power_supply/usb/real_type",
)

/**
 * Rooted-flavor Battery controller. Sub-batch 5d wires fuel-gauge raw +
 * cell monitor + full dump + charging-type override + reset.
 * Charging-profile + thermal-bypass land in 5e and currently return
 * [BatteryControllerResult.Unsupported].
 */
@Singleton
class RootedBatteryController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val psuSysfs: PowerSupplySysfs,
    private val dumpWriter: BatteryDumpWriter,
    private val chargingProfile: ChargingProfileOverride,
    private val thermalBypass: ThermalThrottleBypass,
    private val mutationLog: SysfsMutationLog,
) : BatteryController {

    override suspend fun fuelGaugeRaw(): BatteryControllerResult =
        runGated(RootFeatureKey.BatteryFuelGaugeRaw) {
            val psuDirs = psuSysfs.listPsuDirs()
            if (psuDirs.isEmpty()) return@runGated BatteryControllerResult.Unsupported
            val merged = LinkedHashMap<String, String>()
            for (dir in psuDirs) {
                val name = dir.trimEnd('/').substringAfterLast('/')
                val nodes = psuSysfs.readPsuMap(dir)
                for ((k, v) in nodes) merged["$name.$k"] = v
            }
            BatteryControllerResult.FuelGaugeReading(nodes = merged)
        }

    override suspend fun cellMonitor(): BatteryControllerResult =
        runGated(RootFeatureKey.BatteryCellMonitor) {
            val cells = mutableListOf<CellReading>()
            val psuDirs = psuSysfs.listPsuDirs()
            for (dir in psuDirs) {
                val name = dir.trimEnd('/').substringAfterLast('/')
                if (!name.startsWith("battery") && !name.startsWith("bms")) continue
                val nodes = psuSysfs.readPsuMap(dir)
                for ((node, value) in nodes) {
                    val cellMatch = Regex("cell_?([0-9]+)_(voltage|current|temp)").find(node)
                        ?: continue
                    val index = cellMatch.groupValues[1].toIntOrNull() ?: continue
                    val kind = cellMatch.groupValues[2]
                    val parsed = value.toLongOrNull()
                    val existing = cells.firstOrNull { it.cellIndex == index }
                    val updated = (existing ?: CellReading(index, null, null, null)).let {
                        when (kind) {
                            "voltage" -> it.copy(voltageMicroVolts = parsed)
                            "current" -> it.copy(currentMicroAmps = parsed)
                            "temp" -> it.copy(temperatureDeciCelsius = parsed?.toInt())
                            else -> it
                        }
                    }
                    if (existing == null) cells += updated
                    else cells[cells.indexOf(existing)] = updated
                }
            }
            if (cells.isEmpty()) {
                BatteryControllerResult.Unsupported
            } else {
                BatteryControllerResult.CellSnapshot(cells.sortedBy { it.cellIndex })
            }
        }

    override suspend fun chargingProfile(config: ChargingProfileConfig): BatteryControllerResult =
        runGated(RootFeatureKey.BatteryChargingProfile) { chargingProfile.apply(config) }

    override suspend fun thermalBypass(config: ThermalBypassConfig): BatteryControllerResult =
        runGated(RootFeatureKey.BatteryThermalBypass) { thermalBypass.apply(config) }

    override suspend fun chargingTypeOverride(config: ChargingTypeOverrideConfig): BatteryControllerResult =
        runGated(RootFeatureKey.BatteryChargingTypeOverride) {
            val targetNode = PSU_USB_TYPE_NODES.firstOrNull { node ->
                psuSysfs.readNode(node) != null
            } ?: return@runGated BatteryControllerResult.Unsupported
            val original = psuSysfs.readNode(targetNode)
                ?: return@runGated BatteryControllerResult.Unsupported
            mutationLog.register(targetNode, original)
            val effectiveDuration = config.durationMillis
                .coerceAtMost(CHARGING_TYPE_OVERRIDE_HARD_CEILING_MILLIS)
            try {
                val ok = psuSysfs.writeNode(targetNode, config.type)
                if (!ok) {
                    mutationLog.unregister(targetNode)
                    return@runGated BatteryControllerResult.HardwareError(
                        "write to $targetNode rejected by kernel",
                    )
                }
                delay(effectiveDuration)
                BatteryControllerResult.Ok()
            } finally {
                withContext(NonCancellable) {
                    if (psuSysfs.writeNode(targetNode, original)) {
                        mutationLog.unregister(targetNode)
                    }
                }
            }
        }

    override suspend fun fullDump(): BatteryControllerResult =
        runGated(RootFeatureKey.BatteryFullDump) {
            val psuDirs = psuSysfs.listPsuDirs()
            val psuMap = LinkedHashMap<String, Map<String, String>>()
            for (dir in psuDirs) {
                val name = dir.trimEnd('/').substringAfterLast('/')
                psuMap[name] = psuSysfs.readPsuMap(dir)
            }
            val zones = psuSysfs.listThermalZones().map { psuSysfs.readThermalZone(it) }
            val file = dumpWriter.write(psuMap, zones)
                ?: return@runGated BatteryControllerResult.HardwareError("could not write dump file")
            BatteryControllerResult.DumpWritten(absolutePath = file.absolutePath)
        }

    override suspend fun resetAllBatteryMutations(): BatteryControllerResult {
        val outcome = mutationLog.revertAll(BATTERY_RESET_PREFIXES)
        return BatteryControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> BatteryControllerResult,
    ): BatteryControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it !is BatteryControllerResult.OptedOut &&
                it !is BatteryControllerResult.Unsupported &&
                it !is BatteryControllerResult.RateLimited &&
                it !is BatteryControllerResult.HardwareError
            ) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> BatteryControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            BatteryControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> BatteryControllerResult.Unsupported
    }
}
