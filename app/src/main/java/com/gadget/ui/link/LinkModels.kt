package com.gadget.ui.link

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ─── Comparison operators ───────────────────────────────────────────────────
enum class LinkOperator(val key: String, val symbol: String) {
    GREATER_THAN("gt", ">"),
    LESS_THAN("lt", "<"),
    EQUAL("eq", "="),
    NOT_EQUAL("neq", "!="),
    GREATER_THAN_OR_EQUAL("gte", ">="),
    LESS_THAN_OR_EQUAL("lte", "<="),
    BETWEEN("between", "↔"),
    OUTSIDE("outside", "↕");

    /** True when the operator requires both a low and high threshold. */
    val isRange: Boolean get() = this == BETWEEN || this == OUTSIDE

    companion object {
        fun fromKey(key: String): LinkOperator = entries.find { it.key == key } ?: GREATER_THAN
    }
}

// ─── Action types ───────────────────────────────────────────────────────────
enum class LinkActionType(val key: String, val label: String) {
    TORCH_ON("torch_on", "Torch On"),
    TORCH_OFF("torch_off", "Torch Off"),
    STROBE_START("strobe_start", "Start Strobe"),
    STROBE_STOP("strobe_stop", "Stop Strobe"),
    VIBRATE("vibrate", "Vibrate"),
    NOTIFICATION("notification", "Send Notification"),
    LOCK("lock", "Lock Screen"),
    RING("ring", "Phone Ring"),
    LOG_ENTRY("log_entry", "Log Entry");

    companion object {
        fun fromKey(key: String): LinkActionType = entries.find { it.key == key } ?: NOTIFICATION
    }
}

// ─── Link rule data class ───────────────────────────────────────────────────
data class LinkRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val enabled: Boolean = true,
    val metricKey: String = "",
    val operator: String = LinkOperator.GREATER_THAN.key,
    val threshold: String = "0",
    val thresholdHigh: String = "",
    val actionType: String = LinkActionType.NOTIFICATION.key,
    val actionConfig: Map<String, String> = emptyMap(),
    val cooldownSec: Int = 10,
    val lastTriggeredMs: Long = 0L,
)

// ─── JSON serialization ────────────────────────────────────────────────────
fun LinkRule.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("enabled", enabled)
    put("metricKey", metricKey)
    put("operator", operator)
    put("threshold", threshold)
    put("thresholdHigh", thresholdHigh)
    put("actionType", actionType)
    put("cooldownSec", cooldownSec)
    put("lastTriggeredMs", lastTriggeredMs)
    put("actionConfig", JSONObject().apply {
        actionConfig.forEach { (k, v) -> put(k, v) }
    })
}

fun linkRuleFromJson(json: JSONObject): LinkRule {
    val configObj = json.optJSONObject("actionConfig")
    val config = mutableMapOf<String, String>()
    configObj?.keys()?.forEach { key -> config[key] = configObj.optString(key, "") }
    return LinkRule(
        id = json.optString("id", UUID.randomUUID().toString()),
        name = json.optString("name", ""),
        enabled = json.optBoolean("enabled", true),
        metricKey = json.optString("metricKey", ""),
        operator = json.optString("operator", "gt"),
        threshold = json.optString("threshold", "0"),
        thresholdHigh = json.optString("thresholdHigh", ""),
        actionType = json.optString("actionType", "notification"),
        actionConfig = config,
        cooldownSec = json.optInt("cooldownSec", 10),
        lastTriggeredMs = json.optLong("lastTriggeredMs", 0L),
    )
}

fun saveRules(rules: List<LinkRule>): String {
    val arr = JSONArray()
    rules.forEach { arr.put(it.toJson()) }
    return arr.toString()
}

fun loadRules(json: String): List<LinkRule> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { linkRuleFromJson(arr.getJSONObject(it)) }
    } catch (_: Exception) {
        emptyList()
    }
}

/** Extract the first numeric value from a formatted metric string (e.g. "9.81 m/s²" → 9.81). */
fun extractNumeric(formatted: String): Double? {
    return """(-?\d+\.?\d*)""".toRegex().find(formatted)?.groupValues?.get(1)?.toDoubleOrNull()
}

/** Evaluate whether a metric value satisfies a rule's condition. */
fun evaluateCondition(
    metricValue: String,
    operator: String,
    threshold: String,
    thresholdHigh: String = "",
): Boolean {
    val actual = extractNumeric(metricValue) ?: return false
    val target = threshold.toDoubleOrNull() ?: return false
    return when (LinkOperator.fromKey(operator)) {
        LinkOperator.GREATER_THAN          -> actual > target
        LinkOperator.LESS_THAN             -> actual < target
        LinkOperator.EQUAL                 -> kotlin.math.abs(actual - target) < 0.01
        LinkOperator.NOT_EQUAL             -> kotlin.math.abs(actual - target) >= 0.01
        LinkOperator.GREATER_THAN_OR_EQUAL -> actual >= target
        LinkOperator.LESS_THAN_OR_EQUAL    -> actual <= target
        LinkOperator.BETWEEN -> {
            val high = thresholdHigh.toDoubleOrNull() ?: return false
            actual in target..high
        }
        LinkOperator.OUTSIDE -> {
            val high = thresholdHigh.toDoubleOrNull() ?: return false
            actual < target || actual > high
        }
    }
}

/** Returns recommended threshold guidance for a given metric, or null if none defined. */
fun recommendedThresholds(metricKey: String): String? = when (metricKey) {
    // ── Battery ─────────────────────────────────────────────────────────
    "battery_level"       -> "Critical: <10% | Low: <20% | Normal: 20-80% | Full: 100%"
    "battery_temp"        -> "Cold: <10\u00B0C | Normal: 20-35\u00B0C | Warm: >40\u00B0C | Hot: >45\u00B0C"
    "battery_voltage"     -> "Low: <3.4V | Normal: 3.7-4.2V | Max: 4.2V"
    "battery_current"     -> "Idle: ~100mA | Screen on: ~300mA | Heavy: >1000mA"
    "battery_charge_time" -> "Quick: <30min | Normal: 1-2h | Slow: >3h"
    // ── Network ─────────────────────────────────────────────────────────
    "wifi_signal"         -> "Excellent: >-50dBm | Good: -50 to -60 | Fair: -60 to -70 | Weak: <-70"
    "wifi_speed"          -> "Slow: <50Mbps | Normal: 50-200Mbps | Fast: >200Mbps"
    "wifi_freq"           -> "2.4GHz: 2412-2484MHz | 5GHz: 4915-5825MHz"
    "cell_signal"         -> "Strong: >-80dBm | OK: -80 to -100 | Weak: <-100 | No signal: <-120"
    // ── Sensors ─────────────────────────────────────────────────────────
    "accel"               -> "Rest: ~9.8m/s\u00B2 | Walking: ~12 | Shaking: >15 | Free fall: ~0"
    "gyro"                -> "Still: <0.05rad/s | Motion: >0.5 | Rapid spin: >2.0"
    "magneto"             -> "Normal: 25-65\u00B5T | Near magnet: >100\u00B5T"
    "light"               -> "Dark: <10lux | Indoor: 100-500lux | Outdoor: >10000lux | Direct sun: >100000lux"
    "proximity"           -> "Near: <5cm | Far: \u22655cm"
    "barometer"           -> "Low pressure: <1000hPa | Normal: ~1013hPa | High: >1025hPa"
    "ambient_temp"        -> "Cold: <10\u00B0C | Comfortable: 18-24\u00B0C | Hot: >30\u00B0C"
    "humidity"            -> "Dry: <30% | Comfortable: 30-60% | Humid: >60% | Very humid: >80%"
    "steps"               -> "Sedentary: <5000 | Active: 5000-10000 | Very active: >10000"
    // ── Device ──────────────────────────────────────────────────────────
    "brightness"          -> "Dim: <20% | Medium: 40-60% | Bright: >80%"
    // ── Location ────────────────────────────────────────────────────────
    "gps_altitude"        -> "Sea level: 0m | Hill: ~200m | Mountain: >1000m | High alt: >3000m"
    "gps_speed"           -> "Walking: ~5km/h | Cycling: ~20km/h | Driving: >50km/h | Highway: >100km/h"
    "gps_lat"             -> "Range: -90.0 to 90.0"
    "gps_lon"             -> "Range: -180.0 to 180.0"
    else                  -> null
}

// ─── Link statistics ───────────────────────────────────────────────────────

data class LinkRuleStats(
    val ruleId: String = "",
    val triggerCount: Int = 0,
    val cooldownBlockCount: Int = 0,
    val lastTriggeredIso: String = "",
    val lastCooldownIso: String = "",
)

fun LinkRuleStats.toJson(): JSONObject = JSONObject().apply {
    put("ruleId", ruleId)
    put("triggerCount", triggerCount)
    put("cooldownBlockCount", cooldownBlockCount)
    put("lastTriggeredIso", lastTriggeredIso)
    put("lastCooldownIso", lastCooldownIso)
}

fun linkRuleStatsFromJson(json: JSONObject): LinkRuleStats = LinkRuleStats(
    ruleId = json.optString("ruleId", ""),
    triggerCount = json.optInt("triggerCount", 0),
    cooldownBlockCount = json.optInt("cooldownBlockCount", 0),
    lastTriggeredIso = json.optString("lastTriggeredIso", ""),
    lastCooldownIso = json.optString("lastCooldownIso", ""),
)

fun saveLinkStats(stats: Map<String, LinkRuleStats>): String {
    val obj = JSONObject()
    stats.forEach { (id, s) -> obj.put(id, s.toJson()) }
    return obj.toString()
}

fun loadLinkStats(json: String): Map<String, LinkRuleStats> {
    if (json.isBlank()) return emptyMap()
    return try {
        val obj = JSONObject(json)
        val map = mutableMapOf<String, LinkRuleStats>()
        obj.keys().forEach { key -> map[key] = linkRuleStatsFromJson(obj.getJSONObject(key)) }
        map
    } catch (_: Exception) {
        emptyMap()
    }
}
