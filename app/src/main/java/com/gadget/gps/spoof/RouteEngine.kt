package com.gadget.gps.spoof

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure-Kotlin time-driven interpolator. Given a list of waypoints and a
 * monotonic clock, emits an interpolated [Waypoint] for the current playback
 * time. No Android imports — JVM unit-testable.
 *
 * Two interpolation modes (per [SpoofConfig.Route.Interpolation]):
 *  - **Linear**  — straight-line lerp between adjacent waypoints (great
 *                  circle distance for the haversine-based bearing/speed
 *                  calculation).
 *  - **Cubic**   — Catmull-Rom spline through the waypoint sequence; gives a
 *                  smoother path that doesn't snap heading at every point.
 *
 * Pacing:
 *  - If a waypoint has [Waypoint.timestampMs] set, the engine plays the
 *    sequence at the recorded timing scaled by [speedMultiplier].
 *  - Otherwise it derives per-leg duration from [defaultSpeedMps] and the
 *    great-circle distance between adjacent points.
 */
class RouteEngine(
    private val waypoints: List<Waypoint>,
    private val interpolation: SpoofConfig.Route.Interpolation = SpoofConfig.Route.Interpolation.Linear,
    private val defaultSpeedMps: Float = 5f,
    private val speedMultiplier: Float = 1f,
    private val loop: Boolean = false,
) {

    init {
        require(waypoints.size >= 2) { "RouteEngine requires >= 2 waypoints" }
        require(speedMultiplier > 0f) { "speedMultiplier must be > 0" }
        require(defaultSpeedMps > 0f) { "defaultSpeedMps must be > 0" }
    }

    /**
     * Cumulative wall-clock offset (ms from playback start) at which each
     * waypoint should be reached. Index aligns with [waypoints].
     */
    private val cumulativeMs: LongArray = buildCumulativeOffsets()

    /** Total playback duration of one pass through the waypoint list. */
    val totalDurationMs: Long = cumulativeMs.last()

    /**
     * Sample the route at [elapsedMs] from playback start. When [loop] is
     * false and elapsed exceeds the route duration, returns the last
     * waypoint (caller should stop the emitter).
     */
    fun sample(elapsedMs: Long): Sample {
        val finite = elapsedMs.coerceAtLeast(0)
        val effective = if (loop && totalDurationMs > 0) {
            finite % totalDurationMs
        } else {
            finite.coerceAtMost(totalDurationMs)
        }

        if (effective >= totalDurationMs) {
            val tail = waypoints.last()
            val prev = waypoints[waypoints.size - 2]
            return Sample(
                waypoint = tail,
                bearingDegrees = haversineBearing(prev.lat, prev.lon, tail.lat, tail.lon).toFloat(),
                speedMps = legSpeedMps(waypoints.size - 2),
                isFinal = !loop,
            )
        }

        val legIndex = lowerBoundLeg(effective)
        val legStart = cumulativeMs[legIndex]
        val legEnd = cumulativeMs[legIndex + 1]
        val legDuration = (legEnd - legStart).coerceAtLeast(1)
        val rawT = ((effective - legStart).toDouble() / legDuration.toDouble())
            .coerceIn(0.0, 1.0)

        val a = waypoints[legIndex]
        val b = waypoints[legIndex + 1]

        val (lat, lon, alt) = when (interpolation) {
            SpoofConfig.Route.Interpolation.Linear -> linearInterp(a, b, rawT)
            SpoofConfig.Route.Interpolation.Cubic -> cubicInterp(legIndex, rawT)
        }

        val bearing = haversineBearing(a.lat, a.lon, b.lat, b.lon).toFloat()

        return Sample(
            waypoint = Waypoint(lat = lat, lon = lon, alt = alt),
            bearingDegrees = bearing,
            speedMps = legSpeedMps(legIndex),
            isFinal = false,
        )
    }

    private fun lowerBoundLeg(elapsed: Long): Int {
        // Find largest i such that cumulativeMs[i] <= elapsed and i < last index.
        var lo = 0
        var hi = cumulativeMs.size - 2
        while (lo < hi) {
            val mid = (lo + hi + 1) ushr 1
            if (cumulativeMs[mid] <= elapsed) lo = mid else hi = mid - 1
        }
        return lo
    }

    private fun linearInterp(a: Waypoint, b: Waypoint, t: Double): Triple<Double, Double, Double?> {
        val lat = a.lat + (b.lat - a.lat) * t
        val lon = a.lon + (b.lon - a.lon) * t
        val alt = if (a.alt != null && b.alt != null) a.alt + (b.alt - a.alt) * t
        else a.alt ?: b.alt
        return Triple(lat, lon, alt)
    }

    private fun cubicInterp(legIndex: Int, t: Double): Triple<Double, Double, Double?> {
        // Catmull-Rom needs P0..P3; clamp at boundaries.
        val p0 = waypoints[(legIndex - 1).coerceAtLeast(0)]
        val p1 = waypoints[legIndex]
        val p2 = waypoints[legIndex + 1]
        val p3 = waypoints[(legIndex + 2).coerceAtMost(waypoints.size - 1)]

        val lat = catmullRom(p0.lat, p1.lat, p2.lat, p3.lat, t)
        val lon = catmullRom(p0.lon, p1.lon, p2.lon, p3.lon, t)
        val alt = if (p1.alt != null && p2.alt != null) {
            val a0 = p0.alt ?: p1.alt
            val a3 = p3.alt ?: p2.alt
            catmullRom(a0, p1.alt, p2.alt, a3, t)
        } else null
        return Triple(lat, lon, alt)
    }

    private fun catmullRom(p0: Double, p1: Double, p2: Double, p3: Double, t: Double): Double {
        val t2 = t * t
        val t3 = t2 * t
        return 0.5 * (
            (2.0 * p1) +
                (-p0 + p2) * t +
                (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 +
                (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3
            )
    }

    private fun legSpeedMps(legIndex: Int): Float {
        val a = waypoints[legIndex]
        val b = waypoints[legIndex + 1]
        a.speedMps?.let { return it }
        if (a.timestampMs != null && b.timestampMs != null && b.timestampMs > a.timestampMs) {
            val dist = haversineMeters(a.lat, a.lon, b.lat, b.lon)
            val secs = (b.timestampMs - a.timestampMs) / 1000.0
            return ((dist / secs) * speedMultiplier).toFloat()
        }
        return defaultSpeedMps * speedMultiplier
    }

    private fun buildCumulativeOffsets(): LongArray {
        val out = LongArray(waypoints.size)
        out[0] = 0L
        var acc = 0L
        for (i in 1 until waypoints.size) {
            val a = waypoints[i - 1]
            val b = waypoints[i]
            val legMs = legDurationMs(a, b)
            acc += legMs
            out[i] = acc
        }
        return out
    }

    private fun legDurationMs(a: Waypoint, b: Waypoint): Long {
        if (a.timestampMs != null && b.timestampMs != null && b.timestampMs > a.timestampMs) {
            val raw = (b.timestampMs - a.timestampMs)
            return ((raw.toDouble() / speedMultiplier).toLong()).coerceAtLeast(1L)
        }
        val dist = haversineMeters(a.lat, a.lon, b.lat, b.lon)
        val secs = dist / (defaultSpeedMps * speedMultiplier)
        return ((secs * 1000.0).toLong()).coerceAtLeast(1L)
    }

    data class Sample(
        val waypoint: Waypoint,
        val bearingDegrees: Float,
        val speedMps: Float,
        val isFinal: Boolean,
    )

    companion object {
        private const val EARTH_RADIUS_M = 6_371_000.0

        internal fun haversineMeters(
            lat1: Double, lon1: Double, lat2: Double, lon2: Double,
        ): Double {
            val phi1 = lat1 * PI / 180.0
            val phi2 = lat2 * PI / 180.0
            val dPhi = (lat2 - lat1) * PI / 180.0
            val dLambda = (lon2 - lon1) * PI / 180.0
            val a = sin(dPhi / 2.0).let { it * it } +
                cos(phi1) * cos(phi2) * sin(dLambda / 2.0).let { it * it }
            val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
            return EARTH_RADIUS_M * c
        }

        internal fun haversineBearing(
            lat1: Double, lon1: Double, lat2: Double, lon2: Double,
        ): Double {
            val phi1 = lat1 * PI / 180.0
            val phi2 = lat2 * PI / 180.0
            val dLambda = (lon2 - lon1) * PI / 180.0
            val y = sin(dLambda) * cos(phi2)
            val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
            val brg = atan2(y, x) * 180.0 / PI
            return (brg + 360.0) % 360.0
        }
    }
}
