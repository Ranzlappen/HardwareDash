package dev.ranzlappen.gadget.feature.vibration.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunction
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunctionBehavior
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.automation.VibrationActionHandler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Vibration's [WidgetFunction] catalog — the widget-side projection of
 * [VibrationActionHandler.actions]. Most functions are **momentary** (a discrete
 * buzz / pattern play / rooted burst); the **continuous ("perma") vibrate** is a
 * [WidgetFunctionBehavior.Toggle] (start/stop) whose live on/off comes from the
 * `"vibration:vibration_running"` `WidgetStateSource` (bound in
 * [VibrationWidgetStateModule]).
 *
 * The generic customization sheet filters this list by flavor (a
 * `requiresRoot` function is dropped when root is unavailable), and the generic
 * provider dispatches the config's selected function through the
 * `WidgetFunctionDispatcher`. Param schemas mirror the action handler verbatim
 * so the auto-generated editors match what `dispatch` parses.
 *
 * Built once at injection — labels resolve eagerly off the application context.
 */
@Singleton
class VibrationWidgetFunctionCatalog @Inject constructor(
    @ApplicationContext context: Context,
) {

    val functions: List<WidgetFunction> = listOf(
        WidgetFunction(
            id = VibrationWidgetConfig.FUNCTION_ONESHOT,
            label = context.getString(R.string.vibration_widget_function_oneshot),
            params = listOf(
                ActionParam(VibrationActionHandler.PARAM_AMPLITUDE, ActionParamType.Int, "60", 1f, 100f),
                ActionParam(VibrationActionHandler.PARAM_DURATION_MS, ActionParamType.Int, "300", 10f, 5_000f),
            ),
            behavior = WidgetFunctionBehavior.Momentary(VibrationActionHandler.ACTION_ONESHOT),
        ),
        WidgetFunction(
            id = VibrationWidgetConfig.FUNCTION_CONTINUOUS,
            label = context.getString(R.string.vibration_widget_function_continuous),
            params = listOf(
                ActionParam(VibrationActionHandler.PARAM_AMPLITUDE, ActionParamType.Int, "60", 1f, 100f),
            ),
            behavior = WidgetFunctionBehavior.Toggle(
                onActionKey = VibrationActionHandler.ACTION_VIBRATE_CONTINUOUS,
                offActionKey = VibrationActionHandler.ACTION_STOP,
                stateKey = STATE_VIBRATION_RUNNING,
            ),
        ),
        WidgetFunction(
            id = VibrationWidgetConfig.FUNCTION_PATTERN,
            label = context.getString(R.string.vibration_widget_function_pattern),
            params = listOf(
                ActionParam(VibrationActionHandler.PARAM_PATTERN_ID, ActionParamType.Text),
            ),
            behavior = WidgetFunctionBehavior.Momentary(VibrationActionHandler.ACTION_PATTERN_PLAY),
        ),
        WidgetFunction(
            id = VibrationWidgetConfig.FUNCTION_PATTERN_TOGGLE,
            label = context.getString(R.string.vibration_widget_function_pattern_toggle),
            params = listOf(
                ActionParam(VibrationActionHandler.PARAM_PATTERN_ID, ActionParamType.Text),
                ActionParam(VibrationActionHandler.PARAM_LOOP, ActionParamType.Bool, "true"),
            ),
            behavior = WidgetFunctionBehavior.Toggle(
                onActionKey = VibrationActionHandler.ACTION_PATTERN_PLAY,
                offActionKey = VibrationActionHandler.ACTION_STOP,
                stateKey = STATE_VIBRATION_RUNNING,
            ),
        ),
        // ─── Rooted functions — filtered out by the VM on standard ──────────
        WidgetFunction(
            id = VibrationActionHandler.ACTION_EXTREME_AMPLITUDE,
            label = context.getString(R.string.vibration_widget_function_extreme_amplitude),
            requiresRoot = true,
            params = listOf(
                ActionParam(VibrationActionHandler.PARAM_AMPLITUDE, ActionParamType.Int, "100", 1f, 100f),
                ActionParam(VibrationActionHandler.PARAM_DURATION_MS, ActionParamType.Int, "1000", 10f, 3_000f),
            ),
            behavior = WidgetFunctionBehavior.Momentary(VibrationActionHandler.ACTION_EXTREME_AMPLITUDE),
        ),
        WidgetFunction(
            id = VibrationActionHandler.ACTION_DIRECT_PWM,
            label = context.getString(R.string.vibration_widget_function_direct_pwm),
            requiresRoot = true,
            params = listOf(
                ActionParam(VibrationActionHandler.PARAM_PWM_ON_MICROS, ActionParamType.Int, "8000", 100f, 1_000_000f),
                ActionParam(VibrationActionHandler.PARAM_PWM_OFF_MICROS, ActionParamType.Int, "12000", 5_000f, 1_000_000f),
                ActionParam(VibrationActionHandler.PARAM_PWM_PULSES, ActionParamType.Int, "20", 1f, 200f),
            ),
            behavior = WidgetFunctionBehavior.Momentary(VibrationActionHandler.ACTION_DIRECT_PWM),
        ),
        WidgetFunction(
            id = VibrationActionHandler.ACTION_SUSTAINED_RUMBLE,
            label = context.getString(R.string.vibration_widget_function_sustained_rumble),
            requiresRoot = true,
            params = listOf(
                ActionParam(VibrationActionHandler.PARAM_AMPLITUDE, ActionParamType.Int, "35", 1f, 100f),
                ActionParam(VibrationActionHandler.PARAM_DURATION_MS, ActionParamType.Int, "60000", 1_000f, 300_000f),
            ),
            behavior = WidgetFunctionBehavior.Momentary(VibrationActionHandler.ACTION_SUSTAINED_RUMBLE),
        ),
    )

    /** The function bound to [actionKey], or `null` for a removed/renamed key. */
    fun functionFor(actionKey: String): WidgetFunction? = functions.firstOrNull { it.id == actionKey }

    companion object {
        /** State-source key for the continuous ("perma") vibrate toggle.
         *  Combined with the feature id yields the `"vibration:vibration_running"`
         *  multibinding key bound in [VibrationWidgetStateModule]. */
        const val STATE_VIBRATION_RUNNING: String = "vibration_running"
    }
}
