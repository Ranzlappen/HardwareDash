package dev.ranzlappen.gadget.feature.battery.automation

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
import dev.ranzlappen.gadget.feature.battery.R
import dev.ranzlappen.gadget.feature.battery.control.BatteryController
import dev.ranzlappen.gadget.feature.battery.control.BatteryControllerResult
import dev.ranzlappen.gadget.feature.battery.control.ChargingProfileConfig
import dev.ranzlappen.gadget.feature.battery.control.ChargingTypeOverrideConfig
import dev.ranzlappen.gadget.feature.battery.control.HoldSocConfig
import dev.ranzlappen.gadget.feature.battery.control.ThermalBypassConfig
import dev.ranzlappen.gadget.feature.battery.control.WirelessCoilCurrentConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Battery's invocable-action surface for the automation engine. Reuses the
 * existing [BatteryController] rather than re-implementing sysfs control.
 *
 * Baseline battery telemetry (level / temperature / voltage) is read-only on
 * standard Android — there is nothing to invoke there, so it is covered by
 * `BatteryMetricSources` only. Every action here routes through
 * [BatteryController], which is documented as a "rooted-only Battery
 * capability surface": the standard flavor binds `StandardBatteryController`
 * (every mutating method returns [BatteryControllerResult.Unsupported]) while
 * the rooted flavor binds `RootedBatteryController`. All actions therefore
 * carry `requiresRoot = true`, mirroring `VibrationActionHandler`'s
 * `VibrationRootCapabilities`-backed actions, so the rule builder can gate
 * them and a standard-build dispatch maps cleanly onto
 * [ActionResult.Failure].
 *
 * [BatteryController.fuelGaugeRaw] and [BatteryController.cellMonitor] are
 * deliberately NOT exposed as actions: they are pure reads with no
 * persisted side effect, so triggering them from automation would have
 * nowhere to deliver the result ([ActionResult] carries no data payload).
 * [BatteryController.fullDump] and [BatteryController.batteryHealthDeepDump]
 * *are* exposed because both persist a JSON snapshot to the Logbook
 * directory — invoking them has an observable effect even though
 * `dispatch()` itself only reports success/failure.
 */
@Singleton
class BatteryActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: BatteryController,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_CHARGING_PROFILE_OVERRIDE,
            label = context.getString(R.string.battery_action_charging_profile_override),
            requiresRoot = true,
            params = listOf(
                // Blank means "leave this node untouched" — mirrors the
                // nullable fields on ChargingProfileConfig.
                ActionParam(PARAM_MAX_CURRENT_UA, ActionParamType.Int),
                ActionParam(PARAM_MAX_VOLTAGE_UV, ActionParamType.Int),
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "30000", 1_000f, 30_000f),
            ),
        ),
        ModuleAction(
            key = ACTION_THERMAL_BYPASS,
            label = context.getString(R.string.battery_action_thermal_bypass),
            requiresRoot = true,
            params = listOf(ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "60000", 1_000f, 60_000f)),
        ),
        ModuleAction(
            key = ACTION_CHARGING_TYPE_OVERRIDE,
            label = context.getString(R.string.battery_action_charging_type_override),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_TYPE, ActionParamType.Text, DEFAULT_CHARGING_TYPE),
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "30000", 1_000f, 30_000f),
            ),
        ),
        ModuleAction(
            key = ACTION_FULL_DUMP,
            label = context.getString(R.string.battery_action_full_dump),
            requiresRoot = true,
        ),
        ModuleAction(
            key = ACTION_RESET_MUTATIONS,
            label = context.getString(R.string.battery_action_reset_mutations),
            requiresRoot = true,
        ),
        ModuleAction(
            key = ACTION_HOLD_SOC,
            label = context.getString(R.string.battery_action_hold_soc),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_TARGET_SOC_PERCENT, ActionParamType.Int, "80", 20f, 90f),
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "60000", 1_000f, 600_000f),
            ),
        ),
        ModuleAction(
            key = ACTION_WIRELESS_COIL_CURRENT,
            label = context.getString(R.string.battery_action_wireless_coil_current),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_MAX_CURRENT_UA, ActionParamType.Int, "1000000", 0f, 1_500_000f),
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "10000", 1_000f, 30_000f),
            ),
        ),
        ModuleAction(
            key = ACTION_HEALTH_SNAPSHOT,
            label = context.getString(R.string.battery_action_health_snapshot),
            requiresRoot = true,
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_CHARGING_PROFILE_OVERRIDE -> controller.chargingProfile(
                ChargingProfileConfig(
                    maxCurrentMicroAmps = params.longOrNull(PARAM_MAX_CURRENT_UA),
                    maxVoltageMicroVolts = params.longOrNull(PARAM_MAX_VOLTAGE_UV),
                    durationMillis = params.longOr(PARAM_DURATION_MS, 30_000),
                ),
            ).toActionResult()
            ACTION_THERMAL_BYPASS -> controller.thermalBypass(
                ThermalBypassConfig(durationMillis = params.longOr(PARAM_DURATION_MS, 60_000)),
            ).toActionResult()
            ACTION_CHARGING_TYPE_OVERRIDE -> controller.chargingTypeOverride(
                ChargingTypeOverrideConfig(
                    type = params[PARAM_TYPE]?.takeIf { it.isNotBlank() } ?: DEFAULT_CHARGING_TYPE,
                    durationMillis = params.longOr(PARAM_DURATION_MS, 30_000),
                ),
            ).toActionResult()
            ACTION_FULL_DUMP -> controller.fullDump().toActionResult()
            ACTION_RESET_MUTATIONS -> controller.resetAllBatteryMutations().toActionResult()
            ACTION_HOLD_SOC -> controller.holdStateOfCharge(
                HoldSocConfig(
                    targetSocPercent = params.intOr(PARAM_TARGET_SOC_PERCENT, 80),
                    durationMillis = params.longOr(PARAM_DURATION_MS, 60_000),
                ),
            ).toActionResult()
            ACTION_WIRELESS_COIL_CURRENT -> controller.wirelessCoilCurrent(
                WirelessCoilCurrentConfig(
                    maxCurrentMicroAmps = params.longOr(PARAM_MAX_CURRENT_UA, 1_000_000),
                    durationMillis = params.longOr(PARAM_DURATION_MS, 10_000),
                ),
            ).toActionResult()
            ACTION_HEALTH_SNAPSHOT -> controller.batteryHealthDeepDump().toActionResult()
            else -> ActionResult.Unsupported
        }

    private fun BatteryControllerResult.toActionResult(): ActionResult = when (this) {
        is BatteryControllerResult.Ok -> ActionResult.Success
        BatteryControllerResult.Unsupported -> ActionResult.Failure("requires the rooted app version")
        is BatteryControllerResult.RateLimited -> ActionResult.Failure("rate-limited; retry in ${retryAfterMillis}ms")
        BatteryControllerResult.OptedOut -> ActionResult.Failure("turned off in Settings")
        is BatteryControllerResult.HardwareError -> ActionResult.Failure(message)
        is BatteryControllerResult.ResetCompleted -> if (failed > 0) {
            ActionResult.Failure("restored $restored mutation(s), failed to restore $failed")
        } else {
            ActionResult.Success
        }
        is BatteryControllerResult.FuelGaugeReading -> ActionResult.Success
        is BatteryControllerResult.CellSnapshot -> ActionResult.Success
        is BatteryControllerResult.DumpWritten -> ActionResult.Success
        is BatteryControllerResult.DangerousAborted -> ActionResult.Failure(reason)
        is BatteryControllerResult.HoldSocSnapshot -> ActionResult.Success
        is BatteryControllerResult.BatteryHealthReading -> ActionResult.Success
        is BatteryControllerResult.WirelessCoilSnapshot -> ActionResult.Success
    }

    private fun Map<String, String>.intOr(key: String, fallback: Int): Int =
        this[key]?.toIntOrNull() ?: fallback

    private fun Map<String, String>.longOr(key: String, fallback: Long): Long =
        this[key]?.toLongOrNull() ?: fallback

    private fun Map<String, String>.longOrNull(key: String): Long? =
        this[key]?.toLongOrNull()

    companion object {
        const val FEATURE_ID = "battery"
        const val ACTION_CHARGING_PROFILE_OVERRIDE = "charging_profile_override"
        const val ACTION_THERMAL_BYPASS = "thermal_bypass"
        const val ACTION_CHARGING_TYPE_OVERRIDE = "charging_type_override"
        const val ACTION_FULL_DUMP = "full_dump"
        const val ACTION_RESET_MUTATIONS = "reset_mutations"
        const val ACTION_HOLD_SOC = "hold_soc"
        const val ACTION_WIRELESS_COIL_CURRENT = "wireless_coil_current"
        const val ACTION_HEALTH_SNAPSHOT = "health_snapshot"
        const val PARAM_MAX_CURRENT_UA = "max_current_ua"
        const val PARAM_MAX_VOLTAGE_UV = "max_voltage_uv"
        const val PARAM_DURATION_MS = "duration_ms"
        const val PARAM_TYPE = "type"
        const val PARAM_TARGET_SOC_PERCENT = "target_soc_percent"
        const val DEFAULT_CHARGING_TYPE = "DCP"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface BatteryActionModule {

    @Binds
    @IntoMap
    @StringKey(BatteryActionHandler.FEATURE_ID)
    fun bindBatteryActionHandler(handler: BatteryActionHandler): ActionHandler
}
