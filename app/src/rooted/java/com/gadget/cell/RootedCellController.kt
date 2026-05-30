package com.gadget.cell

import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted-flavor Cellular controller. All methods are read-only — the
 * Batch-6 plan deliberately omits an AT-command write path because the
 * diagnostic nodes are Qualcomm-specific and OEM-locked on most
 * devices.
 */
@Singleton
class RootedCellController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val modemDiag: ModemDiagHelper,
) : CellController {

    override suspend fun rawModemDump(): CellControllerResult =
        runGated(RootFeatureKey.CellRawModemDump) {
            val nodes = modemDiag.dumpModem()
            if (nodes.isEmpty()) {
                CellControllerResult.Unsupported
            } else {
                CellControllerResult.ModemDump(nodes)
            }
        }

    override suspend fun signalDeepDump(): CellControllerResult =
        runGated(RootFeatureKey.CellSignalDeepDump) {
            val nodes = modemDiag.dumpSignalDeep()
            if (nodes.isEmpty()) {
                CellControllerResult.Unsupported
            } else {
                CellControllerResult.SignalDeepDump(nodes)
            }
        }

    override suspend fun resetAllCellMutations(): CellControllerResult =
        CellControllerResult.ResetCompleted(restored = 0, failed = 0)

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> CellControllerResult,
    ): CellControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is CellControllerResult.ModemDump || it is CellControllerResult.SignalDeepDump) {
                safetyGate.recordInvocation(feature)
            }
        }
        RootGateDecision.BlockedByUser -> CellControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            CellControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> CellControllerResult.Unsupported
    }
}
