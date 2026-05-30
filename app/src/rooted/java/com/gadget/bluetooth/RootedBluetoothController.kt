package com.gadget.bluetooth

import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val BLUETOOTH_RESET_PREFIXES = listOf(
    "/sys/class/bluetooth/",
    "bt-mgmt://",
)
private val BLUETOOTH_TX_POWER_PREFIXES = listOf(
    "bt-mgmt://hci0/txpower",
)

/**
 * Rooted-flavor Bluetooth controller. Routes every privileged call
 * through `RootSafetyGate.check(...)` and delegates to dedicated
 * helpers. Hard cutoffs (TX-power dBm ceiling, active windows) are
 * enforced inside the helpers.
 */
@Singleton
class RootedBluetoothController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val mgmt: BluetoothMgmtHelper,
    private val snoop: HciSnoopHelper,
    private val mutationLog: SysfsMutationLog,
) : BluetoothController {

    override suspend fun rfkillToggle(config: BluetoothRfkillConfig): BluetoothControllerResult =
        runGated(RootFeatureKey.BluetoothRfkillToggle) {
            if (!mgmt.isRfkillAvailable()) return@runGated BluetoothControllerResult.Unsupported
            val ok = mgmt.setRfkillBlocked(config.block)
            if (!ok) return@runGated BluetoothControllerResult.HardwareError("rfkill rejected the toggle")
            val effectiveDuration = config.durationMillis.coerceAtMost(BLUETOOTH_RFKILL_HARD_CEILING_MILLIS)
            try {
                delay(effectiveDuration)
                BluetoothControllerResult.Ok()
            } finally {
                withContext(NonCancellable) { mgmt.setRfkillBlocked(!config.block) }
            }
        }

    override suspend fun txPowerOverride(config: BluetoothTxPowerConfig): BluetoothControllerResult =
        runGated(RootFeatureKey.BluetoothTxPowerOverride) {
            if (!mgmt.isAnyMgmtAvailable()) return@runGated BluetoothControllerResult.Unsupported
            val handle = mgmt.setTxPower(config.targetDbm)
                ?: return@runGated BluetoothControllerResult.HardwareError("could not snapshot or write txpower")
            val effectiveDuration = config.durationMillis.coerceAtMost(BLUETOOTH_TX_POWER_HARD_CEILING_MILLIS)
            try {
                delay(effectiveDuration)
                val effectiveDbm = config.targetDbm.coerceIn(0, BLUETOOTH_TX_POWER_HARD_DBM_CEILING)
                val note = if (effectiveDbm < config.targetDbm) {
                    "Clamped to ${effectiveDbm}dBm (Class-1 ceiling)"
                } else null
                BluetoothControllerResult.Ok(statusNote = note)
            } finally {
                withContext(NonCancellable) { mgmt.restoreTxPower(handle) }
            }
        }

    override suspend fun hciSnoopDump(): BluetoothControllerResult =
        runGated(RootFeatureKey.BluetoothHciSnoopDump) {
            val tail = snoop.tail() ?: return@runGated BluetoothControllerResult.Unsupported
            BluetoothControllerResult.HciSnoopExcerpt(tailLines = tail)
        }

    override suspend fun resetAllBluetoothMutations(): BluetoothControllerResult {
        val outcome = mutationLog.revertAll(BLUETOOTH_RESET_PREFIXES)
        return BluetoothControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    override suspend fun revertTxPowerOnly(): BluetoothControllerResult {
        val outcome = mutationLog.revertAll(BLUETOOTH_TX_POWER_PREFIXES)
        return BluetoothControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> BluetoothControllerResult,
    ): BluetoothControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is BluetoothControllerResult.Ok ||
                it is BluetoothControllerResult.HciSnoopExcerpt
            ) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> BluetoothControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            BluetoothControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> BluetoothControllerResult.Unsupported
    }
}
