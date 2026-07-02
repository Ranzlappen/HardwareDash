package dev.ranzlappen.gadget.feature.battery.control


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

    /**
     * Holds the pack at a target SOC for the given duration. [appliedTargetSoc]
     * may differ from the caller-supplied value if the helper clamped it
     * (allow-listed range 20–90).
     */
    data class HoldSocSnapshot(
        val appliedTargetSocPercent: Int,
        val appliedDurationMillis: Long,
        val initialSocPercent: Int?,
    ) : BatteryControllerResult()

    /**
     * Read-only fuel-gauge IC deep-dump. [cycleCount], [designCapacityUah],
     * [fullChargeCapacityUah] are best-effort — null on devices that don't
     * expose those nodes. [persistedFile] is the absolute path of the JSON
     * snapshot in the Logbook directory if the write succeeded.
     */
    data class BatteryHealthReading(
        val nodes: Map<String, String>,
        val cycleCount: Int?,
        val designCapacityUah: Long?,
        val fullChargeCapacityUah: Long?,
        val persistedFile: String?,
    ) : BatteryControllerResult()

    /**
     * Wireless-coil current cap snapshot. [appliedCoilCurrentMicroAmps] is the
     * value actually written after the hard 1.5 A clamp was applied.
     */
    data class WirelessCoilSnapshot(
        val appliedCoilCurrentMicroAmps: Long,
        val priorCoilCurrentMicroAmps: Long?,
    ) : BatteryControllerResult()
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
