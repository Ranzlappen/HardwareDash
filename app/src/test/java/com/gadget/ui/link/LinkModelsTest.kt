package com.gadget.ui.link

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkModelsTest {

    // ── extractNumeric ──────────────────────────────────────────────────────

    @Test
    fun `extractNumeric parses value with unit suffix`() {
        assertEquals(9.81, extractNumeric("9.81 m/s²")!!, 0.001)
    }

    @Test
    fun `extractNumeric parses negative value`() {
        assertEquals(-5.3, extractNumeric("-5.3dBm")!!, 0.001)
    }

    @Test
    fun `extractNumeric returns null for NA`() {
        assertNull(extractNumeric("N/A"))
    }

    @Test
    fun `extractNumeric returns null for empty string`() {
        assertNull(extractNumeric(""))
    }

    @Test
    fun `extractNumeric parses percentage`() {
        assertEquals(100.0, extractNumeric("100%")!!, 0.001)
    }

    @Test
    fun `extractNumeric parses integer without decimal`() {
        assertEquals(42.0, extractNumeric("42 lux")!!, 0.001)
    }

    @Test
    fun `extractNumeric parses zero`() {
        assertEquals(0.0, extractNumeric("0.00 rad/s")!!, 0.001)
    }

    // ── evaluateCondition ───────────────────────────────────────────────────

    @Test
    fun `evaluateCondition GT true when actual exceeds threshold`() {
        assertTrue(evaluateCondition("75%", "gt", "50"))
    }

    @Test
    fun `evaluateCondition GT false when actual equals threshold`() {
        assertFalse(evaluateCondition("50%", "gt", "50"))
    }

    @Test
    fun `evaluateCondition GT false when actual below threshold`() {
        assertFalse(evaluateCondition("25%", "gt", "50"))
    }

    @Test
    fun `evaluateCondition LT true when actual below threshold`() {
        assertTrue(evaluateCondition("25%", "lt", "50"))
    }

    @Test
    fun `evaluateCondition LT false when actual equals threshold`() {
        assertFalse(evaluateCondition("50%", "lt", "50"))
    }

    @Test
    fun `evaluateCondition LT false when actual exceeds threshold`() {
        assertFalse(evaluateCondition("75%", "lt", "50"))
    }

    @Test
    fun `evaluateCondition EQ true within tolerance`() {
        assertTrue(evaluateCondition("50.005%", "eq", "50"))
    }

    @Test
    fun `evaluateCondition EQ false outside tolerance`() {
        assertFalse(evaluateCondition("50.02%", "eq", "50"))
    }

    @Test
    fun `evaluateCondition EQ true for exact match`() {
        assertTrue(evaluateCondition("50%", "eq", "50"))
    }

    @Test
    fun `evaluateCondition NEQ true when values differ`() {
        assertTrue(evaluateCondition("75%", "neq", "50"))
    }

    @Test
    fun `evaluateCondition NEQ false when values within tolerance`() {
        assertFalse(evaluateCondition("50.005%", "neq", "50"))
    }

    @Test
    fun `evaluateCondition GTE true when actual equals threshold`() {
        assertTrue(evaluateCondition("50%", "gte", "50"))
    }

    @Test
    fun `evaluateCondition GTE true when actual exceeds threshold`() {
        assertTrue(evaluateCondition("75%", "gte", "50"))
    }

    @Test
    fun `evaluateCondition GTE false when actual below threshold`() {
        assertFalse(evaluateCondition("25%", "gte", "50"))
    }

    @Test
    fun `evaluateCondition LTE true when actual equals threshold`() {
        assertTrue(evaluateCondition("50%", "lte", "50"))
    }

    @Test
    fun `evaluateCondition LTE true when actual below threshold`() {
        assertTrue(evaluateCondition("25%", "lte", "50"))
    }

    @Test
    fun `evaluateCondition LTE false when actual exceeds threshold`() {
        assertFalse(evaluateCondition("75%", "lte", "50"))
    }

    @Test
    fun `evaluateCondition BETWEEN true when actual inside range`() {
        assertTrue(evaluateCondition("50%", "between", "20", "80"))
    }

    @Test
    fun `evaluateCondition BETWEEN true at lower boundary`() {
        assertTrue(evaluateCondition("20%", "between", "20", "80"))
    }

    @Test
    fun `evaluateCondition BETWEEN true at upper boundary`() {
        assertTrue(evaluateCondition("80%", "between", "20", "80"))
    }

    @Test
    fun `evaluateCondition BETWEEN false when actual below range`() {
        assertFalse(evaluateCondition("10%", "between", "20", "80"))
    }

    @Test
    fun `evaluateCondition BETWEEN false when actual above range`() {
        assertFalse(evaluateCondition("90%", "between", "20", "80"))
    }

    @Test
    fun `evaluateCondition BETWEEN false when thresholdHigh is missing`() {
        assertFalse(evaluateCondition("50%", "between", "20", ""))
    }

    @Test
    fun `evaluateCondition OUTSIDE true when actual below range`() {
        assertTrue(evaluateCondition("10%", "outside", "20", "80"))
    }

    @Test
    fun `evaluateCondition OUTSIDE true when actual above range`() {
        assertTrue(evaluateCondition("90%", "outside", "20", "80"))
    }

    @Test
    fun `evaluateCondition OUTSIDE false when actual inside range`() {
        assertFalse(evaluateCondition("50%", "outside", "20", "80"))
    }

    @Test
    fun `evaluateCondition OUTSIDE false at boundaries`() {
        assertFalse(evaluateCondition("20%", "outside", "20", "80"))
        assertFalse(evaluateCondition("80%", "outside", "20", "80"))
    }

    @Test
    fun `evaluateCondition OUTSIDE false when thresholdHigh is missing`() {
        assertFalse(evaluateCondition("10%", "outside", "20", ""))
    }

    @Test
    fun `evaluateCondition returns false when metric is non-numeric`() {
        assertFalse(evaluateCondition("N/A", "gt", "50"))
    }

    @Test
    fun `evaluateCondition returns false when threshold is non-numeric`() {
        assertFalse(evaluateCondition("75%", "gt", "abc"))
    }

    @Test
    fun `evaluateCondition with negative values`() {
        assertTrue(evaluateCondition("-60 dBm", "lt", "-50"))
        assertFalse(evaluateCondition("-40 dBm", "lt", "-50"))
    }

    // ── JSON round-trip ────────────────────────────────────────────────────

    @Test
    fun `saveRules and loadRules round-trip preserves single rule`() {
        val rule = LinkRule(
            id = "test-id-1",
            name = "Battery Low",
            enabled = true,
            metricKey = "battery_level",
            operator = "lt",
            threshold = "20",
            thresholdHigh = "",
            actionType = "notification",
            actionConfig = mapOf("title" to "Low Battery", "body" to "Charge soon"),
            cooldownSec = 30,
            lastTriggeredMs = 1000L,
        )
        val json = saveRules(listOf(rule))
        val loaded = loadRules(json)
        assertEquals(1, loaded.size)
        val r = loaded[0]
        assertEquals("test-id-1", r.id)
        assertEquals("Battery Low", r.name)
        assertTrue(r.enabled)
        assertEquals("battery_level", r.metricKey)
        assertEquals("lt", r.operator)
        assertEquals("20", r.threshold)
        assertEquals("", r.thresholdHigh)
        assertEquals("notification", r.actionType)
        assertEquals("Low Battery", r.actionConfig["title"])
        assertEquals("Charge soon", r.actionConfig["body"])
        assertEquals(30, r.cooldownSec)
        assertEquals(1000L, r.lastTriggeredMs)
    }

    @Test
    fun `saveRules and loadRules round-trip preserves multiple rules`() {
        val rules = listOf(
            LinkRule(id = "r1", name = "Rule 1", metricKey = "battery_level"),
            LinkRule(id = "r2", name = "Rule 2", metricKey = "wifi_signal"),
            LinkRule(id = "r3", name = "Rule 3", metricKey = "light"),
        )
        val json = saveRules(rules)
        val loaded = loadRules(json)
        assertEquals(3, loaded.size)
        assertEquals("r1", loaded[0].id)
        assertEquals("r2", loaded[1].id)
        assertEquals("r3", loaded[2].id)
    }

    @Test
    fun `saveRules and loadRules round-trip with empty list`() {
        val json = saveRules(emptyList())
        val loaded = loadRules(json)
        assertTrue(loaded.isEmpty())
    }

    @Test
    fun `loadRules returns empty list for empty string`() {
        assertTrue(loadRules("").isEmpty())
    }

    @Test
    fun `loadRules returns empty list for blank string`() {
        assertTrue(loadRules("   ").isEmpty())
    }

    @Test
    fun `loadRules returns empty list for malformed JSON`() {
        assertTrue(loadRules("not valid json").isEmpty())
    }

    @Test
    fun `loadRules returns empty list for partial JSON`() {
        assertTrue(loadRules("[{\"id\":").isEmpty())
    }

    // ── linkRuleFromJson ───────────────────────────────────────────────────

    @Test
    fun `linkRuleFromJson uses defaults for missing fields`() {
        val json = JSONObject().apply { put("id", "minimal") }
        val rule = linkRuleFromJson(json)
        assertEquals("minimal", rule.id)
        assertEquals("", rule.name)
        assertTrue(rule.enabled)
        assertEquals("", rule.metricKey)
        assertEquals("gt", rule.operator)
        assertEquals("0", rule.threshold)
        assertEquals("", rule.thresholdHigh)
        assertEquals("notification", rule.actionType)
        assertTrue(rule.actionConfig.isEmpty())
        assertEquals(10, rule.cooldownSec)
        assertEquals(0L, rule.lastTriggeredMs)
    }

    @Test
    fun `linkRuleFromJson parses actionConfig correctly`() {
        val json = JSONObject().apply {
            put("id", "cfg-test")
            put("actionConfig", JSONObject().apply {
                put("key1", "value1")
                put("key2", "value2")
            })
        }
        val rule = linkRuleFromJson(json)
        assertEquals("value1", rule.actionConfig["key1"])
        assertEquals("value2", rule.actionConfig["key2"])
    }

    // ── recommendedThresholds ──────────────────────────────────────────────

    @Test
    fun `recommendedThresholds returns non-null for battery_level`() {
        val result = recommendedThresholds("battery_level")
        assertNotNull(result)
        assertTrue(result!!.contains("Critical"))
    }

    @Test
    fun `recommendedThresholds returns non-null for wifi_signal`() {
        val result = recommendedThresholds("wifi_signal")
        assertNotNull(result)
        assertTrue(result!!.contains("dBm"))
    }

    @Test
    fun `recommendedThresholds returns null for unknown metric`() {
        assertNull(recommendedThresholds("unknown_metric_xyz"))
    }

    @Test
    fun `recommendedThresholds returns non-null for all known metric keys`() {
        val knownKeys = listOf(
            "battery_level", "battery_temp", "battery_voltage", "battery_current",
            "battery_charge_time", "wifi_signal", "wifi_speed", "wifi_freq",
            "cell_signal", "accel", "gyro", "magneto", "light", "proximity",
            "barometer", "ambient_temp", "humidity", "steps", "brightness",
            "gps_altitude", "gps_speed", "gps_lat", "gps_lon",
        )
        for (key in knownKeys) {
            assertNotNull("Expected non-null threshold for $key", recommendedThresholds(key))
        }
    }

    // ── LinkOperator ───────────────────────────────────────────────────────

    @Test
    fun `LinkOperator fromKey returns correct operator`() {
        assertEquals(LinkOperator.GREATER_THAN, LinkOperator.fromKey("gt"))
        assertEquals(LinkOperator.LESS_THAN, LinkOperator.fromKey("lt"))
        assertEquals(LinkOperator.EQUAL, LinkOperator.fromKey("eq"))
        assertEquals(LinkOperator.NOT_EQUAL, LinkOperator.fromKey("neq"))
        assertEquals(LinkOperator.GREATER_THAN_OR_EQUAL, LinkOperator.fromKey("gte"))
        assertEquals(LinkOperator.LESS_THAN_OR_EQUAL, LinkOperator.fromKey("lte"))
        assertEquals(LinkOperator.BETWEEN, LinkOperator.fromKey("between"))
        assertEquals(LinkOperator.OUTSIDE, LinkOperator.fromKey("outside"))
    }

    @Test
    fun `LinkOperator fromKey defaults to GREATER_THAN for unknown key`() {
        assertEquals(LinkOperator.GREATER_THAN, LinkOperator.fromKey("invalid"))
    }

    @Test
    fun `LinkOperator isRange true only for BETWEEN and OUTSIDE`() {
        assertTrue(LinkOperator.BETWEEN.isRange)
        assertTrue(LinkOperator.OUTSIDE.isRange)
        assertFalse(LinkOperator.GREATER_THAN.isRange)
        assertFalse(LinkOperator.LESS_THAN.isRange)
        assertFalse(LinkOperator.EQUAL.isRange)
        assertFalse(LinkOperator.NOT_EQUAL.isRange)
        assertFalse(LinkOperator.GREATER_THAN_OR_EQUAL.isRange)
        assertFalse(LinkOperator.LESS_THAN_OR_EQUAL.isRange)
    }
}
