package dev.ranzlappen.gadget.core.monitoring

import dev.ranzlappen.gadget.core.datastore.FeaturePreferences
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-metric [MonitorConfig] persistence. Mirrors the
 * `TorchWidgetConfigRepository` pattern: one DataStore file, one record
 * per metric, keyed by a stable non-negative hash of the metricKey
 * (the underlying [FeaturePreferences] is `Int`-keyed).
 */
@Singleton
class MonitorConfigRepository @Inject constructor(
    factory: FeaturePreferencesFactory,
) {
    private val prefs: FeaturePreferences<MonitorConfig> = factory.create(
        fileName = "monitor_config",
        keyPrefix = "monitor_",
        serializer = MonitorConfig.serializer(),
    )

    /** Live config for a metric, defaulting to a disabled [MonitorConfig]. */
    fun config(metricKey: String): Flow<MonitorConfig> =
        prefs.all.map { it[idFor(metricKey)] ?: MonitorConfig() }

    suspend fun get(metricKey: String): MonitorConfig =
        prefs.get(idFor(metricKey)) ?: MonitorConfig()

    suspend fun save(metricKey: String, config: MonitorConfig) =
        prefs.save(idFor(metricKey), config)

    private fun idFor(metricKey: String): Int = metricKey.hashCode() and Int.MAX_VALUE
}
