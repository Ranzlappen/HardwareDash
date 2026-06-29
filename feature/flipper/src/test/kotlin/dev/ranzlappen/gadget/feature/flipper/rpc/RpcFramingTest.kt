package dev.ranzlappen.gadget.feature.flipper.rpc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RpcFramingTest {

    @Test
    fun `frame then read round-trips a single message`() {
        val msg = byteArrayOf(1, 2, 3, 4, 5)
        val framed = RpcFraming.frame(msg)
        val out = FrameReader().feed(framed)
        assertEquals(1, out.size)
        assertArrayEquals(msg, out[0])
    }

    @Test
    fun `a message split across two feeds is reassembled`() {
        val msg = ByteArray(200) { (it and 0xFF).toByte() }
        val framed = RpcFraming.frame(msg) // varint length prefix (200 -> 2 bytes)
        val reader = FrameReader()

        val firstHalf = framed.copyOfRange(0, 50)
        val secondHalf = framed.copyOfRange(50, framed.size)

        assertTrue("partial frame yields nothing", reader.feed(firstHalf).isEmpty())
        val out = reader.feed(secondHalf)
        assertEquals(1, out.size)
        assertArrayEquals(msg, out[0])
    }

    @Test
    fun `two concatenated frames in one feed return both messages`() {
        val a = byteArrayOf(10, 11)
        val b = byteArrayOf(20, 21, 22)
        val combined = RpcFraming.frame(a) + RpcFraming.frame(b)
        val out = FrameReader().feed(combined)
        assertEquals(2, out.size)
        assertArrayEquals(a, out[0])
        assertArrayEquals(b, out[1])
    }

    @Test
    fun `leftover bytes after a frame are buffered for the next feed`() {
        val a = byteArrayOf(7, 7, 7)
        val framedA = RpcFraming.frame(a)
        val reader = FrameReader()
        // Feed frame A plus the length prefix of frame B, but not B's body yet.
        val out1 = reader.feed(framedA + byteArrayOf(2))
        assertEquals(1, out1.size)
        assertArrayEquals(a, out1[0])
        // Now feed B's body.
        val out2 = reader.feed(byteArrayOf(8, 9))
        assertEquals(1, out2.size)
        assertArrayEquals(byteArrayOf(8, 9), out2[0])
    }
}
