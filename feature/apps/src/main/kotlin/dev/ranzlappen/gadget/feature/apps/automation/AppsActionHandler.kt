package dev.ranzlappen.gadget.feature.apps.automation

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
import dev.ranzlappen.gadget.core.data.apps.AppsDao
import dev.ranzlappen.gadget.feature.apps.AppLauncher
import dev.ranzlappen.gadget.feature.apps.AppRepository
import dev.ranzlappen.gadget.feature.apps.R
import dev.ranzlappen.gadget.feature.apps.ui.folder.FolderPopupActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-Organizer's invocable-action surface for the future automation tool —
 * reuses the existing controller/repository layer rather than reimplementing
 * app-launch or folder logic:
 *  - [ACTION_REFRESH_APPS] calls the same [AppRepository.requestRefresh] the
 *    app's startup path uses to force a rescan.
 *  - [ACTION_OPEN_FOLDER] opens the same [FolderPopupActivity] the folder
 *    widget launches on tap, addressed by folder id (its intent already sets
 *    `FLAG_ACTIVITY_NEW_TASK`, so it starts cleanly from a non-Activity
 *    automation-engine context).
 *  - [ACTION_LAUNCH_APP] resolves an `AppRecord` by its stable `appKey` (via
 *    [AppsDao.getAppRecord]) and dispatches through [AppLauncher] — the same
 *    launch path the folder popup uses. `appKey` (not a bare package name)
 *    is required because it's the only identifier that disambiguates
 *    work-profile duplicates and web-link "apps", which share the package
 *    namespace of `appKey`-less lookups.
 */
@Singleton
class AppsActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val appLauncher: AppLauncher,
    private val dao: AppsDao,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(ACTION_REFRESH_APPS, context.getString(R.string.apps_action_refresh_apps)),
        ModuleAction(
            key = ACTION_OPEN_FOLDER,
            label = context.getString(R.string.apps_action_open_folder),
            params = listOf(ActionParam(PARAM_FOLDER_ID, ActionParamType.Int)),
        ),
        ModuleAction(
            key = ACTION_LAUNCH_APP,
            label = context.getString(R.string.apps_action_launch_app),
            params = listOf(ActionParam(PARAM_APP_KEY, ActionParamType.Text)),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_REFRESH_APPS -> {
                appRepository.requestRefresh()
                ActionResult.Success
            }
            ACTION_OPEN_FOLDER -> openFolder(params)
            ACTION_LAUNCH_APP -> launchApp(params)
            else -> ActionResult.Unsupported
        }

    private suspend fun openFolder(params: Map<String, String>): ActionResult {
        val folderId = params[PARAM_FOLDER_ID]?.toLongOrNull()
            ?: return ActionResult.Failure("missing or invalid $PARAM_FOLDER_ID")
        if (dao.getFolder(folderId) == null) return ActionResult.Failure("folder not found")
        context.startActivity(FolderPopupActivity.intent(context, folderId))
        return ActionResult.Success
    }

    private suspend fun launchApp(params: Map<String, String>): ActionResult {
        val appKey = params[PARAM_APP_KEY]?.takeIf { it.isNotBlank() }
            ?: return ActionResult.Failure("missing $PARAM_APP_KEY")
        val record = dao.getAppRecord(appKey) ?: return ActionResult.Failure("app not found")
        return if (appLauncher.launch(record)) {
            ActionResult.Success
        } else {
            ActionResult.Failure("launch failed")
        }
    }

    companion object {
        const val FEATURE_ID = "apps"
        const val ACTION_REFRESH_APPS = "refresh_apps"
        const val ACTION_OPEN_FOLDER = "open_folder"
        const val ACTION_LAUNCH_APP = "launch_app"
        const val PARAM_FOLDER_ID = "folder_id"
        const val PARAM_APP_KEY = "app_key"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface AppsActionModule {

    @Binds
    @IntoMap
    @StringKey(AppsActionHandler.FEATURE_ID)
    fun bindAppsActionHandler(handler: AppsActionHandler): ActionHandler
}
