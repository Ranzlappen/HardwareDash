package com.gadget.ir

import com.gadget.root.RootFeatureKey
import com.gadget.root.RootGateDecision
import com.gadget.root.RootSafetyGate
import com.gadget.root.sysfs.SysfsMutationLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val IR_RESET_PREFIXES = listOf(
    "/sys/class/lirc/",
    "/sys/class/leds/",
)

/**
 * Rooted-flavor IR controller. Hard cutoffs (carrier 20–100 kHz,
 * 30 s carrier window, 5 s raw-pattern burst, ≤ 50 % duty cycle)
 * are enforced inside the helpers.
 */
@Singleton
class RootedIrController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val carrier: LircCarrierHelper,
    private val ledSysfs: IrLedSysfsHelper,
    private val mutationLog: SysfsMutationLog,
) : IrController {

    override suspend fun customCarrier(config: IrCarrierConfig): IrControllerResult =
        runGated(RootFeatureKey.IrCustomCarrier) {
            val handle = carrier.setCarrier(config.carrierHz)
                ?: return@runGated IrControllerResult.Unsupported
            val effectiveDuration = config.durationMillis.coerceAtMost(IR_CARRIER_HARD_CEILING_MILLIS)
            try {
                delay(effectiveDuration)
                val effectiveHz = config.carrierHz.coerceIn(IR_CARRIER_HARD_LOW_HZ, IR_CARRIER_HARD_HIGH_HZ)
                val note = if (effectiveHz != config.carrierHz) {
                    "Clamped to ${effectiveHz}Hz (20-100kHz allow-range)"
                } else null
                IrControllerResult.Ok(statusNote = note)
            } finally {
                withContext(NonCancellable) { carrier.restoreCarrier(handle) }
            }
        }

    override suspend fun rawGpioPattern(config: IrRawPatternConfig): IrControllerResult =
        runGated(RootFeatureKey.IrRawGpioPattern) { ledSysfs.pulse(config) }

    override suspend fun resetAllIrMutations(): IrControllerResult {
        val outcome = mutationLog.revertAll(IR_RESET_PREFIXES)
        return IrControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> IrControllerResult,
    ): IrControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is IrControllerResult.Ok) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> IrControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            IrControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> IrControllerResult.Unsupported
    }
}
