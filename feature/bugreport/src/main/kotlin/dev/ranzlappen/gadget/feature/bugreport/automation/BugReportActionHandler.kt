package dev.ranzlappen.gadget.feature.bugreport.automation

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.ModuleAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BugReportActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_ASSERT_PERMISSION,
            label = "Assert permission granted",
            params = listOf(ActionParam(name = "permission", type = ActionParamType.Text)),
        ),
        ModuleAction(key = ACTION_ASSERT_ADB, label = "Assert ADB diagnostics available", requiresRoot = true),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_ASSERT_PERMISSION -> {
                val permission = params["permission"]?.takeIf { it.isNotBlank() }
                    ?: return ActionResult.Failure("No permission specified")
                val granted = ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) ActionResult.Success
                else ActionResult.Failure("Permission $permission is denied")
            }
            ACTION_ASSERT_ADB -> ActionResult.Unsupported
            else -> ActionResult.Unsupported
        }

    companion object {
        const val FEATURE_ID = "bugreport"
        const val ACTION_ASSERT_PERMISSION = "bugreport_assert_permission"
        const val ACTION_ASSERT_ADB = "bugreport_assert_adb"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BugReportActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(BugReportActionHandler.FEATURE_ID)
    abstract fun bindBugReportActionHandler(impl: BugReportActionHandler): ActionHandler
}
