package dev.ranzlappen.gadget.feature.radios.wifi.automation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.feature.radios.wifi.WifiMonitor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiActionHandler @Inject constructor(
    private val monitor: WifiMonitor,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_CHECK_ENABLED,
            label = "Assert WiFi enabled",
        ),
        ModuleAction(
            key = ACTION_CHECK_CONNECTED,
            label = "Assert WiFi connected",
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_CHECK_ENABLED -> {
                if (monitor.state.value.enabled) ActionResult.Success
                else ActionResult.Failure("WiFi is not enabled")
            }
            ACTION_CHECK_CONNECTED -> {
                if (monitor.state.value.connected) ActionResult.Success
                else ActionResult.Failure("WiFi is not connected")
            }
            else -> ActionResult.Unsupported
        }

    companion object {
        const val FEATURE_ID = "wifi"
        const val ACTION_CHECK_ENABLED = "wifi_enabled_check"
        const val ACTION_CHECK_CONNECTED = "wifi_connected_check"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WifiActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(WifiActionHandler.FEATURE_ID)
    abstract fun bindWifiActionHandler(impl: WifiActionHandler): ActionHandler
}
