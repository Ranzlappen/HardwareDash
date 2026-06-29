package dev.ranzlappen.gadget.feature.flipper.automation

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
import dev.ranzlappen.gadget.feature.flipper.FlipperConnectionManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Automation surface for the Flipper Zero bridge. Assert-connected and ping are
 * always available; the transmit actions hand a Flipper-native `.sub` / `.ir`
 * file body to the connected device.
 */
@Singleton
class FlipperActionHandler @Inject constructor(
    private val manager: FlipperConnectionManager,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(key = ACTION_ASSERT_CONNECTED, label = "Assert Flipper connected"),
        ModuleAction(key = ACTION_PING, label = "Ping the Flipper"),
        ModuleAction(
            key = ACTION_TRANSMIT_SUBGHZ,
            label = "Transmit a Sub-GHz .sub file",
            params = listOf(ActionParam(name = PARAM_FILE, type = ActionParamType.Text)),
        ),
        ModuleAction(
            key = ACTION_TRANSMIT_IR,
            label = "Transmit an IR .ir file",
            params = listOf(ActionParam(name = PARAM_FILE, type = ActionParamType.Text)),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult {
        return when (actionKey) {
            ACTION_ASSERT_CONNECTED -> {
                if (manager.state.value is FlipperConnectionManager.State.Connected) ActionResult.Success
                else ActionResult.Failure("No Flipper connected")
            }
            ACTION_PING -> {
                val system = manager.system ?: return ActionResult.Failure("No Flipper connected")
                if (runCatching { system.ping() }.getOrDefault(false)) ActionResult.Success
                else ActionResult.Failure("Flipper did not respond to ping")
            }
            ACTION_TRANSMIT_SUBGHZ -> {
                val body = params[PARAM_FILE]?.takeIf { it.isNotBlank() }
                    ?: return ActionResult.Failure("Missing .sub file body")
                val subGhz = manager.subGhz ?: return ActionResult.Failure("No Flipper connected")
                runCatching { subGhz.transmitSubFile(body) }.fold(
                    onSuccess = { ActionResult.Success },
                    onFailure = { ActionResult.Failure("Sub-GHz transmit failed: ${it.message}") },
                )
            }
            ACTION_TRANSMIT_IR -> {
                val body = params[PARAM_FILE]?.takeIf { it.isNotBlank() }
                    ?: return ActionResult.Failure("Missing .ir file body")
                val infrared = manager.infrared ?: return ActionResult.Failure("No Flipper connected")
                runCatching { infrared.transmitIrFile(body) }.fold(
                    onSuccess = { ActionResult.Success },
                    onFailure = { ActionResult.Failure("IR transmit failed: ${it.message}") },
                )
            }
            else -> ActionResult.Unsupported
        }
    }

    companion object {
        const val FEATURE_ID = "flipper"
        const val ACTION_ASSERT_CONNECTED = "flipper_assert_connected"
        const val ACTION_PING = "flipper_ping"
        const val ACTION_TRANSMIT_SUBGHZ = "flipper_transmit_subghz"
        const val ACTION_TRANSMIT_IR = "flipper_transmit_ir"
        const val PARAM_FILE = "file"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FlipperActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(FlipperActionHandler.FEATURE_ID)
    abstract fun bindFlipperActionHandler(impl: FlipperActionHandler): ActionHandler
}
