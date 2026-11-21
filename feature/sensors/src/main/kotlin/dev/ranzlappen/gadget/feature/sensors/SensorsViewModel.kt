package dev.ranzlappen.gadget.feature.sensors

import android.hardware.Sensor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.hardware.DeviceSensors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The sensor explorer's list (v3 B5): every sensor the device reports, not
 * just the three v2 had. Static information only — live values are
 * streamed per sensor on the detail screen, so the list registers no
 * listeners.
 */
@HiltViewModel
class SensorsViewModel @Inject constructor(
    sensors: DeviceSensors,
) : ViewModel() {
    val sensors: List<SensorInfo> = sensors.all().mapIndexed { index, sensor -> sensor.toInfo(index) }
}

/**
 * One sensor's detail screen: its specs plus a live, rolling [SensorLive]
 * that is only collected while the screen is visible (`WhileSubscribed`), so
 * leaving the screen unregisters the listener.
 */
@HiltViewModel
class SensorDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sensors: DeviceSensors,
) : ViewModel() {

    private val sensor: Sensor? =
        savedStateHandle.get<Int>(ARG_SENSOR_INDEX)?.let { sensors.all().getOrNull(it) }

    val info: SensorInfo? = sensor?.toInfo(savedStateHandle.get<Int>(ARG_SENSOR_INDEX) ?: -1)

    val live: StateFlow<SensorLive> = (
        if (sensor != null && info?.isOneShot == false) {
            sensors.readings(sensor).runningFold(SensorLive()) { state, reading ->
                state.next(reading.values, reading.accuracy, reading.timestampNanos)
            }
        } else {
            flowOf(SensorLive())
        }
        ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_FLOW_TIMEOUT_MS),
        initialValue = SensorLive(),
    )

    companion object {
        const val ARG_SENSOR_INDEX = "sensorIndex"
        private const val STATE_FLOW_TIMEOUT_MS = 5_000L
    }
}

internal fun Sensor.toInfo(index: Int): SensorInfo = SensorInfo(
    index = index,
    type = type,
    typeLabel = sensorTypeLabel(stringType, type),
    name = name.orEmpty(),
    vendor = vendor.orEmpty(),
    version = version,
    maxRange = maximumRange,
    resolution = resolution,
    powerMa = power,
    minDelayUs = minDelay,
    isWakeUp = isWakeUpSensor,
    reportingMode = reportingMode,
    kind = sensorKind(type),
)
