package dev.ranzlappen.gadget.feature.apps.rooted.automation

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
import dev.ranzlappen.gadget.feature.apps.rooted.R
import dev.ranzlappen.gadget.feature.apps.root.AppsRootController
import dev.ranzlappen.gadget.feature.apps.root.AppsRootControllerResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-Organizer's rooted invocable-action surface for the automation
 * engine. Mirrors `TorchActionHandler`/`VibrationActionHandler`/
 * `AdbDebugActionHandler`'s shape: a constructor-injected controller
 * interface, one [ModuleAction] per privileged operation (all
 * `requiresRoot = true`), and a `toActionResult()` mapper from the
 * controller's sealed result to [ActionResult].
 *
 * All three actions take the same [PARAM_PACKAGE_NAME] text param. The
 * injected [AppsRootController] (bound to `RootedAppsRootController` on the
 * rooted flavor, `StandardAppsRootController` on standard) owns the
 * deny-list + [dev.ranzlappen.gadget.core.root.RootSafetyGate] check, so a
 * rule can never freeze/force-stop a protected system package — this
 * handler only translates the result.
 */
@Singleton
class AppsRootActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: AppsRootController,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_FREEZE,
            label = context.getString(R.string.apps_root_action_freeze),
            requiresRoot = true,
            params = listOf(ActionParam(PARAM_PACKAGE_NAME, ActionParamType.Text)),
        ),
        ModuleAction(
            key = ACTION_UNFREEZE,
            label = context.getString(R.string.apps_root_action_unfreeze),
            requiresRoot = true,
            params = listOf(ActionParam(PARAM_PACKAGE_NAME, ActionParamType.Text)),
        ),
        ModuleAction(
            key = ACTION_FORCE_STOP,
            label = context.getString(R.string.apps_root_action_force_stop),
            requiresRoot = true,
            params = listOf(ActionParam(PARAM_PACKAGE_NAME, ActionParamType.Text)),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult {
        val packageName = params[PARAM_PACKAGE_NAME]?.takeIf { it.isNotBlank() }
            ?: return ActionResult.Failure("missing required param: $PARAM_PACKAGE_NAME")
        return when (actionKey) {
            ACTION_FREEZE -> controller.freezeApp(packageName).toActionResult()
            ACTION_UNFREEZE -> controller.unfreezeApp(packageName).toActionResult()
            ACTION_FORCE_STOP -> controller.forceStopApp(packageName).toActionResult()
            else -> ActionResult.Unsupported
        }
    }

    private fun AppsRootControllerResult.toActionResult(): ActionResult = when (this) {
        is AppsRootControllerResult.Ok -> ActionResult.Success
        AppsRootControllerResult.Unsupported -> ActionResult.Failure("requires the rooted app version")
        AppsRootControllerResult.OptedOut -> ActionResult.Failure("turned off in Settings")
        is AppsRootControllerResult.Denied -> ActionResult.Failure(message)
        is AppsRootControllerResult.RateLimited -> ActionResult.Failure("rate-limited; retry in ${retryAfterMillis}ms")
        is AppsRootControllerResult.HardwareError -> ActionResult.Failure(message)
    }

    companion object {
        const val FEATURE_ID = "apps_root"
        const val ACTION_FREEZE = "apps_root_freeze"
        const val ACTION_UNFREEZE = "apps_root_unfreeze"
        const val ACTION_FORCE_STOP = "apps_root_force_stop"
        const val PARAM_PACKAGE_NAME = "package_name"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface AppsRootActionModule {

    @Binds
    @IntoMap
    @StringKey(AppsRootActionHandler.FEATURE_ID)
    fun bindAppsRootActionHandler(handler: AppsRootActionHandler): ActionHandler
}
