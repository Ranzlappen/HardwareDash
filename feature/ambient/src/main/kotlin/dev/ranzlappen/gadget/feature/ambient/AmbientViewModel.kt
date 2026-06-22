package dev.ranzlappen.gadget.feature.ambient

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AmbientViewModel @Inject constructor(
    private val sensor: AmbientSensor,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    val state: StateFlow<AmbientState> = sensor.state
    val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor
}
