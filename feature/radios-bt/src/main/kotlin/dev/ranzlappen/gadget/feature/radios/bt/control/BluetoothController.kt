package dev.ranzlappen.gadget.feature.radios.bt.control

/**
 * Rooted-only Bluetooth capability surface. Standard flavor returns
 * [BluetoothControllerResult.Unsupported] for every method.
 *
 * On modern Android (14+) the BlueZ CLI tools (`hcitool`, `hciconfig`)
 * are commonly absent. Each method probes its tool chain via `which`
 * and surfaces `Unsupported` cleanly when no usable backend exists.
 */
interface BluetoothController {

    /** Toggle BT radio via `rfkill block / unblock bluetooth`. */
    suspend fun rfkillToggle(config: BluetoothRfkillConfig): BluetoothControllerResult

    /**
     * Override TX power via `bluetoothctl` → `hcitool` → mgmt-socket
     * fallback chain. Hard 10 dBm ceiling. Snapshot + restore in finally.
     */
    suspend fun txPowerOverride(config: BluetoothTxPowerConfig): BluetoothControllerResult

    /**
     * Read-only tail of `/data/misc/bluetooth/logs/btsnoop_hci.log` if
     * the user has enabled HCI snoop logging in developer options.
     */
    suspend fun hciSnoopDump(): BluetoothControllerResult

    /** Reverts every BT-surface mutation registered with the shared log. */
    suspend fun resetAllBluetoothMutations(): BluetoothControllerResult

    /**
     * Auto-revert path called on `RadiosScreen` dispose. Filters by
     * TX-power-only path prefixes — leaves any rfkill state alone.
     */
    suspend fun revertTxPowerOnly(): BluetoothControllerResult
}
