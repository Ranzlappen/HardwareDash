package com.gadget.storage

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Storage controller. Every privileged method returns
 * [StorageControllerResult.Unsupported] — there is no privileged shell
 * in this APK so `dumpsys diskstats`, `/proc/mountinfo` reads, `fstrim`,
 * and `drop_caches` writes are physically impossible.
 *
 * Shared UI checks the result and hides the corresponding control. Compose
 * code never branches on `BuildConfig.IS_ROOTED` — it just asks the
 * controller and trusts the answer.
 */
@Singleton
class StandardStorageController @Inject constructor() : StorageController {

    override suspend fun dumpDiskstats(persist: Boolean): StorageControllerResult =
        StorageControllerResult.Unsupported

    override suspend fun enumerateMounts(): StorageControllerResult =
        StorageControllerResult.Unsupported

    override suspend fun trimFilesystem(config: FstrimConfig): StorageControllerResult =
        StorageControllerResult.Unsupported

    override suspend fun dropKernelCaches(config: DropCachesConfig): StorageControllerResult =
        StorageControllerResult.Unsupported

    override suspend fun resetAllStorageMutations(): StorageControllerResult =
        StorageControllerResult.ResetCompleted(restored = 0, failed = 0)

    override suspend fun revertOnScreenExit(): StorageControllerResult =
        StorageControllerResult.ResetCompleted(restored = 0, failed = 0)
}
