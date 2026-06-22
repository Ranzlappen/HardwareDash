package dev.ranzlappen.gadget.feature.storage.rooted

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootedStorageActionHandler @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val shell: RootShell,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(key = ACTION_DUMP_DISKSTATS, label = "Dump diskstats", requiresRoot = true),
        ModuleAction(key = ACTION_ENUMERATE_MOUNTS, label = "Enumerate mounts", requiresRoot = true),
        ModuleAction(key = ACTION_FSTRIM, label = "Run fstrim on /data", requiresRoot = true),
        ModuleAction(key = ACTION_DROP_CACHES, label = "Drop page/dentry/inode caches", requiresRoot = true),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_DUMP_DISKSTATS -> runGated(RootFeatureKey.StorageDumpDiskstats) {
                val result = shell.exec("dumpsys diskstats", timeoutMillis = 10_000)
                if (result.isSuccess) ActionResult.Success
                else ActionResult.Failure("diskstats failed: ${result.stderr.firstOrNull().orEmpty()}")
            }
            ACTION_ENUMERATE_MOUNTS -> runGated(RootFeatureKey.StorageEnumerateMounts) {
                val result = shell.exec("cat /proc/mounts", timeoutMillis = 5_000)
                if (result.isSuccess) ActionResult.Success
                else ActionResult.Failure("mounts failed: ${result.stderr.firstOrNull().orEmpty()}")
            }
            ACTION_FSTRIM -> runGated(RootFeatureKey.StorageFstrim) {
                val result = shell.exec("fstrim -v /data", timeoutMillis = 30_000)
                if (result.isSuccess) ActionResult.Success
                else ActionResult.Failure("fstrim failed: ${result.stderr.firstOrNull().orEmpty()}")
            }
            ACTION_DROP_CACHES -> runGated(RootFeatureKey.StorageDropCaches) {
                val result = shell.exec(
                    listOf("sync", "echo 3 > /proc/sys/vm/drop_caches"),
                    timeoutMillis = 10_000,
                )
                if (result.isSuccess) ActionResult.Success
                else ActionResult.Failure("drop_caches failed: ${result.stderr.firstOrNull().orEmpty()}")
            }
            else -> ActionResult.Unsupported
        }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> ActionResult,
    ): ActionResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is ActionResult.Success) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> ActionResult.Failure("Blocked by user preference")
        is RootGateDecision.BlockedByLimiter ->
            ActionResult.Failure("Rate limited; retry after ${gate.retryAfterMillis}ms")
        RootGateDecision.Unsupported -> ActionResult.Unsupported
    }

    companion object {
        const val FEATURE_ID = "storage_root"
        const val ACTION_DUMP_DISKSTATS = "storage_root_dump_diskstats"
        const val ACTION_ENUMERATE_MOUNTS = "storage_root_enumerate_mounts"
        const val ACTION_FSTRIM = "storage_root_fstrim"
        const val ACTION_DROP_CACHES = "storage_root_drop_caches"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RootedStorageActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(RootedStorageActionHandler.FEATURE_ID)
    abstract fun bindRootedStorageActionHandler(impl: RootedStorageActionHandler): ActionHandler
}
