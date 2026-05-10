package com.gadget.gps.spoof

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GpxParserTest {

    @Test
    fun `parses timestamped trkpt`() {
        val pts = parseResource("spoof/track.gpx")
        assertEquals(3, pts.size)
        assertEquals(37.7749, pts[0].lat, 1e-9)
        assertEquals(-122.4194, pts[0].lon, 1e-9)
        assertEquals(10.0, pts[0].alt!!, 1e-9)
        assertNotNull(pts[0].timestampMs)
        // Timestamps strictly increasing.
        assert(pts[0].timestampMs!! < pts[1].timestampMs!!)
    }

    @Test
    fun `parses untimed rtept`() {
        val pts = parseResource("spoof/route.gpx")
        assertEquals(3, pts.size)
        assertEquals(40.7128, pts[0].lat, 1e-9)
        assertNull(pts[0].timestampMs)
    }

    @Test(expected = GpxParseException::class)
    fun `rejects empty document`() {
        GpxParser.parse("<gpx version=\"1.1\"></gpx>".byteInputStream())
    }

    private fun parseResource(path: String): List<Waypoint> {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream(path))
        return stream.use { GpxParser.parse(it) }
    }
}
