package dev.ranzlappen.gadget.feature.torch

import dev.ranzlappen.gadget.core.datastore.FeaturePreferences
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single-record persistence for the rooted Torch tools' tunable parameters
 * ([TorchRootToolsConfig]).
 *
 * Mirrors [dev.ranzlappen.gadget.core.monitoring.MonitorConfigRepository]:
 * `@Inject constructor` over a [FeaturePreferencesFactory] (no Hilt module
 * needed). Unlike the monitor/widget repos there is exactly one global record
 * — the rooted-tool settings are app-wide, not per-id — so every read/write
 * uses the fixed [SINGLETON_ID].
 */
@Singleton
class RootToolsConfigRepository @Inject constructor(
    factory: FeaturePreferencesFactory,
) {
    private val prefs: FeaturePreferences<TorchRootToolsConfig> = factory.create(
        fileName = "torch_root_tools",
        keyPrefix = "root_tools_",
        serializer = TorchRootToolsConfig.serializer(),
    )

    /** Live config, defaulting to the preset [TorchRootToolsConfig] before the
     *  user has tuned anything. */
    val config: Flow<TorchRootToolsConfig> =
        prefs.all.map { it[SINGLETON_ID] ?: TorchRootToolsConfig() }

    suspend fun get(): TorchRootToolsConfig =
        prefs.get(SINGLETON_ID) ?: TorchRootToolsConfig()

    suspend fun save(config: TorchRootToolsConfig) =
        prefs.save(SINGLETON_ID, config)

    private companion object {
        /** The one and only record id — settings are global, not per-widget. */
        const val SINGLETON_ID: Int = 0
    }
}
