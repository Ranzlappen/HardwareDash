package dev.ranzlappen.gadget.feature.logbook.automation

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.core.data.logbook.LogbookTagColor
import dev.ranzlappen.gadget.feature.logbook.LogbookRepository
import dev.ranzlappen.gadget.feature.logbook.R
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The logbook automation surface. `add_entry` lets a rule log a timestamped
 * note (e.g. "log an entry when the battery drops below 10 %"); the
 * assert-style `assert_open_below` gates a rule on the current open-checkpoint
 * backlog, mirroring the sensor/ambient assert pattern.
 */
@Singleton
class LogbookActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LogbookRepository,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_ADD_ENTRY,
            label = context.getString(R.string.logbook_action_add_entry),
            params = listOf(
                ActionParam(PARAM_TEXT, ActionParamType.Text),
            ),
        ),
        ModuleAction(
            key = ACTION_ASSERT_OPEN_BELOW,
            label = context.getString(R.string.logbook_action_assert_open_below),
            params = listOf(
                ActionParam(
                    PARAM_THRESHOLD,
                    ActionParamType.Int,
                    DEFAULT_OPEN_THRESHOLD.toString(),
                    0f,
                    MAX_OPEN_THRESHOLD,
                ),
            ),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_ADD_ENTRY -> {
                val text = params[PARAM_TEXT]?.takeIf { it.isNotBlank() }
                    ?: return ActionResult.Failure("No entry text supplied")
                repository.addEntry(text, LogbookTagColor.None)
                ActionResult.Success
            }
            ACTION_ASSERT_OPEN_BELOW -> {
                val threshold = params[PARAM_THRESHOLD]?.toIntOrNull() ?: DEFAULT_OPEN_THRESHOLD
                val open = repository.openCheckpointCount.first()
                if (open <= threshold) {
                    ActionResult.Success
                } else {
                    ActionResult.Failure("$open open checkpoints exceeds the threshold of $threshold")
                }
            }
            else -> ActionResult.Unsupported
        }

    companion object {
        const val FEATURE_ID = "logbook"
        const val ACTION_ADD_ENTRY = "logbook_add_entry"
        const val ACTION_ASSERT_OPEN_BELOW = "logbook_assert_open_below"
        const val PARAM_TEXT = "text"
        const val PARAM_THRESHOLD = "threshold"
        const val DEFAULT_OPEN_THRESHOLD = 5
        private const val MAX_OPEN_THRESHOLD = 100f
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface LogbookActionModule {

    @Binds
    @IntoMap
    @StringKey(LogbookActionHandler.FEATURE_ID)
    fun bindLogbookActionHandler(handler: LogbookActionHandler): ActionHandler
}
