package dev.ranzlappen.gadget.feature.diagnostics

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DiagnosticsState(isRootedFlavor = rootCapabilityRegistry.isRootedFlavor),
    )
    val state: StateFlow<DiagnosticsState> = _state
}
