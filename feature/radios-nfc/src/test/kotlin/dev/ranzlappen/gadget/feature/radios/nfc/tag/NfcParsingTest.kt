package dev.ranzlappen.gadget.feature.radios.nfc.tag

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class NfcParsingTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    // ---- NDEF records ----

    @Test
    fun `text record decodes language and UTF-8 text`() {
        val payload = byteArrayOf(0x02) + "en".toByteArray() + "Hallo Welt".toByteArray()
        val record = NfcParsing.parseRecord(NfcParsing.TNF_WELL_KNOWN, "T".toByteArray(), payload)
        assertEquals(NdefRecordView(NdefRecordKind.Text, "Hallo Welt", "en"), record)
    }

    @Test
    fun `text record honours the UTF-16 status bit`() {
        val payload = byteArrayOf(0x82.toByte()) + "de".toByteArray() + "Grüße".toByteArray(Charsets.UTF_16)
        val record = NfcParsing.parseRecord(NfcParsing.TNF_WELL_KNOWN, "T".toByteArray(), payload)
        assertEquals("Grüße", record.value)
    }

    @Test
    fun `uri record expands the identifier-code prefix`() {
        val payload = byteArrayOf(0x04) + "example.com/a".toByteArray()
        val record = NfcParsing.parseRecord(NfcParsing.TNF_WELL_KNOWN, "U".toByteArray(), payload)
        assertEquals(NdefRecordView(NdefRecordKind.Uri, "https://example.com/a"), record)
        assertEquals("tel:+49123", NfcParsing.parseUri(byteArrayOf(0x05) + "+49123".toByteArray()))
        assertEquals("urn:nfc:x", NfcParsing.parseUri(byteArrayOf(0x23) + "x".toByteArray()))
    }

    @Test
    fun `mime record shows printable payloads as text and binary ones as hex`() {
        val text = NfcParsing.parseRecord(NfcParsing.TNF_MIME_MEDIA, "text/vcard".toByteArray(), "BEGIN:VCARD".toByteArray())
        assertEquals(NdefRecordView(NdefRecordKind.Mime, "BEGIN:VCARD", "text/vcard"), text)
        val binary = NfcParsing.parseRecord(NfcParsing.TNF_MIME_MEDIA, "image/png".toByteArray(), bytes(0x89, 0x50, 0x00, 0x01))
        assertEquals("89 50 00 01", binary.value)
    }

    @Test
    fun `absolute uri, external, empty and unknown records`() {
        assertEquals(
            NdefRecordView(NdefRecordKind.Uri, "https://x.y"),
            NfcParsing.parseRecord(NfcParsing.TNF_ABSOLUTE_URI, "https://x.y".toByteArray(), ByteArray(0)),
        )
        assertEquals(
            NdefRecordView(NdefRecordKind.External, "com.example.app", "android.com:pkg"),
            NfcParsing.parseRecord(NfcParsing.TNF_EXTERNAL_TYPE, "android.com:pkg".toByteArray(), "com.example.app".toByteArray()),
        )
        assertEquals(NdefRecordKind.Empty, NfcParsing.parseRecord(NfcParsing.TNF_EMPTY, ByteArray(0), ByteArray(0)).kind)
        assertEquals("01 02", NfcParsing.parseRecord(6, ByteArray(0), bytes(1, 2)).value)
    }

    // ---- chip identification ----

    private fun version(product: Int, storage: Int) = bytes(0x00, 0x04, product, 0x02, 0x01, 0x00, storage, 0x03)

    @Test
    fun `GET_VERSION identifies NTAG21x and Ultralight EV1`() {
        assertEquals("NTAG213", NfcParsing.identifyNtag(version(0x04, 0x0F))?.name)
        assertEquals(135, NfcParsing.identifyNtag(version(0x04, 0x11))?.totalPages)
        assertEquals(0xE3, NfcParsing.identifyNtag(version(0x04, 0x13))?.configPage)
        assertEquals("NTAG212", NfcParsing.identifyNtag(version(0x04, 0x0E))?.name)
        assertEquals("MIFARE Ultralight EV1 (MF0UL21)", NfcParsing.identifyNtag(version(0x03, 0x0E))?.name)
        assertNull(NfcParsing.identifyNtag(version(0x04, 0x7F)))
        assertNull(NfcParsing.identifyNtag(bytes(0x00, 0x05, 0x04, 0, 0, 0, 0x0F, 0))) // not NXP
        assertNull(NfcParsing.identifyNtag(bytes(0x00)))
    }

    // ---- protection ----

    private val ntag215 = NfcParsing.identifyNtag(version(0x04, 0x11))

    /** READ(page 2) result: page 2 = UID tail + lock bytes, page 3 = CC, then pages 4–5. */
    private fun pages(lock0: Int, lock1: Int, ccAccess: Int) =
        bytes(0x00, 0x00, lock0, lock1, 0xE1, 0x10, 0x3E, ccAccess) + ByteArray(8)

    @Test
    fun `factory-fresh tag reports no protection`() {
        val p = NfcParsing.interpretProtection(
            pages(0, 0, 0x00), ntag215, bytes(0, 0, 0, 0xBD), bytes(0x04, 0, 0, 0xFF, 0x00),
        )
        assertFalse(p.anyProtection)
        assertEquals(false, p.passwordActive)
        assertTrue(p.details.any { it.startsWith("AUTH0 0xFF") })
    }

    @Test
    fun `lock bits, read-only CC and AUTH0 are each detected`() {
        assertEquals(true, NfcParsing.interpretProtection(pages(0x00, 0xF0, 0x00), ntag215, null, null).lockBitsSet)
        assertEquals(true, NfcParsing.interpretProtection(pages(0, 0, 0x00), ntag215, bytes(0x01, 0, 0), null).lockBitsSet)
        assertEquals(true, NfcParsing.interpretProtection(pages(0, 0, 0x0F), ntag215, null, null).readOnlyFlag)
        val pwd = NfcParsing.interpretProtection(pages(0, 0, 0), ntag215, null, bytes(0x04, 0, 0, 0x04, 0x80))
        assertEquals(true, pwd.passwordActive)
        assertTrue(pwd.anyProtection)
    }

    @Test
    fun `unreadable registers stay unknown`() {
        val p = NfcParsing.interpretProtection(null, null, null, null)
        assertNull(p.lockBitsSet)
        assertNull(p.readOnlyFlag)
        assertNull(p.passwordActive)
        assertFalse(p.anyProtection)
    }

    // ---- helpers ----

    @Test
    fun `ndef type labels, url normalisation and hex`() {
        assertEquals("Type 2", NfcParsing.ndefTypeLabel("org.nfcforum.ndef.type2"))
        assertEquals("MIFARE Classic", NfcParsing.ndefTypeLabel("com.nxp.ndef.mifareclassic"))
        assertNull(NfcParsing.ndefTypeLabel(null))
        assertEquals("https://example.com", NfcParsing.normalizeUrl(" example.com "))
        assertEquals("tel:123", NfcParsing.normalizeUrl("tel:123"))
        assertEquals("http://a.b", NfcParsing.normalizeUrl("http://a.b"))
        assertEquals("04:A1:FF", NfcParsing.hex(bytes(0x04, 0xA1, 0xFF), ":"))
    }
}
