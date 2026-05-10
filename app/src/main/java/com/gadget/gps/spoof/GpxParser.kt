package com.gadget.gps.spoof

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Minimal GPX parser. Handles the three GPX top-level elements that produce
 * routable point sequences:
 *
 *  - `<trk>` with `<trkseg>` and `<trkpt>` (timestamped tracks)
 *  - `<rte>` with `<rtept>` (untimestamped routes — paced by defaultSpeedMps)
 *  - `<wpt>` (single waypoints — useful only when at least 2 are present)
 *
 * Pure Kotlin; uses only the JVM's `XmlPullParserFactory` (which Android also
 * provides). JVM-testable.
 */
internal object GpxParser {

    /** Reject files larger than this to keep parsing predictable. */
    const val MAX_BYTES: Long = 5L * 1024L * 1024L

    /**
     * @throws GpxParseException on malformed XML, missing required attrs, or
     *   < 2 points across all containers.
     */
    @Throws(GpxParseException::class)
    fun parse(input: InputStream): List<Waypoint> {
        val parser = try {
            XmlPullParserFactory.newInstance().newPullParser()
        } catch (t: XmlPullParserException) {
            throw GpxParseException("Failed to acquire XML parser", t)
        }

        parser.setInput(input, null)

        val out = mutableListOf<Waypoint>()
        var inTrkpt = false
        var inRtept = false
        var inWpt = false
        var pendingLat: Double? = null
        var pendingLon: Double? = null
        var pendingEle: Double? = null
        var pendingTimeMs: Long? = null
        var inEle = false
        var inTime = false
        val textBuffer = StringBuilder()

        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "trkpt" -> {
                                inTrkpt = true
                                pendingLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                                pendingLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                                pendingEle = null
                                pendingTimeMs = null
                            }
                            "rtept" -> {
                                inRtept = true
                                pendingLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                                pendingLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                                pendingEle = null
                                pendingTimeMs = null
                            }
                            "wpt" -> {
                                inWpt = true
                                pendingLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                                pendingLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                                pendingEle = null
                                pendingTimeMs = null
                            }
                            "ele" -> if (inTrkpt || inRtept || inWpt) {
                                inEle = true
                                textBuffer.setLength(0)
                            }
                            "time" -> if (inTrkpt || inRtept || inWpt) {
                                inTime = true
                                textBuffer.setLength(0)
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inEle || inTime) textBuffer.append(parser.text)
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "ele" -> {
                                if (inEle) {
                                    pendingEle = textBuffer.toString().trim().toDoubleOrNull()
                                    inEle = false
                                }
                            }
                            "time" -> {
                                if (inTime) {
                                    pendingTimeMs = parseIso8601(textBuffer.toString().trim())
                                    inTime = false
                                }
                            }
                            "trkpt" -> {
                                emit(out, pendingLat, pendingLon, pendingEle, pendingTimeMs)
                                inTrkpt = false
                                pendingLat = null; pendingLon = null
                                pendingEle = null; pendingTimeMs = null
                            }
                            "rtept" -> {
                                emit(out, pendingLat, pendingLon, pendingEle, pendingTimeMs)
                                inRtept = false
                                pendingLat = null; pendingLon = null
                                pendingEle = null; pendingTimeMs = null
                            }
                            "wpt" -> {
                                emit(out, pendingLat, pendingLon, pendingEle, pendingTimeMs)
                                inWpt = false
                                pendingLat = null; pendingLon = null
                                pendingEle = null; pendingTimeMs = null
                            }
                        }
                    }
                }
                event = parser.next()
            }
        } catch (t: XmlPullParserException) {
            throw GpxParseException("Malformed GPX: ${t.message}", t)
        }

        if (out.size < 2) {
            throw GpxParseException("GPX contained ${out.size} points; need >= 2")
        }
        return out
    }

    private fun emit(
        out: MutableList<Waypoint>,
        lat: Double?,
        lon: Double?,
        ele: Double?,
        timeMs: Long?,
    ) {
        if (lat == null || lon == null) return
        out += Waypoint(lat = lat, lon = lon, alt = ele, timestampMs = timeMs)
    }

    /** GPX uses ISO 8601 timestamps; returns epoch millis or null. */
    private fun parseIso8601(raw: String): Long? {
        if (raw.isEmpty()) return null
        // Strip fractional seconds + any timezone designator beyond Z that
        // SimpleDateFormat can't handle uniformly across all input flavors.
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

class GpxParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
