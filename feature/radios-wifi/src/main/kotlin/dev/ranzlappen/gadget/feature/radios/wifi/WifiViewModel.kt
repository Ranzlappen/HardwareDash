package dev.ranzlappen.gadget.feature.radios.wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.feature.radios.wifi.control.WifiController
import dev.ranzlappen.gadget.feature.radios.wifi.control.WifiControllerResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The rooted-tools panel state for the wifi screen (W6 in-screen surface). */
data class WifiRootToolsState(
    val injectionProbe: RootActionState = RootActionState(),
)

@HiltViewModel
class WifiViewModel @Inject constructor(
    private val monitor: WifiMonitor,
    private val wifiController: WifiController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    val state: StateFlow<WifiState> = monitor.state
    val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor

    private val _rootTools = MutableStateFlow(WifiRootToolsState())

    /** Live status of the rooted read-only injection-capability probe. */
    val rootTools: StateFlow<WifiRootToolsState> = _rootTools.asStateFlow()

    fun onProbeInjection() {
        viewModelScope.launch {
            _rootTools.update { it.copy(injectionProbe = it.injectionProbe.copy(running = true)) }
            val result = wifiController.probeInjectionCapability()
            _rootTools.update { it.copy(injectionProbe = result.toActionState()) }
        }
    }

    private fun WifiControllerResult.toActionState(): RootActionState = when (this) {
        is WifiControllerResult.Ok ->
            RootActionState(message = statusNote ?: "Done")
        WifiControllerResult.Unsupported ->
            RootActionState(message = "Requires the rooted app version", isError = true)
        is WifiControllerResult.RateLimited ->
            RootActionState(message = "Rate limited — retry in ${retryAfterMillis}ms", isError = true)
        WifiControllerResult.OptedOut ->
            RootActionState(message = "Blocked by your root-safety opt-out", isError = true)
        is WifiControllerResult.HardwareError ->
            RootActionState(message = message, isError = true)
        is WifiControllerResult.ResetCompleted ->
            RootActionState(message = "Reset $restored restored, $failed failed")
        is WifiControllerResult.RfkillState ->
            RootActionState(message = if (blocked) "Radio blocked" else "Radio unblocked")
        is WifiControllerResult.InjectionCapabilityProbe ->
            RootActionState(message = "Monitor: $supportsMonitor · IBSS: $supportsIbss")
    }
}
