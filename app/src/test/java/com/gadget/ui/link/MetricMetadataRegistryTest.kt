package com.gadget.ui.link

import com.gadget.widget.WidgetMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricMetadataRegistryTest {

    @Test fun `every WidgetMetric key has metadata`() {
        for (m in WidgetMetric.entries) {
            assertNotNull(
                "Missing MetricMetadata for ${m.key}",
                MetricMetadataRegistry.get(m.key),
            )
        }
    }

    @Test fun `numeric metadata defaultThreshold sits within min-max bounds`() {
        MetricMetadataRegistry.all()
            .filterValues { !it.isCategorical && it.min != null && it.max != null }
            .forEach { (key, meta) ->
                val min = meta.min!!
                val max = meta.max!!
                assertTrue(
                    "$key: defaultThreshold ${meta.defaultThreshold} outside [$min, $max]",
                    meta.defaultThreshold in min..max,
                )
            }
    }

    @Test fun `categorical metadata has no presets`() {
        val all = MetricMetadataRegistry.all()
        for ((key, meta) in all) {
            if (meta.isCategorical) {
                assertTrue("$key: categorical metric must have empty presets", meta.presets.isEmpty())
            }
        }
    }

    @Test fun `categorical metric exposes either allowed values or treats threshold as free text`() {
        val statuses = MetricMetadataRegistry.get("battery_status")
        assertNotNull(statuses)
        assertTrue(statuses!!.isCategorical)
        assertTrue("battery_status should expose enum values", statuses.allowedValues.isNotEmpty())

        val ssid = MetricMetadataRegistry.get("wifi_ssid")
        assertNotNull(ssid)
        assertTrue(ssid!!.isCategorical)
        // SSIDs are user-specific → empty allowedValues is acceptable (free text).
    }

    @Test fun `presets reference operators that the editor will accept`() {
        val all = MetricMetadataRegistry.all()
        for ((key, meta) in all) {
            for (preset in meta.presets) {
                if (preset.operator.isRange) {
                    assertFalse(
                        "$key preset '${preset.label}' has NaN high for range operator",
                        preset.high.isNaN(),
                    )
                }
            }
        }
    }

    @Test fun `sliderRange returns min-max when bounds known`() {
        val battery = MetricMetadataRegistry.get("battery_level")!!
        val range = battery.sliderRange()
        assertEquals(0.0, range.start, 0.001)
        assertEquals(100.0, range.endInclusive, 0.001)
    }

    @Test fun `hintString returns a non-empty value for every numeric metric`() {
        for ((key, meta) in MetricMetadataRegistry.all()) {
            if (meta.isCategorical) continue
            val hint = meta.hintString()
            assertNotNull("Numeric metric $key should produce a hint", hint)
            assertTrue(hint!!.isNotBlank())
        }
    }
}
