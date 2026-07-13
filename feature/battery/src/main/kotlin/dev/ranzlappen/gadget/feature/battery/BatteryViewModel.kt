package dev.ranzlappen.gadget.feature.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.feature.battery.control.BatteryController
import dev.ranzlappen.gadget.feature.battery.control.BatteryControllerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The rooted-tools panel state for the battery screen (W6 in-screen surface). */
data class BatteryRootToolsState(
    val fuelGauge: RootActionState = RootActionState(),
    val health: RootActionState = RootActionState(),
)

@HiltViewModel
class BatteryViewModel @Inject constructor(
    monitor: BatteryMonitor,
    private val batteryController: BatteryController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {
    val state: StateFlow<BatteryState> = monitor.state
    val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor

    private val _rootTools = MutableStateFlow(BatteryRootToolsState())

    /** Live status of the rooted read-only fuel-gauge and health dumps. */
    val rootTools: StateFlow<BatteryRootToolsState> = _rootTools.asStateFlow()

    fun onDumpFuelGauge() {
        viewModelScope.launch {
            _rootTools.update { it.copy(fuelGauge = it.fuelGauge.copy(running = true)) }
            val result = batteryController.fuelGaugeRaw()
            _rootTools.update { it.copy(fuelGauge = result.toActionState()) }
        }
    }

    fun onDeepHealthDump() {
        viewModelScope.launch {
            _rootTools.update { it.copy(health = it.health.copy(running = true)) }
            val result = batteryController.batteryHealthDeepDump()
            _rootTools.update { it.copy(health = result.toActionState()) }
        }
    }

    private fun BatteryControllerResult.toActionState(): RootActionState = when (this) {
        is BatteryControllerResult.Ok ->
            RootActionState(message = statusNote ?: "Done")
        BatteryControllerResult.Unsupported ->
            RootActionState(message = "Requires the rooted app version", isError = true)
        is BatteryControllerResult.RateLimited ->
            RootActionState(message = "Rate limited — retry in ${retryAfterMillis}ms", isError = true)
        BatteryControllerResult.OptedOut ->
            RootActionState(message = "Blocked by your root-safety opt-out", isError = true)
        is BatteryControllerResult.HardwareError ->
            RootActionState(message = message, isError = true)
        is BatteryControllerResult.ResetCompleted ->
            RootActionState(message = "Reset $restored restored, $failed failed")
        is BatteryControllerResult.FuelGaugeReading ->
            RootActionState(message = "Read ${nodes.size} fuel-gauge nodes")
        is BatteryControllerResult.CellSnapshot ->
            RootActionState(message = "Read ${cells.size} cells")
        is BatteryControllerResult.DumpWritten ->
            RootActionState(message = "Wrote snapshot to $absolutePath")
        is BatteryControllerResult.DangerousAborted ->
            RootActionState(message = reason, isError = true)
        is BatteryControllerResult.HoldSocSnapshot ->
            RootActionState(message = "Holding at ${appliedTargetSocPercent}%")
        is BatteryControllerResult.BatteryHealthReading ->
            RootActionState(message = "Health: ${cycleCount ?: "?"} cycles, ${nodes.size} nodes")
        is BatteryControllerResult.WirelessCoilSnapshot ->
            RootActionState(message = "Coil current ${appliedCoilCurrentMicroAmps}µA")
    }
}
