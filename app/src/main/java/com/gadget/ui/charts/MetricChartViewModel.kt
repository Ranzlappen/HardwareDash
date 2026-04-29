package com.gadget.ui.charts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gadget.data.db.MetricReading
import com.gadget.data.repository.MetricRepository
import com.gadget.widget.WidgetMetric
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TimeRange(val label: String, val millis: Long) {
    ONE_HOUR("1h", 3_600_000L),
    SIX_HOURS("6h", 21_600_000L),
    ONE_DAY("24h", 86_400_000L),
    SEVEN_DAYS("7d", 604_800_000L),
    THIRTY_DAYS("30d", 2_592_000_000L),
}

data class ChartState(
    val metricKey: String = "",
    val metricName: String = "",
    val metricUnit: String = "",
    val readings: List<MetricReading> = emptyList(),
    val timeRange: TimeRange = TimeRange.ONE_HOUR,
    val currentValue: String = "--",
    val minValue: String = "--",
    val maxValue: String = "--",
    val avgValue: String = "--",
)

@HiltViewModel
class MetricChartViewModel @Inject constructor(
    private val metricRepository: MetricRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val metricKey: String = savedStateHandle["metricKey"] ?: ""

    private val _state = MutableStateFlow(ChartState(metricKey = metricKey))
    val state: StateFlow<ChartState> = _state.asStateFlow()

    private val _timeRange = MutableStateFlow(TimeRange.ONE_HOUR)

    init {
        val metric = WidgetMetric.fromKey(metricKey)
        if (metric != null) {
            _state.update { it.copy(metricName = metric.displayName, metricUnit = metric.unit) }
        }

        viewModelScope.launch {
            _timeRange.collectLatest { range ->
                val now = System.currentTimeMillis()
                val start = now - range.millis
                metricRepository.getReadingsInRange(metricKey, start, now)
                    .collect { readings ->
                        val values = readings.map { it.rawValue }
                        _state.update { state ->
                            state.copy(
                                readings = readings,
                                timeRange = range,
                                currentValue = readings.lastOrNull()?.formattedValue ?: "--",
                                minValue = values.minOrNull()?.let { "%.2f".format(it) } ?: "--",
                                maxValue = values.maxOrNull()?.let { "%.2f".format(it) } ?: "--",
                                avgValue = if (values.isNotEmpty()) "%.2f".format(values.average()) else "--",
                            )
                        }
                    }
            }
        }
    }

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
    }
}
