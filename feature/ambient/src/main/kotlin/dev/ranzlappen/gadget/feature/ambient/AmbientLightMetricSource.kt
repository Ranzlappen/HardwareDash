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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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

    override suspend fun sample(): Float =
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { _ ->
            var reading = 0f
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) { reading = event.values[0] }
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }
            sensorManager.registerListener(
                listener,
                sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!,
                SensorManager.SENSOR_DELAY_NORMAL,
            )
            sensorManager.unregisterListener(listener)
            reading
        } ?: 0f

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
    }
}
