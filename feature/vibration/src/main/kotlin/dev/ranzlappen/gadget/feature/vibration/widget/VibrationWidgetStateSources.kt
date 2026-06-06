package dev.ranzlappen.gadget.feature.vibration.widget

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetStateSource
import dev.ranzlappen.gadget.feature.vibration.VibrationRuntime
import javax.inject.Inject

/**
 * Live on/off state for the **continuous ("perma") vibrate** toggle, read by the
 * generic provider (via the kit's `WidgetFunctionDispatcher`) to drive the
 * active/inactive icon swap and decide which paired action a tap dispatches.
 *
 * Reads [VibrationRuntime] `state.isSustained` — set only by a looping/sustained
 * command and cleared on `stop`, so a transient one-shot or pattern (which flips
 * `isActive` but not `isSustained`) never makes the toggle read "on". The
 * [dev.ranzlappen.gadget.feature.vibration.VibrationPlaybackService] keeps the
 * process alive while continuous runs; if it has been torn down, a cold-process
 * tap re-creates the runtime at `isSustained = false` (correct: stopped).
 */
class VibrationRunningStateSource @Inject constructor(
    private val runtime: VibrationRuntime,
) : WidgetStateSource {
    override fun isActive(): Boolean = runtime.state.value.isSustained
}

/**
 * Binds the continuous-vibrate toggle's [WidgetStateSource] into the kit-side
 * `Map<String, WidgetStateSource>` multibinding keyed `"<featureId>:<stateKey>"`.
 *
 * The key is the literal `"vibration:vibration_running"` (matching
 * `VibrationActionHandler.FEATURE_ID` + `VibrationWidgetFunctionCatalog
 * .STATE_VIBRATION_RUNNING`) — `@StringKey` needs a compile-time constant and
 * some Hilt/KSP configs don't const-fold a Kotlin string template, so it is
 * spelled out (mirrors `TorchWidgetStateModule`).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class VibrationWidgetStateModule {

    @Binds
    @IntoMap
    @StringKey("vibration:vibration_running")
    abstract fun bindVibrationRunning(impl: VibrationRunningStateSource): WidgetStateSource
}
