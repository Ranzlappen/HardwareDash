package com.gadget.gps.spoof

/**
 * Pure-Kotlin point on a spoofed track. Lives in the shared package so the
 * RouteEngine can be JVM-tested. The Android-side LocationAdapter is the only
 * place that turns this into an `android.location.Location`, so this stays
 * dependency-free.
 *
 * `timestampMs` is wall-clock time recorded in the source file (GPX `<time>`,
 * KML `<when>`); when null the RouteEngine paces playback off the
 * SpoofConfig's defaultSpeedMps. `speedMps` overrides the inter-point
 * derivation when set.
 */
data class Waypoint(
    val lat: Double,
    val lon: Double,
    val alt: Double? = null,
    val timestampMs: Long? = null,
    val speedMps: Float? = null,
)
