package dev.ranzlappen.gadget.feature.battery.widget

import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the persisted shape of [BatteryWidgetConfig]. The config is stored
 * per `appWidgetId` via DataStore JSON, so a serialization regression would
 * make placed widgets unreadable.
 */
class BatteryWidgetConfigTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `round-trips through json`() {
        val config = BatteryWidgetConfig(
            sizePreset = WidgetSizePreset.Large,
            displayName = "My Battery",
        )
        val decoded = json.decodeFromString(
            BatteryWidgetConfig.serializer(),
            json.encodeToString(BatteryWidgetConfig.serializer(), config),
        )
        assertEquals(config, decoded)
    }

    @Test
    fun `defaults match the kit contract`() {
        val config = BatteryWidgetConfig()
        assertEquals(WidgetSizePreset.Medium, config.sizePreset)
        assertEquals(1, config.schemaVersion)
        assertEquals(false, config.removed)
    }
}
