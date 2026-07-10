package dev.ranzlappen.gadget.feature.radios.cell.automation

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
import dev.ranzlappen.gadget.feature.radios.cell.CellSignalMetricSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `:feature:radios-cell`'s invocable-action surface.
 *
 * The module is read-only by design —
 * [dev.ranzlappen.gadget.feature.radios.cell.control.CellController]
 * deliberately has no AT-command write path (its kdoc: the diagnostic
 * nodes are Qualcomm-specific and OEM-locked on most devices), so — mirroring
 * `:feature:ambient` / `:feature:sensors`' pure-read action surface — the
 * actions here are threshold *assertions* sampling
 * [CellSignalMetricSource] directly: "assert cell signal above/below N
 * bars" lets an automation rule gate on cellular coverage without a new
 * signal path.
 *
 * [dev.ranzlappen.gadget.feature.radios.cell.control.CellController.resetAllCellMutations]
 * is deliberately **not** exposed as an action here: its own kdoc says it's
 * "always `ResetCompleted(0, 0)` — no mutations to revert" (there is
 * nothing this module ever writes, rooted or not). An automation action
 * that always reports success without changing or asserting anything isn't
 * a useful rule step, so it's left off the action list rather than padded
 * in for shape parity.
 */
@Singleton
class CellActionHandler @Inject constructor(
    private val signal: CellSignalMetricSource,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_ASSERT_ABOVE,
            label = "Assert cell signal above threshold",
            params = listOf(
                ActionParam(
                    name = PARAM_THRESHOLD_BARS,
                    type = ActionParamType.Float,
                    default = DEFAULT_THRESHOLD_BARS.toString(),
                    min = signal.descriptor.min,
                    max = signal.descriptor.max,
                ),
            ),
        ),
        ModuleAction(
            key = ACTION_ASSERT_BELOW,
            label = "Assert cell signal below threshold",
            params = listOf(
                ActionParam(
                    name = PARAM_THRESHOLD_BARS,
                    type = ActionParamType.Float,
                    default = DEFAULT_THRESHOLD_BARS.toString(),
                    min = signal.descriptor.min,
                    max = signal.descriptor.max,
                ),
            ),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult {
        val threshold = params[PARAM_THRESHOLD_BARS]?.toFloatOrNull() ?: DEFAULT_THRESHOLD_BARS
        val bars = signal.sample()
        return when (actionKey) {
            ACTION_ASSERT_ABOVE ->
                if (bars >= threshold) {
                    ActionResult.Success
                } else {
                    ActionResult.Failure("Cell signal $bars bars is not above $threshold bars")
                }
            ACTION_ASSERT_BELOW ->
                if (bars <= threshold) {
                    ActionResult.Success
                } else {
                    ActionResult.Failure("Cell signal $bars bars is not below $threshold bars")
                }
            else -> ActionResult.Unsupported
        }
    }

    companion object {
        const val FEATURE_ID = "cell"
        const val ACTION_ASSERT_ABOVE = "cell_assert_signal_above"
        const val ACTION_ASSERT_BELOW = "cell_assert_signal_below"
        const val PARAM_THRESHOLD_BARS = "threshold_bars"
        const val DEFAULT_THRESHOLD_BARS = 2f
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CellActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(CellActionHandler.FEATURE_ID)
    abstract fun bindCellActionHandler(impl: CellActionHandler): ActionHandler
}
