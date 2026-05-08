package com.gadget.adbdebug

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor ADB Debugging controller. Every method returns
 * [AdbDebuggingControllerResult.Unsupported] — the standard APK has no
 * privileged shell so direct `settings put global adb_enabled` and
 * `setprop` writes are impossible regardless of permissions.
 */
@Singleton
class StandardAdbDebuggingController @Inject constructor() : AdbDebuggingController {

    override suspend fun toggleAdbEnabled(enabled: Boolean): AdbDebuggingControllerResult =
        AdbDebuggingControllerResult.Unsupported

    override suspend fun toggleAdbOverNetwork(
        config: AdbNetworkConfig,
    ): AdbDebuggingControllerResult = AdbDebuggingControllerResult.Unsupported

    override suspend fun dumpProperties(persist: Boolean): AdbDebuggingControllerResult =
        AdbDebuggingControllerResult.Unsupported

    override suspend fun overrideSystemProperty(
        config: SetPropConfig,
    ): AdbDebuggingControllerResult = AdbDebuggingControllerResult.Unsupported

    override suspend fun resetAllAdbMutations(): AdbDebuggingControllerResult =
        AdbDebuggingControllerResult.ResetCompleted(restored = 0, failed = 0)

    override suspend fun revertOnScreenExit(): AdbDebuggingControllerResult =
        AdbDebuggingControllerResult.ResetCompleted(restored = 0, failed = 0)
}
