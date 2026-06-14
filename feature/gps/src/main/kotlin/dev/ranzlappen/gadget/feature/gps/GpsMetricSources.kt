package dev.ranzlappen.gadget.feature.gps

import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * GPS metric sources wired `@IntoMap @StringKey(METRIC_KEY)` via [di.GpsModule]. Both
 * are push sources backed by [GpsLocationTracker.state] — no periodic polling overhead
 * when the device is stationary.
 */

@Singleton
class GpsSpeedMetricSource @Inject constructor(
    private val tracker: GpsLocationTracker,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "GPS speed",
        unit = "km/h",
        min = 0f,
        max = 300f,
        category = MetricCategory.Location,
    )

    override suspend fun sample(): Float = tracker.state.value.speedKmh

    override fun stream(): Flow<Float> = tracker.state.map { it.speedKmh }

    companion object {
        const val METRIC_KEY = "gps_speed"
    }
}

@Singleton
class GpsAltitudeMetricSource @Inject constructor(
    private val tracker: GpsLocationTracker,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "GPS altitude",
        unit = "m",
        min = -500f,
        max = 8849f,
        category = MetricCategory.Location,
    )

    override suspend fun sample(): Float = tracker.state.value.altitudeMeters.toFloat()

    override fun stream(): Flow<Float> = tracker.state.map { it.altitudeMeters.toFloat() }

    companion object {
        const val METRIC_KEY = "gps_altitude"
    }
}
