package dev.ranzlappen.gadget.feature.gps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.feature.gps.control.GpsController
import dev.ranzlappen.gadget.feature.gps.control.GpsControllerResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The rooted-tools panel state for the GPS screen (W6 in-screen surface). */
data class GpsRootToolsState(
    val constellation: RootActionState = RootActionState(),
)

@HiltViewModel
class GpsViewModel @Inject constructor(
    private val tracker: GpsLocationTracker,
    private val gpsController: GpsController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    val state: StateFlow<GpsState> = tracker.state
    val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor

    private val _rootTools = MutableStateFlow(GpsRootToolsState())

    /** Live status of the rooted read-only constellation dump. */
    val rootTools: StateFlow<GpsRootToolsState> = _rootTools.asStateFlow()

    fun onDumpConstellation() {
        viewModelScope.launch {
            _rootTools.update { it.copy(constellation = it.constellation.copy(running = true)) }
            val result = gpsController.constellationDump()
            _rootTools.update { it.copy(constellation = result.toActionState()) }
        }
    }

    private fun GpsControllerResult.toActionState(): RootActionState = when (this) {
        is GpsControllerResult.Ok ->
            RootActionState(message = statusNote ?: "Done")
        GpsControllerResult.Unsupported ->
            RootActionState(message = "Requires the rooted app version", isError = true)
        is GpsControllerResult.RateLimited ->
            RootActionState(message = "Rate limited — retry in ${retryAfterMillis}ms", isError = true)
        GpsControllerResult.OptedOut ->
            RootActionState(message = "Blocked by your root-safety opt-out", isError = true)
        is GpsControllerResult.HardwareError ->
            RootActionState(message = message, isError = true)
        is GpsControllerResult.ResetCompleted ->
            RootActionState(message = "Reset $restored restored, $failed failed")
        is GpsControllerResult.NmeaSnapshot ->
            RootActionState(message = "Captured ${sentences.size} NMEA sentences")
        is GpsControllerResult.ConstellationSnapshot ->
            RootActionState(message = "Enumerated ${satellites.size} satellites")
    }

    fun onPermissionGranted() = tracker.startTracking()

    fun onPermissionRevoked() = tracker.stopTracking()

    override fun onCleared() {
        tracker.stopTracking()
    }
}
