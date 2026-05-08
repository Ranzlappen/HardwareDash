package com.gadget.nfc

import com.gadget.root.RootFeatureKey
import com.gadget.root.RootGateDecision
import com.gadget.root.RootSafetyGate
import com.gadget.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

private val NFC_RESET_PREFIXES = listOf("/sys/class/nfc/")

/**
 * Rooted-flavor NFC controller. Currently exposes raw NCI command
 * exchange only.
 */
@Singleton
class RootedNfcController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val nciHelper: NciCommandHelper,
    private val mutationLog: SysfsMutationLog,
) : NfcController {

    override suspend fun sendRawNciCommand(config: RawNciCommandConfig): NfcControllerResult =
        runGated(RootFeatureKey.NfcRawNciCommand) { nciHelper.send(config.payloadHex) }

    override suspend fun resetAllNfcMutations(): NfcControllerResult {
        val outcome = mutationLog.revertAll(NFC_RESET_PREFIXES)
        return NfcControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> NfcControllerResult,
    ): NfcControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is NfcControllerResult.Ok || it is NfcControllerResult.NciResponse) {
                safetyGate.recordInvocation(feature)
            }
        }
        RootGateDecision.BlockedByUser -> NfcControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            NfcControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> NfcControllerResult.Unsupported
    }
}
