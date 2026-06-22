package dev.ranzlappen.gadget.feature.radios.wifi

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class WifiViewModel @Inject constructor(
    private val monitor: WifiMonitor,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    val state: StateFlow<WifiState> = monitor.state
    val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor
}
