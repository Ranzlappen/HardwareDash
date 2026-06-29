package dev.ranzlappen.gadget.feature.bugreport.rooted.automation

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
import dev.ranzlappen.gadget.feature.bugreport.rooted.PermissionGrantResult
import dev.ranzlappen.gadget.feature.bugreport.rooted.RootedPermissionGranter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted automation surface for the Health permission manager. Bound under
 * `bugreport_root` (distinct from any standard handler) and present only in the
 * rooted flavor, so a standard build cannot invoke it.
 */
@Singleton
class RootedBugReportActionHandler @Inject constructor(
    private val granter: RootedPermissionGranter,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_FORCE_GRANT,
            label = "Force-grant a permission (root)",
            requiresRoot = true,
            params = listOf(ActionParam(name = PARAM_PERMISSION, type = ActionParamType.Text)),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_FORCE_GRANT -> {
                val permission = params[PARAM_PERMISSION].orEmpty()
                granter.forceGrant(permission).toActionResult()
            }
            else -> ActionResult.Unsupported
        }

    private fun PermissionGrantResult.toActionResult(): ActionResult = when (this) {
        PermissionGrantResult.Ok -> ActionResult.Success
        PermissionGrantResult.InvalidPermission -> ActionResult.Failure("Invalid permission name")
        PermissionGrantResult.OptedOut -> ActionResult.Failure("Blocked by user safety preference")
        is PermissionGrantResult.RateLimited ->
            ActionResult.Failure("Rate limited; retry after ${retryAfterMillis}ms")
        PermissionGrantResult.Unsupported -> ActionResult.Unsupported
        is PermissionGrantResult.Error -> ActionResult.Failure("pm grant failed: $reason")
    }

    companion object {
        const val FEATURE_ID = "bugreport_root"
        const val ACTION_FORCE_GRANT = "bugreport_root_force_grant"
        const val PARAM_PERMISSION = "permission"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RootedBugReportActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(RootedBugReportActionHandler.FEATURE_ID)
    abstract fun bindRootedBugReportActionHandler(impl: RootedBugReportActionHandler): ActionHandler
}
