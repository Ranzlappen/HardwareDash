package dev.ranzlappen.gadget.feature.torch.widget

import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.feature.torch.automation.TorchActionHandler
import dev.ranzlappen.gadget.feature.torch.widget.migration.TorchWidgetMigrator
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Decode + migration tests for [TorchWidgetConfig] across the v1 → v2
 * (function-driven) schema bump.
 *
 * The real disk persistence goes through
 * [dev.ranzlappen.gadget.core.datastore.FeaturePreferences] (its own
 * integration test). These tests pin two things that would otherwise surface
 * only as a silent on-disk incompatibility for upgrading users:
 *  1. v1 JSON (the legacy `type` / `rateHz` / `sosMode` / `morseText` shape)
 *     still **decodes** — the deprecated decode-only fields survive the lenient
 *     decoder so [TorchWidgetMigrator] can read them.
 *  2. [TorchWidgetMigrator] folds each v1 variant into the right v2
 *     `actionKey` + `params`, preserves `appearance`/`displayName`, and nulls
 *     the legacy fields.
 *
 * The decoder mirrors `FeaturePreferencesFactory.sharedJson`
 * (`ignoreUnknownKeys = true`).
 */
class TorchWidgetConfigTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val migrator = TorchWidgetMigrator()

    private fun decode(raw: String): TorchWidgetConfig =
        json.decodeFromString(TorchWidgetConfig.serializer(), raw)

    @Test
    fun `v2 flashlight config round-trips losslessly`() {
        val original = TorchWidgetConfig(
            displayName = "My flashlight",
            actionKey = TorchWidgetConfig.FUNCTION_FLASHLIGHT,
        )
        val encoded = json.encodeToString(TorchWidgetConfig.serializer(), original)
        val decoded = decode(encoded)
        // Already v2 → migrator is a no-op passthrough.
        assertEquals(original, migrator.migrate(decoded))
    }

    @Test
    fun `v1 flashlight json migrates to the power toggle function`() {
        val migrated = migrator.migrate(
            decode("""{"type":"Flashlight","displayName":"x","schemaVersion":1}"""),
        )
        assertEquals(TorchWidgetConfig.FUNCTION_FLASHLIGHT, migrated.actionKey)
        assertEquals(emptyMap(), migrated.params)
        assertEquals(TorchWidgetConfig.SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals(WidgetSizePreset.Medium, migrated.sizePreset)
        assertEquals("x", migrated.displayName)
        // Legacy fields nulled after the fold.
        @Suppress("DEPRECATION")
        assertNull(migrated.type)
    }

    @Test
    fun `v1 strobe json migrates to the strobe toggle with the persisted rate`() {
        val migrated = migrator.migrate(
            decode("""{"type":"Strobe","displayName":"Loud","rateHz":12.0,"sosMode":false,"schemaVersion":1}"""),
        )
        assertEquals(TorchWidgetConfig.FUNCTION_STROBE, migrated.actionKey)
        assertEquals("12.0", migrated.params[TorchActionHandler.PARAM_RATE_HZ])
        assertEquals(TorchWidgetConfig.SCHEMA_VERSION, migrated.schemaVersion)
    }

    @Test
    fun `v1 sosMode strobe migrates to the morse momentary with text and rate`() {
        val migrated = migrator.migrate(
            decode("""{"type":"Strobe","displayName":"SOS","rateHz":5.0,"sosMode":true,"morseText":"HELP","schemaVersion":1}"""),
        )
        assertEquals(TorchWidgetConfig.FUNCTION_MORSE, migrated.actionKey)
        assertEquals("HELP", migrated.params[TorchActionHandler.PARAM_TEXT])
        assertEquals("5.0", migrated.params[TorchActionHandler.PARAM_RATE_HZ])
    }

    @Test
    fun `v1 sosMode strobe with blank morse text defaults to SOS`() {
        val migrated = migrator.migrate(
            decode("""{"type":"Strobe","displayName":"SOS","sosMode":true,"morseText":"","schemaVersion":1}"""),
        )
        assertEquals(TorchWidgetConfig.FUNCTION_MORSE, migrated.actionKey)
        assertEquals("SOS", migrated.params[TorchActionHandler.PARAM_TEXT])
    }

    @Test
    fun `migration preserves appearance and removed flag`() {
        // A v1 record carrying an appearance + removed flag must keep both
        // through the fold (only the function-routing fields change).
        val raw = """{"type":"Flashlight","displayName":"keep","removed":true,"schemaVersion":1}"""
        val migrated = migrator.migrate(decode(raw))
        assertTrue(migrated.removed)
        assertEquals("keep", migrated.displayName)
    }

    @Test
    fun `decoding json with unknown fields tolerates them`() {
        // Forward-compatibility: a future field must not break loading older
        // on-disk JSON (the `ignoreUnknownKeys = true` decoder config).
        val decoded = decode(
            """{"displayName":"Old","actionKey":"torch_power","futureField":42}""",
        )
        assertEquals(TorchWidgetConfig.FUNCTION_FLASHLIGHT, decoded.actionKey)
        assertEquals("Old", decoded.displayName)
    }
}
