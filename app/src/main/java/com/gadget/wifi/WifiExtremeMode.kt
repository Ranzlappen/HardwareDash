package com.gadget.wifi

/**
 * Toggles the Wi-Fi rfkill block state. [block] = true issues `rfkill block
 * wifi` (radio off); false issues `rfkill unblock wifi`. Hard 60-second
 * active window enforced inside the helper.
 */
data class RfkillConfig(
    val block: Boolean,
    val durationMillis: Long,
)

/**
 * Override the OEM TX-power cap by writing to `/sys/class/ieee80211/phy<N>/...`
 * or via `iw phy <phy> set txpower fixed <mBm>`. **Pushing beyond OEM
 * defaults can violate FCC / ETSI regulations and is the user's
 * responsibility.** The impl enforces a hard 100 mW (20 dBm) absolute
 * ceiling regardless of the requested value, snapshots the original via
 * the shared mutation log, and restores in `NonCancellable` finally.
 */
data class TxPowerConfig(
    val targetDbm: Int,
    val durationMillis: Long,
)

/**
 * Set the explicit Wi-Fi channel via `iw dev wlan0 set channel`. Restricted
 * to the legal 2.4 GHz channel allow-list (1–14) plus standard 5 GHz UNII
 * bands inside the helper. Snapshots the current channel and restores in
 * finally.
 */
data class ChannelConfig(
    val channel: Int,
    val durationMillis: Long,
)
