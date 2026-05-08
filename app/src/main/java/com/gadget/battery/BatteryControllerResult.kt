package com.gadget.battery

/**
 * Result returned by every [BatteryController] extreme-tier method. Same
 * shape as `SensorsControllerResult` / `CameraControllerResult`.
 */
sealed class BatteryControllerResult {
    data class Ok(val statusNote: String? = null) : BatteryControllerResult()
    data object Unsupported : BatteryControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : BatteryControllerResult()
    data object OptedOut : BatteryControllerResult()
    data class HardwareError(val message: String) : BatteryControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : BatteryControllerResult()
    data class FuelGaugeReading(val nodes: Map<String, String>) : BatteryControllerResult()
    data class CellSnapshot(val cells: List<CellReading>) : BatteryControllerResult()
    data class DumpWritten(val absolutePath: String) : BatteryControllerResult()
    data class DangerousAborted(val reason: String) : BatteryControllerResult()
}

/**
 * One per-cell reading from a multi-cell pack. Phones rarely expose this;
 * foldables and high-end gaming devices occasionally do.
 */
data class CellReading(
    val cellIndex: Int,
    val voltageMicroVolts: Long?,
    val currentMicroAmps: Long?,
    val temperatureDeciCelsius: Int?,
)
