package dev.ranzlappen.gadget.feature.adbdebug.automation

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
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbDebuggingController
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbDebuggingControllerResult
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbNetworkConfig
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbNetworkPortRange
import dev.ranzlappen.gadget.feature.adbdebug.control.SetPropConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ADB Debugging's invocable-action surface for the automation engine. Wraps
 * the four privileged [AdbDebuggingController] methods (mirrors
 * `TorchActionHandler` / `VibrationActionHandler`'s shape).
 *
 * Every action carries `requiresRoot = true`: the standard flavor binds
 * `StandardAdbDebuggingController`, whose every method returns
 * [AdbDebuggingControllerResult.Unsupported] — there is no standard-tier
 * write path for `adb_enabled` / `service.adb.tcp.port` / allow-listed
 * `setprop` (the screen's standard tier is read-only + a Settings deep-link).
 * The rooted flavor binds `RootedAdbDebuggingController`, which routes every
 * call through `RootSafetyGate`.
 *
 * [AdbDebuggingController.resetAllAdbMutations] and
 * [AdbDebuggingController.revertOnScreenExit] are deliberately NOT exposed
 * here — the former is a screen-only convenience action, the latter is an
 * auto-revert lifecycle hook, and neither is a "invoke this from a rule"
 * primitive the way the four wrapped methods are.
 */
@Singleton
class AdbDebugActionHandler @Inject constructor(
    private val controller: AdbDebuggingController,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_TOGGLE_ADB_ENABLED,
            label = "Toggle ADB enabled",
            requiresRoot = true,
            params = listOf(ActionParam(PARAM_ENABLED, ActionParamType.Bool, "true")),
        ),
        ModuleAction(
            key = ACTION_TOGGLE_ADB_OVER_NETWORK,
            label = "Toggle ADB over network",
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_ENABLED, ActionParamType.Bool, "true"),
                ActionParam(
                    PARAM_PORT,
                    ActionParamType.Int,
                    AdbNetworkPortRange.DEFAULT.toString(),
                    AdbNetworkPortRange.MIN.toFloat(),
                    AdbNetworkPortRange.MAX.toFloat(),
                ),
            ),
        ),
        ModuleAction(
            key = ACTION_DUMP_PROPERTIES,
            label = "Dump getprop snapshot",
            requiresRoot = true,
            params = listOf(ActionParam(PARAM_PERSIST, ActionParamType.Bool, "false")),
        ),
        ModuleAction(
            key = ACTION_OVERRIDE_SETPROP,
            label = "Override allow-listed property",
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_KEY, ActionParamType.Text),
                ActionParam(PARAM_VALUE, ActionParamType.Text),
            ),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_TOGGLE_ADB_ENABLED -> controller.toggleAdbEnabled(
                enabled = params.boolOr(PARAM_ENABLED, true),
            ).toActionResult()
            ACTION_TOGGLE_ADB_OVER_NETWORK -> controller.toggleAdbOverNetwork(
                AdbNetworkConfig(
                    enabled = params.boolOr(PARAM_ENABLED, true),
                    port = params.intOr(PARAM_PORT, AdbNetworkPortRange.DEFAULT),
                ),
            ).toActionResult()
            ACTION_DUMP_PROPERTIES -> controller.dumpProperties(
                persist = params.boolOr(PARAM_PERSIST, false),
            ).toActionResult()
            ACTION_OVERRIDE_SETPROP -> {
                val key = params[PARAM_KEY]?.takeIf { it.isNotBlank() }
                if (key == null) {
                    ActionResult.Failure("missing required param: $PARAM_KEY")
                } else {
                    controller.overrideSystemProperty(
                        SetPropConfig(key = key, value = params[PARAM_VALUE].orEmpty()),
                    ).toActionResult()
                }
            }
            else -> ActionResult.Unsupported
        }

    private fun AdbDebuggingControllerResult.toActionResult(): ActionResult = when (this) {
        is AdbDebuggingControllerResult.Ok -> ActionResult.Success
        AdbDebuggingControllerResult.Unsupported -> ActionResult.Failure("requires the rooted app version")
        is AdbDebuggingControllerResult.RateLimited -> ActionResult.Failure("rate-limited; retry in ${retryAfterMillis}ms")
        AdbDebuggingControllerResult.OptedOut -> ActionResult.Failure("turned off in Settings")
        is AdbDebuggingControllerResult.HardwareError -> ActionResult.Failure(message)
        is AdbDebuggingControllerResult.ResetCompleted -> if (failed > 0) {
            ActionResult.Failure("restored $restored mutation(s), failed to restore $failed")
        } else {
            ActionResult.Success
        }
        is AdbDebuggingControllerResult.AdbToggleSnapshot -> ActionResult.Success
        is AdbDebuggingControllerResult.AdbNetworkSnapshot -> ActionResult.Success
        is AdbDebuggingControllerResult.PropertyDump -> ActionResult.Success
        is AdbDebuggingControllerResult.SetpropSnapshot -> ActionResult.Success
    }

    private fun Map<String, String>.boolOr(key: String, fallback: Boolean): Boolean =
        this[key]?.toBooleanStrictOrNull() ?: fallback

    private fun Map<String, String>.intOr(key: String, fallback: Int): Int =
        this[key]?.toIntOrNull() ?: fallback

    companion object {
        const val FEATURE_ID = "adbdebug"
        const val ACTION_TOGGLE_ADB_ENABLED = "toggle_adb_enabled"
        const val ACTION_TOGGLE_ADB_OVER_NETWORK = "toggle_adb_over_network"
        const val ACTION_DUMP_PROPERTIES = "dump_properties"
        const val ACTION_OVERRIDE_SETPROP = "override_setprop"
        const val PARAM_ENABLED = "enabled"
        const val PARAM_PORT = "port"
        const val PARAM_PERSIST = "persist"
        const val PARAM_KEY = "key"
        const val PARAM_VALUE = "value"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface AdbDebugActionModule {

    @Binds
    @IntoMap
    @StringKey(AdbDebugActionHandler.FEATURE_ID)
    fun bindAdbDebugActionHandler(handler: AdbDebugActionHandler): ActionHandler
}
