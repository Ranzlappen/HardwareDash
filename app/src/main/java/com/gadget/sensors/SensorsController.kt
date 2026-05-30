package com.gadget.sensors

/**
 * Rooted-only Sensors capability surface. The standard-flavor implementation
 * always returns [SensorsControllerResult.Unsupported] so shared UI uses one
 * code path for both flavors.
 *
 * Every method routes through `dev.ranzlappen.gadget.core.root.RootSafetyGate` before doing
 * anything privileged. Hard cutoffs (max polling rate, active windows) are
 * enforced inside the impl and cannot be extended by callers.
 *
 * The interface deliberately exposes ONLY extreme-tier operations. Baseline
 * sensor reads continue to flow through `SensorManager` in `SensorsScreen`
 * — this controller is for the rooted "Root extras" surface only.
 */
interface SensorsController {

    /**
     * Pushes the named sensor's polling rate above `SENSOR_DELAY_FASTEST`
     * via direct IIO `sampling_frequency` writes. Default ceiling is
     * 400 Hz; opting into the `SensorsHighPollingExpert` key raises the
     * ceiling to 1000 Hz. Hard 60-second per-call ceiling.
     */
    suspend fun highPolling(config: HighPollingConfig): SensorsControllerResult

    /**
     * Disables driver-side LPF / HPF filtering by writing zero to filter
     * cutoff nodes. Snapshotted + restored in `NonCancellable` finally.
     */
    suspend fun rawUnfiltered(config: RawUnfilteredConfig): SensorsControllerResult

    /**
     * Read-only walk over `/sys/bus/iio/devices/iio:device*` and
     * `/sys/class/sensors/`: returns each sensor's `_raw` + `_scale` +
     * `_offset` triple. No mutations.
     */
    suspend fun readSysfs(): SensorsControllerResult

    /**
     * Pushes ODR / range registers via `i2cset`. Snapshot+restore;
     * mandatory explicit-confirm on the descriptor.
     */
    suspend fun overclock(config: OverclockConfig): SensorsControllerResult

    /**
     * Disables Android HAL fusion and emits a coalesced raw stream.
     * Restores fusion mode in finally.
     */
    suspend fun fusionOverride(config: FusionOverrideConfig): SensorsControllerResult

    /**
     * Read-only enumeration of every sensor node visible to the kernel —
     * including hall sensors, lid switches, and vendor-proprietary IMUs
     * the SDK conceals.
     */
    suspend fun enumerateHidden(): SensorsControllerResult

    /**
     * Reverts every mutation this controller has registered with the
     * shared `SysfsMutationLog`, filtered by sensor-related path
     * prefixes. Standard-flavor returns `ResetCompleted(0, 0)`.
     */
    suspend fun resetAllSensorMutations(): SensorsControllerResult
}
