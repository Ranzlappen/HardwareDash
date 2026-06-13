package dev.ranzlappen.gadget.feature.gps

import androidx.compose.runtime.Immutable

@Immutable
data class GpsState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitudeMeters: Double = 0.0,
    val speedKmh: Float = 0f,
    val bearingDegrees: Float = 0f,
    val accuracyMeters: Float = 0f,
    val hasLocation: Boolean = false,
    val permissionGranted: Boolean = false,
)
