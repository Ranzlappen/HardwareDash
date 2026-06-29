package dev.ranzlappen.gadget.feature.radios.subghz

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SubghzViewModel @Inject constructor(
    private val monitor: SubghzMonitor,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    val state: StateFlow<SubghzState> = monitor.state
    val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor

    fun onRefresh() = monitor.refresh()
}
