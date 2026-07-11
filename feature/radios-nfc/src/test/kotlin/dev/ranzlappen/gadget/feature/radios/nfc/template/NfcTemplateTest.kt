package dev.ranzlappen.gadget.feature.radios.nfc.template

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [NfcTemplate]'s computed properties: [NfcTemplate.placeholders]
 * (regex-extracted `{{name}}` tokens, deduped, in first-seen order) and
 * [NfcTemplate.resolve] (naive string substitution of every supplied value).
 * Pure Kotlin — no Android surface involved.
 */
class NfcTemplateTest {

    private fun template(template: String) = NfcTemplate(
        id = "1",
        name = "Test",
        category = "custom",
        mode = "TEXT",
        template = template,
    )

    // ---- placeholders ----

    @Test
    fun `placeholders is empty when the template has no tokens`() {
        assertEquals(emptyList(), template("plain text, no tokens").placeholders)
    }

    @Test
    fun `placeholders extracts every distinct token name`() {
        assertEquals(
            listOf("wifi_ssid", "wifi_password"),
            template("WIFI:S:{{wifi_ssid}};P:{{wifi_password}};;").placeholders,
        )
    }

    @Test
    fun `placeholders dedupes repeated tokens keeping first-seen order`() {
        assertEquals(
            listOf("name", "phone"),
            template("Contact: {{name}} ({{phone}}) - ask for {{name}}").placeholders,
        )
    }

    @Test
    fun `placeholders ignores malformed braces`() {
        assertEquals(emptyList(), template("{not a token} {{ }} {{}}").placeholders)
    }

    // ---- resolve ----

    @Test
    fun `resolve substitutes every supplied value`() {
        val result = template("WIFI:S:{{ssid}};P:{{password}};;")
            .resolve(mapOf("ssid" to "HomeNet", "password" to "hunter2"))

        assertEquals("WIFI:S:HomeNet;P:hunter2;;", result)
    }

    @Test
    fun `resolve leaves a token untouched when its value is missing`() {
        val result = template("Hello {{name}}, you are {{age}}").resolve(mapOf("name" to "Ann"))

        assertEquals("Hello Ann, you are {{age}}", result)
    }

    @Test
    fun `resolve returns the template unchanged when values are empty`() {
        val raw = "Hello {{name}}"
        assertEquals(raw, template(raw).resolve(emptyMap()))
    }

    @Test
    fun `resolve substitutes repeated occurrences of the same token`() {
        val result = template("{{name}} loves {{name}}").resolve(mapOf("name" to "Cat"))

        assertEquals("Cat loves Cat", result)
    }

    @Test
    fun `resolve ignores values with no matching token`() {
        val result = template("Hello {{name}}").resolve(mapOf("name" to "Ann", "unused" to "value"))

        assertEquals("Hello Ann", result)
    }
}
