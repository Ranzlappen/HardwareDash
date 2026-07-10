package dev.ranzlappen.gadget.feature.apps.root

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor [AppsRootController]. Every method returns
 * [AppsRootControllerResult.Unsupported] — there is no privileged shell in
 * this APK so `pm disable-user` / `pm enable` / `am force-stop` are
 * physically impossible. Shared UI checks the result and hides/disables
 * the corresponding row; Compose code never branches on
 * `BuildConfig.IS_ROOTED`.
 */
@Singleton
class StandardAppsRootController @Inject constructor() : AppsRootController {

    override suspend fun freezeApp(packageName: String): AppsRootControllerResult =
        AppsRootControllerResult.Unsupported

    override suspend fun unfreezeApp(packageName: String): AppsRootControllerResult =
        AppsRootControllerResult.Unsupported

    override suspend fun forceStopApp(packageName: String): AppsRootControllerResult =
        AppsRootControllerResult.Unsupported
}
