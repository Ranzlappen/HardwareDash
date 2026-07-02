package dev.ranzlappen.gadget.feature.radios.ir.control

/**
 * Custom IR carrier frequency outside `ConsumerIrManager`'s reported
 * range. The impl clamps [carrierHz] to 20–100 kHz and enforces a
 * 30-second burst ceiling. Snapshot+restore via the shared mutation log.
 */
data class IrCarrierConfig(
    val carrierHz: Int,
    val durationMillis: Long,
)

/**
 * Direct GPIO toggling of the IR LED via the IR-LED brightness sysfs node
 * for arbitrary timing patterns the framework rejects. The impl
 * enforces a ≤ 50 % duty cycle and a 5-second hard burst ceiling.
 */
data class IrRawPatternConfig(
    val onMillis: Long,
    val offMillis: Long,
    val totalDurationMillis: Long,
)
