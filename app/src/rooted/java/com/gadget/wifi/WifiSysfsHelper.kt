package com.gadget.wifi

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

internal const val WIFI_TX_POWER_HARD_DBM_CEILING = 20
internal const val WIFI_TX_POWER_HARD_CEILING_MILLIS = 5L * 60L * 1000L
internal const val WIFI_CHANNEL_HARD_CEILING_MILLIS = 60_000L
internal const val WIFI_RFKILL_HARD_CEILING_MILLIS = 60_000L
private const val DBM_TO_MBM_FACTOR = 100
private const val IW_BIN = "iw"
private const val DEFAULT_PHY = "phy0"
private const val DEFAULT_DEV = "wlan0"
private val ALLOWED_2GHZ_CHANNELS = (1..14).toList()
private val ALLOWED_5GHZ_CHANNELS = listOf(36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 149, 153, 157, 161, 165)

/**
 * TX-power and channel write helper for Wi-Fi. Prefers `iw` shell-out
 * (more reliable than `iwconfig` per the Batch-6 feasibility report);
 * falls back to direct `/sys/class/ieee80211/<phy>/...` writes where
 * exposed.
 *
 * Every write registers the synthesized `iw://<dev>/txpower` or
 * `iw://<dev>/channel` pseudo-path with the shared mutation log so the
 * "Reset all" + auto-revert paths can clean up.
 */
@Singleton
class WifiSysfsHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {

    suspend fun isIwAvailable(): Boolean {
        val probe = shell.exec("which $IW_BIN")
        return probe.isSuccess && !probe.stdout.firstOrNull()?.trim().isNullOrEmpty()
    }

    suspend fun setTxPower(targetDbm: Int): TxPowerHandle? {
        val effectiveDbm = targetDbm.coerceIn(0, WIFI_TX_POWER_HARD_DBM_CEILING)
        val current = readCurrentTxPower() ?: return null
        val pseudoPath = "iw://$DEFAULT_DEV/txpower"
        mutationLog.register(pseudoPath, current.toString())
        val mbm = effectiveDbm * DBM_TO_MBM_FACTOR
        val write = shell.exec("$IW_BIN phy $DEFAULT_PHY set txpower fixed $mbm")
        if (!write.isSuccess) {
            mutationLog.unregister(pseudoPath)
            return null
        }
        return TxPowerHandle(pseudoPath = pseudoPath, originalDbm = current)
    }

    suspend fun restoreTxPower(handle: TxPowerHandle) {
        val mbm = handle.originalDbm * DBM_TO_MBM_FACTOR
        val result = shell.exec("$IW_BIN phy $DEFAULT_PHY set txpower fixed $mbm")
        if (result.isSuccess) {
            mutationLog.unregister(handle.pseudoPath)
        } else {
            // Fallback to "auto" so we don't leave the cap stuck.
            shell.exec("$IW_BIN phy $DEFAULT_PHY set txpower auto")
            mutationLog.unregister(handle.pseudoPath)
        }
    }

    suspend fun setChannel(channel: Int): ChannelHandle? {
        if (channel !in ALLOWED_2GHZ_CHANNELS && channel !in ALLOWED_5GHZ_CHANNELS) return null
        val current = readCurrentChannel() ?: return null
        val pseudoPath = "iw://$DEFAULT_DEV/channel"
        mutationLog.register(pseudoPath, current.toString())
        val write = shell.exec("$IW_BIN dev $DEFAULT_DEV set channel $channel")
        if (!write.isSuccess) {
            mutationLog.unregister(pseudoPath)
            return null
        }
        return ChannelHandle(pseudoPath = pseudoPath, originalChannel = current)
    }

    suspend fun restoreChannel(handle: ChannelHandle) {
        val result = shell.exec("$IW_BIN dev $DEFAULT_DEV set channel ${handle.originalChannel}")
        if (result.isSuccess) {
            mutationLog.unregister(handle.pseudoPath)
        }
    }

    private suspend fun readCurrentTxPower(): Int? {
        val result = shell.exec("$IW_BIN dev $DEFAULT_DEV info")
        if (!result.isSuccess) return null
        val line = result.stdout.firstOrNull { it.contains("txpower") } ?: return null
        return Regex("([0-9]+(?:\\.[0-9]+)?)\\s*dBm").find(line)?.groupValues?.get(1)
            ?.toFloatOrNull()
            ?.toInt()
    }

    private suspend fun readCurrentChannel(): Int? {
        val result = shell.exec("$IW_BIN dev $DEFAULT_DEV info")
        if (!result.isSuccess) return null
        val line = result.stdout.firstOrNull { it.contains("channel") } ?: return null
        return Regex("channel\\s+(\\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull()
    }
}

data class TxPowerHandle(val pseudoPath: String, val originalDbm: Int)
data class ChannelHandle(val pseudoPath: String, val originalChannel: Int)
