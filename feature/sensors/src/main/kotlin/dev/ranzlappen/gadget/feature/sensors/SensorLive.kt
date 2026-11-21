package dev.ranzlappen.gadget.feature.sensors

import androidx.compose.runtime.Immutable

/**
 * Rolling live state for one sensor on the detail screen: latest values,
 * a short per-axis history for the sparklines, accuracy and a smoothed event
 * rate. Pure (no Android types) so `SensorLiveTest` can pin it.
 */
@Immutable
data class SensorLive(
    val values: List<Float> = emptyList(),
    /** One list per axis, oldest first, at most [HISTORY_SIZE] samples. */
    val history: List<List<Float>> = emptyList(),
    /** `SensorManager.SENSOR_STATUS_*`, or null before the first event. */
    val accuracy: Int? = null,
    /** Events per second, exponentially smoothed; null until two events arrived. */
    val rateHz: Float? = null,
    val lastTimestampNanos: Long = 0L,
) {
    /** State after one more event. */
    fun next(values: FloatArray, accuracy: Int, timestampNanos: Long): SensorLive {
        val newHistory = values.indices.map { i ->
            (history.getOrElse(i) { emptyList() } + values[i]).takeLast(HISTORY_SIZE)
        }
        val delta = timestampNanos - lastTimestampNanos
        val instantHz = if (lastTimestampNanos > 0L && delta > 0L) NANOS_PER_SECOND / delta else null
        val smoothed = when {
            instantHz == null -> rateHz
            rateHz == null -> instantHz
            else -> rateHz * (1f - RATE_SMOOTHING) + instantHz * RATE_SMOOTHING
        }
        return SensorLive(
            values = values.toList(),
            history = newHistory,
            accuracy = accuracy,
            rateHz = smoothed,
            lastTimestampNanos = timestampNanos,
        )
    }

    companion object {
        const val HISTORY_SIZE = 120
        private const val NANOS_PER_SECOND = 1_000_000_000f
        private const val RATE_SMOOTHING = 0.2f
    }
}
