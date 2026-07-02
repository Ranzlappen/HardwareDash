package dev.ranzlappen.gadget.feature.radios.wifi.rooted.automation

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
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.feature.radios.wifi.rooted.WifiRootCommands
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted automation surface for the Wi-Fi module. Bound under `wifi_root`
 * (distinct from the standard `:feature:radios-wifi` handler's `wifi` id) and
 * present only in the rooted flavor — the standard automation map simply lacks
 * this feature, so a standard build cannot invoke it.
 *
 * Every action clears [RootSafetyGate] for its own [RootFeatureKey] before
 * touching the radio, then shells out through [RootShell] (`rfkill` / `iw`).
 * Bounds — the 20 dBm TX-power ceiling and the channel allow-list — live in
 * the pure [WifiRootCommands] helper so they're enforced identically here and
 * in tests. All numeric arguments are parsed to `Int` before a command string
 * is built, so no rule text is ever interpolated into the shell.
 */
@Singleton
class RootedWifiActionHandler @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val shell: RootShell,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_RFKILL,
            label = "Block / unblock the Wi-Fi radio (rfkill)",
            requiresRoot = true,
            params = listOf(
                ActionParam(name = PARAM_BLOCKED, type = ActionParamType.Bool, default = "true"),
            ),
        ),
        ModuleAction(
            key = ACTION_TX_POWER,
            label = "Override Wi-Fi TX power (dBm, capped at 20)",
            requiresRoot = true,
            params = listOf(
                ActionParam(
                    name = PARAM_DBM,
                    type = ActionParamType.Int,
                    default = WifiRootCommands.MAX_TX_POWER_DBM.toString(),
                    min = WifiRootCommands.MIN_TX_POWER_DBM.toFloat(),
                    max = WifiRootCommands.MAX_TX_POWER_DBM.toFloat(),
                ),
            ),
        ),
        ModuleAction(
            key = ACTION_CHANNEL,
            label = "Lock onto a Wi-Fi channel",
            requiresRoot = true,
            params = listOf(
                ActionParam(name = PARAM_CHANNEL, type = ActionParamType.Int, default = "36"),
            ),
        ),
        ModuleAction(
            key = ACTION_PROBE_INJECTION,
            label = "Probe monitor / IBSS injection capability (read-only)",
            requiresRoot = true,
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_RFKILL -> runGated(RootFeatureKey.WifiRfkillToggle) {
                val blocked = params[PARAM_BLOCKED]?.toBooleanStrictOrNull() ?: true
                execExpectingSuccess(WifiRootCommands.rfkill(blocked), "rfkill")
            }
            ACTION_TX_POWER -> runGated(RootFeatureKey.WifiTxPowerOverride) {
                val dbm = params[PARAM_DBM]?.toIntOrNull()
                    ?: return@runGated ActionResult.Failure("Missing or non-numeric dBm")
                execExpectingSuccess(WifiRootCommands.setTxPower(dbm), "txpower")
            }
            ACTION_CHANNEL -> runGated(RootFeatureKey.WifiChannelOverride) {
                val channel = params[PARAM_CHANNEL]?.toIntOrNull()
                    ?: return@runGated ActionResult.Failure("Missing or non-numeric channel")
                if (!WifiRootCommands.isAllowedChannel(channel)) {
                    return@runGated ActionResult.Failure("Channel $channel not in regulatory allow-list")
                }
                execExpectingSuccess(WifiRootCommands.setChannel(channel), "channel")
            }
            ACTION_PROBE_INJECTION -> runGated(RootFeatureKey.WifiInjectionProbe) {
                val result = shell.exec(WifiRootCommands.INJECTION_PROBE_COMMAND, timeoutMillis = 5_000)
                if (result.isUnsupported) return@runGated ActionResult.Unsupported
                if (!result.isSuccess) {
                    return@runGated ActionResult.Failure("iw phy info failed")
                }
                val joined = result.stdout.joinToString("\n")
                if (WifiRootCommands.supportsMonitor(joined) || WifiRootCommands.supportsIbss(joined)) {
                    ActionResult.Success
                } else {
                    ActionResult.Failure("Adapter advertises neither monitor nor IBSS mode")
                }
            }
            else -> ActionResult.Unsupported
        }

    private suspend fun execExpectingSuccess(command: String, label: String): ActionResult {
        val result = shell.exec(command, timeoutMillis = 5_000)
        return when {
            result.isUnsupported -> ActionResult.Unsupported
            result.isSuccess -> ActionResult.Success
            else -> ActionResult.Failure("$label failed: ${result.stderr.firstOrNull().orEmpty()}")
        }
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> ActionResult,
    ): ActionResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is ActionResult.Success) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> ActionResult.Failure("Blocked by user preference")
        is RootGateDecision.BlockedByLimiter ->
            ActionResult.Failure("Rate limited; retry after ${gate.retryAfterMillis}ms")
        RootGateDecision.Unsupported -> ActionResult.Unsupported
    }

    companion object {
        const val FEATURE_ID = "wifi_root"
        const val ACTION_RFKILL = "wifi_root_rfkill"
        const val ACTION_TX_POWER = "wifi_root_tx_power"
        const val ACTION_CHANNEL = "wifi_root_channel"
        const val ACTION_PROBE_INJECTION = "wifi_root_probe_injection"
        const val PARAM_BLOCKED = "blocked"
        const val PARAM_DBM = "dbm"
        const val PARAM_CHANNEL = "channel"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RootedWifiActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(RootedWifiActionHandler.FEATURE_ID)
    abstract fun bindRootedWifiActionHandler(impl: RootedWifiActionHandler): ActionHandler
}
