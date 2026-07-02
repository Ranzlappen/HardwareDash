package dev.ranzlappen.gadget.feature.gps.rooted.control

import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.feature.gps.control.GpsController
import dev.ranzlappen.gadget.feature.gps.control.GpsControllerResult
import dev.ranzlappen.gadget.feature.gps.control.NmeaTapConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted-flavor GPS controller. All methods are read-only; per the
 * Batch-6 plan, baseline `LocationManager` continues to drive every
 * non-rooted code path unchanged.
 */
@Singleton
class RootedGpsController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val nmeaTap: NmeaTapHelper,
    private val constellationDump: ConstellationDumpHelper,
) : GpsController {

    override suspend fun nmeaRawTap(config: NmeaTapConfig): GpsControllerResult =
        runGated(RootFeatureKey.GpsNmeaRawTap) {
            val sentences = nmeaTap.tap(config.durationMillis)
                ?: return@runGated GpsControllerResult.Unsupported
            GpsControllerResult.NmeaSnapshot(sentences = sentences)
        }

    override suspend fun constellationDump(): GpsControllerResult =
        runGated(RootFeatureKey.GpsConstellationDump) {
            val sats = constellationDump.dump()
            if (sats.isEmpty()) {
                GpsControllerResult.Unsupported
            } else {
                GpsControllerResult.ConstellationSnapshot(satellites = sats)
            }
        }

    override suspend fun resetAllGpsMutations(): GpsControllerResult =
        GpsControllerResult.ResetCompleted(restored = 0, failed = 0)

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> GpsControllerResult,
    ): GpsControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is GpsControllerResult.NmeaSnapshot ||
                it is GpsControllerResult.ConstellationSnapshot
            ) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> GpsControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            GpsControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> GpsControllerResult.Unsupported
    }
}
