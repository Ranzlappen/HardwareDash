package dev.ranzlappen.gadget.feature.flipper.rooted.automation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.feature.flipper.rooted.FlipperRootResult
import dev.ranzlappen.gadget.feature.flipper.rooted.RootedFlipperController
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted automation surface for the Flipper bridge. Bound under `flipper_root`
 * (distinct from the standard `flipper` handler) and present only in the rooted
 * flavor, so a standard build cannot invoke it.
 */
@Singleton
class RootedFlipperActionHandler @Inject constructor(
    private val controller: RootedFlipperController,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_GRANT_USB,
            label = "Grant Flipper USB access (root)",
            requiresRoot = true,
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_GRANT_USB -> controller.grantUsbAccess().toActionResult()
            else -> ActionResult.Unsupported
        }

    private fun FlipperRootResult.toActionResult(): ActionResult = when (this) {
        FlipperRootResult.Ok -> ActionResult.Success
        FlipperRootResult.NoDevice -> ActionResult.Failure("No Flipper attached over USB")
        FlipperRootResult.OptedOut -> ActionResult.Failure("Blocked by user safety preference")
        is FlipperRootResult.RateLimited ->
            ActionResult.Failure("Rate limited; retry after ${retryAfterMillis}ms")
        FlipperRootResult.Unsupported -> ActionResult.Unsupported
        is FlipperRootResult.Error -> ActionResult.Failure("USB grant failed: $reason")
    }

    companion object {
        const val FEATURE_ID = "flipper_root"
        const val ACTION_GRANT_USB = "flipper_root_grant_usb"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RootedFlipperActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(RootedFlipperActionHandler.FEATURE_ID)
    abstract fun bindRootedFlipperActionHandler(impl: RootedFlipperActionHandler): ActionHandler
}
