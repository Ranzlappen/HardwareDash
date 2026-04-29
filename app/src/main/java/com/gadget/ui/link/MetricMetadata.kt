package com.gadget.ui.link

/**
 * Structured metadata for a metric — used by the Link rule editor to set sensible
 * slider bounds, pre-fill thresholds, surface presets, and switch to a categorical
 * picker when the value is a string label rather than a number.
 */
data class MetricMetadata(
    val key: String,
    val unit: String,
    val min: Double?,
    val max: Double?,
    val typical: ClosedFloatingPointRange<Double>?,
    val defaultThreshold: Double,
    val step: Double,
    val isInteger: Boolean = false,
    val isCategorical: Boolean = false,
    val allowedValues: List<String> = emptyList(),
    val presets: List<Preset> = emptyList(),
) {
    data class Preset(
        val label: String,
        val operator: LinkOperator,
        val low: Double,
        val high: Double = Double.NaN,
    )

    /** Default slider range, falling back through min/max → typical → ±1 around default. */
    fun sliderRange(): ClosedFloatingPointRange<Double> {
        if (min != null && max != null) return min..max
        if (typical != null) return typical
        val span = kotlin.math.max(1.0, kotlin.math.abs(defaultThreshold))
        return (defaultThreshold - span)..(defaultThreshold + span)
    }

    /** Render a single-line human-readable hint string from the presets. */
    fun hintString(): String? {
        if (presets.isEmpty()) {
            // No named presets — fall back to a Range string when bounds are known.
            if (min != null && max != null) {
                return "Range: ${formatNum(min)} to ${formatNum(max)}" +
                    if (unit.isNotBlank()) " $unit" else ""
            }
            return null
        }
        return presets.joinToString(" | ") { p ->
            val low = formatNum(p.low)
            when (p.operator) {
                LinkOperator.GREATER_THAN          -> "${p.label}: >$low"
                LinkOperator.LESS_THAN             -> "${p.label}: <$low"
                LinkOperator.GREATER_THAN_OR_EQUAL -> "${p.label}: ≥$low"
                LinkOperator.LESS_THAN_OR_EQUAL    -> "${p.label}: ≤$low"
                LinkOperator.EQUAL                 -> "${p.label}: =$low"
                LinkOperator.NOT_EQUAL             -> "${p.label}: ≠$low"
                LinkOperator.BETWEEN               -> "${p.label}: $low–${formatNum(p.high)}"
                LinkOperator.OUTSIDE               -> "${p.label}: outside $low–${formatNum(p.high)}"
            } + if (unit.isNotBlank()) " $unit" else ""
        }
    }

    private fun formatNum(v: Double): String =
        if (isInteger) v.toLong().toString()
        else if (v == v.toLong().toDouble()) v.toLong().toString()
        else "%.${decimalsForStep()}f".format(v)

    private fun decimalsForStep(): Int = when {
        step >= 1.0 -> 0
        step >= 0.1 -> 1
        step >= 0.01 -> 2
        step >= 0.001 -> 3
        else -> 4
    }
}

/**
 * Registry of metadata for every [com.gadget.widget.WidgetMetric] key. Looking up
 * an unknown key returns null.
 */
object MetricMetadataRegistry {

    private val table: Map<String, MetricMetadata> = buildMap {

        // ── Battery ─────────────────────────────────────────────────────────
        put("battery_level", MetricMetadata(
            key = "battery_level", unit = "%",
            min = 0.0, max = 100.0,
            typical = 20.0..100.0,
            defaultThreshold = 20.0, step = 1.0, isInteger = true,
            presets = listOf(
                MetricMetadata.Preset("Critical", LinkOperator.LESS_THAN, 10.0),
                MetricMetadata.Preset("Low", LinkOperator.LESS_THAN, 20.0),
                MetricMetadata.Preset("Full", LinkOperator.GREATER_THAN_OR_EQUAL, 95.0),
            ),
        ))
        put("battery_status", MetricMetadata(
            key = "battery_status", unit = "",
            min = null, max = null, typical = null,
            defaultThreshold = 0.0, step = 1.0,
            isCategorical = true,
            allowedValues = listOf("Charging", "Discharging", "Full", "Not Charging", "Unknown"),
        ))
        put("battery_temp", MetricMetadata(
            key = "battery_temp", unit = "°C",
            min = -20.0, max = 70.0,
            typical = 15.0..40.0,
            defaultThreshold = 40.0, step = 0.5,
            presets = listOf(
                MetricMetadata.Preset("Cold", LinkOperator.LESS_THAN, 0.0),
                MetricMetadata.Preset("Hot", LinkOperator.GREATER_THAN, 40.0),
                MetricMetadata.Preset("Dangerous", LinkOperator.GREATER_THAN, 45.0),
            ),
        ))
        put("battery_voltage", MetricMetadata(
            key = "battery_voltage", unit = "V",
            min = 3.0, max = 4.4,
            typical = 3.4..4.2,
            defaultThreshold = 3.4, step = 0.01,
            presets = listOf(
                MetricMetadata.Preset("Low", LinkOperator.LESS_THAN, 3.4),
                MetricMetadata.Preset("Charging-full", LinkOperator.GREATER_THAN_OR_EQUAL, 4.15),
            ),
        ))
        put("battery_health", MetricMetadata(
            key = "battery_health", unit = "",
            min = null, max = null, typical = null,
            defaultThreshold = 0.0, step = 1.0,
            isCategorical = true,
            allowedValues = listOf("Good", "Overheat", "Dead", "Over Voltage", "Cold", "Unknown"),
        ))
        put("battery_current", MetricMetadata(
            key = "battery_current", unit = "mA",
            min = -3000.0, max = 3000.0,
            typical = -2000.0..2000.0,
            defaultThreshold = 1000.0, step = 50.0, isInteger = true,
            presets = listOf(
                MetricMetadata.Preset("Heavy charge", LinkOperator.GREATER_THAN, 1500.0),
                MetricMetadata.Preset("Heavy drain", LinkOperator.LESS_THAN, -1500.0),
            ),
        ))
        put("battery_charge_time", MetricMetadata(
            key = "battery_charge_time", unit = "min",
            min = 0.0, max = 600.0,
            typical = 0.0..240.0,
            defaultThreshold = 60.0, step = 1.0, isInteger = true,
            presets = listOf(
                MetricMetadata.Preset("Quick", LinkOperator.LESS_THAN, 30.0),
                MetricMetadata.Preset("Slow", LinkOperator.GREATER_THAN, 180.0),
            ),
        ))

        // ── Network ─────────────────────────────────────────────────────────
        put("wifi_ssid", MetricMetadata(
            key = "wifi_ssid", unit = "",
            min = null, max = null, typical = null,
            defaultThreshold = 0.0, step = 1.0,
            isCategorical = true,
            // SSIDs are user-specific; leave allowedValues empty so the editor offers free-text.
        ))
        put("wifi_signal", MetricMetadata(
            key = "wifi_signal", unit = "dBm",
            min = -100.0, max = -30.0,
            typical = -75.0..-40.0,
            defaultThreshold = -70.0, step = 1.0, isInteger = true,
            presets = listOf(
                MetricMetadata.Preset("Excellent", LinkOperator.GREATER_THAN_OR_EQUAL, -50.0),
                MetricMetadata.Preset("Good", LinkOperator.BETWEEN, -60.0, -50.0),
                MetricMetadata.Preset("Weak", LinkOperator.LESS_THAN, -70.0),
            ),
        ))
        put("wifi_speed", MetricMetadata(
            key = "wifi_speed", unit = "Mbps",
            min = 0.0, max = 2400.0,
            typical = 50.0..600.0,
            defaultThreshold = 50.0, step = 1.0, isInteger = true,
            presets = listOf(
                MetricMetadata.Preset("Slow", LinkOperator.LESS_THAN, 50.0),
                MetricMetadata.Preset("Fast", LinkOperator.GREATER_THAN, 300.0),
            ),
        ))
        put("wifi_freq", MetricMetadata(
            key = "wifi_freq", unit = "MHz",
            min = 2400.0, max = 6000.0,
            typical = 2400.0..5825.0,
            defaultThreshold = 5180.0, step = 5.0, isInteger = true,
            presets = listOf(
                MetricMetadata.Preset("2.4 GHz band", LinkOperator.BETWEEN, 2400.0, 2484.0),
                MetricMetadata.Preset("5 GHz band", LinkOperator.BETWEEN, 4915.0, 5825.0),
                MetricMetadata.Preset("6 GHz band", LinkOperator.GREATER_THAN, 5925.0),
            ),
        ))
        put("bt_status", MetricMetadata(
            key = "bt_status", unit = "",
            min = null, max = null, typical = null,
            defaultThreshold = 0.0, step = 1.0,
            isCategorical = true,
            allowedValues = listOf("On", "Off"),
        ))
        put("cell_signal", MetricMetadata(
            key = "cell_signal", unit = "dBm",
            min = -120.0, max = -50.0,
            typical = -100.0..-70.0,
            defaultThreshold = -100.0, step = 1.0, isInteger = true,
            presets = listOf(
                MetricMetadata.Preset("Strong", LinkOperator.GREATER_THAN, -80.0),
                MetricMetadata.Preset("Weak", LinkOperator.LESS_THAN, -100.0),
                MetricMetadata.Preset("None", LinkOperator.LESS_THAN, -115.0),
            ),
        ))
        put("net_type", MetricMetadata(
            key = "net_type", unit = "",
            min = null, max = null, typical = null,
            defaultThreshold = 0.0, step = 1.0,
            isCategorical = true,
            allowedValues = listOf("LTE", "5G NR", "HSPA", "UMTS", "EDGE", "GPRS", "Other"),
        ))
        put("nfc_status", MetricMetadata(
            key = "nfc_status", unit = "",
            min = null, max = null, typical = null,
            defaultThreshold = 0.0, step = 1.0,
            isCategorical = true,
            allowedValues = listOf("Enabled", "Disabled", "Not available"),
        ))

        // ── Sensors ─────────────────────────────────────────────────────────
        put("accel", MetricMetadata(
            key = "accel", unit = "m/s²",
            min = 0.0, max = 50.0,
            typical = 9.0..12.0,
            defaultThreshold = 15.0, step = 0.1,
            presets = listOf(
                MetricMetadata.Preset("Free-fall", LinkOperator.LESS_THAN, 2.0),
                MetricMetadata.Preset("Walking", LinkOperator.BETWEEN, 10.0, 12.0),
                MetricMetadata.Preset("Shake", LinkOperator.GREATER_THAN, 15.0),
                MetricMetadata.Preset("Impact", LinkOperator.GREATER_THAN, 25.0),
            ),
        ))
        put("gyro", MetricMetadata(
            key = "gyro", unit = "rad/s",
            min = 0.0, max = 35.0,
            typical = 0.0..2.0,
            defaultThreshold = 0.5, step = 0.05,
            presets = listOf(
                MetricMetadata.Preset("Still", LinkOperator.LESS_THAN, 0.05),
                MetricMetadata.Preset("Motion", LinkOperator.GREATER_THAN, 0.5),
                MetricMetadata.Preset("Rapid spin", LinkOperator.GREATER_THAN, 2.0),
            ),
        ))
        put("magneto", MetricMetadata(
            key = "magneto", unit = "µT",
            min = 0.0, max = 1000.0,
            typical = 25.0..65.0,
            defaultThreshold = 100.0, step = 1.0,
            presets = listOf(
                MetricMetadata.Preset("Earth field", LinkOperator.BETWEEN, 25.0, 65.0),
                MetricMetadata.Preset("Magnet present", LinkOperator.GREATER_THAN, 100.0),
            ),
        ))
        listOf("accel_x", "accel_y", "accel_z").forEach { key ->
            put(key, MetricMetadata(
                key = key, unit = "m/s²",
                min = -50.0, max = 50.0,
                typical = -12.0..12.0,
                defaultThreshold = 5.0, step = 0.1,
                presets = listOf(
                    MetricMetadata.Preset("Negative tilt", LinkOperator.LESS_THAN, -5.0),
                    MetricMetadata.Preset("Flat / rest", LinkOperator.BETWEEN, -1.0, 1.0),
                    MetricMetadata.Preset("Positive tilt", LinkOperator.GREATER_THAN, 5.0),
                ),
            ))
        }
        listOf("gyro_x", "gyro_y", "gyro_z").forEach { key ->
            put(key, MetricMetadata(
                key = key, unit = "rad/s",
                min = -35.0, max = 35.0,
                typical = -2.0..2.0,
                defaultThreshold = 0.5, step = 0.05,
                presets = listOf(
                    MetricMetadata.Preset("Spin negative", LinkOperator.LESS_THAN, -0.5),
                    MetricMetadata.Preset("Still", LinkOperator.BETWEEN, -0.05, 0.05),
                    MetricMetadata.Preset("Spin positive", LinkOperator.GREATER_THAN, 0.5),
                ),
            ))
        }
        listOf("magneto_x", "magneto_y", "magneto_z").forEach { key ->
            put(key, MetricMetadata(
                key = key, unit = "µT",
                min = -1000.0, max = 1000.0,
                typical = -65.0..65.0,
                defaultThreshold = 50.0, step = 1.0,
                presets = listOf(
                    MetricMetadata.Preset("South pole", LinkOperator.LESS_THAN, -50.0),
                    MetricMetadata.Preset("Earth field", LinkOperator.BETWEEN, -65.0, 65.0),
                    MetricMetadata.Preset("North pole", LinkOperator.GREATER_THAN, 50.0),
                ),
            ))
        }
        put("light", MetricMetadata(
            key = "light", unit = "lux",
            min = 0.0, max = 120000.0,
            typical = 100.0..10000.0,
            defaultThreshold = 50.0, step = 10.0, isInteger = true,
            presets = listOf(
                MetricMetadata.Preset("Dark", LinkOperator.LESS_THAN, 10.0),
                MetricMetadata.Preset("Indoor", LinkOperator.BETWEEN, 100.0, 500.0),
                MetricMetadata.Preset("Outdoor", LinkOperator.GREATER_THAN, 10000.0),
                MetricMetadata.Preset("Direct sun", LinkOperator.GREATER_THAN, 50000.0),
            ),
        ))
        put("proximity", MetricMetadata(
            key = "proximity", unit = "cm",
            min = 0.0, max = 10.0,
            typical = 0.0..10.0,
            defaultThreshold = 5.0, step = 0.5,
            presets = listOf(
                MetricMetadata.Preset("Near", LinkOperator.LESS_THAN, 5.0),
                MetricMetadata.Preset("Far", LinkOperator.GREATER_THAN_OR_EQUAL, 5.0),
            ),
        ))
        put("barometer", MetricMetadata(
            key = "barometer", unit = "hPa",
            min = 870.0, max = 1085.0,
            typical = 980.0..1030.0,
            defaultThreshold = 1013.0, step = 0.5,
            presets = listOf(
                MetricMetadata.Preset("Storm", LinkOperator.LESS_THAN, 990.0),
                MetricMetadata.Preset("Normal", LinkOperator.BETWEEN, 1005.0, 1025.0),
                MetricMetadata.Preset("High", LinkOperator.GREATER_THAN, 1025.0),
            ),
        ))
        put("ambient_temp", MetricMetadata(
            key = "ambient_temp", unit = "°C",
            min = -40.0, max = 85.0,
            typical = 0.0..40.0,
            defaultThreshold = 25.0, step = 0.5,
            presets = listOf(
                MetricMetadata.Preset("Freezing", LinkOperator.LESS_THAN_OR_EQUAL, 0.0),
                MetricMetadata.Preset("Comfortable", LinkOperator.BETWEEN, 18.0, 24.0),
                MetricMetadata.Preset("Hot", LinkOperator.GREATER_THAN, 30.0),
            ),
        ))
        put("humidity", MetricMetadata(
            key = "humidity", unit = "%",
            min = 0.0, max = 100.0,
            typical = 20.0..80.0,
            defaultThreshold = 60.0, step = 1.0, isInteger = true,
            presets = listOf(
                MetricMetadata.Preset("Dry", LinkOperator.LESS_THAN, 30.0),
                MetricMetadata.Preset("Comfortable", LinkOperator.BETWEEN, 30.0, 60.0),
                MetricMetadata.Preset("Humid", LinkOperator.GREATER_THAN, 70.0),
            ),
        ))
        put("steps", MetricMetadata(
            key = "steps", unit = "steps",
            min = 0.0, max = 50000.0,
            typical = 0.0..15000.0,
            defaultThreshold = 10000.0, step = 100.0, isInteger = true,
            presets = listOf(
                MetricMetadata.Preset("Sedentary", LinkOperator.LESS_THAN, 5000.0),
                MetricMetadata.Preset("Active", LinkOperator.GREATER_THAN, 7500.0),
                MetricMetadata.Preset("Very active", LinkOperator.GREATER_THAN, 12500.0),
            ),
        ))

        // ── Device ──────────────────────────────────────────────────────────
        put("brightness", MetricMetadata(
            key = "brightness", unit = "%",
            min = 0.0, max = 100.0,
            typical = 0.0..100.0,
            defaultThreshold = 50.0, step = 1.0, isInteger = true,
            presets = listOf(
                MetricMetadata.Preset("Dim", LinkOperator.LESS_THAN, 20.0),
                MetricMetadata.Preset("Bright", LinkOperator.GREATER_THAN, 80.0),
            ),
        ))

        // ── Location ────────────────────────────────────────────────────────
        put("gps_location", MetricMetadata(
            key = "gps_location", unit = "",
            min = null, max = null, typical = null,
            defaultThreshold = 0.0, step = 1.0,
            isCategorical = true,
            // Free-text "lat, lon" — equality only.
        ))
        put("gps_altitude", MetricMetadata(
            key = "gps_altitude", unit = "m",
            min = -500.0, max = 9000.0,
            typical = 0.0..500.0,
            defaultThreshold = 100.0, step = 10.0, isInteger = true,
            presets = listOf(
                MetricMetadata.Preset("Sea level", LinkOperator.BETWEEN, -50.0, 50.0),
                MetricMetadata.Preset("Hill", LinkOperator.GREATER_THAN, 200.0),
                MetricMetadata.Preset("Mountain", LinkOperator.GREATER_THAN, 1000.0),
            ),
        ))
        put("gps_speed", MetricMetadata(
            key = "gps_speed", unit = "km/h",
            min = 0.0, max = 400.0,
            typical = 0.0..120.0,
            defaultThreshold = 5.0, step = 1.0, isInteger = true,
            presets = listOf(
                MetricMetadata.Preset("Walking", LinkOperator.LESS_THAN_OR_EQUAL, 6.0),
                MetricMetadata.Preset("Cycling", LinkOperator.BETWEEN, 15.0, 25.0),
                MetricMetadata.Preset("Driving", LinkOperator.GREATER_THAN, 50.0),
                MetricMetadata.Preset("Highway", LinkOperator.GREATER_THAN, 100.0),
            ),
        ))
        put("gps_lat", MetricMetadata(
            key = "gps_lat", unit = "°",
            min = -90.0, max = 90.0,
            typical = null,
            defaultThreshold = 0.0, step = 0.0001,
        ))
        put("gps_lon", MetricMetadata(
            key = "gps_lon", unit = "°",
            min = -180.0, max = 180.0,
            typical = null,
            defaultThreshold = 0.0, step = 0.0001,
        ))
    }

    fun get(key: String): MetricMetadata? = table[key]
    fun all(): Map<String, MetricMetadata> = table
}
