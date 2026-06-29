package dev.ranzlappen.gadget.feature.flipper.rpc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PbMainTest {

    @Test
    fun `encode then decode round-trips id, content field and body`() {
        val body = byteArrayOf(0x0A, 0x03, 0x61, 0x62, 0x63)
        val encoded = PbMain.encode(commandId = 42, contentField = PbMain.C_PING_REQUEST, contentBody = body)
        val decoded = PbMain.decode(encoded)

        assertEquals(42, decoded.commandId)
        assertEquals(PbMain.C_PING_REQUEST, decoded.contentField)
        assertArrayEquals(body, decoded.contentBody)
        assertFalse(decoded.hasNext)
    }

    @Test
    fun `hasNext flag survives the round-trip`() {
        val encoded = PbMain.encode(7, PbMain.C_STORAGE_WRITE_REQUEST, byteArrayOf(1), hasNext = true)
        assertTrue(PbMain.decode(encoded).hasNext)
    }

    @Test
    fun `encodeEmpty produces a body-less content field`() {
        val decoded = PbMain.decode(PbMain.encodeEmpty(3, PbMain.C_DEVICE_INFO_REQUEST))
        assertEquals(3, decoded.commandId)
        assertEquals(PbMain.C_DEVICE_INFO_REQUEST, decoded.contentField)
        assertEquals(0, decoded.contentBody.size)
    }

    @Test
    fun `a key-value pair body decodes via PbReader`() {
        // { string key = 1 = "charge_level"; string value = 2 = "87" }
        val body = pbMessage {
            writeString(1, "charge_level")
            writeString(2, "87")
        }
        val r = PbReader(body)
        val pairs = mutableMapOf<String, String>()
        var key: String? = null
        var value: String? = null
        while (r.hasMore()) {
            val tag = r.readTag()
            when (r.fieldOf(tag)) {
                1 -> key = r.readString()
                2 -> value = r.readString()
                else -> r.skip(r.wireOf(tag))
            }
        }
        if (key != null) pairs[key] = value.orEmpty()
        assertEquals("87", pairs["charge_level"])
    }
}
