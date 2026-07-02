package dev.ranzlappen.gadget.feature.gps.spoof

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class KmlParserTest {

    @Test
    fun `parses gx Track with timestamps`() {
        val pts = parseResource("spoof/track.kml")
        assertEquals(3, pts.size)
        assertEquals(37.7749, pts[0].lat, 1e-9)
        assertEquals(-122.4194, pts[0].lon, 1e-9)
        assertEquals(10.0, pts[0].alt!!, 1e-9)
        assertNotNull(pts[0].timestampMs)
        assert(pts[0].timestampMs!! < pts[1].timestampMs!!)
    }

    @Test
    fun `parses untimed LineString`() {
        val pts = parseResource("spoof/linestring.kml")
        assertEquals(3, pts.size)
        assertEquals(40.7128, pts[0].lat, 1e-9)
        assertEquals(-74.0060, pts[0].lon, 1e-9)
        assertNull(pts[0].timestampMs)
    }

    @Test(expected = KmlParseException::class)
    fun `rejects single-point document`() {
        val xml = """
            <kml xmlns="http://www.opengis.net/kml/2.2">
              <Placemark><Point><coordinates>0,0</coordinates></Point></Placemark>
            </kml>
        """.trimIndent()
        KmlParser.parse(xml.byteInputStream())
    }

    private fun parseResource(path: String): List<Waypoint> {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream(path))
        return stream.use { KmlParser.parse(it) }
    }
}
