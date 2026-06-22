package dev.ranzlappen.gadget.feature.ambient.automation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.feature.ambient.AmbientSensor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmbientActionHandler @Inject constructor(
    private val sensor: AmbientSensor,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_ASSERT_BRIGHT,
            label = "Assert ambient light above threshold",
        ),
        ModuleAction(
            key = ACTION_ASSERT_DARK,
            label = "Assert ambient light below threshold",
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_ASSERT_BRIGHT -> {
                val threshold = params["threshold_lux"]?.toFloatOrNull() ?: 100f
                val lux = sensor.state.value.luxLevel ?: 0f
                if (lux >= threshold) ActionResult.Success
                else ActionResult.Failure("Ambient light ${lux} lux is below threshold ${threshold} lux")
            }
            ACTION_ASSERT_DARK -> {
                val threshold = params["threshold_lux"]?.toFloatOrNull() ?: 10f
                val lux = sensor.state.value.luxLevel ?: 0f
                if (lux <= threshold) ActionResult.Success
                else ActionResult.Failure("Ambient light ${lux} lux is above threshold ${threshold} lux")
            }
            else -> ActionResult.Unsupported
        }

    companion object {
        const val FEATURE_ID = "ambient"
        const val ACTION_ASSERT_BRIGHT = "ambient_assert_bright"
        const val ACTION_ASSERT_DARK = "ambient_assert_dark"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AmbientActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(AmbientActionHandler.FEATURE_ID)
    abstract fun bindAmbientActionHandler(impl: AmbientActionHandler): ActionHandler
}
