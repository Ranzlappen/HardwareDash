package com.gadget.sensors

import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val SENSORS_RESET_PREFIXES = listOf(
    "/sys/bus/iio/devices/",
    "/sys/class/sensors/",
)

/**
 * Rooted-flavor Sensors controller. Sub-batch 5b wires high-polling +
 * raw-unfiltered + sysfs-read + reset; overclock + fusion-override +
 * hidden-enumeration land in 5c. Until then, those methods route through
 * the safety gate (so the limiter still counts them if invoked) but
 * return [SensorsControllerResult.Unsupported] at the impl layer.
 */
@Singleton
class RootedSensorsController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val iio: IioSensorsHelper,
    private val overclocker: SensorOverclocker,
    private val fusionOverride: MultiFusionOverride,
    private val hiddenEnumerator: HiddenSensorEnumerator,
    private val mutationLog: SysfsMutationLog,
) : SensorsController {

    override suspend fun highPolling(config: HighPollingConfig): SensorsControllerResult =
        runGated(RootFeatureKey.SensorsHighPolling) {
            val expertGate = safetyGate.check(RootFeatureKey.SensorsHighPollingExpert)
            val ceiling = if (expertGate is RootGateDecision.Allowed) {
                SENSORS_HIGH_POLL_EXPERT_HZ_CEILING
            } else {
                SENSORS_HIGH_POLL_DEFAULT_HZ_CEILING
            }
            val effectiveHz = config.requestedHz.coerceIn(1, ceiling)
            val handle = iio.setSamplingFrequency(config.sensorTag, effectiveHz)
                ?: return@runGated SensorsControllerResult.Unsupported
            val effectiveDuration = config.durationMillis
                .coerceAtMost(SENSORS_HIGH_POLL_HARD_CEILING_MILLIS)
            try {
                delay(effectiveDuration)
                val note = if (effectiveHz < config.requestedHz) {
                    "Clamped to ${effectiveHz}Hz (expert key not opted in)"
                } else {
                    null
                }
                SensorsControllerResult.Ok(statusNote = note)
            } finally {
                withContext(NonCancellable) { iio.restoreSamplingFrequency(handle) }
                if (expertGate is RootGateDecision.Allowed) {
                    safetyGate.recordInvocation(RootFeatureKey.SensorsHighPollingExpert)
                }
            }
        }

    override suspend fun rawUnfiltered(config: RawUnfilteredConfig): SensorsControllerResult =
        runGated(RootFeatureKey.SensorsRawUnfiltered) {
            val handle = iio.disableFilters(config.sensorTag)
                ?: return@runGated SensorsControllerResult.Unsupported
            val effectiveDuration = config.durationMillis
                .coerceAtMost(SENSORS_RAW_UNFILTERED_HARD_CEILING_MILLIS)
            try {
                delay(effectiveDuration)
                SensorsControllerResult.Ok()
            } finally {
                withContext(NonCancellable) { iio.restoreFilters(handle) }
            }
        }

    override suspend fun readSysfs(): SensorsControllerResult =
        runGated(RootFeatureKey.SensorsSysfsRead) {
            val devices = iio.listIioDevices() + iio.listLegacySensorDirs()
            if (devices.isEmpty()) return@runGated SensorsControllerResult.Unsupported
            val readings = iio.readSysfsTriples(devices)
            SensorsControllerResult.SysfsRead(nodeReadings = readings)
        }

    override suspend fun overclock(config: OverclockConfig): SensorsControllerResult =
        runGated(RootFeatureKey.SensorsOverclock) { overclocker.overclock(config) }

    override suspend fun fusionOverride(config: FusionOverrideConfig): SensorsControllerResult =
        runGated(RootFeatureKey.SensorsFusionOverride) { fusionOverride.apply(config) }

    override suspend fun enumerateHidden(): SensorsControllerResult =
        runGated(RootFeatureKey.SensorsHiddenEnumeration) {
            val nodes = hiddenEnumerator.enumerate()
            if (nodes.isEmpty()) {
                SensorsControllerResult.Unsupported
            } else {
                SensorsControllerResult.EnumerationCompleted(nodes)
            }
        }

    override suspend fun resetAllSensorMutations(): SensorsControllerResult {
        val outcome = mutationLog.revertAll(SENSORS_RESET_PREFIXES)
        return SensorsControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> SensorsControllerResult,
    ): SensorsControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is SensorsControllerResult.Ok ||
                it is SensorsControllerResult.SysfsRead ||
                it is SensorsControllerResult.EnumerationCompleted
            ) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> SensorsControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            SensorsControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> SensorsControllerResult.Unsupported
    }
}
