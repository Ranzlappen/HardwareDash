package dev.ranzlappen.gadget.feature.lock.rooted.automation

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
import dev.ranzlappen.gadget.feature.lock.rooted.LockOverlayCommands
import dev.ranzlappen.gadget.feature.lock.rooted.LockOverlayResult
import dev.ranzlappen.gadget.feature.lock.rooted.RootedLockOverlayController
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted automation surface for the lock module. Bound under `lock_root`
 * (distinct from the standard `:feature:lock` handler's `lock` id), and only
 * present in the rooted flavor — the standard automation map simply lacks this
 * feature, so a standard build cannot invoke it.
 */
@Singleton
class RootedLockActionHandler @Inject constructor(
    private val overlay: RootedLockOverlayController,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_SHOW_SECURE_OVERLAY,
            label = "Show secure lock overlay",
            requiresRoot = true,
            params = listOf(
                ActionParam(name = PARAM_MESSAGE, type = ActionParamType.Text, default = DEFAULT_MESSAGE),
                ActionParam(
                    name = PARAM_DURATION_MILLIS,
                    type = ActionParamType.Int,
                    default = LockOverlayCommands.DEFAULT_DURATION_MILLIS.toString(),
                    min = LockOverlayCommands.MIN_DURATION_MILLIS.toFloat(),
                    max = LockOverlayCommands.HARD_CEILING_MILLIS.toFloat(),
                ),
            ),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_SHOW_SECURE_OVERLAY -> {
                val message = params[PARAM_MESSAGE]?.takeIf { it.isNotBlank() } ?: DEFAULT_MESSAGE
                val duration = params[PARAM_DURATION_MILLIS]?.toLongOrNull()
                    ?: LockOverlayCommands.DEFAULT_DURATION_MILLIS
                overlay.showSecureOverlay(message, duration).toActionResult()
            }
            else -> ActionResult.Unsupported
        }

    private fun LockOverlayResult.toActionResult(): ActionResult = when (this) {
        LockOverlayResult.Ok -> ActionResult.Success
        LockOverlayResult.OptedOut -> ActionResult.Failure("Blocked by user safety preference")
        is LockOverlayResult.RateLimited ->
            ActionResult.Failure("Rate limited; retry after ${retryAfterMillis}ms")
        LockOverlayResult.Unsupported -> ActionResult.Unsupported
        is LockOverlayResult.Error -> ActionResult.Failure("Overlay refused: $reason")
    }

    companion object {
        const val FEATURE_ID = "lock_root"
        const val ACTION_SHOW_SECURE_OVERLAY = "lock_root_show_secure_overlay"
        const val PARAM_MESSAGE = "message"
        const val PARAM_DURATION_MILLIS = "duration_ms"
        const val DEFAULT_MESSAGE = "Device locked"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RootedLockActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(RootedLockActionHandler.FEATURE_ID)
    abstract fun bindRootedLockActionHandler(impl: RootedLockActionHandler): ActionHandler
}
