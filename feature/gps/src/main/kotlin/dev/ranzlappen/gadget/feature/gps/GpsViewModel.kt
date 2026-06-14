package dev.ranzlappen.gadget.feature.gps

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class GpsViewModel @Inject constructor(
    private val tracker: GpsLocationTracker,
) : ViewModel() {

    val state: StateFlow<GpsState> = tracker.state

    fun onPermissionGranted() = tracker.startTracking()

    fun onPermissionRevoked() = tracker.stopTracking()

    override fun onCleared() {
        tracker.stopTracking()
    }
}
