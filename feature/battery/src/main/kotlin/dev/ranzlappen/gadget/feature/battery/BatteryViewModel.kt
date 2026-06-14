package dev.ranzlappen.gadget.feature.battery

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BatteryViewModel @Inject constructor(
    monitor: BatteryMonitor,
) : ViewModel() {
    val state: StateFlow<BatteryState> = monitor.state
}
