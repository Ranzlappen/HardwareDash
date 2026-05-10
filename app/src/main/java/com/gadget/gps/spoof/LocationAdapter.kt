package com.gadget.gps.spoof

import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock

/**
 * Builds [Location] objects for the test provider. The single most important
 * job: set `time` and `elapsedRealtimeNanos` to "now" on every emission.
 * FusedLocationProvider rejects locations whose `elapsedRealtimeNanos` is
 * stale (or 0 / unset), and stale `time` values trip "is this location
 * recent?" checks in detection apps.
 *
 * Source-file timestamps (GPX `<time>`, KML `<when>`) drive playback
 * scheduling in [RouteEngine] — they MUST NOT leak into the emitted Location.
 */
internal object LocationAdapter {

    /** Default provider name used by all three test providers. */
    fun build(
        provider: String,
        lat: Double,
        lon: Double,
        alt: Double = 0.0,
        accuracy: Float = 5f,
        verticalAccuracy: Float = 5f,
        bearing: Float = 0f,
        speed: Float = 0f,
        bearingAccuracy: Float = 5f,
        speedAccuracy: Float = 1f,
    ): Location {
        val loc = Location(provider)
        loc.latitude = lat
        loc.longitude = lon
        loc.altitude = alt
        loc.accuracy = accuracy
        loc.bearing = bearing
        loc.speed = speed
        loc.time = System.currentTimeMillis()
        loc.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            loc.verticalAccuracyMeters = verticalAccuracy
            loc.bearingAccuracyDegrees = bearingAccuracy
            loc.speedAccuracyMetersPerSecond = speedAccuracy
        }
        // Some FusedLocationProvider builds gate on the "isMock" extra; the
        // standard flavor can't suppress this, but the LSPosed module strips
        // it at the framework level when active.
        loc.extras = Bundle().apply { putBoolean("mockLocation", true) }
        return loc
    }

    fun fromStatic(provider: String, cfg: SpoofConfig.Static): Location =
        build(
            provider = provider,
            lat = cfg.lat,
            lon = cfg.lon,
            alt = cfg.alt,
            accuracy = cfg.accuracy,
            verticalAccuracy = cfg.verticalAccuracy,
            bearing = cfg.bearing,
            speed = cfg.speed,
        )

    fun fromSample(provider: String, sample: RouteEngine.Sample, accuracy: Float = 5f): Location =
        build(
            provider = provider,
            lat = sample.waypoint.lat,
            lon = sample.waypoint.lon,
            alt = sample.waypoint.alt ?: 0.0,
            accuracy = accuracy,
            bearing = sample.bearingDegrees,
            speed = sample.speedMps,
        )

    /** All provider names we register as test providers. */
    val ALL_PROVIDERS: List<String> = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    )
}
