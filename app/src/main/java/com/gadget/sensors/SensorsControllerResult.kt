package com.gadget.sensors

/**
 * Result returned by every [SensorsController] extreme-tier method. Same
 * shape as `CameraControllerResult` / `MicrophoneControllerResult`.
 *
 * [Ok] may carry an optional [statusNote] — used e.g. by `highPolling` to
 * communicate that the requested rate was clamped to the safe-default
 * 400 Hz ceiling because the user has not opted into the expert key.
 */
sealed class SensorsControllerResult {
    data class Ok(val statusNote: String? = null) : SensorsControllerResult()
    data object Unsupported : SensorsControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : SensorsControllerResult()
    data object OptedOut : SensorsControllerResult()
    data class HardwareError(val message: String) : SensorsControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : SensorsControllerResult()
    data class EnumerationCompleted(val nodes: List<String>) : SensorsControllerResult()
    data class SysfsRead(val nodeReadings: List<SysfsReading>) : SensorsControllerResult()
}

/** One IIO node reading: typically `_raw` plus its sibling `_scale` / `_offset`. */
data class SysfsReading(
    val path: String,
    val raw: String,
    val scale: String? = null,
    val offset: String? = null,
)
