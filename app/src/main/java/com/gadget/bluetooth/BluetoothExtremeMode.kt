package com.gadget.bluetooth

/**
 * Toggles the Bluetooth rfkill block state. Hard 60-second active window
 * enforced inside the helper.
 */
data class BluetoothRfkillConfig(
    val block: Boolean,
    val durationMillis: Long,
)

/**
 * Override the OEM Bluetooth TX-power cap. **Pushing beyond OEM defaults
 * can violate FCC / ETSI regulations and is the user's responsibility.**
 * Hard 10 dBm (Class-1) ceiling enforced inside the helper. Snapshot +
 * restore via the shared mutation log.
 */
data class BluetoothTxPowerConfig(
    val targetDbm: Int,
    val durationMillis: Long,
)
