package dev.ranzlappen.gadget.feature.vibration

import dev.ranzlappen.gadget.core.datastore.FeaturePreferences
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistence for user-saved [VibrationPattern]s. Backed by a per-feature
 * [FeaturePreferences] (one DataStore file, one record per pattern keyed by a
 * stable non-negative hash of the pattern id). Mirrors the
 * `MonitorConfigRepository` shape (`@Inject constructor` + factory — no Hilt
 * module needed).
 */
@Singleton
class PatternRepository @Inject constructor(
    factory: FeaturePreferencesFactory,
) {
    private val prefs: FeaturePreferences<VibrationPattern> = factory.create(
        fileName = "vibration_patterns",
        keyPrefix = "pattern_",
        serializer = VibrationPattern.serializer(),
    )

    /** Live list of saved patterns, ordered by name. */
    val patterns: Flow<List<VibrationPattern>> =
        prefs.all.map { map -> map.values.sortedBy { it.name.lowercase() } }

    suspend fun get(id: String): VibrationPattern? = prefs.get(idFor(id))

    suspend fun save(pattern: VibrationPattern) = prefs.save(idFor(pattern.id), pattern)

    suspend fun delete(id: String) = prefs.delete(idFor(id))

    private fun idFor(patternId: String): Int = patternId.hashCode() and Int.MAX_VALUE
}
