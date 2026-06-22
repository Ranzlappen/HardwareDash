package dev.ranzlappen.gadget.feature.sensors

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * One row per sensor signal for the screen's live-readout cards.
 * [value] is null until the first reading lands (or forever when the
 * sensor is [available] = false).
 */
@Immutable
data class SensorRowUi(
    val metricKey: String,
    val name: String,
    val unit: String,
    val available: Boolean,
    val value: Float?,
)

/**
 * ViewModel backing the Sensors screen: combines the three signal streams
 * into one live row list. Streams are collected only while the screen is
 * subscribed ([SharingStarted.WhileSubscribed]) — leaving the screen
 * unregisters every sensor listener via [DeviceSensors]' cold flows, so the
 * module is idle off-screen.
 */
@HiltViewModel
class SensorsViewModel @Inject constructor(
    proximity: ProximityMetricSource,
    light: LightMetricSource,
    acceleration: AccelerationMetricSource,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor

    private val sources: List<MetricSource> = listOf(proximity, light, acceleration)

    val rows: StateFlow<List<SensorRowUi>> = combine(
        sources.map { source ->
            val stream = source.stream()
            if (stream != null) {
                stream
                    .map { value -> source.row(available = true, value = value) }
                    // A row renders (value pending) before the first event.
                    .onStart { emit(source.row(available = true, value = null)) }
            } else {
                flowOf(source.row(available = false, value = null))
            }
        },
    ) { it.toList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_FLOW_TIMEOUT_MS),
            initialValue = sources.map { it.row(available = it.stream() != null, value = null) },
        )

    private fun MetricSource.row(available: Boolean, value: Float?) = SensorRowUi(
        metricKey = descriptor.metricKey,
        name = descriptor.displayName,
        unit = descriptor.unit,
        available = available,
        value = value,
    )

    private companion object {
        const val STATE_FLOW_TIMEOUT_MS = 5_000L
    }
}
