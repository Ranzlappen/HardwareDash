package dev.ranzlappen.gadget.feature.gps.spoof

import android.net.Uri

/**
 * What the user wants to emit. Static is a single fixed point; the playback
 * variants run inside the foreground LocationSpoofService and stream a
 * Waypoint sequence over time.
 *
 * Static does NOT need a foreground service — it just sets one location and
 * exits. The other three drive RouteEngine and require the service so Doze
 * doesn't kill the emitter.
 */
sealed interface SpoofConfig {

    data class Static(
        val lat: Double,
        val lon: Double,
        val alt: Double = 0.0,
        val accuracy: Float = 5f,
        val verticalAccuracy: Float = 5f,
        val bearing: Float = 0f,
        val speed: Float = 0f,
    ) : SpoofConfig

    data class GpxPlayback(
        val source: Uri,
        val speedMultiplier: Float = 1.0f,
        val loop: Boolean = false,
        /** Used when the GPX has no `<time>` elements (e.g. `<rte>` routes). */
        val defaultSpeedMps: Float = 1.4f,
    ) : SpoofConfig

    data class KmlPlayback(
        val source: Uri,
        val speedMultiplier: Float = 1.0f,
        val loop: Boolean = false,
        /** Used when the KML has no `<when>` elements (e.g. `<LineString>`). */
        val defaultSpeedMps: Float = 1.4f,
    ) : SpoofConfig

    data class Route(
        val waypoints: List<Waypoint>,
        val interpolation: Interpolation = Interpolation.Linear,
        val defaultSpeedMps: Float = 5f,
        val loop: Boolean = false,
    ) : SpoofConfig {
        enum class Interpolation { Linear, Cubic }
    }
}
