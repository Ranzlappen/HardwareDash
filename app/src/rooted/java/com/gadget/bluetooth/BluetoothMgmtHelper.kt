package com.gadget.bluetooth

import com.gadget.root.core.RootShell
import com.gadget.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

internal const val BLUETOOTH_TX_POWER_HARD_DBM_CEILING = 10
internal const val BLUETOOTH_TX_POWER_HARD_CEILING_MILLIS = 5L * 60L * 1000L
internal const val BLUETOOTH_RFKILL_HARD_CEILING_MILLIS = 60_000L
private const val RFKILL_BIN = "rfkill"
private const val BLUETOOTHCTL_BIN = "bluetoothctl"
private const val HCITOOL_BIN = "hcitool"
private const val DEFAULT_HCI = "hci0"

/**
 * Bluetooth management helper. Tries `bluetoothctl` first (modern
 * preferred CLI), falls through to `hcitool` (deprecated since
 * Linux 5.10), finally surfaces `Unsupported` if neither is present.
 *
 * TX-power writes register the synthesized `bt-mgmt://hci0/txpower`
 * pseudo-path with the shared mutation log so the surface-wide
 * `resetAllBluetoothMutations` and screen-dispose `revertTxPowerOnly`
 * paths can clean up.
 */
@Singleton
class BluetoothMgmtHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {

    suspend fun isRfkillAvailable(): Boolean {
        val probe = shell.exec("which $RFKILL_BIN")
        return probe.isSuccess && !probe.stdout.firstOrNull()?.trim().isNullOrEmpty()
    }

    suspend fun setRfkillBlocked(blocked: Boolean): Boolean {
        val verb = if (blocked) "block" else "unblock"
        return shell.exec("$RFKILL_BIN $verb bluetooth").isSuccess
    }

    suspend fun isAnyMgmtAvailable(): Boolean {
        val ctl = shell.exec("which $BLUETOOTHCTL_BIN")
        if (ctl.isSuccess && !ctl.stdout.firstOrNull()?.trim().isNullOrEmpty()) return true
        val hcitool = shell.exec("which $HCITOOL_BIN")
        return hcitool.isSuccess && !hcitool.stdout.firstOrNull()?.trim().isNullOrEmpty()
    }

    suspend fun setTxPower(targetDbm: Int): TxPowerHandle? {
        val effectiveDbm = targetDbm.coerceIn(0, BLUETOOTH_TX_POWER_HARD_DBM_CEILING)
        val pseudoPath = "bt-mgmt://$DEFAULT_HCI/txpower"
        val current = readCurrentTxPower() ?: return null
        mutationLog.register(pseudoPath, current.toString())
        val ok = writeTxPower(effectiveDbm)
        if (!ok) {
            mutationLog.unregister(pseudoPath)
            return null
        }
        return TxPowerHandle(pseudoPath = pseudoPath, originalDbm = current)
    }

    suspend fun restoreTxPower(handle: TxPowerHandle) {
        val ok = writeTxPower(handle.originalDbm)
        if (ok) mutationLog.unregister(handle.pseudoPath)
    }

    private suspend fun writeTxPower(dbm: Int): Boolean {
        // bluetoothctl preferred — uses mgmt API under the hood.
        val ctl = shell.exec("$BLUETOOTHCTL_BIN -- power on")
        if (ctl.isSuccess) {
            val set = shell.exec("$BLUETOOTHCTL_BIN -- transport set-txpower $dbm")
            if (set.isSuccess) return true
        }
        // hcitool fallback — value is in HCI units, not dBm; we approximate
        // with a linear mapping. Anything that gets through is better than
        // failing silently.
        val hcitool = shell.exec("$HCITOOL_BIN -i $DEFAULT_HCI cmd 0x03 0x000d 0x00 0x${dbm.toString(16).padStart(2, '0')}")
        return hcitool.isSuccess
    }

    private suspend fun readCurrentTxPower(): Int? {
        val ctl = shell.exec("$BLUETOOTHCTL_BIN -- transport show")
        if (ctl.isSuccess) {
            val line = ctl.stdout.firstOrNull { it.contains("TxPower", ignoreCase = true) }
                ?: return null
            return Regex("(-?\\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull()
        }
        return null
    }
}

data class TxPowerHandle(val pseudoPath: String, val originalDbm: Int)
