package dev.ranzlappen.gadget.feature.radios.bt

import android.bluetooth.BluetoothDevice

/**
 * Flavor seam for per-peripheral battery level, connection state, RSSI, and
 * codec data. The standard flavor provides connection state + GATT-based BLE
 * battery/RSSI via public APIs. The rooted flavor additionally reads battery
 * for classic BT devices via the hidden `BluetoothDevice.getBatteryLevel()`
 * API and negotiated A2DP codec via reflection — both gated by
 * [dev.ranzlappen.gadget.core.root.RootSafetyGate].
 *
 * Hilt binds [StandardBtEnhancedInfoProvider] in the standard flavor and
 * [RootedBtEnhancedInfoProvider] in the rooted flavor via each flavor's
 * `RootBindings`. Never branch on `BuildConfig.IS_ROOTED` — consult
 * [isRootedFlavor] only for UI labelling.
 */
interface BtEnhancedInfoProvider {

    /** True only in the rooted build; drives optional UI labels. */
    val isRootedFlavor: Boolean

    /**
     * MAC addresses of all devices currently connected across GATT, A2DP,
     * and HFP profiles. Cheap — no I/O.
     */
    fun connectedAddresses(): Set<String>

    /**
     * Opens a GATT connection to [device], reads the Battery Level
     * characteristic (service 0x180F / char 0x2A19) and the RSSI, then
     * closes the connection. Only appropriate for BLE / Dual devices.
     * Returns (batteryPercent, rssiDbm); either may be null if the device
     * doesn't expose that value.
     */
    suspend fun readGattBatteryAndRssi(device: BluetoothDevice): Pair<Int?, Int?>

    /**
     * Reads the Android Bluetooth stack's **cached** battery level for
     * [device] via the hidden `BluetoothDevice.getBatteryLevel()` reflection
     * call — covers BLE *and* classic BT devices that the stack polls
     * internally (Galaxy Buds, Sony WH/WF series, AirPods via the HFP AT
     * exchange, etc.).
     *
     * Always returns `null` on the standard flavor. On the rooted flavor
     * returns `null` if the gate is blocked or the API returns -1 (unknown).
     */
    fun hiddenBatteryLevel(device: BluetoothDevice): Int?

    /**
     * Negotiated A2DP codec for [device] via `BluetoothA2dp.getCodecStatus()`
     * reflection (e.g. "AAC", "LDAC", "SBC"). Always `null` on standard.
     */
    fun a2dpCodecName(device: BluetoothDevice): String?
}
