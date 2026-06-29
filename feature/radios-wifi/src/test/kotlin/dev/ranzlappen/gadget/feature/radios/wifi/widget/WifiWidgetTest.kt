package dev.ranzlappen.gadget.feature.radios.wifi.widget

import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiWidgetTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `config round-trips through json`() {
        val config = WifiWidgetConfig(sizePreset = WidgetSizePreset.Large, displayName = "Home WiFi")
        val decoded = json.decodeFromString(
            WifiWidgetConfig.serializer(),
            json.encodeToString(WifiWidgetConfig.serializer(), config),
        )
        assertEquals(config, decoded)
    }

    @Test
    fun `signalPercent clamps the dBm window to 0-100`() {
        assertEquals(0, WifiSignal.signalPercent(-100))
        assertEquals(0, WifiSignal.signalPercent(-90))
        assertEquals(100, WifiSignal.signalPercent(-40))
        assertEquals(100, WifiSignal.signalPercent(-20))
    }

    @Test
    fun `signalPercent is monotonic across the mid range`() {
        val weak = WifiSignal.signalPercent(-80)
        val mid = WifiSignal.signalPercent(-65)
        val strong = WifiSignal.signalPercent(-50)
        assertTrue(weak < mid)
        assertTrue(mid < strong)
    }
}
