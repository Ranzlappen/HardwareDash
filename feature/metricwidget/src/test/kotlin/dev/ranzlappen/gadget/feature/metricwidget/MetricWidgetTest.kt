package dev.ranzlappen.gadget.feature.metricwidget

import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.feature.metricwidget.widget.MetricWidgetProvider
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricWidgetTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `config round-trips through json`() {
        val config = MetricWidgetConfig(
            metricKey = "battery_level",
            display = MetricWidgetDisplay.Sparkline,
            showLabel = false,
            tintArgb = 0xFF00FF00L,
            windowSeconds = 900,
            sizePreset = WidgetSizePreset.Large,
            displayName = "Battery level",
        )
        val decoded = json.decodeFromString(
            MetricWidgetConfig.serializer(),
            json.encodeToString(MetricWidgetConfig.serializer(), config),
        )
        assertEquals(config, decoded)
    }

    @Test
    fun `empty json decodes to unbound defaults`() {
        val decoded = json.decodeFromString(MetricWidgetConfig.serializer(), "{}")
        assertEquals(MetricWidgetConfig.NO_METRIC, decoded.metricKey)
        assertFalse(decoded.isBound)
        assertEquals(MetricWidgetDisplay.ValueAndBar, decoded.display)
        assertTrue(decoded.showLabel)
        assertEquals(MetricWidgetConfig.DEFAULT_WINDOW_SECONDS, decoded.windowSeconds)
    }

    @Test
    fun `isBound reflects a non-empty metric key`() {
        assertFalse(MetricWidgetConfig().isBound)
        assertTrue(MetricWidgetConfig(metricKey = "x").isBound)
    }

    @Test
    fun `barProgress scales value across the descriptor span`() {
        val d = MetricDescriptor(metricKey = "k", displayName = "K", min = 0f, max = 200f)
        assertEquals(0, MetricWidgetProvider.barProgress(0f, d))
        assertEquals(50, MetricWidgetProvider.barProgress(100f, d))
        assertEquals(100, MetricWidgetProvider.barProgress(200f, d))
        // Clamped outside the range.
        assertEquals(100, MetricWidgetProvider.barProgress(500f, d))
        assertEquals(0, MetricWidgetProvider.barProgress(-5f, d))
    }

    @Test
    fun `barProgress honours a non-zero minimum`() {
        val d = MetricDescriptor(metricKey = "k", displayName = "K", min = 20f, max = 120f)
        assertEquals(0, MetricWidgetProvider.barProgress(20f, d))
        assertEquals(50, MetricWidgetProvider.barProgress(70f, d))
    }

    @Test
    fun `barProgress is zero for a degenerate span`() {
        val d = MetricDescriptor(metricKey = "k", displayName = "K", min = 10f, max = 10f)
        assertEquals(0, MetricWidgetProvider.barProgress(10f, d))
    }

    @Test
    fun `formatValue keeps one decimal for small fractional magnitudes`() {
        assertEquals("5.5", MetricWidgetProvider.formatValue(5.5f))
        assertEquals("42", MetricWidgetProvider.formatValue(42f))
        assertEquals("100", MetricWidgetProvider.formatValue(99.6f))
        assertEquals("7", MetricWidgetProvider.formatValue(7f))
    }
}
