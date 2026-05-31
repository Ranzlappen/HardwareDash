package dev.ranzlappen.gadget.feature.vibration.automation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.feature.vibration.PwmPulse
import dev.ranzlappen.gadget.feature.vibration.VibrationController
import dev.ranzlappen.gadget.feature.vibration.VibrationRootCapabilities
import dev.ranzlappen.gadget.feature.vibration.VibrationRootResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Vibration's invocable-action surface for the future automation tool. Reuses
 * the existing [VibrationController] (standard tier) and
 * [VibrationRootCapabilities] (extreme tier) rather than re-implementing
 * hardware control. The four rooted actions carry `requiresRoot = true` so the
 * rule builder can gate them; they map a rooted `Unsupported`/`OptedOut`
 * outcome onto [ActionResult.Failure] with a readable reason.
 */
@Singleton
class VibrationActionHandler @Inject constructor(
    private val controller: VibrationController,
    private val rootCapabilities: VibrationRootCapabilities,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_ONESHOT,
            label = "One-shot vibrate",
            params = listOf(
                ActionParam(PARAM_AMPLITUDE, ActionParamType.Int, "60", 1f, 100f),
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "300", 10f, 5_000f),
            ),
        ),
        ModuleAction(ACTION_STOP, "Stop vibration"),
        ModuleAction(
            key = ACTION_EXTREME_AMPLITUDE,
            label = "Extreme amplitude (rooted)",
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_AMPLITUDE, ActionParamType.Int, "100", 1f, 100f),
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "1000", 10f, 3_000f),
            ),
        ),
        ModuleAction(
            key = ACTION_DIRECT_PWM,
            label = "Direct PWM burst (rooted)",
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_PWM_ON_MICROS, ActionParamType.Int, "8000", 100f, 1_000_000f),
                ActionParam(PARAM_PWM_OFF_MICROS, ActionParamType.Int, "12000", 5_000f, 1_000_000f),
                ActionParam(PARAM_PWM_PULSES, ActionParamType.Int, "20", 1f, 200f),
            ),
        ),
        ModuleAction(
            key = ACTION_SUSTAINED_RUMBLE,
            label = "Sustained rumble (rooted)",
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_AMPLITUDE, ActionParamType.Int, "35", 1f, 100f),
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "60000", 1_000f, 300_000f),
            ),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_ONESHOT -> {
                controller.oneShot(
                    amplitudePercent = params.intOr(PARAM_AMPLITUDE, 60),
                    durationMillis = params.longOr(PARAM_DURATION_MS, 300),
                )
                ActionResult.Success
            }
            ACTION_STOP -> { controller.stop(); ActionResult.Success }
            ACTION_EXTREME_AMPLITUDE -> rootCapabilities.extremeAmplitude(
                amplitudePercent = params.intOr(PARAM_AMPLITUDE, 100),
                durationMillis = params.longOr(PARAM_DURATION_MS, 1_000),
            ).toActionResult()
            ACTION_DIRECT_PWM -> {
                val on = params.longOr(PARAM_PWM_ON_MICROS, 8_000)
                val off = params.longOr(PARAM_PWM_OFF_MICROS, 12_000)
                val pulses = params.intOr(PARAM_PWM_PULSES, 20)
                rootCapabilities.directPwm(List(pulses) { PwmPulse(on, off) }).toActionResult()
            }
            ACTION_SUSTAINED_RUMBLE -> rootCapabilities.sustainedRumble(
                durationMillis = params.longOr(PARAM_DURATION_MS, 60_000),
                amplitudePercent = params.intOr(PARAM_AMPLITUDE, 35),
            ).toActionResult()
            else -> ActionResult.Unsupported
        }

    private fun VibrationRootResult.toActionResult(): ActionResult = when (this) {
        VibrationRootResult.Ok -> ActionResult.Success
        VibrationRootResult.Unsupported -> ActionResult.Failure("requires the rooted app version")
        VibrationRootResult.OptedOut -> ActionResult.Failure("turned off in Settings")
        is VibrationRootResult.RateLimited -> ActionResult.Failure("rate-limited; retry in ${retryAfterMillis}ms")
        is VibrationRootResult.Error -> ActionResult.Failure(message)
    }

    private fun Map<String, String>.intOr(key: String, fallback: Int): Int =
        this[key]?.toIntOrNull() ?: fallback

    private fun Map<String, String>.longOr(key: String, fallback: Long): Long =
        this[key]?.toLongOrNull() ?: fallback

    companion object {
        const val FEATURE_ID = "vibration"
        const val ACTION_ONESHOT = "oneshot"
        const val ACTION_STOP = "stop"
        const val ACTION_EXTREME_AMPLITUDE = "extreme_amplitude"
        const val ACTION_DIRECT_PWM = "direct_pwm"
        const val ACTION_SUSTAINED_RUMBLE = "sustained_rumble"
        const val PARAM_AMPLITUDE = "amplitude"
        const val PARAM_DURATION_MS = "duration_ms"
        const val PARAM_PWM_ON_MICROS = "pwm_on_micros"
        const val PARAM_PWM_OFF_MICROS = "pwm_off_micros"
        const val PARAM_PWM_PULSES = "pwm_pulses"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface VibrationActionModule {

    @Binds
    @IntoMap
    @StringKey(VibrationActionHandler.FEATURE_ID)
    fun bindVibrationActionHandler(handler: VibrationActionHandler): ActionHandler
}
