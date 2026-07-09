package dev.ranzlappen.gadget.feature.sensors.automation

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.sensors.AccelerationMetricSource
import dev.ranzlappen.gadget.feature.sensors.LightMetricSource
import dev.ranzlappen.gadget.feature.sensors.ProximityMetricSource
import dev.ranzlappen.gadget.feature.sensors.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sensors' invocable-action surface for automation. Unlike torch/vibration
 * there is no actuator/controller here to drive — `:feature:sensors` is a
 * pure read side (three [MetricSource]s with no start/stop/configure
 * methods; see [SensorMetricSources][dev.ranzlappen.gadget.feature.sensors.SensorMetricSources]).
 * So rather than force a fake "control" action, this reuses the same
 * read-only pattern `:feature:ambient`'s `AmbientActionHandler` already
 * established: threshold *assertions* over the module's existing
 * [MetricSource.sample] reads, so a rule can use a sensor reading as an
 * automation condition/action step ("assert proximity is below 5 cm" —
 * the wiki's canonical proximity-trigger example) without re-plumbing a new
 * signal path. Each source is the exact instance already injected into
 * `SensorsViewModel` — no new sensor plumbing, no feature-to-feature import.
 */
@Singleton
class SensorsActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val proximity: ProximityMetricSource,
    private val light: LightMetricSource,
    private val acceleration: AccelerationMetricSource,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_PROXIMITY_NEAR,
            label = context.getString(R.string.sensors_action_proximity_near),
            params = listOf(
                ActionParam(
                    PARAM_THRESHOLD_CM,
                    ActionParamType.Float,
                    DEFAULT_PROXIMITY_THRESHOLD_CM.toString(),
                    0f,
                    proximity.descriptor.max,
                ),
            ),
        ),
        ModuleAction(
            key = ACTION_PROXIMITY_FAR,
            label = context.getString(R.string.sensors_action_proximity_far),
            params = listOf(
                ActionParam(
                    PARAM_THRESHOLD_CM,
                    ActionParamType.Float,
                    DEFAULT_PROXIMITY_THRESHOLD_CM.toString(),
                    0f,
                    proximity.descriptor.max,
                ),
            ),
        ),
        ModuleAction(
            key = ACTION_LIGHT_BRIGHT,
            label = context.getString(R.string.sensors_action_light_bright),
            params = listOf(
                ActionParam(
                    PARAM_THRESHOLD_LUX,
                    ActionParamType.Float,
                    DEFAULT_LIGHT_BRIGHT_THRESHOLD_LUX.toString(),
                    0f,
                    light.descriptor.max,
                ),
            ),
        ),
        ModuleAction(
            key = ACTION_LIGHT_DARK,
            label = context.getString(R.string.sensors_action_light_dark),
            params = listOf(
                ActionParam(
                    PARAM_THRESHOLD_LUX,
                    ActionParamType.Float,
                    DEFAULT_LIGHT_DARK_THRESHOLD_LUX.toString(),
                    0f,
                    light.descriptor.max,
                ),
            ),
        ),
        ModuleAction(
            key = ACTION_ACCELERATION_ABOVE,
            label = context.getString(R.string.sensors_action_acceleration_above),
            params = listOf(
                ActionParam(
                    PARAM_THRESHOLD_MS2,
                    ActionParamType.Float,
                    DEFAULT_ACCELERATION_ABOVE_THRESHOLD_MS2.toString(),
                    0f,
                    acceleration.descriptor.max,
                ),
            ),
        ),
        ModuleAction(
            key = ACTION_ACCELERATION_BELOW,
            label = context.getString(R.string.sensors_action_acceleration_below),
            params = listOf(
                ActionParam(
                    PARAM_THRESHOLD_MS2,
                    ActionParamType.Float,
                    DEFAULT_ACCELERATION_BELOW_THRESHOLD_MS2.toString(),
                    0f,
                    acceleration.descriptor.max,
                ),
            ),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_PROXIMITY_NEAR -> assertBelow(
                source = proximity,
                sensorName = "Proximity",
                unit = "cm",
                threshold = params.floatOr(PARAM_THRESHOLD_CM, DEFAULT_PROXIMITY_THRESHOLD_CM),
            )
            ACTION_PROXIMITY_FAR -> assertAbove(
                source = proximity,
                sensorName = "Proximity",
                unit = "cm",
                threshold = params.floatOr(PARAM_THRESHOLD_CM, DEFAULT_PROXIMITY_THRESHOLD_CM),
            )
            ACTION_LIGHT_BRIGHT -> assertAbove(
                source = light,
                sensorName = "Ambient light",
                unit = "lx",
                threshold = params.floatOr(PARAM_THRESHOLD_LUX, DEFAULT_LIGHT_BRIGHT_THRESHOLD_LUX),
            )
            ACTION_LIGHT_DARK -> assertBelow(
                source = light,
                sensorName = "Ambient light",
                unit = "lx",
                threshold = params.floatOr(PARAM_THRESHOLD_LUX, DEFAULT_LIGHT_DARK_THRESHOLD_LUX),
            )
            ACTION_ACCELERATION_ABOVE -> assertAbove(
                source = acceleration,
                sensorName = "Acceleration",
                unit = "m/s²",
                threshold = params.floatOr(PARAM_THRESHOLD_MS2, DEFAULT_ACCELERATION_ABOVE_THRESHOLD_MS2),
            )
            ACTION_ACCELERATION_BELOW -> assertBelow(
                source = acceleration,
                sensorName = "Acceleration",
                unit = "m/s²",
                threshold = params.floatOr(PARAM_THRESHOLD_MS2, DEFAULT_ACCELERATION_BELOW_THRESHOLD_MS2),
            )
            else -> ActionResult.Unsupported
        }

    /**
     * [MetricSource.sample] returns the signal's absent-value (0f) rather
     * than failing when the device lacks the sensor (see the class doc on
     * `SensorMetricSources`), which would let an absent sensor silently
     * satisfy a "below threshold" assertion. Guard on [MetricSource.stream]
     * — non-null exactly when the sensor is present — the same check
     * `SensorsViewModel` uses to decide row availability.
     */
    private suspend fun assertBelow(
        source: MetricSource,
        sensorName: String,
        unit: String,
        threshold: Float,
    ): ActionResult {
        if (source.stream() == null) return ActionResult.Failure("$sensorName not present on this device")
        val value = source.sample()
        return if (value <= threshold) {
            ActionResult.Success
        } else {
            ActionResult.Failure("$sensorName $value$unit is not below $threshold$unit")
        }
    }

    private suspend fun assertAbove(
        source: MetricSource,
        sensorName: String,
        unit: String,
        threshold: Float,
    ): ActionResult {
        if (source.stream() == null) return ActionResult.Failure("$sensorName not present on this device")
        val value = source.sample()
        return if (value >= threshold) {
            ActionResult.Success
        } else {
            ActionResult.Failure("$sensorName $value$unit is not above $threshold$unit")
        }
    }

    private fun Map<String, String>.floatOr(key: String, fallback: Float): Float =
        this[key]?.toFloatOrNull() ?: fallback

    companion object {
        const val FEATURE_ID = "sensors"
        const val ACTION_PROXIMITY_NEAR = "proximity_assert_near"
        const val ACTION_PROXIMITY_FAR = "proximity_assert_far"
        const val ACTION_LIGHT_BRIGHT = "light_assert_bright"
        const val ACTION_LIGHT_DARK = "light_assert_dark"
        const val ACTION_ACCELERATION_ABOVE = "acceleration_assert_above"
        const val ACTION_ACCELERATION_BELOW = "acceleration_assert_below"
        const val PARAM_THRESHOLD_CM = "threshold_cm"
        const val PARAM_THRESHOLD_LUX = "threshold_lux"
        const val PARAM_THRESHOLD_MS2 = "threshold_ms2"

        // The wiki's canonical proximity-trigger example ("if proximity < 5
        // cm then torch off").
        const val DEFAULT_PROXIMITY_THRESHOLD_CM = 5f

        // Mirrors `AmbientActionHandler`'s bright/dark defaults — the same
        // physical signal, same reasonable thresholds.
        const val DEFAULT_LIGHT_BRIGHT_THRESHOLD_LUX = 100f
        const val DEFAULT_LIGHT_DARK_THRESHOLD_LUX = 10f

        // TYPE_ACCELEROMETER reports gravity too, so a resting device already
        // reads ~9.8 m/s² (1g) — these defaults are picked relative to that
        // baseline, not to zero: comfortably above it for shake/impact
        // detection, comfortably below it for near-weightlessness (free
        // fall/toss), not for ordinary rest.
        const val DEFAULT_ACCELERATION_ABOVE_THRESHOLD_MS2 = 15f
        const val DEFAULT_ACCELERATION_BELOW_THRESHOLD_MS2 = 3f
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface SensorsActionModule {

    @Binds
    @IntoMap
    @StringKey(SensorsActionHandler.FEATURE_ID)
    fun bindSensorsActionHandler(handler: SensorsActionHandler): ActionHandler
}
