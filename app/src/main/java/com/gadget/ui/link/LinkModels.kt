package com.gadget.ui.link

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ─── Comparison operators ───────────────────────────────────────────────────
enum class LinkOperator(val key: String, val symbol: String) {
    GREATER_THAN("gt", ">"),
    LESS_THAN("lt", "<"),
    EQUAL("eq", "="),
    NOT_EQUAL("neq", "!=");

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
    RING("ring", "Phone Ring");

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
fun evaluateCondition(metricValue: String, operator: String, threshold: String): Boolean {
    val actual = extractNumeric(metricValue) ?: return false
    val target = threshold.toDoubleOrNull() ?: return false
    return when (LinkOperator.fromKey(operator)) {
        LinkOperator.GREATER_THAN -> actual > target
        LinkOperator.LESS_THAN -> actual < target
        LinkOperator.EQUAL -> kotlin.math.abs(actual - target) < 0.01
        LinkOperator.NOT_EQUAL -> kotlin.math.abs(actual - target) >= 0.01
    }
}
