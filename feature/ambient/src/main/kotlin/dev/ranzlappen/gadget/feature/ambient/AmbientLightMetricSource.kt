package dev.ranzlappen.gadget.feature.ambient

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class AmbientLightMetricSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : MetricSource {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Ambient light",
        unit = "lux",
        min = 0f,
        max = 20_000f,
        category = MetricCategory.Sensor,
    )

    // NOTE: previously this registered a listener and unregistered it again on
    // the very next line, never actually suspending for a callback — so it
    // always returned the initial `reading = 0f` regardless of the real light
    // level. That silently broke every automation rule keyed on this metric,
    // since `RuleFireExecutor`/`AutomationService` call `sample()` directly
    // (bypassing `stream()`) to evaluate `MetricThreshold`/`MetricCompare`
    // conditions. Fixed to actually await the sensor's first callback, bounded
    // by [SAMPLE_TIMEOUT_MS] so an absent/silent sensor still falls back to 0f.
    override suspend fun sample(): Float {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) ?: return 0f
        return withTimeoutOrNull(SAMPLE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sensorManager.unregisterListener(this)
                        if (continuation.isActive) continuation.resume(event.values[0])
                    }
                    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
                }
                // Registered before starting the listener in case the sensor
                // reports (and resumes the continuation) synchronously.
                continuation.invokeOnCancellation { sensorManager.unregisterListener(listener) }
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } ?: 0f
    }

    override fun stream(): Flow<Float> = callbackFlow {
        trySend(0f)
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) { trySend(event.values[0]) }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    companion object {
        const val METRIC_KEY = "ambient_light"
        private const val SAMPLE_TIMEOUT_MS = 1_000L
    }
}
