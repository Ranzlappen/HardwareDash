package dev.ranzlappen.gadget.feature.camera

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [BarcodeResult]'s computed properties — `isUrl` / `isWifi`
 * are trivial `displayType` checks, but `parsedWifi` hand-rolls a `WIFI:...;`
 * payload parser (no library involved) that is genuinely worth pinning:
 * key/value splitting on `;`/`:`, the SSID-required contract, and the
 * password/type defaults for an open network.
 */
class BarcodeResultTest {

    private fun result(displayType: String, rawValue: String) = BarcodeResult(
        id = "1",
        rawValue = rawValue,
        format = "QR_CODE",
        displayType = displayType,
        timestamp = 1_000L,
    )

    @Test
    fun `isUrl is true only for a URL displayType`() {
        assertTrue(result("URL", "https://example.com").isUrl())
        assertFalse(result("Text", "https://example.com").isUrl())
    }

    @Test
    fun `isWifi is true only for a WiFi displayType`() {
        assertTrue(result("WiFi", "WIFI:T:WPA;S:Net;P:pw;;").isWifi())
        assertFalse(result("Text", "WIFI:T:WPA;S:Net;P:pw;;").isWifi())
    }

    @Test
    fun `parsedWifi is null when displayType is not WiFi even if the payload looks like one`() {
        val subject = result("Text", "WIFI:T:WPA;S:Net;P:pw;;")

        assertNull(subject.parsedWifi())
    }

    @Test
    fun `parsedWifi extracts ssid, password and type from a WPA payload`() {
        val subject = result("WiFi", "WIFI:T:WPA;S:MyNetwork;P:password;;")

        assertEquals(
            BarcodeResult.ParsedWifi(ssid = "MyNetwork", password = "password", type = "WPA"),
            subject.parsedWifi(),
        )
    }

    @Test
    fun `parsedWifi defaults password to empty and type to nopass for an open network`() {
        val subject = result("WiFi", "WIFI:T:nopass;S:OpenNet;;")

        assertEquals(
            BarcodeResult.ParsedWifi(ssid = "OpenNet", password = "", type = "nopass"),
            subject.parsedWifi(),
        )
    }

    @Test
    fun `parsedWifi defaults type to nopass when the T field is absent`() {
        val subject = result("WiFi", "WIFI:S:OpenNet;;")

        assertEquals(
            BarcodeResult.ParsedWifi(ssid = "OpenNet", password = "", type = "nopass"),
            subject.parsedWifi(),
        )
    }

    @Test
    fun `parsedWifi is null when the ssid field is missing`() {
        val subject = result("WiFi", "WIFI:T:WPA;P:secret;;")

        assertNull(subject.parsedWifi())
    }
}
