package dev.ranzlappen.gadget.feature.vibration

import dev.ranzlappen.gadget.core.datastore.FeaturePreferences
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single-record persistence for the rooted Vibration tools' tunable parameters
 * ([VibrationRootToolsConfig]). Mirror of torch's `RootToolsConfigRepository`:
 * `@Inject constructor` over a [FeaturePreferencesFactory] (no Hilt module
 * needed), one global record at [SINGLETON_ID].
 */
@Singleton
class RootToolsConfigRepository @Inject constructor(
    factory: FeaturePreferencesFactory,
) {
    private val prefs: FeaturePreferences<VibrationRootToolsConfig> = factory.create(
        fileName = "vibration_root_tools",
        keyPrefix = "root_tools_",
        serializer = VibrationRootToolsConfig.serializer(),
    )

    val config: Flow<VibrationRootToolsConfig> =
        prefs.all.map { it[SINGLETON_ID] ?: VibrationRootToolsConfig() }

    suspend fun get(): VibrationRootToolsConfig =
        prefs.get(SINGLETON_ID) ?: VibrationRootToolsConfig()

    suspend fun save(config: VibrationRootToolsConfig) = prefs.save(SINGLETON_ID, config)

    private companion object {
        const val SINGLETON_ID: Int = 0
    }
}
