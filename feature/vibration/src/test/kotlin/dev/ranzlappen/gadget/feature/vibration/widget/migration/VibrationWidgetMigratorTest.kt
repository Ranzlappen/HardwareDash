package dev.ranzlappen.gadget.feature.vibration.widget.migration

import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.feature.vibration.automation.VibrationActionHandler
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationWidgetConfig
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Verifies the v1→v2 fold: an old `type` + amplitude/duration/patternId record
 * still decodes (the v2 data class keeps the legacy fields as nullable
 * decode-only carriers) and the migrator rewrites it into the function-driven
 * `actionKey` + `params` shape, preserving appearance + the removed flag and
 * nulling the legacy carriers. v2 records pass through untouched.
 */
class VibrationWidgetMigratorTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val migrator = VibrationWidgetMigrator()

    @Test
    fun `v1 Vibrate folds to the oneshot function with amplitude and duration params`() {
        val v1 = """
            {"type":"Vibrate","displayName":"Buzz","amplitudePercent":75,
             "durationMillis":400,"patternId":"","schemaVersion":1}
        """.trimIndent()
        val decoded = json.decodeFromString(VibrationWidgetConfig.serializer(), v1)

        val migrated = migrator.migrate(decoded)

        assertEquals(VibrationWidgetConfig.FUNCTION_ONESHOT, migrated.actionKey)
        assertEquals("75", migrated.params[VibrationActionHandler.PARAM_AMPLITUDE])
        assertEquals("400", migrated.params[VibrationActionHandler.PARAM_DURATION_MS])
        assertEquals(VibrationWidgetConfig.SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals(WidgetSizePreset.Medium, migrated.sizePreset)
        assertLegacyFieldsNulled(migrated)
    }

    @Test
    fun `v1 Pattern folds to the pattern_play function with the pattern_id param`() {
        val v1 = """
            {"type":"Pattern","displayName":"SOS","amplitudePercent":60,
             "durationMillis":300,"patternId":"pattern-9","schemaVersion":1}
        """.trimIndent()
        val decoded = json.decodeFromString(VibrationWidgetConfig.serializer(), v1)

        val migrated = migrator.migrate(decoded)

        assertEquals(VibrationWidgetConfig.FUNCTION_PATTERN, migrated.actionKey)
        assertEquals("pattern-9", migrated.params[VibrationActionHandler.PARAM_PATTERN_ID])
        assertEquals(VibrationWidgetConfig.SCHEMA_VERSION, migrated.schemaVersion)
        assertLegacyFieldsNulled(migrated)
    }

    @Test
    fun `v1 preserves appearance and the removed flag`() {
        val appearance = WidgetAppearance()
        val decoded = VibrationWidgetConfig(
            displayName = "Old",
            appearance = appearance,
            removed = true,
            schemaVersion = 1,
            type = "Vibrate",
            amplitudePercent = 30,
            durationMillis = 200,
        )

        val migrated = migrator.migrate(decoded)

        assertEquals(appearance, migrated.appearance)
        assertEquals(true, migrated.removed)
    }

    @Test
    fun `v1 with absent amplitude and duration uses defaults`() {
        val v1 = """{"type":"Vibrate","displayName":"Bare","schemaVersion":1}"""
        val decoded = json.decodeFromString(VibrationWidgetConfig.serializer(), v1)

        val migrated = migrator.migrate(decoded)

        assertEquals(
            VibrationWidgetConfig.DEFAULT_AMPLITUDE_PERCENT.toString(),
            migrated.params[VibrationActionHandler.PARAM_AMPLITUDE],
        )
        assertEquals(
            VibrationWidgetConfig.DEFAULT_DURATION_MS.toString(),
            migrated.params[VibrationActionHandler.PARAM_DURATION_MS],
        )
    }

    @Test
    fun `a v2 config passes through untouched`() {
        val v2 = VibrationWidgetConfig(
            displayName = "Already v2",
            actionKey = VibrationWidgetConfig.FUNCTION_PATTERN,
            params = mapOf(VibrationActionHandler.PARAM_PATTERN_ID to "x"),
        )
        assertEquals(v2, migrator.migrate(v2))
    }

    @Suppress("DEPRECATION")
    private fun assertLegacyFieldsNulled(config: VibrationWidgetConfig) {
        assertNull(config.type)
        assertNull(config.amplitudePercent)
        assertNull(config.durationMillis)
        assertNull(config.patternId)
    }
}
