package dev.ranzlappen.gadget.feature.motion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.hardware.DeviceSensors
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MotionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sensors: DeviceSensors,
    private val rotationRate: RotationRateMetricSource,
    private val stepCounter: StepCounterMetricSource,
    private val motionDetected: MotionDetectedMetricSource,
    private val rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow(MotionState())
    val state: StateFlow<MotionState> = _state

    init {
        _state.update {
            it.copy(
                hasGyroscope = sensors.has(Sensor.TYPE_GYROSCOPE),
                hasStepCounter = sensors.has(Sensor.TYPE_STEP_COUNTER),
                hasMotionDetect = sensors.has(Sensor.TYPE_MOTION_DETECT),
                activityPermissionGranted = checkActivityPermission(),
                isRootedFlavor = rootCapabilityRegistry.isRootedFlavor,
            )
        }
        collectSensors()
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(activityPermissionGranted = granted) }
    }

    fun refresh() {
        _state.update { it.copy(activityPermissionGranted = checkActivityPermission()) }
    }

    private fun checkActivityPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED
        } else true

    private fun collectSensors() {
        rotationRate.stream()?.let { flow ->
            viewModelScope.launch { flow.collect { v -> _state.update { it.copy(rotationRate = v) } } }
        }
        stepCounter.stream()?.let { flow ->
            viewModelScope.launch { flow.collect { v -> _state.update { it.copy(stepCount = v) } } }
        }
        motionDetected.stream()?.let { flow ->
            viewModelScope.launch { flow.collect { v -> _state.update { it.copy(motionDetected = v > 0.5f) } } }
        }
    }
}
