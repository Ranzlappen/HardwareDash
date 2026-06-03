package dev.ranzlappen.gadget.feature.torch.widget

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetStateSource
import dev.ranzlappen.gadget.feature.torch.TorchController
import dev.ranzlappen.gadget.feature.torch.strobe.StrobeRuntime
import javax.inject.Inject

/**
 * Live on/off state for the torch **power** toggle, read by the generic
 * provider (via the kit's `WidgetFunctionDispatcher`) to drive the
 * active/inactive icon swap and decide which paired action a tap dispatches.
 *
 * Reads the hot [TorchController.state] `StateFlow.value` — non-suspend, so the
 * provider can compute pre-tap state cheaply on the broadcast path.
 */
class TorchPowerStateSource @Inject constructor(
    private val controller: TorchController,
) : WidgetStateSource {
    override fun isActive(): Boolean = controller.state.value.isOn
}

/**
 * Live running state for the **strobe** toggle, mirroring
 * [TorchPowerStateSource] but reading [StrobeRuntime.running].
 */
class StrobeRunningStateSource @Inject constructor(
    private val runtime: StrobeRuntime,
) : WidgetStateSource {
    override fun isActive(): Boolean = runtime.running.value
}

/**
 * Binds torch's two [WidgetStateSource] toggles into the kit-side
 * `Map<String, WidgetStateSource>` multibinding keyed `"<featureId>:<stateKey>"`.
 *
 * The keys are spelled out literally (`"torch:torch_power"` /
 * `"torch:strobe_running"`) rather than composed from the
 * `TorchBootRearmHandler.FEATURE_ID` + `TorchWidgetFunctionCatalog.STATE_*`
 * constants: `@StringKey` is a Java annotation value and some Hilt/KSP
 * configurations don't const-fold a Kotlin string-template into the required
 * compile-time constant. The literals match those constants by construction.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TorchWidgetStateModule {

    @Binds
    @IntoMap
    @StringKey("torch:torch_power")
    abstract fun bindTorchPower(impl: TorchPowerStateSource): WidgetStateSource

    @Binds
    @IntoMap
    @StringKey("torch:strobe_running")
    abstract fun bindStrobeRunning(impl: StrobeRunningStateSource): WidgetStateSource
}
