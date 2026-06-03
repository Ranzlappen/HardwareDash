package dev.ranzlappen.gadget.feature.vibration.widget

import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.feature.vibration.automation.VibrationActionHandler
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Serialization round-trip for the v2 [VibrationWidgetConfig] (pins the JSON
 * shape so a field rename surfaces as a test failure, not a silent on-disk
 * incompatibility). The v1→v2 fold is covered by [migration.VibrationWidgetMigratorTest].
 */
class VibrationWidgetConfigTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `oneshot config round-trips`() {
        val original = VibrationWidgetConfig(
            displayName = "Buzz",
            actionKey = VibrationWidgetConfig.FUNCTION_ONESHOT,
            params = mapOf(
                VibrationActionHandler.PARAM_AMPLITUDE to "75",
                VibrationActionHandler.PARAM_DURATION_MS to "400",
            ),
            sizePreset = WidgetSizePreset.Large,
        )
        val decoded = json.decodeFromString(
            VibrationWidgetConfig.serializer(),
            json.encodeToString(VibrationWidgetConfig.serializer(), original),
        )
        assertEquals(original, decoded)
    }

    @Test
    fun `pattern config round-trips with a pattern_id param`() {
        val original = VibrationWidgetConfig(
            displayName = "SOS pattern",
            actionKey = VibrationWidgetConfig.FUNCTION_PATTERN,
            params = mapOf(VibrationActionHandler.PARAM_PATTERN_ID to "pattern-123"),
        )
        val decoded = json.decodeFromString(
            VibrationWidgetConfig.serializer(),
            json.encodeToString(VibrationWidgetConfig.serializer(), original),
        )
        assertEquals(original, decoded)
    }

    @Test
    fun `decoding tolerates unknown fields`() {
        val withExtra = """{"displayName":"X","actionKey":"oneshot","futureField":42}"""
        val decoded = json.decodeFromString(VibrationWidgetConfig.serializer(), withExtra)
        assertEquals("X", decoded.displayName)
        assertEquals(VibrationWidgetConfig.FUNCTION_ONESHOT, decoded.actionKey)
    }

    @Test
    fun `defaults to oneshot at the current schema version`() {
        val config = VibrationWidgetConfig(displayName = "Y")
        assertEquals(VibrationWidgetConfig.FUNCTION_ONESHOT, config.actionKey)
        assertEquals(VibrationWidgetConfig.SCHEMA_VERSION, config.schemaVersion)
        assertEquals(WidgetSizePreset.Medium, config.sizePreset)
    }
}
