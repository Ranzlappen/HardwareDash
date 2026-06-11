package dev.ranzlappen.gadget.feature.sensors

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel backing the Sensors screen. Inject the feature's controller /
 * repositories here and expose an immutable view-state StateFlow.
 */
@HiltViewModel
class SensorsViewModel @Inject constructor() : ViewModel()
