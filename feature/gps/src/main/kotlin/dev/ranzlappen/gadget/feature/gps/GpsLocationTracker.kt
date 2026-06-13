package dev.ranzlappen.gadget.feature.gps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton wrapper around [com.google.android.gms.location.FusedLocationProviderClient].
 * Call [startTracking] once location permission is granted; call [stopTracking] when the
 * screen leaves composition or permission is revoked.
 */
@Singleton
class GpsLocationTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    private val _state = MutableStateFlow(GpsState())
    val state: StateFlow<GpsState> = _state.asStateFlow()

    private var tracking = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            _state.update { loc.toGpsState(permissionGranted = true) }
        }
    }

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        INTERVAL_MS,
    ).setMinUpdateIntervalMillis(MIN_INTERVAL_MS).build()

    fun startTracking() {
        if (tracking) return
        if (!hasPermission()) {
            _state.update { GpsState(permissionGranted = false) }
            return
        }
        tracking = true
        _state.update { it.copy(permissionGranted = true) }
        requestLastKnown()
        requestUpdates()
    }

    fun stopTracking() {
        if (!tracking) return
        tracking = false
        fusedClient.removeLocationUpdates(locationCallback)
        _state.update { it.copy(permissionGranted = false, hasLocation = false) }
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun requestLastKnown() {
        fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) _state.update { loc.toGpsState(permissionGranted = true) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestUpdates() {
        fusedClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper(),
        )
    }

    private companion object {
        const val INTERVAL_MS = 2_000L
        const val MIN_INTERVAL_MS = 1_000L
    }
}

private fun Location.toGpsState(permissionGranted: Boolean) = GpsState(
    latitude = latitude,
    longitude = longitude,
    altitudeMeters = altitude,
    speedKmh = speed * 3.6f,
    bearingDegrees = bearing,
    accuracyMeters = accuracy,
    hasLocation = true,
    permissionGranted = permissionGranted,
)
