package com.gadget.wifi

import com.gadget.root.RootFeatureKey
import com.gadget.root.RootGateDecision
import com.gadget.root.RootSafetyGate
import com.gadget.root.sysfs.SysfsMutationLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val WIFI_RESET_PREFIXES = listOf(
    "/sys/class/net/wlan0/",
    "/sys/class/ieee80211/",
    "iw://wlan0/",
)
private val WIFI_TX_POWER_PREFIXES = listOf(
    "iw://wlan0/txpower",
    "/sys/class/ieee80211/phy0/cfg80211_dev/txpower",
)

/**
 * Rooted-flavor Wi-Fi controller. Routes every privileged call through
 * `RootSafetyGate.check(...)` and delegates to dedicated helper classes.
 * Hard cutoffs (TX-power dBm ceiling, channel allow-list, active windows)
 * are enforced inside the helpers.
 */
@Singleton
class RootedWifiController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val rfkill: WifiRfkillHelper,
    private val sysfs: WifiSysfsHelper,
    private val injectionProbe: WifiInjectionProbe,
    private val mutationLog: SysfsMutationLog,
) : WifiController {

    override suspend fun rfkillToggle(config: RfkillConfig): WifiControllerResult =
        runGated(RootFeatureKey.WifiRfkillToggle) {
            if (!rfkill.isAvailable()) return@runGated WifiControllerResult.Unsupported
            val ok = rfkill.setBlocked(config.block)
            if (!ok) return@runGated WifiControllerResult.HardwareError("rfkill rejected the toggle")
            val effectiveDuration = config.durationMillis.coerceAtMost(WIFI_RFKILL_HARD_CEILING_MILLIS)
            try {
                delay(effectiveDuration)
                WifiControllerResult.Ok()
            } finally {
                withContext(NonCancellable) { rfkill.setBlocked(!config.block) }
            }
        }

    override suspend fun txPowerOverride(config: TxPowerConfig): WifiControllerResult =
        runGated(RootFeatureKey.WifiTxPowerOverride) {
            if (!sysfs.isIwAvailable()) return@runGated WifiControllerResult.Unsupported
            val handle = sysfs.setTxPower(config.targetDbm)
                ?: return@runGated WifiControllerResult.HardwareError("could not snapshot or write txpower")
            val effectiveDuration = config.durationMillis.coerceAtMost(WIFI_TX_POWER_HARD_CEILING_MILLIS)
            try {
                delay(effectiveDuration)
                val effectiveDbm = config.targetDbm.coerceIn(0, WIFI_TX_POWER_HARD_DBM_CEILING)
                val note = if (effectiveDbm < config.targetDbm) {
                    "Clamped to ${effectiveDbm}dBm (regulatory ceiling)"
                } else null
                WifiControllerResult.Ok(statusNote = note)
            } finally {
                withContext(NonCancellable) { sysfs.restoreTxPower(handle) }
            }
        }

    override suspend fun channelOverride(config: ChannelConfig): WifiControllerResult =
        runGated(RootFeatureKey.WifiChannelOverride) {
            if (!sysfs.isIwAvailable()) return@runGated WifiControllerResult.Unsupported
            val handle = sysfs.setChannel(config.channel)
                ?: return@runGated WifiControllerResult.HardwareError(
                    "channel ${config.channel} not in allow-list or write failed",
                )
            val effectiveDuration = config.durationMillis.coerceAtMost(WIFI_CHANNEL_HARD_CEILING_MILLIS)
            try {
                delay(effectiveDuration)
                WifiControllerResult.Ok()
            } finally {
                withContext(NonCancellable) { sysfs.restoreChannel(handle) }
            }
        }

    override suspend fun probeInjectionCapability(): WifiControllerResult =
        runGated(RootFeatureKey.WifiInjectionProbe) {
            injectionProbe.probe() ?: WifiControllerResult.Unsupported
        }

    override suspend fun resetAllWifiMutations(): WifiControllerResult {
        val outcome = mutationLog.revertAll(WIFI_RESET_PREFIXES)
        return WifiControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    override suspend fun revertTxPowerOnly(): WifiControllerResult {
        val outcome = mutationLog.revertAll(WIFI_TX_POWER_PREFIXES)
        return WifiControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> WifiControllerResult,
    ): WifiControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is WifiControllerResult.Ok ||
                it is WifiControllerResult.InjectionCapabilityProbe ||
                it is WifiControllerResult.RfkillState
            ) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> WifiControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            WifiControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> WifiControllerResult.Unsupported
    }
}
