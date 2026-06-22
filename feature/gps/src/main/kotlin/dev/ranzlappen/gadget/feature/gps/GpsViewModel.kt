package dev.ranzlappen.gadget.feature.gps

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class GpsViewModel @Inject constructor(
    private val tracker: GpsLocationTracker,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    val state: StateFlow<GpsState> = tracker.state
    val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor

    fun onPermissionGranted() = tracker.startTracking()

    fun onPermissionRevoked() = tracker.stopTracking()

    override fun onCleared() {
        tracker.stopTracking()
    }
}
