package dev.ranzlappen.gadget.feature.radios.wifi.widget

/**
 * Pure RSSI → signal-percent mapping for the widget gauge. Kept Android-free so
 * the bucketing round-trips in a plain JVM test.
 */
object WifiSignal {

    /** Below this dBm the link is effectively unusable (0%). */
    private const val MIN_DBM = -90

    /** At/above this dBm the signal is full (100%). */
    private const val MAX_DBM = -40

    private const val FULL_PERCENT = 100

    /**
     * Map an RSSI in dBm to a 0–100 signal percentage, clamped to the
     * [-90, -40] dBm window that spans "unusable" to "excellent".
     */
    fun signalPercent(rssiDbm: Int): Int {
        val clamped = rssiDbm.coerceIn(MIN_DBM, MAX_DBM)
        return (clamped - MIN_DBM) * FULL_PERCENT / (MAX_DBM - MIN_DBM)
    }
}
