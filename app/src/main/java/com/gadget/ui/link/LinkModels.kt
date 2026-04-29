package com.gadget.ui.link

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
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

// ─── Numeric helpers ───────────────────────────────────────────────────────

/** Extract the first numeric value from a formatted metric string (e.g. "9.81 m/s²" → 9.81). */
fun extractNumeric(formatted: String): Double? {
    return """(-?\d+\.?\d*)""".toRegex().find(formatted)?.groupValues?.get(1)?.toDoubleOrNull()
}

/** Evaluate whether a metric value satisfies a single comparison. */
fun evaluateCondition(
    metricValue: String,
    operator: String,
    threshold: String,
    thresholdHigh: String = "",
): Boolean {
    val op = LinkOperator.fromKey(operator)

    // Categorical / string comparisons (only EQUAL and NOT_EQUAL are meaningful)
    if (op == LinkOperator.EQUAL || op == LinkOperator.NOT_EQUAL) {
        val numericA = extractNumeric(metricValue)
        val numericB = threshold.toDoubleOrNull()
        if (numericA != null && numericB != null) {
            val match = kotlin.math.abs(numericA - numericB) < 0.01
            return if (op == LinkOperator.EQUAL) match else !match
        }
        // Fall back to case-insensitive string equality
        val match = metricValue.trim().equals(threshold.trim(), ignoreCase = true)
        return if (op == LinkOperator.EQUAL) match else !match
    }

    val actual = extractNumeric(metricValue) ?: return false
    val target = threshold.toDoubleOrNull() ?: return false
    return when (op) {
        LinkOperator.GREATER_THAN          -> actual > target
        LinkOperator.LESS_THAN             -> actual < target
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
        // EQUAL / NOT_EQUAL handled above
        else -> false
    }
}

/**
 * Returns recommended threshold guidance for a given metric, or null if none defined.
 * Derived from [MetricMetadataRegistry] for back-compat with older callers; prefer
 * looking up structured metadata directly.
 */
fun recommendedThresholds(metricKey: String): String? =
    MetricMetadataRegistry.get(metricKey)?.hintString()

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
    } catch (e: Exception) {
        Timber.e(e, "Failed to load link stats")
        emptyMap()
    }
}

// ─── V1 legacy model (read-only; kept for migration of existing user data) ─

@Deprecated("V1 model retained only to migrate legacy persisted rules. Use LinkRuleV2.")
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

@Suppress("DEPRECATION")
private fun linkRuleFromJsonV1(json: JSONObject): LinkRule {
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

@Suppress("DEPRECATION")
private fun loadRulesV1(json: String): List<LinkRule> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { linkRuleFromJsonV1(arr.getJSONObject(it)) }
    } catch (e: Exception) {
        Timber.e(e, "Failed to load V1 link rules")
        emptyList()
    }
}

// ─── V2 model — boolean expression tree + action chain + schedule ──────────

private val linkJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "kind"
}

@Serializable
enum class LogicOperator { AND, OR }

/**
 * A boolean expression node. Either a [Leaf] (a single condition against a sensor
 * metric) or a [Group] (an AND/OR aggregation of children). Both support a
 * per-node [negate] flag (NOT) so users can build any boolean shape without a
 * dedicated NOT operator.
 */
@Serializable
sealed class ConditionNode {

    /** True when this node is currently satisfied; applied AFTER any [negate]. */
    abstract val negate: Boolean

    @Serializable
    @SerialName("leaf")
    data class Leaf(
        val metricKey: String,
        val operator: String,
        val threshold: String,
        val thresholdHigh: String = "",
        override val negate: Boolean = false,
        /** When > 0, the underlying comparison must hold continuously for this many seconds. */
        val sustainSec: Int = 0,
    ) : ConditionNode()

    @Serializable
    @SerialName("group")
    data class Group(
        val logic: LogicOperator = LogicOperator.AND,
        val children: List<ConditionNode> = emptyList(),
        override val negate: Boolean = false,
    ) : ConditionNode()
}

@Serializable
data class ActionStep(
    val actionType: String,
    val actionConfig: Map<String, String> = emptyMap(),
    /** Delay before THIS step runs, on top of any prior step's delay. */
    val delayMs: Long = 0,
)

@Serializable
data class TimeSchedule(
    /** Calendar.DAY_OF_WEEK values (Sun=1 .. Sat=7). Empty = every day. */
    val daysOfWeek: Set<Int> = emptySet(),
    val startTime: String = "00:00",
    val endTime: String = "23:59",
)

@Serializable
data class LinkRuleV2(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val enabled: Boolean = true,
    val root: ConditionNode = ConditionNode.Group(),
    val actions: List<ActionStep> = emptyList(),
    val cooldownSec: Int = 10,
    /** Wait this many seconds between condition becoming true and the first action firing. */
    val triggerDelaySec: Int = 0,
    /** When true, abort the pending fire if the condition is no longer satisfied at fire time. */
    val cancelDelayIfFalse: Boolean = true,
    val lastTriggeredMs: Long = 0L,
    val schedule: TimeSchedule? = null,
)

// ─── V1 → V2 migration ─────────────────────────────────────────────────────

/** Wraps a single-condition / single-action V1 rule as a V2 rule with a 1-leaf root group. */
@Suppress("DEPRECATION")
fun migrateToV2(rule: LinkRule): LinkRuleV2 = LinkRuleV2(
    id = rule.id,
    name = rule.name,
    enabled = rule.enabled,
    root = ConditionNode.Group(
        logic = LogicOperator.AND,
        children = listOf(
            ConditionNode.Leaf(
                metricKey = rule.metricKey,
                operator = rule.operator,
                threshold = rule.threshold,
                thresholdHigh = rule.thresholdHigh,
            ),
        ),
    ),
    actions = listOf(
        ActionStep(
            actionType = rule.actionType,
            actionConfig = rule.actionConfig,
            delayMs = 0,
        ),
    ),
    cooldownSec = rule.cooldownSec,
    lastTriggeredMs = rule.lastTriggeredMs,
)

// ─── V2 JSON serialization ─────────────────────────────────────────────────

fun saveRulesV2(rules: List<LinkRuleV2>): String =
    linkJson.encodeToString(rules)

/**
 * Deserialize rules from JSON. Tries V2 format first; if that fails, falls back to
 * V1 parsing and migrates each rule to V2 automatically. Empty/blank input → empty list.
 */
fun loadRulesV2(json: String): List<LinkRuleV2> {
    if (json.isBlank()) return emptyList()
    return try {
        linkJson.decodeFromString<List<LinkRuleV2>>(json)
    } catch (_: Exception) {
        try {
            loadRulesV1(json).map { migrateToV2(it) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load V2 link rules (including V1 fallback)")
            emptyList()
        }
    }
}

// ─── Tree helpers ──────────────────────────────────────────────────────────

/** Flatten all [ConditionNode.Leaf]s in pre-order. Useful for summaries and stats. */
fun ConditionNode.allLeaves(): List<ConditionNode.Leaf> = when (this) {
    is ConditionNode.Leaf -> listOf(this)
    is ConditionNode.Group -> children.flatMap { it.allLeaves() }
}

/** Maximum depth of the tree (a single Leaf has depth 1). */
fun ConditionNode.depth(): Int = when (this) {
    is ConditionNode.Leaf -> 1
    is ConditionNode.Group -> 1 + (children.maxOfOrNull { it.depth() } ?: 0)
}
