package dev.ranzlappen.gadget.core.monitoring

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class MonitorConfigTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `config round-trips through json`() {
        val original = MonitorConfig(
            enabled = true,
            pollIntervalMs = 2_000L,
            chartLayout = MonitorChartLayout.Bars,
            windowSeconds = 120,
            yMax = 50f,
            widgetEnabled = true,
            notificationEnabled = true,
        )
        val decoded = json.decodeFromString(
            MonitorConfig.serializer(),
            json.encodeToString(MonitorConfig.serializer(), original),
        )
        assertEquals(original, decoded)
    }

    @Test
    fun `empty object decodes to defaults`() {
        val decoded = json.decodeFromString(MonitorConfig.serializer(), "{}")
        assertEquals(MonitorConfig(), decoded)
    }
}
