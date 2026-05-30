package dev.ranzlappen.gadget.core.widgetkit.config

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wire-format regression tests for the appearance value family.
 *
 * Critical invariant: [WidgetAppearance] moved from
 * `dev.ranzlappen.gadget.feature.torch.widget.customization` to
 * `dev.ranzlappen.gadget.core.widgetkit.config` in refactor-2026 / C1.
 * Every previously-persisted user widget config must keep decoding —
 * that requires the polymorphic [ToggleFeedback] discriminator to stay
 * pinned to its legacy FQN regardless of the new code location.
 *
 * If these tests turn red, a `@SerialName` pin in
 * [WidgetAppearance.kt] has been removed or edited. **Do not "fix" the
 * test** — restore the pin instead, or pair the change with a
 * `Migrator<T>` that rewrites the discriminator on read (planned for
 * C4).
 */
class WidgetAppearanceSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    @Test
    fun toggleFeedbackNonePinsLegacyDiscriminator() {
        val encoded = json.encodeToString(ToggleFeedback.serializer(), ToggleFeedback.None)
        assertTrue(
            "encoded=$encoded must contain the legacy FQN discriminator",
            encoded.contains("\"dev.ranzlappen.gadget.feature.torch.widget.customization.ToggleFeedback.None\""),
        )
    }

    @Test
    fun toggleFeedbackToastPinsLegacyDiscriminator() {
        val encoded = json.encodeToString(
            ToggleFeedback.serializer(),
            ToggleFeedback.Toast(template = "{name} is {state}"),
        )
        assertTrue(
            "encoded=$encoded must contain the legacy FQN discriminator",
            encoded.contains("\"dev.ranzlappen.gadget.feature.torch.widget.customization.ToggleFeedback.Toast\""),
        )
    }

    @Test
    fun toggleFeedbackNotificationPinsLegacyDiscriminator() {
        val encoded = json.encodeToString(
            ToggleFeedback.serializer(),
            ToggleFeedback.Notification(titleTemplate = "T", bodyTemplate = "B"),
        )
        assertTrue(
            "encoded=$encoded must contain the legacy FQN discriminator",
            encoded.contains("\"dev.ranzlappen.gadget.feature.torch.widget.customization.ToggleFeedback.Notification\""),
        )
    }

    @Test
    fun legacyJsonWithOldFqnStillDecodes() {
        // Synthetic legacy on-disk JSON produced before the package move.
        // The discriminator is the kotlinx.serialization default
        // (`type` key, FQN string value). This MUST keep decoding after
        // any future kit-level refactor.
        val legacy = """{"type":"dev.ranzlappen.gadget.feature.torch.widget.customization.ToggleFeedback.Toast","template":"old"}"""
        val decoded = json.decodeFromString(ToggleFeedback.serializer(), legacy)
        assertEquals(ToggleFeedback.Toast("old"), decoded)
    }

    @Test
    fun roundTripWidgetAppearance() {
        val original = WidgetAppearance(
            background = BackgroundMode.Solid,
            solidColor = 0xFFAABBCCL,
            iconStyle = IconStyle(
                activeKey = "x",
                inactiveKey = "y",
                tint = IconTint.MonochromeWhite,
                customTintArgb = 0xFF112233L,
            ),
            tap = TapBehavior(animation = TapAnimation.Pulse, enabled = false),
            feedback = ToggleFeedback.Notification("t", "b"),
        )
        val encoded = json.encodeToString(WidgetAppearance.serializer(), original)
        val decoded = json.decodeFromString(WidgetAppearance.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun missingFieldsDecodeToDefaults() {
        // Older persisted records may predate fields. The defaults on
        // each field keep them migrable — an empty record decodes as
        // the default-constructed value.
        val decoded = json.decodeFromString(WidgetAppearance.serializer(), "{}")
        assertEquals(WidgetAppearance(), decoded)
    }
}
