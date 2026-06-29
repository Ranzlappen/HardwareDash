package dev.ranzlappen.gadget.feature.storage.widget

import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the persisted shape of [StorageWidgetConfig]. The config is stored
 * per `appWidgetId` via DataStore JSON, so a serialization regression would
 * make placed widgets unreadable.
 */
class StorageWidgetConfigTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `round-trips through json`() {
        val config = StorageWidgetConfig(
            sizePreset = WidgetSizePreset.Small,
            displayName = "Internal",
        )
        val decoded = json.decodeFromString(
            StorageWidgetConfig.serializer(),
            json.encodeToString(StorageWidgetConfig.serializer(), config),
        )
        assertEquals(config, decoded)
    }

    @Test
    fun `defaults match the kit contract`() {
        val config = StorageWidgetConfig()
        assertEquals(WidgetSizePreset.Medium, config.sizePreset)
        assertEquals(1, config.schemaVersion)
        assertEquals(false, config.removed)
    }
}
