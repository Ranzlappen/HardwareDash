package com.gadget.gps

/**
 * Result returned by every [GpsController] extreme-tier method. All
 * Batch-6 GPS methods are read-only.
 */
sealed class GpsControllerResult {
    data class Ok(val statusNote: String? = null) : GpsControllerResult()
    data object Unsupported : GpsControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : GpsControllerResult()
    data object OptedOut : GpsControllerResult()
    data class HardwareError(val message: String) : GpsControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : GpsControllerResult()
    data class NmeaSnapshot(val sentences: List<String>) : GpsControllerResult()
    data class ConstellationSnapshot(val satellites: List<SatelliteEntry>) : GpsControllerResult()
}

/** One per-satellite reading from the constellation dump. */
data class SatelliteEntry(
    val constellation: String,
    val svId: Int,
    val cn0DbHz: Double?,
    val elevationDegrees: Double?,
    val azimuthDegrees: Double?,
    val usedInFix: Boolean,
)
