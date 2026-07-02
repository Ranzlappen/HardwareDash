package dev.ranzlappen.gadget.feature.storage.rooted.control

import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dev.ranzlappen.gadget.feature.storage.control.DropCachesConfig
import dev.ranzlappen.gadget.feature.storage.control.FstrimConfig
import dev.ranzlappen.gadget.feature.storage.control.StorageController
import dev.ranzlappen.gadget.feature.storage.control.StorageControllerResult
import javax.inject.Inject
import javax.inject.Singleton

private val STORAGE_RESET_PREFIXES = listOf("mount://")
private val STORAGE_SCREEN_EXIT_PREFIXES = listOf("mount://")

/**
 * Rooted-flavor Storage controller. Wires the safety gate to the four
 * storage helpers. fstrim and drop_caches are intrinsically
 * non-reversible, so the screen-exit revert and reset filter the
 * `mount://` prefix only — currently empty, but kept stable so a future
 * write feature that mutates a mount can plug in without touching the
 * controller.
 */
@Singleton
class RootedStorageController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val diskstatsHelper: DiskstatsHelper,
    private val mountInfoHelper: MountInfoHelper,
    private val fstrimHelper: FstrimHelper,
    private val dropCachesHelper: DropCachesHelper,
    private val mutationLog: SysfsMutationLog,
) : StorageController {

    override suspend fun dumpDiskstats(persist: Boolean): StorageControllerResult =
        runGated(RootFeatureKey.StorageDumpDiskstats) {
            val excerpt = diskstatsHelper.snapshot()
                ?: return@runGated StorageControllerResult.HardwareError("dumpsys diskstats failed")
            val persistedFile = if (persist) {
                diskstatsHelper.persistToLogbook(excerpt)?.absolutePath
            } else {
                null
            }
            StorageControllerResult.DiskstatsExcerpt(
                excerpt = excerpt,
                persistedFile = persistedFile,
            )
        }

    override suspend fun enumerateMounts(): StorageControllerResult =
        runGated(RootFeatureKey.StorageEnumerateMounts) {
            val mounts = mountInfoHelper.enumerate()
            if (mounts.isEmpty()) {
                StorageControllerResult.HardwareError("could not read /proc/mountinfo")
            } else {
                StorageControllerResult.MountList(mounts)
            }
        }

    override suspend fun trimFilesystem(config: FstrimConfig): StorageControllerResult =
        runGated(RootFeatureKey.StorageFstrim) {
            val outcome = fstrimHelper.trim(config.partitions, config.verbose)
            if (outcome.trimmed.isEmpty()) {
                StorageControllerResult.HardwareError(
                    "no partition trimmed; skipped=${outcome.skipped}",
                )
            } else {
                StorageControllerResult.Ok(
                    statusNote = "trimmed=${outcome.trimmed} skipped=${outcome.skipped}",
                )
            }
        }

    override suspend fun dropKernelCaches(config: DropCachesConfig): StorageControllerResult =
        runGated(RootFeatureKey.StorageDropCaches) {
            val ok = dropCachesHelper.drop()
            if (ok) {
                StorageControllerResult.Ok(statusNote = "drop_caches=3 written")
            } else {
                StorageControllerResult.HardwareError("drop_caches write rejected")
            }
        }

    override suspend fun resetAllStorageMutations(): StorageControllerResult {
        val outcome = mutationLog.revertAll(STORAGE_RESET_PREFIXES)
        return StorageControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    override suspend fun revertOnScreenExit(): StorageControllerResult {
        val outcome = mutationLog.revertAll(STORAGE_SCREEN_EXIT_PREFIXES)
        return StorageControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> StorageControllerResult,
    ): StorageControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is StorageControllerResult.Ok ||
                it is StorageControllerResult.DiskstatsExcerpt ||
                it is StorageControllerResult.MountList
            ) {
                safetyGate.recordInvocation(feature)
            }
        }
        RootGateDecision.BlockedByUser -> StorageControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            StorageControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> StorageControllerResult.Unsupported
    }
}
