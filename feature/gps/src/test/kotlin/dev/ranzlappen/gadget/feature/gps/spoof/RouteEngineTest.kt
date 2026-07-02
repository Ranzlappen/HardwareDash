package dev.ranzlappen.gadget.feature.gps.spoof

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteEngineTest {

    private val twoPoints = listOf(
        Waypoint(lat = 0.0, lon = 0.0, timestampMs = 0L),
        Waypoint(lat = 0.0, lon = 1.0, timestampMs = 60_000L),
    )

    @Test
    fun `linear interpolation midpoint`() {
        val e = RouteEngine(twoPoints, SpoofConfig.Route.Interpolation.Linear)
        val sample = e.sample(30_000L)
        assertEquals(0.0, sample.waypoint.lat, 1e-9)
        assertEquals(0.5, sample.waypoint.lon, 1e-9)
        assertEquals(false, sample.isFinal)
    }

    @Test
    fun `clamps to last waypoint past end without loop`() {
        val e = RouteEngine(twoPoints)
        val sample = e.sample(99_999_999L)
        assertEquals(0.0, sample.waypoint.lat, 1e-9)
        assertEquals(1.0, sample.waypoint.lon, 1e-9)
        assertTrue(sample.isFinal)
    }

    @Test
    fun `loop wraps elapsed time`() {
        val e = RouteEngine(twoPoints, loop = true)
        val a = e.sample(15_000L)
        val b = e.sample(15_000L + e.totalDurationMs)
        assertEquals(a.waypoint.lat, b.waypoint.lat, 1e-9)
        assertEquals(a.waypoint.lon, b.waypoint.lon, 1e-9)
    }

    @Test
    fun `untimed waypoints paced by defaultSpeedMps`() {
        // Two points 1 deg latitude apart (~111 km), default speed 1000 m/s ⇒ ~111 s.
        val pts = listOf(Waypoint(lat = 0.0, lon = 0.0), Waypoint(lat = 1.0, lon = 0.0))
        val e = RouteEngine(pts, defaultSpeedMps = 1000f)
        val expectedMs = 111_320  // approximate, generous tolerance
        assertTrue(
            "expected ~${expectedMs}ms, got ${e.totalDurationMs}",
            kotlin.math.abs(e.totalDurationMs - expectedMs) < 5_000L,
        )
    }

    @Test
    fun `cubic interpolation differs from linear at midpoint`() {
        val pts = listOf(
            Waypoint(lat = 0.0, lon = 0.0, timestampMs = 0L),
            Waypoint(lat = 1.0, lon = 1.0, timestampMs = 30_000L),
            Waypoint(lat = 2.0, lon = 0.0, timestampMs = 60_000L),
        )
        val linear = RouteEngine(pts, SpoofConfig.Route.Interpolation.Linear).sample(15_000L)
        val cubic = RouteEngine(pts, SpoofConfig.Route.Interpolation.Cubic).sample(15_000L)
        // Cubic spline should curve away from the straight line through the
        // segment endpoints. Allow tiny floating-point equivalence to count
        // as "different" since both paths share the segment endpoints.
        assertNotEquals(linear.waypoint.lat, cubic.waypoint.lat, 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `single waypoint rejected`() {
        RouteEngine(listOf(Waypoint(lat = 0.0, lon = 0.0)))
    }
}
