package dev.ranzlappen.gadget.feature.torch.automation

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
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
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.TorchController
import dev.ranzlappen.gadget.feature.torch.strobe.StrobeService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Torch's invocable-action surface for the future automation tool — the
 * reference [ActionHandler]. Reuses the existing [TorchController] and
 * [StrobeService] rather than re-implementing hardware control (the fix
 * for the legacy Link engine, which bypassed feature controllers and
 * hardcoded each action inside `LinkService`).
 */
@Singleton
class TorchActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: TorchController,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(ACTION_TORCH_ON, context.getString(R.string.torch_action_turn_on)),
        ModuleAction(ACTION_TORCH_OFF, context.getString(R.string.torch_action_turn_off)),
        ModuleAction(
            key = ACTION_STROBE_START,
            label = context.getString(R.string.torch_action_strobe_start),
            params = listOf(ActionParam(PARAM_RATE_HZ, ActionParamType.Float, "5", 1f, 20f)),
        ),
        ModuleAction(ACTION_STROBE_STOP, context.getString(R.string.torch_action_strobe_stop)),
        ModuleAction(
            key = ACTION_MORSE,
            label = context.getString(R.string.torch_action_morse),
            params = listOf(
                ActionParam(PARAM_TEXT, ActionParamType.Text, StrobeService.DEFAULT_MORSE_TEXT),
                ActionParam(PARAM_RATE_HZ, ActionParamType.Float, "5", 1f, 20f),
            ),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_TORCH_ON -> { controller.setOn(true); ActionResult.Success }
            ACTION_TORCH_OFF -> { controller.setOn(false); ActionResult.Success }
            ACTION_STROBE_START -> { startStrobe(params, morse = null); ActionResult.Success }
            ACTION_STROBE_STOP -> { stopStrobe(); ActionResult.Success }
            ACTION_MORSE -> {
                val text = params[PARAM_TEXT]?.takeIf { it.isNotBlank() }
                    ?: StrobeService.DEFAULT_MORSE_TEXT
                startStrobe(params, morse = text)
                ActionResult.Success
            }
            else -> ActionResult.Unsupported
        }

    private fun startStrobe(params: Map<String, String>, morse: String?) {
        val rate = params[PARAM_RATE_HZ]?.toFloatOrNull() ?: DEFAULT_RATE_HZ
        val intent = Intent(context, StrobeService::class.java).apply {
            putExtra(StrobeService.EXTRA_RATE_HZ, rate)
            if (morse != null) putExtra(StrobeService.EXTRA_MORSE_TEXT, morse)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopStrobe() {
        context.startService(
            Intent(context, StrobeService::class.java).setAction(StrobeService.ACTION_STOP),
        )
    }

    companion object {
        const val FEATURE_ID = "torch"
        const val ACTION_TORCH_ON = "torch_on"
        const val ACTION_TORCH_OFF = "torch_off"
        const val ACTION_STROBE_START = "strobe_start"
        const val ACTION_STROBE_STOP = "strobe_stop"
        const val ACTION_MORSE = "morse"
        const val PARAM_RATE_HZ = "rate_hz"
        const val PARAM_TEXT = "text"
        const val DEFAULT_RATE_HZ = 5f
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface TorchActionModule {

    @Binds
    @IntoMap
    @StringKey(TorchActionHandler.FEATURE_ID)
    fun bindTorchActionHandler(handler: TorchActionHandler): ActionHandler
}
