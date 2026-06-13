package dev.ranzlappen.gadget.feature.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StorageViewModel @Inject constructor(
    monitor: StorageMonitor,
) : ViewModel() {

    val volumes: StateFlow<List<StorageVolumeInfo>> = monitor.volumes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )
}
