package dev.ranzlappen.gadget.feature.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Thin, injectable seam over [SensorManager] — the only place the sensors
 * feature touches the framework API, so the [MetricSource]s and the
 * ViewModel stay declarative.
 *
 * [stream] is a **cold** flow: the listener registers on collect and
 * unregisters on cancel ([awaitClose]), so an uncollected signal costs zero
 * wakeups — the push contract `MetricSource.stream()` wants. [conflate]
 * keeps a slow collector from backing up the sensor callback thread.
 */
@Singleton
class DeviceSensors @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    fun sensor(type: Int): Sensor? = sensorManager?.getDefaultSensor(type)

    fun has(type: Int): Boolean = sensor(type) != null

    /** The sensor's reported full-scale ceiling, or null when absent. */
    fun maxRange(type: Int): Float? = sensor(type)?.maximumRange

    /**
     * Cold change-stream of [type]'s readings mapped through [transform]
     * (e.g. `values[0]` for proximity/light, magnitude for accelerometer).
     * Empty flow when the sensor is absent.
     */
    fun stream(type: Int, transform: (FloatArray) -> Float): Flow<Float> = callbackFlow {
        val manager = sensorManager
        val sensor = sensor(type)
        if (manager == null || sensor == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(transform(event.values))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { manager.unregisterListener(listener) }
    }.conflate()

    /**
     * One-shot current value for `MetricSource.sample()`: register, take the
     * first event, unregister. On-change sensors (proximity, light) deliver
     * their current value immediately on registration, so this is fast;
     * [timeoutMs] bounds the absent/silent case (returns null).
     */
    suspend fun oneShot(
        type: Int,
        timeoutMs: Long = ONE_SHOT_TIMEOUT_MS,
        transform: (FloatArray) -> Float,
    ): Float? =
        if (!has(type)) null
        else withTimeoutOrNull(timeoutMs) { stream(type, transform).first() }

    private companion object {
        const val ONE_SHOT_TIMEOUT_MS = 1_000L
    }
}
