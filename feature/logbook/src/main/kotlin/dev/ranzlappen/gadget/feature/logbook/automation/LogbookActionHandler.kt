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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The logbook automation surface. `add_entry` lets a rule log a timestamped
 * note (e.g. "log an entry when the battery drops below 10 %"). To gate a
 * rule on the open-checkpoint backlog, use a metric condition on the
 * logbook MetricSource instead of an action.
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
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_ADD_ENTRY -> {
                // No non-local return: dispatch has an expression body.
                val text = params[PARAM_TEXT]?.takeIf { it.isNotBlank() }
                if (text == null) {
                    ActionResult.Failure("No entry text supplied")
                } else {
                    repository.addEntry(text, LogbookTagColor.None)
                    ActionResult.Success
                }
            }
            else -> ActionResult.Unsupported
        }

    companion object {
        const val FEATURE_ID = "logbook"
        const val ACTION_ADD_ENTRY = "logbook_add_entry"
        const val PARAM_TEXT = "text"
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
