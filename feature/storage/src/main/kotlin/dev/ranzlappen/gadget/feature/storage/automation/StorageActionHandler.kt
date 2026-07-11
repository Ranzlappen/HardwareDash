package dev.ranzlappen.gadget.feature.storage.automation

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
import dev.ranzlappen.gadget.feature.storage.R
import dev.ranzlappen.gadget.feature.storage.StorageMonitor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The standard-tier automation surface for the storage module — the
 * assert-style gate the W3 sweep left out of scope. Baseline storage
 * telemetry is read-only `StatFs` with no controller state, so the only
 * meaningful standard action is an assertion over free space (the rooted
 * fstrim / drop-caches mutations live in `RootedStorageActionHandler`
 * under the separate `storage_root` feature id).
 */
@Singleton
class StorageActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val monitor: StorageMonitor,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_ASSERT_FREE_SPACE,
            label = context.getString(R.string.storage_action_assert_free_space),
            params = listOf(
                ActionParam(
                    PARAM_THRESHOLD_GB,
                    ActionParamType.Float,
                    DEFAULT_THRESHOLD_GB.toString(),
                    0f,
                    MAX_THRESHOLD_GB,
                ),
            ),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_ASSERT_FREE_SPACE -> {
                val thresholdGb =
                    params[PARAM_THRESHOLD_GB]?.toFloatOrNull() ?: DEFAULT_THRESHOLD_GB
                val freeGb = monitor.internalFreeBytes().toFloat() / BYTES_PER_GB
                if (freeGb >= thresholdGb) {
                    ActionResult.Success
                } else {
                    ActionResult.Failure(
                        "Internal storage has %.1f GB free, below the %.1f GB threshold"
                            .format(freeGb, thresholdGb),
                    )
                }
            }
            else -> ActionResult.Unsupported
        }

    companion object {
        const val FEATURE_ID = "storage"
        const val ACTION_ASSERT_FREE_SPACE = "storage_assert_free_space"
        const val PARAM_THRESHOLD_GB = "threshold_gb"
        const val DEFAULT_THRESHOLD_GB = 5f
        private const val MAX_THRESHOLD_GB = 512f
        private const val BYTES_PER_GB = 1024f * 1024f * 1024f
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface StorageActionModule {

    @Binds
    @IntoMap
    @StringKey(StorageActionHandler.FEATURE_ID)
    fun bindStorageActionHandler(handler: StorageActionHandler): ActionHandler
}
