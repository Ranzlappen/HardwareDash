package dev.ranzlappen.gadget.feature.sensors

import android.hardware.Sensor
import androidx.compose.runtime.Immutable
import java.util.Locale

/**
 * How the explorer groups sensors — the same split the Android docs use
 * (motion / position / environment), plus everything else.
 */
enum class SensorGroup { Motion, Position, Environment, Other }

/**
 * Display knowledge for a sensor type: its unit and what each value in
 * `SensorEvent.values` means. Vendor and unknown types get no unit and
 * index-based axis names ([axisLabel]).
 */
@Immutable
data class SensorKind(
    val unit: String,
    val axes: List<String>,
    val group: SensorGroup,
)

private val XYZ = listOf("X", "Y", "Z")
private val XYZ_BIAS = listOf("X", "Y", "Z", "Bias X", "Bias Y", "Bias Z")
private val QUATERNION = listOf("X", "Y", "Z", "W")

/**
 * Units, axis meanings and group for the standard sensor types (the set v1
 * showed, plus a few newer ones). `Sensor.TYPE_*` are compile-time constants,
 * so this stays a pure, JVM-testable table.
 */
@Suppress("DEPRECATION") // TYPE_ORIENTATION / TYPE_TEMPERATURE still exist on old devices.
internal fun sensorKind(type: Int): SensorKind = when (type) {
    Sensor.TYPE_ACCELEROMETER,
    Sensor.TYPE_GRAVITY,
    Sensor.TYPE_LINEAR_ACCELERATION -> SensorKind("m/s²", XYZ, SensorGroup.Motion)
    Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> SensorKind("m/s²", XYZ_BIAS, SensorGroup.Motion)
    Sensor.TYPE_GYROSCOPE -> SensorKind("rad/s", XYZ, SensorGroup.Motion)
    Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> SensorKind("rad/s", XYZ_BIAS, SensorGroup.Motion)
    Sensor.TYPE_STEP_COUNTER -> SensorKind("steps", listOf("Steps since boot"), SensorGroup.Motion)
    Sensor.TYPE_STEP_DETECTOR -> SensorKind("", listOf("Step"), SensorGroup.Motion)
    Sensor.TYPE_SIGNIFICANT_MOTION -> SensorKind("", listOf("Triggered"), SensorGroup.Motion)
    Sensor.TYPE_MOTION_DETECT,
    Sensor.TYPE_STATIONARY_DETECT -> SensorKind("", listOf("State"), SensorGroup.Motion)

    Sensor.TYPE_MAGNETIC_FIELD -> SensorKind("µT", XYZ, SensorGroup.Position)
    Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> SensorKind("µT", XYZ_BIAS, SensorGroup.Position)
    Sensor.TYPE_ROTATION_VECTOR,
    Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> SensorKind("", QUATERNION + "Heading accuracy (rad)", SensorGroup.Position)
    Sensor.TYPE_GAME_ROTATION_VECTOR -> SensorKind("", QUATERNION, SensorGroup.Position)
    Sensor.TYPE_ORIENTATION -> SensorKind("°", listOf("Azimuth", "Pitch", "Roll"), SensorGroup.Position)
    Sensor.TYPE_PROXIMITY -> SensorKind("cm", listOf("Distance"), SensorGroup.Position)
    Sensor.TYPE_HINGE_ANGLE -> SensorKind("°", listOf("Angle"), SensorGroup.Position)
    Sensor.TYPE_HEADING -> SensorKind("°", listOf("Heading", "Accuracy (°)"), SensorGroup.Position)

    Sensor.TYPE_LIGHT -> SensorKind("lx", listOf("Illuminance"), SensorGroup.Environment)
    Sensor.TYPE_PRESSURE -> SensorKind("hPa", listOf("Pressure"), SensorGroup.Environment)
    Sensor.TYPE_AMBIENT_TEMPERATURE,
    Sensor.TYPE_TEMPERATURE -> SensorKind("°C", listOf("Temperature"), SensorGroup.Environment)
    Sensor.TYPE_RELATIVE_HUMIDITY -> SensorKind("%", listOf("Relative humidity"), SensorGroup.Environment)

    Sensor.TYPE_HEART_RATE -> SensorKind("bpm", listOf("Heart rate"), SensorGroup.Other)
    else -> SensorKind("", emptyList(), SensorGroup.Other)
}

/** Name for value [index] of a sensor of [kind]: its known meaning, or "Value n". */
internal fun axisLabel(kind: SensorKind, index: Int): String =
    kind.axes.getOrNull(index) ?: "Value ${index + 1}"

/**
 * Human label for a sensor type from `Sensor.getStringType()` — this works for
 * vendor types too: `android.sensor.magnetic_field_uncalibrated` →
 * "Magnetic field uncalibrated", `com.vendor.sensor.light_cct` → "Light cct".
 * Falls back to "Type n" when the string type is missing.
 */
internal fun sensorTypeLabel(stringType: String?, type: Int): String {
    val last = stringType?.substringAfterLast('.')?.trim().orEmpty()
    if (last.isEmpty()) return "Type $type"
    val words = last.replace('_', ' ').replace('-', ' ').trim()
    return words.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}

/**
 * The persisted MetricSource key (history charts, rules, widgets) that
 * matches a sensor type, or null when none exists. Keys are stable —
 * never rename them (MASTER-PLAN §7).
 */
internal fun metricKeyFor(type: Int): String? = when (type) {
    Sensor.TYPE_PROXIMITY -> ProximityMetricSource.METRIC_KEY
    Sensor.TYPE_LIGHT -> LightMetricSource.METRIC_KEY
    Sensor.TYPE_ACCELEROMETER -> AccelerationMetricSource.METRIC_KEY
    Sensor.TYPE_GYROSCOPE -> RotationRateMetricSource.METRIC_KEY
    Sensor.TYPE_STEP_COUNTER -> StepCounterMetricSource.METRIC_KEY
    Sensor.TYPE_MOTION_DETECT -> MotionDetectedMetricSource.METRIC_KEY
    else -> null
}

/** Step sensors need `ACTIVITY_RECOGNITION` (API 29+) before they deliver events. */
internal fun needsActivityRecognition(type: Int): Boolean =
    type == Sensor.TYPE_STEP_COUNTER || type == Sensor.TYPE_STEP_DETECTOR

/** Everything the explorer shows about one sensor — static, from `android.hardware.Sensor`. */
@Immutable
data class SensorInfo(
    /** Index into `DeviceSensors.all()` — the route argument. */
    val index: Int,
    val type: Int,
    val typeLabel: String,
    val name: String,
    val vendor: String,
    val version: Int,
    val maxRange: Float,
    val resolution: Float,
    val powerMa: Float,
    val minDelayUs: Int,
    val isWakeUp: Boolean,
    /** `Sensor.REPORTING_MODE_*`. */
    val reportingMode: Int,
    val kind: SensorKind,
) {
    /** Trigger sensors (significant motion …) fire once and can't be streamed. */
    val isOneShot: Boolean get() = reportingMode == Sensor.REPORTING_MODE_ONE_SHOT
}

/** Fixed-precision number for readouts and reports (locale-independent). */
internal fun formatValue(value: Float): String = String.format(Locale.ROOT, "%.3f", value)

/**
 * Plain-text block describing [info] and (optionally) its latest [values],
 * for the clipboard — the v1 "copy readings" feature.
 */
internal fun sensorReport(info: SensorInfo, values: FloatArray? = null): String = buildString {
    val unit = info.kind.unit.let { if (it.isEmpty()) "" else " $it" }
    appendLine("${info.typeLabel} — ${info.name}")
    appendLine("Vendor: ${info.vendor} · version ${info.version}")
    appendLine("Range: ${formatValue(info.maxRange)}$unit · resolution ${formatValue(info.resolution)}$unit")
    appendLine("Power: ${formatValue(info.powerMa)} mA · min delay ${info.minDelayUs} µs")
    val flags = listOfNotNull(
        "wake-up".takeIf { info.isWakeUp },
        "one-shot trigger".takeIf { info.isOneShot },
    )
    if (flags.isNotEmpty()) appendLine("Flags: ${flags.joinToString()}")
    values?.forEachIndexed { i, v ->
        appendLine("${axisLabel(info.kind, i)}: ${formatValue(v)}$unit")
    }
}.trimEnd()
