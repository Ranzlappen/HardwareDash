package com.gadget.ui.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkModelsTest {

    // ── extractNumeric ──────────────────────────────────────────────────────

    @Test fun `extractNumeric parses value with unit suffix`() {
        assertEquals(9.81, extractNumeric("9.81 m/s²")!!, 0.001)
    }

    @Test fun `extractNumeric parses negative value`() {
        assertEquals(-5.3, extractNumeric("-5.3dBm")!!, 0.001)
    }

    @Test fun `extractNumeric returns null for NA`() {
        assertNull(extractNumeric("N/A"))
    }

    @Test fun `extractNumeric returns null for empty string`() {
        assertNull(extractNumeric(""))
    }

    @Test fun `extractNumeric parses percentage`() {
        assertEquals(100.0, extractNumeric("100%")!!, 0.001)
    }

    @Test fun `extractNumeric parses integer without decimal`() {
        assertEquals(42.0, extractNumeric("42 lux")!!, 0.001)
    }

    @Test fun `extractNumeric parses zero`() {
        assertEquals(0.0, extractNumeric("0.00 rad/s")!!, 0.001)
    }

    // ── evaluateCondition ───────────────────────────────────────────────────

    @Test fun `evaluateCondition GT true when actual exceeds threshold`() {
        assertTrue(evaluateCondition("75%", "gt", "50"))
    }

    @Test fun `evaluateCondition GT false when actual equals threshold`() {
        assertFalse(evaluateCondition("50%", "gt", "50"))
    }

    @Test fun `evaluateCondition GT false when actual below threshold`() {
        assertFalse(evaluateCondition("25%", "gt", "50"))
    }

    @Test fun `evaluateCondition LT true when actual below threshold`() {
        assertTrue(evaluateCondition("25%", "lt", "50"))
    }

    @Test fun `evaluateCondition LT false when actual exceeds threshold`() {
        assertFalse(evaluateCondition("75%", "lt", "50"))
    }

    @Test fun `evaluateCondition EQ numeric within tolerance`() {
        assertTrue(evaluateCondition("50.005%", "eq", "50"))
    }

    @Test fun `evaluateCondition EQ numeric outside tolerance`() {
        assertFalse(evaluateCondition("50.02%", "eq", "50"))
    }

    @Test fun `evaluateCondition EQ string match for categorical metric`() {
        assertTrue(evaluateCondition("Charging", "eq", "Charging"))
        assertFalse(evaluateCondition("Discharging", "eq", "Charging"))
    }

    @Test fun `evaluateCondition NEQ string mismatch true`() {
        assertTrue(evaluateCondition("Discharging", "neq", "Charging"))
    }

    @Test fun `evaluateCondition GTE inclusive at threshold`() {
        assertTrue(evaluateCondition("50%", "gte", "50"))
    }

    @Test fun `evaluateCondition LTE inclusive at threshold`() {
        assertTrue(evaluateCondition("50%", "lte", "50"))
    }

    @Test fun `evaluateCondition BETWEEN inside range`() {
        assertTrue(evaluateCondition("50%", "between", "20", "80"))
    }

    @Test fun `evaluateCondition BETWEEN at boundaries inclusive`() {
        assertTrue(evaluateCondition("20%", "between", "20", "80"))
        assertTrue(evaluateCondition("80%", "between", "20", "80"))
    }

    @Test fun `evaluateCondition BETWEEN outside range`() {
        assertFalse(evaluateCondition("10%", "between", "20", "80"))
        assertFalse(evaluateCondition("90%", "between", "20", "80"))
    }

    @Test fun `evaluateCondition OUTSIDE true when below low`() {
        assertTrue(evaluateCondition("10%", "outside", "20", "80"))
    }

    @Test fun `evaluateCondition OUTSIDE true when above high`() {
        assertTrue(evaluateCondition("90%", "outside", "20", "80"))
    }

    @Test fun `evaluateCondition OUTSIDE false at boundaries`() {
        assertFalse(evaluateCondition("20%", "outside", "20", "80"))
        assertFalse(evaluateCondition("80%", "outside", "20", "80"))
    }

    @Test fun `evaluateCondition returns false when metric is non-numeric and non-eq`() {
        assertFalse(evaluateCondition("N/A", "gt", "50"))
    }

    @Test fun `evaluateCondition with negative values`() {
        assertTrue(evaluateCondition("-60 dBm", "lt", "-50"))
        assertFalse(evaluateCondition("-40 dBm", "lt", "-50"))
    }

    // ── recommendedThresholds (now derived from MetricMetadata) ────────────

    @Test fun `recommendedThresholds returns non-null for battery_level`() {
        val r = recommendedThresholds("battery_level")
        assertNotNull(r)
        assertTrue(r!!.contains("Critical"))
    }

    @Test fun `recommendedThresholds returns non-null for wifi_signal`() {
        val r = recommendedThresholds("wifi_signal")
        assertNotNull(r)
        assertTrue(r!!.contains("dBm"))
    }

    @Test fun `recommendedThresholds returns null for unknown metric`() {
        assertNull(recommendedThresholds("unknown_metric_xyz"))
    }

    @Test fun `recommendedThresholds covers all numeric metric keys`() {
        val numericKeys = listOf(
            "battery_level", "battery_temp", "battery_voltage", "battery_current",
            "battery_charge_time", "wifi_signal", "wifi_speed", "wifi_freq",
            "cell_signal", "accel", "gyro", "magneto", "light", "proximity",
            "barometer", "ambient_temp", "humidity", "steps", "brightness",
            "gps_altitude", "gps_speed", "gps_lat", "gps_lon",
        )
        for (key in numericKeys) {
            assertNotNull("Expected non-null hint for $key", recommendedThresholds(key))
        }
    }

    // ── LinkOperator ───────────────────────────────────────────────────────

    @Test fun `LinkOperator fromKey returns correct operator`() {
        assertEquals(LinkOperator.GREATER_THAN, LinkOperator.fromKey("gt"))
        assertEquals(LinkOperator.LESS_THAN, LinkOperator.fromKey("lt"))
        assertEquals(LinkOperator.EQUAL, LinkOperator.fromKey("eq"))
        assertEquals(LinkOperator.NOT_EQUAL, LinkOperator.fromKey("neq"))
        assertEquals(LinkOperator.GREATER_THAN_OR_EQUAL, LinkOperator.fromKey("gte"))
        assertEquals(LinkOperator.LESS_THAN_OR_EQUAL, LinkOperator.fromKey("lte"))
        assertEquals(LinkOperator.BETWEEN, LinkOperator.fromKey("between"))
        assertEquals(LinkOperator.OUTSIDE, LinkOperator.fromKey("outside"))
    }

    @Test fun `LinkOperator fromKey defaults to GREATER_THAN for unknown key`() {
        assertEquals(LinkOperator.GREATER_THAN, LinkOperator.fromKey("invalid"))
    }

    @Test fun `LinkOperator isRange true only for BETWEEN and OUTSIDE`() {
        assertTrue(LinkOperator.BETWEEN.isRange)
        assertTrue(LinkOperator.OUTSIDE.isRange)
        for (op in listOf(
            LinkOperator.GREATER_THAN, LinkOperator.LESS_THAN, LinkOperator.EQUAL,
            LinkOperator.NOT_EQUAL, LinkOperator.GREATER_THAN_OR_EQUAL, LinkOperator.LESS_THAN_OR_EQUAL,
        )) {
            assertFalse(op.isRange)
        }
    }
}
