package dev.ranzlappen.gadget.feature.torch.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunction
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunctionBehavior
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.automation.TorchActionHandler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Torch's per-feature widget-function catalog — the list of functions a torch
 * widget can be bound to, surfaced in the customization dialog's function
 * picker and resolved by the generic provider on tap.
 *
 * Each [WidgetFunction] names a stable id (persisted as
 * [TorchWidgetConfig.actionKey]), a localized label, a param schema reused from
 * [TorchActionHandler], and a [WidgetFunctionBehavior] pairing the underlying
 * `:core:automation` action key(s):
 *
 * - **Flashlight** — a toggle pairing [TorchActionHandler.ACTION_TORCH_ON] /
 *   `..._OFF`, with live state from the `"torch:torch_power"`
 *   [dev.ranzlappen.gadget.core.widgetkit.function.WidgetStateSource].
 * - **Strobe** — a toggle pairing [TorchActionHandler.ACTION_STROBE_START] /
 *   `..._STOP` (param `rate_hz`), live state from `"torch:strobe_running"`.
 * - **Morse / SOS** — a momentary [TorchActionHandler.ACTION_MORSE] (params
 *   `text`, `rate_hz`); no live-state source needed for a one-shot.
 *
 * `@Inject`-constructable (resolves [R.string] labels from the
 * [ApplicationContext]); the provider's Hilt entry point returns it directly so
 * the tap path can map a stored `actionKey` back to its function via
 * [functionFor].
 */
@Singleton
class TorchWidgetFunctionCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val functions: List<WidgetFunction> = listOf(
        WidgetFunction(
            id = TorchWidgetConfig.FUNCTION_FLASHLIGHT,
            label = context.getString(R.string.torch_widget_function_flashlight),
            behavior = WidgetFunctionBehavior.Toggle(
                onActionKey = TorchActionHandler.ACTION_TORCH_ON,
                offActionKey = TorchActionHandler.ACTION_TORCH_OFF,
                stateKey = STATE_TORCH_POWER,
            ),
        ),
        WidgetFunction(
            id = TorchWidgetConfig.FUNCTION_STROBE,
            label = context.getString(R.string.torch_widget_function_strobe),
            params = listOf(
                ActionParam(TorchActionHandler.PARAM_RATE_HZ, ActionParamType.Float, "5", 1f, 20f),
            ),
            behavior = WidgetFunctionBehavior.Toggle(
                onActionKey = TorchActionHandler.ACTION_STROBE_START,
                offActionKey = TorchActionHandler.ACTION_STROBE_STOP,
                stateKey = STATE_STROBE_RUNNING,
            ),
        ),
        WidgetFunction(
            id = TorchWidgetConfig.FUNCTION_MORSE,
            label = context.getString(R.string.torch_widget_function_morse),
            params = listOf(
                ActionParam(TorchActionHandler.PARAM_TEXT, ActionParamType.Text, "SOS"),
                ActionParam(TorchActionHandler.PARAM_RATE_HZ, ActionParamType.Float, "5", 1f, 20f),
            ),
            behavior = WidgetFunctionBehavior.Momentary(TorchActionHandler.ACTION_MORSE),
        ),
    )

    /** Resolve the function bound to a stored [TorchWidgetConfig.actionKey], or
     *  `null` for an unknown / removed key (the provider then renders inert). */
    fun functionFor(actionKey: String): WidgetFunction? =
        functions.firstOrNull { it.id == actionKey }

    companion object {
        /** State-source key for the binary torch power toggle. Combined with
         *  the feature id yields the `"torch:torch_power"` multibinding key. */
        const val STATE_TORCH_POWER: String = "torch_power"

        /** State-source key for the running-strobe toggle. Combined with the
         *  feature id yields the `"torch:strobe_running"` multibinding key. */
        const val STATE_STROBE_RUNNING: String = "strobe_running"
    }
}
