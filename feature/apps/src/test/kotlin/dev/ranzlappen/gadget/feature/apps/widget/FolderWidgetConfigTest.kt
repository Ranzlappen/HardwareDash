package dev.ranzlappen.gadget.feature.apps.widget

import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wire-format tests for [FolderWidgetConfig] — the per-`appWidgetId` config
 * persisted as JSON through the kit's `WidgetConfigStore`. A placed widget's
 * config must keep decoding across app updates, so:
 *  - a full round-trip is stable, and
 *  - a minimal/older on-disk record (missing later fields) decodes to the
 *    field defaults rather than throwing.
 */
class FolderWidgetConfigTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `round-trips a fully-populated config`() {
        val config = FolderWidgetConfig(
            folderId = 42L,
            sizePreset = WidgetSizePreset.Large,
            showLabel = false,
            coverTintArgb = 0xFF112233L,
            displayName = "Games",
            removed = true,
            schemaVersion = 1,
        )
        val decoded = json.decodeFromString(
            FolderWidgetConfig.serializer(),
            json.encodeToString(FolderWidgetConfig.serializer(), config),
        )
        assertEquals(config, decoded)
    }

    @Test
    fun `minimal JSON decodes to defaults`() {
        val decoded = json.decodeFromString(
            FolderWidgetConfig.serializer(),
            """{"folderId":7}""",
        )
        assertEquals(7L, decoded.folderId)
        assertEquals(WidgetSizePreset.Medium, decoded.sizePreset)
        assertEquals("", decoded.displayName)
        assertEquals(false, decoded.removed)
        assertEquals(1, decoded.schemaVersion)
        // New content-customizer fields fall back to their defaults.
        assertEquals(true, decoded.showLabel)
        assertEquals(FolderWidgetConfig.FOLLOW_FOLDER_COLOR, decoded.coverTintArgb)
    }

    @Test
    fun `empty JSON yields the no-folder default`() {
        val decoded = json.decodeFromString(FolderWidgetConfig.serializer(), "{}")
        assertEquals(FolderWidgetConfig.NO_FOLDER, decoded.folderId)
    }

    @Test
    fun `unknown future keys are ignored`() {
        val decoded = json.decodeFromString(
            FolderWidgetConfig.serializer(),
            """{"folderId":3,"somethingNew":true}""",
        )
        assertTrue(decoded.folderId == 3L)
    }
}
