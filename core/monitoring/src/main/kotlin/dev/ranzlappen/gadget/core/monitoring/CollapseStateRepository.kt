package dev.ranzlappen.gadget.core.monitoring

import dev.ranzlappen.gadget.core.datastore.FeaturePreferences
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.serializer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generic persisted collapsed/expanded state for UI containers, keyed by a
 * caller-chosen stable string id. Default is **expanded** (`true`) — a
 * never-toggled card opens expanded; only an explicit collapse is stored.
 *
 * Mirrors [MonitorConfigRepository]'s DataStore wiring (one file, one record
 * per id under a stable non-negative hash of the id). Lives in
 * `:core:monitoring` because that's the shared Hilt + DataStore core every
 * feature already depends on; it is **not** monitoring-specific — any feature
 * can inject it to back a [dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard].
 */
@Singleton
class CollapseStateRepository @Inject constructor(
    factory: FeaturePreferencesFactory,
) {
    private val prefs: FeaturePreferences<Boolean> = factory.create(
        fileName = "ui_collapse_state",
        keyPrefix = "collapse_",
        serializer = Boolean.serializer(),
    )

    /** Live expanded state for [id], defaulting to expanded (`true`). */
    fun expanded(id: String): Flow<Boolean> =
        prefs.all.map { it[idFor(id)] ?: true }

    /**
     * Live expanded states for a known set of [ids] as one map — convenience
     * for a screen that hoists several sections into its view-state (so it
     * doesn't have to combine N per-id flows). Missing ids default expanded.
     */
    fun expandedStates(ids: List<String>): Flow<Map<String, Boolean>> =
        prefs.all.map { stored -> ids.associateWith { stored[idFor(it)] ?: true } }

    suspend fun setExpanded(id: String, expanded: Boolean) =
        prefs.save(idFor(id), expanded)

    /** Flip the stored state for [id] (treating absent as expanded). */
    suspend fun toggle(id: String) {
        prefs.save(idFor(id), !(prefs.get(idFor(id)) ?: true))
    }

    private fun idFor(id: String): Int = id.hashCode() and Int.MAX_VALUE
}
