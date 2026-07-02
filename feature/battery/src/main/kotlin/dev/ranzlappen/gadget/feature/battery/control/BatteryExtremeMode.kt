package dev.ranzlappen.gadget.feature.battery.control


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
 * `disabled` to each matching `thermal_zone<N>/mode` node. Same severity
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

/**
 * Battery longevity hold. The helper polls `capacity` at 1 Hz and toggles
 * `input_suspend` / `charge_disable` to keep the pack near
 * [targetSocPercent]. The helper clamps the target to 20–90 internally and
 * caps the duration at 600 s — caller-supplied values outside those ranges
 * are silently coerced.
 */
data class HoldSocConfig(
    val targetSocPercent: Int,
    val durationMillis: Long,
)

/**
 * Wireless-charging coil-current cap. Writes to
 * `/sys/class/power_supply/wireless/current_max` (or the qcom-pmic-wireless
 * driver path). The helper clamps [maxCurrentMicroAmps] to 1 500 000 (1.5 A)
 * and caps [durationMillis] at 30 s regardless of caller intent.
 */
data class WirelessCoilCurrentConfig(
    val maxCurrentMicroAmps: Long,
    val durationMillis: Long,
)
