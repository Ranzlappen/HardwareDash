package dev.ranzlappen.gadget.feature.radios.subghz.automation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.feature.radios.subghz.SubghzMonitor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Automation surface for the Sub-GHz bridge. Read-only asserts only — the
 * standard flavor cannot drive an SDR, so there is nothing to actuate; the
 * conditions let a rule gate on whether a bridge (and specifically a sub-GHz
 * capable one) is attached.
 */
@Singleton
class SubghzActionHandler @Inject constructor(
    private val monitor: SubghzMonitor,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_CHECK_BRIDGE,
            label = "Assert Sub-GHz bridge attached",
        ),
        ModuleAction(
            key = ACTION_CHECK_SUBGHZ_CAPABLE,
            label = "Assert Sub-GHz capable radio attached",
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_CHECK_BRIDGE -> {
                if (monitor.state.value.bridgeConnected) ActionResult.Success
                else ActionResult.Failure("No SDR / Sub-GHz bridge attached")
            }
            ACTION_CHECK_SUBGHZ_CAPABLE -> {
                if (monitor.state.value.device?.coversSubGhz == true) ActionResult.Success
                else ActionResult.Failure("No Sub-GHz capable radio attached")
            }
            else -> ActionResult.Unsupported
        }

    companion object {
        const val FEATURE_ID = "subghz"
        const val ACTION_CHECK_BRIDGE = "subghz_bridge_check"
        const val ACTION_CHECK_SUBGHZ_CAPABLE = "subghz_capable_check"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SubghzActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(SubghzActionHandler.FEATURE_ID)
    abstract fun bindSubghzActionHandler(impl: SubghzActionHandler): ActionHandler
}
