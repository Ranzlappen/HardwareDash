package dev.ranzlappen.gadget.feature.radios.wifi.rooted

/**
 * Pure builders + bounds for the rooted Wi-Fi shell controls. Android-free so
 * the genuinely fallible bits — command shape, the regulatory TX-power
 * ceiling, the channel allow-list — round-trip in a plain JVM unit test.
 *
 * Command shapes mirror the legacy `WifiRfkillHelper` / `WifiSysfsHelper`
 * (rfkill + `iw`), the more reliable path than `iwconfig` per the Batch-6
 * feasibility notes. Every numeric argument is parsed to an `Int` before it
 * reaches a command string, so no caller-supplied text is ever interpolated
 * into the shell.
 */
object WifiRootCommands {

    /** Hard regulatory ceiling — a rule can never push TX power past this. */
    const val MAX_TX_POWER_DBM = 20

    /** Floor for a fixed TX-power request. */
    const val MIN_TX_POWER_DBM = 0

    /** `iw` expresses TX power in mBm; 1 dBm = 100 mBm. */
    const val DBM_TO_MBM_FACTOR = 100

    private const val PHY = "phy0"
    private const val DEV = "wlan0"

    /** 2.4 GHz channels 1–14. */
    val ALLOWED_2GHZ_CHANNELS: List<Int> = (1..14).toList()

    /** Common 5 GHz UNII channels (regulatory-safe subset). */
    val ALLOWED_5GHZ_CHANNELS: List<Int> = listOf(
        36, 40, 44, 48, 52, 56, 60, 64,
        100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140,
        149, 153, 157, 161, 165,
    )

    /** Read-only capability probe — inspect `iw phy <phy> info`. */
    const val INJECTION_PROBE_COMMAND = "iw phy $PHY info"

    /** Clamp a requested dBm into the `[MIN, MAX]` regulatory window. */
    fun clampTxPowerDbm(dbm: Int): Int = dbm.coerceIn(MIN_TX_POWER_DBM, MAX_TX_POWER_DBM)

    /** True when [channel] is in the 2.4 GHz or 5 GHz allow-list. */
    fun isAllowedChannel(channel: Int): Boolean =
        channel in ALLOWED_2GHZ_CHANNELS || channel in ALLOWED_5GHZ_CHANNELS

    /** Block or unblock the Wi-Fi radio at the rfkill level. */
    fun rfkill(blocked: Boolean): String = "rfkill ${if (blocked) "block" else "unblock"} wifi"

    /** Set a fixed TX power (clamped to the ceiling), converted to mBm. */
    fun setTxPower(dbm: Int): String =
        "iw phy $PHY set txpower fixed ${clampTxPowerDbm(dbm) * DBM_TO_MBM_FACTOR}"

    /** Lock onto an explicit [channel]. Validate with [isAllowedChannel] first. */
    fun setChannel(channel: Int): String = "iw dev $DEV set channel $channel"

    /** `iw phy info` advertises monitor mode with a `* monitor` line. */
    fun supportsMonitor(phyInfo: String): Boolean = phyInfo.contains("* monitor", ignoreCase = true)

    /** `iw phy info` advertises IBSS with a `* IBSS` line. */
    fun supportsIbss(phyInfo: String): Boolean = phyInfo.contains("* IBSS", ignoreCase = true)
}
