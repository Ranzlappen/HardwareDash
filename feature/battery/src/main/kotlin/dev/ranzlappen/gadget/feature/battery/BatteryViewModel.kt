package dev.ranzlappen.gadget.feature.battery

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BatteryViewModel @Inject constructor(
    monitor: BatteryMonitor,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {
    val state: StateFlow<BatteryState> = monitor.state
    val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor
}
