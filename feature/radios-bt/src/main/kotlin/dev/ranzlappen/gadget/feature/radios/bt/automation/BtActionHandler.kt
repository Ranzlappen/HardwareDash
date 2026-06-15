package dev.ranzlappen.gadget.feature.radios.bt.automation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.feature.radios.bt.BluetoothAdapterWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BtActionHandler @Inject constructor(
    private val adapter: BluetoothAdapterWrapper,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_CHECK_ENABLED,
            label = "Assert Bluetooth enabled",
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_CHECK_ENABLED -> {
                if (adapter.isEnabled()) ActionResult.Success
                else ActionResult.Failure("Bluetooth is not enabled")
            }
            else -> ActionResult.Unsupported
        }

    companion object {
        const val FEATURE_ID = "bluetooth"
        const val ACTION_CHECK_ENABLED = "bt_connect_check"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BtActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(BtActionHandler.FEATURE_ID)
    abstract fun bindBtActionHandler(impl: BtActionHandler): ActionHandler
}
