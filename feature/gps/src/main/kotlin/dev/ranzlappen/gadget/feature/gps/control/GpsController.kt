package dev.ranzlappen.gadget.feature.gps.control

/**
 * Rooted-only GPS / GNSS diagnostics surface. Read-only — no write
 * path. Standard flavor returns [GpsControllerResult.Unsupported].
 */
interface GpsController {

    /**
     * Read-only `cat` of vendor NMEA nodes (`/sys/class/gnss/...`,
     * `/dev/tty*`). Bounded by config + 30 s hard ceiling.
     */
    suspend fun nmeaRawTap(config: NmeaTapConfig): GpsControllerResult

    /**
     * Read-only enumeration of every visible satellite, including
     * those normally filtered from `LocationManager.GpsStatus`.
     */
    suspend fun constellationDump(): GpsControllerResult

    /** Always `ResetCompleted(0, 0)`. */
    suspend fun resetAllGpsMutations(): GpsControllerResult
}
