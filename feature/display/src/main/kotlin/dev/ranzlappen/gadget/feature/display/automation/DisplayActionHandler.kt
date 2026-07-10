package dev.ranzlappen.gadget.feature.display.automation

import android.content.Context
import android.provider.Settings
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
import dev.ranzlappen.gadget.feature.display.DisplayState
import dev.ranzlappen.gadget.feature.display.R
import dev.ranzlappen.gadget.feature.display.control.BrightnessOverrideConfig
import dev.ranzlappen.gadget.feature.display.control.DensityOverrideConfig
import dev.ranzlappen.gadget.feature.display.control.DisplayController
import dev.ranzlappen.gadget.feature.display.control.DisplayControllerResult
import dev.ranzlappen.gadget.feature.display.control.RefreshRateOverrideConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Display's invocable-action surface for the automation engine. Reuses the
 * already flavor-bound [DisplayController] for every privileged op — the
 * controller itself resolves to [dev.ranzlappen.gadget.feature.display.control.StandardDisplayController]
 * (everything [DisplayControllerResult.Unsupported]) or
 * `RootedDisplayController` per flavor, so this handler never branches on
 * root itself, only tags the privileged actions `requiresRoot = true` for
 * the rule builder.
 *
 * [ACTION_SET_BRIGHTNESS] is the one standard-safe action: it writes
 * `Settings.System.SCREEN_BRIGHTNESS` directly (the same WRITE_SETTINGS
 * path the screen's slider uses) rather than going through the controller,
 * since normal brightness control needs no root on Android.
 */
@Singleton
class DisplayActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: DisplayController,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_SET_BRIGHTNESS,
            label = context.getString(R.string.display_action_set_brightness),
            params = listOf(ActionParam(PARAM_PERCENT, ActionParamType.Int, "50", 0f, 100f)),
        ),
        ModuleAction(
            key = ACTION_OVERRIDE_BRIGHTNESS_EXTREME,
            label = context.getString(R.string.display_action_override_brightness_extreme),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_PERCENT, ActionParamType.Int, "130", 0f, 130f),
                ActionParam(PARAM_ACTIVE_WINDOW_MILLIS, ActionParamType.Int, "60000", 1_000f, 60_000f),
            ),
        ),
        ModuleAction(
            key = ACTION_OVERRIDE_REFRESH_RATE,
            label = context.getString(R.string.display_action_override_refresh_rate),
            requiresRoot = true,
            params = listOf(ActionParam(PARAM_TARGET_MODE_ID, ActionParamType.Int, "0", 0f, 100f)),
        ),
        ModuleAction(
            key = ACTION_OVERRIDE_DENSITY,
            label = context.getString(R.string.display_action_override_density),
            requiresRoot = true,
            params = listOf(
                ActionParam(
                    PARAM_DPI,
                    ActionParamType.Int,
                    DisplayState.DEFAULT_DENSITY_DPI.toString(),
                    DisplayState.MIN_DENSITY_DPI.toFloat(),
                    DisplayState.MAX_DENSITY_DPI.toFloat(),
                ),
            ),
        ),
        ModuleAction(
            key = ACTION_RESET_ALL,
            label = context.getString(R.string.display_action_reset_all),
            requiresRoot = true,
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_SET_BRIGHTNESS -> setBrightness(params.intOr(PARAM_PERCENT, 50))
            ACTION_OVERRIDE_BRIGHTNESS_EXTREME -> controller.overrideBrightness(
                BrightnessOverrideConfig(
                    percent = params.intOr(PARAM_PERCENT, 130),
                    activeWindowMillis = params.longOr(PARAM_ACTIVE_WINDOW_MILLIS, 60_000L),
                ),
            ).toActionResult()
            ACTION_OVERRIDE_REFRESH_RATE -> controller.overrideRefreshRate(
                RefreshRateOverrideConfig(targetModeId = params.intOr(PARAM_TARGET_MODE_ID, 0)),
            ).toActionResult()
            ACTION_OVERRIDE_DENSITY -> controller.overrideDensity(
                DensityOverrideConfig(
                    dpi = params.intOr(PARAM_DPI, DisplayState.DEFAULT_DENSITY_DPI)
                        .coerceIn(DisplayState.MIN_DENSITY_DPI, DisplayState.MAX_DENSITY_DPI),
                ),
            ).toActionResult()
            ACTION_RESET_ALL -> controller.resetAllDisplayMutations().toActionResult()
            else -> ActionResult.Unsupported
        }

    private fun setBrightness(percent: Int): ActionResult {
        if (!Settings.System.canWrite(context)) {
            return ActionResult.Failure(context.getString(R.string.display_status_write_settings_denied))
        }
        val clamped = percent.coerceIn(0, 100)
        val raw = (clamped * 255 / 100).coerceIn(0, 255)
        return runCatching {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, raw)
        }.fold(
            onSuccess = { ActionResult.Success },
            onFailure = { ActionResult.Failure(context.getString(R.string.display_status_write_settings_denied)) },
        )
    }

    private fun DisplayControllerResult.toActionResult(): ActionResult = when (this) {
        is DisplayControllerResult.Ok -> ActionResult.Success
        DisplayControllerResult.Unsupported ->
            ActionResult.Failure(context.getString(R.string.display_status_unsupported))
        is DisplayControllerResult.RateLimited ->
            ActionResult.Failure(context.getString(R.string.display_status_rate_limited, retryAfterMillis))
        DisplayControllerResult.OptedOut ->
            ActionResult.Failure(context.getString(R.string.display_status_opted_out))
        is DisplayControllerResult.HardwareError -> ActionResult.Failure(message)
        is DisplayControllerResult.ResetCompleted -> ActionResult.Success
        is DisplayControllerResult.BrightnessSnapshot -> ActionResult.Success
        is DisplayControllerResult.RefreshRateSnapshot -> ActionResult.Success
        is DisplayControllerResult.DensitySnapshot -> ActionResult.Success
        is DisplayControllerResult.SurfaceFlingerExcerpt -> ActionResult.Success
    }

    private fun Map<String, String>.intOr(key: String, fallback: Int): Int =
        this[key]?.toIntOrNull() ?: fallback

    private fun Map<String, String>.longOr(key: String, fallback: Long): Long =
        this[key]?.toLongOrNull() ?: fallback

    companion object {
        const val FEATURE_ID = "display"
        const val ACTION_SET_BRIGHTNESS = "display_set_brightness"
        const val ACTION_OVERRIDE_BRIGHTNESS_EXTREME = "display_override_brightness_extreme"
        const val ACTION_OVERRIDE_REFRESH_RATE = "display_override_refresh_rate"
        const val ACTION_OVERRIDE_DENSITY = "display_override_density"
        const val ACTION_RESET_ALL = "display_reset_all"
        const val PARAM_PERCENT = "percent"
        const val PARAM_ACTIVE_WINDOW_MILLIS = "active_window_millis"
        const val PARAM_TARGET_MODE_ID = "target_mode_id"
        const val PARAM_DPI = "dpi"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface DisplayActionModule {

    @Binds
    @IntoMap
    @StringKey(DisplayActionHandler.FEATURE_ID)
    fun bindDisplayActionHandler(handler: DisplayActionHandler): ActionHandler
}
