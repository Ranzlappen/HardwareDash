package dev.ranzlappen.gadget.feature.motion.automation

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
import dev.ranzlappen.gadget.feature.motion.MotionDetectedMetricSource
import dev.ranzlappen.gadget.feature.motion.R
import dev.ranzlappen.gadget.feature.motion.RotationRateMetricSource
import dev.ranzlappen.gadget.feature.motion.StepCounterMetricSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Motion's invocable-action surface for automation. Motion has no
 * actuator/controller of its own — its three sensors ([RotationRateMetricSource],
 * [StepCounterMetricSource], [MotionDetectedMetricSource]) are read-only, so
 * there is nothing to start/stop/arm. Rather than force a hollow "control"
 * action, this mirrors `AmbientActionHandler` (`:feature:ambient`, the
 * established pattern for a read-only sensor module): expose threshold
 * **assert** actions that sample the existing [dev.ranzlappen.gadget.core.model.MetricSource]s
 * directly, so a rule can gate on "is the device currently moving/still/at
 * step count N" without re-implementing the sensor read.
 */
@Singleton
class MotionActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rotationRate: RotationRateMetricSource,
    private val stepCounter: StepCounterMetricSource,
    private val motionDetected: MotionDetectedMetricSource,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_ASSERT_MOTION_DETECTED,
            label = context.getString(R.string.motion_action_assert_motion_detected),
        ),
        ModuleAction(
            key = ACTION_ASSERT_MOTION_IDLE,
            label = context.getString(R.string.motion_action_assert_motion_idle),
        ),
        ModuleAction(
            key = ACTION_ASSERT_STEPS_ABOVE,
            label = context.getString(R.string.motion_action_assert_steps_above),
            params = listOf(ActionParam(PARAM_THRESHOLD_STEPS, ActionParamType.Int, "1000", 0f, Float.MAX_VALUE)),
        ),
        ModuleAction(
            key = ACTION_ASSERT_ROTATION_ABOVE,
            label = context.getString(R.string.motion_action_assert_rotation_above),
            params = listOf(ActionParam(PARAM_THRESHOLD_RAD_S, ActionParamType.Float, "1.0", 0f, 10f)),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_ASSERT_MOTION_DETECTED -> {
                val detected = motionDetected.sample() > 0.5f
                if (detected) ActionResult.Success else ActionResult.Failure("no motion detected")
            }
            ACTION_ASSERT_MOTION_IDLE -> {
                val detected = motionDetected.sample() > 0.5f
                if (!detected) ActionResult.Success else ActionResult.Failure("motion detected")
            }
            ACTION_ASSERT_STEPS_ABOVE -> {
                val threshold = params[PARAM_THRESHOLD_STEPS]?.toFloatOrNull() ?: DEFAULT_STEPS_THRESHOLD
                val steps = stepCounter.sample()
                if (steps >= threshold) {
                    ActionResult.Success
                } else {
                    ActionResult.Failure("step count $steps is below threshold $threshold")
                }
            }
            ACTION_ASSERT_ROTATION_ABOVE -> {
                val threshold = params[PARAM_THRESHOLD_RAD_S]?.toFloatOrNull() ?: DEFAULT_ROTATION_THRESHOLD
                val rate = rotationRate.sample()
                if (rate >= threshold) {
                    ActionResult.Success
                } else {
                    ActionResult.Failure("rotation rate $rate rad/s is below threshold $threshold rad/s")
                }
            }
            else -> ActionResult.Unsupported
        }

    companion object {
        const val FEATURE_ID = "motion"
        const val ACTION_ASSERT_MOTION_DETECTED = "assert_motion_detected"
        const val ACTION_ASSERT_MOTION_IDLE = "assert_motion_idle"
        const val ACTION_ASSERT_STEPS_ABOVE = "assert_steps_above"
        const val ACTION_ASSERT_ROTATION_ABOVE = "assert_rotation_above"
        const val PARAM_THRESHOLD_STEPS = "threshold_steps"
        const val PARAM_THRESHOLD_RAD_S = "threshold_rad_s"
        const val DEFAULT_STEPS_THRESHOLD = 1_000f
        const val DEFAULT_ROTATION_THRESHOLD = 1.0f
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface MotionActionModule {

    @Binds
    @IntoMap
    @StringKey(MotionActionHandler.FEATURE_ID)
    fun bindMotionActionHandler(handler: MotionActionHandler): ActionHandler
}
