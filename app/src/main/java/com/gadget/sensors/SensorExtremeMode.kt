package com.gadget.sensors

/**
 * Configuration for a high-rate polling session driven via direct sysfs /
 * IIO writes, bypassing the framework's `SENSOR_DELAY_FASTEST` clamp.
 * [requestedHz] is clamped at runtime: by default to a safe 400 Hz, or to
 * a 1000 Hz absolute hard ceiling if the user has separately opted into
 * the `SensorsHighPollingExpert` key. The caller never gets a higher rate
 * than the clamp regardless of what they request.
 */
data class HighPollingConfig(
    val sensorTag: String,
    val requestedHz: Int,
    val durationMillis: Long,
)

/**
 * Disables hardware-side filtering (LPF / HPF) on the named sensor by
 * writing zero to driver-specific filter cutoff nodes where exposed.
 * Original cutoff values are snapshotted and restored in finally.
 */
data class RawUnfilteredConfig(
    val sensorTag: String,
    val durationMillis: Long,
)

/**
 * Pushes ODR / range registers via i2c-tools `i2cset` beyond the driver's
 * stock configuration. Uses snapshot+restore in `NonCancellable` finally.
 * `requiresExplicitConfirm = true` on the descriptor — bad register writes
 * can permanently shift MEMS calibration on some sensor parts.
 */
data class OverclockConfig(
    val sensorTag: String,
    val i2cBus: Int,
    val i2cAddress: Int,
    val odrRegister: Int,
    val odrValue: Int,
    val durationMillis: Long,
)

/**
 * Disables Android's hardware fusion (`TYPE_ROTATION_VECTOR`,
 * `TYPE_GAME_ROTATION_VECTOR`) and emits a coalesced raw stream from
 * accelerometer + gyroscope + magnetometer. Restores HAL fusion mode in
 * finally.
 */
data class FusionOverrideConfig(
    val durationMillis: Long,
)
