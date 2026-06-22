package dev.ranzlappen.gadget.feature.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LockViewModel @Inject constructor(
    monitor: LockMonitor,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val isRootedFlavor = rootCapabilityRegistry.isRootedFlavor

    val state: StateFlow<LockState> = monitor.state
        .map { it.copy(isRootedFlavor = isRootedFlavor) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LockState(isRootedFlavor = isRootedFlavor),
        )
}
