package com.gadget.battery

/**
 * Override for charge-current and / or charge-voltage caps. Either field
 * may be null — the impl writes only the non-null fields. **EXTREMELY
 * DANGEROUS**: pushing beyond OEM-validated limits can cause thermal
 * runaway, swelling, or fire. The impl enforces a hard 30-second active
 * window and aborts immediately on any thermal-zone trip-point breach.
 */
data class ChargingProfileConfig(
    val maxCurrentMicroAmps: Long?,
    val maxVoltageMicroVolts: Long?,
    val durationMillis: Long,
)

/**
 * Disables charger / battery / USB thermal throttling by writing
 * `disabled` to each matching `thermal_zone*/mode` node. Same severity
 * as charging-profile override; hard 60-second active window; aborts on
 * any zone breaching its `trip_point_0_temp`.
 */
data class ThermalBypassConfig(
    val durationMillis: Long,
)

/**
 * Coerces SDP / CDP / DCP / HVDCP detection by writing to
 * `/sys/class/power_supply/usb/type` (or `real_type` on Qualcomm). Lower
 * severity than charging-profile — the USB IC will still negotiate.
 */
data class ChargingTypeOverrideConfig(
    val type: String,
    val durationMillis: Long,
)
