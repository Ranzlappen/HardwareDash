package com.gadget.gps.spoof

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Minimal KML parser for the three location-bearing constructs that matter
 * for spoofing playback:
 *
 *  - `<Placemark><Point><coordinates>lon,lat[,alt]</coordinates></Point></Placemark>`
 *      — single-point placemarks (need >= 2 in the doc)
 *  - `<Placemark><LineString><coordinates>lon,lat[,alt] lon,lat[,alt] ...</coordinates></LineString></Placemark>`
 *      — untimestamped polylines (paced by defaultSpeedMps)
 *  - `<Placemark><gx:Track><when>...</when><gx:coord>lon lat alt</gx:coord></gx:Track></Placemark>`
 *      — timestamped tracks (Google's GPS export shape)
 *
 * Pure Kotlin; XmlPullParser-based; JVM-testable.
 */
internal object KmlParser {

    const val MAX_BYTES: Long = 5L * 1024L * 1024L

    @Throws(KmlParseException::class)
    fun parse(input: InputStream): List<Waypoint> {
        val parser = try {
            XmlPullParserFactory.newInstance().newPullParser()
        } catch (t: XmlPullParserException) {
            throw KmlParseException("Failed to acquire XML parser", t)
        }
        parser.setInput(input, null)

        val out = mutableListOf<Waypoint>()
        var inCoordinates = false
        var inGxCoord = false
        var inWhen = false
        val textBuffer = StringBuilder()
        var inLineString = false
        var inPoint = false
        var inGxTrack = false

        // Buffer of pending <when> values, drained 1:1 against gx:coord in order.
        val pendingTimes: MutableList<Long?> = mutableListOf()

        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "LineString" -> inLineString = true
                            "Point" -> inPoint = true
                            "Track", "gx:Track" -> inGxTrack = true
                            "coordinates" -> {
                                inCoordinates = true
                                textBuffer.setLength(0)
                            }
                            "coord", "gx:coord" -> {
                                inGxCoord = true
                                textBuffer.setLength(0)
                            }
                            "when" -> {
                                inWhen = true
                                textBuffer.setLength(0)
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inCoordinates || inGxCoord || inWhen) textBuffer.append(parser.text)
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "coordinates" -> {
                                if (inCoordinates) {
                                    val raw = textBuffer.toString()
                                    if (inLineString) {
                                        parseLineCoordinates(raw, out)
                                    } else if (inPoint) {
                                        parsePointCoordinates(raw, out)
                                    }
                                    inCoordinates = false
                                }
                            }
                            "coord", "gx:coord" -> {
                                if (inGxCoord && inGxTrack) {
                                    val whenMs = if (pendingTimes.isNotEmpty()) pendingTimes.removeAt(0) else null
                                    parseGxCoord(textBuffer.toString(), whenMs, out)
                                    inGxCoord = false
                                }
                            }
                            "when" -> {
                                if (inWhen) {
                                    pendingTimes.add(parseIso8601(textBuffer.toString().trim()))
                                    inWhen = false
                                }
                            }
                            "LineString" -> inLineString = false
                            "Point" -> inPoint = false
                            "Track", "gx:Track" -> {
                                inGxTrack = false
                                pendingTimes.clear()
                            }
                        }
                    }
                }
                event = parser.next()
            }
        } catch (t: XmlPullParserException) {
            throw KmlParseException("Malformed KML: ${t.message}", t)
        }

        if (out.size < 2) {
            throw KmlParseException("KML contained ${out.size} points; need >= 2")
        }
        return out
    }

    private fun parsePointCoordinates(raw: String, out: MutableList<Waypoint>) {
        val parts = raw.trim().split(',', ' ', '\n', '\t', '\r').filter { it.isNotEmpty() }
        if (parts.size < 2) return
        val lon = parts[0].toDoubleOrNull() ?: return
        val lat = parts[1].toDoubleOrNull() ?: return
        val alt = if (parts.size >= 3) parts[2].toDoubleOrNull() else null
        out += Waypoint(lat = lat, lon = lon, alt = alt)
    }

    private fun parseLineCoordinates(raw: String, out: MutableList<Waypoint>) {
        // KML LineString: whitespace-separated coordinate triples
        // "lon,lat,alt lon,lat,alt ..."
        raw.split(' ', '\n', '\t', '\r')
            .filter { it.isNotBlank() }
            .forEach { tuple ->
                val parts = tuple.split(',')
                if (parts.size >= 2) {
                    val lon = parts[0].toDoubleOrNull()
                    val lat = parts[1].toDoubleOrNull()
                    val alt = if (parts.size >= 3) parts[2].toDoubleOrNull() else null
                    if (lat != null && lon != null) {
                        out += Waypoint(lat = lat, lon = lon, alt = alt)
                    }
                }
            }
    }

    private fun parseGxCoord(raw: String, timeMs: Long?, out: MutableList<Waypoint>) {
        // gx:coord uses space separation: "lon lat alt"
        val parts = raw.trim().split(' ', '\t').filter { it.isNotEmpty() }
        if (parts.size < 2) return
        val lon = parts[0].toDoubleOrNull() ?: return
        val lat = parts[1].toDoubleOrNull() ?: return
        val alt = if (parts.size >= 3) parts[2].toDoubleOrNull() else null
        out += Waypoint(lat = lat, lon = lon, alt = alt, timestampMs = timeMs)
    }

    private fun parseIso8601(raw: String): Long? {
        if (raw.isEmpty()) return null
        val cleaned = raw
            .replace(Regex("\\.\\d+"), "")
            .let { if (it.endsWith("Z")) it else "${it}Z" }
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            fmt.parse(cleaned)?.time
        } catch (_: Exception) {
            null
        }
    }
}

class KmlParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
