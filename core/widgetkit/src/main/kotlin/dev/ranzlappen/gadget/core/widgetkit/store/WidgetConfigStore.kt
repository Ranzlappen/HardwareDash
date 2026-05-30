package dev.ranzlappen.gadget.core.widgetkit.store

import dev.ranzlappen.gadget.core.datastore.FeaturePreferences
import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Generic per-feature widget-config store. The kit's contract type for
 * a per-`appWidgetId` persisted [WidgetKitConfig] collection.
 *
 * Wraps [FeaturePreferences] with two layers the kit owns generically:
 *  - A hot [StateFlow] cache (`all`) — widget providers peek
 *    `all.value` synchronously from `onReceive` paths; Compose
 *    subscribers share one cold-collection.
 *  - A [Migrator] hook — every read passes through `migrator.migrate`
 *    so a schema-bump migrator can transparently upgrade older on-disk
 *    configs without callers ever seeing the legacy shape.
 *
 * The store is **not** `@Inject`-constructable directly — feature
 * modules `@Provides` an instance from a [FeaturePreferencesFactory]
 * with the feature's filename + key prefix + serializer:
 *
 * ```kotlin
 * @Provides @Singleton
 * fun provideTorchStore(
 *     factory: FeaturePreferencesFactory,
 * ): WidgetConfigStore<TorchWidgetConfig> {
 *     val prefs = factory.create(
 *         fileName = "torch_widgets",
 *         keyPrefix = "widget_",
 *         serializer = TorchWidgetConfig.serializer(),
 *     )
 *     return WidgetConfigStore(prefs)
 * }
 * ```
 *
 * Replaces each feature's hand-rolled
 * `<Feature>WidgetConfigRepository` (P1-9 closeout + the C4 of
 * `refactor-2026` Phase 2).
 */
class WidgetConfigStore<T : WidgetKitConfig>(
    private val prefs: FeaturePreferences<T>,
    private val migrator: Migrator<T> = NoOpMigrator(),
) {
    /** Internal hot scope. Survives ViewModel-scope teardown so the
     *  StateFlow stays warm for widget providers that read between
     *  UI sessions. Process-lifetime — the store is `@Singleton`. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Reactive snapshot of every saved widget config, keyed by
     * `appWidgetId`. Migrator runs on every emission.
     *
     * `WhileSubscribed(Long.MAX_VALUE)`: defer the upstream collection
     * until the screen (the only subscriber) actually observes, then
     * stay warm forever — bounds per-feature idle cost as the module
     * count grows.
     */
    val all: StateFlow<Map<Int, T>> = prefs.all
        .map { map -> map.mapValues { (_, value) -> migrator.migrate(value) } }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(Long.MAX_VALUE),
            initialValue = emptyMap(),
        )

    /** One-shot read of all configs. Used by widget providers'
     *  `onUpdate` to render every instance in a single pass. */
    suspend fun getAll(): Map<Int, T> =
        prefs.getAll().mapValues { (_, value) -> migrator.migrate(value) }

    /** Single-config one-shot read. Reads from the cached StateFlow
     *  when warm (microsecond cost) and falls back to a DataStore read
     *  when the StateFlow hasn't emitted yet. */
    suspend fun get(appWidgetId: Int): T? =
        all.value[appWidgetId] ?: prefs.get(appWidgetId)?.let(migrator::migrate)

    /** Authoritative single-config read straight from DataStore,
     *  bypassing the hot `all` cache. Use this when a stale cache entry
     *  (e.g. a self-healed default the StateFlow hasn't replaced yet)
     *  would strand a caller on the wrong config — e.g. the strobe
     *  service reading the just-pinned widget's config. */
    suspend fun getFresh(appWidgetId: Int): T? =
        prefs.get(appWidgetId)?.let(migrator::migrate)

    /** Persist a config keyed by `appWidgetId`. Replaces any
     *  existing entry. */
    suspend fun save(appWidgetId: Int, config: T) =
        prefs.save(appWidgetId, config)

    /** Persist a config only if `appWidgetId` has none yet. Atomic, so
     *  a provider's self-heal can't clobber a real config that the
     *  pin-success receiver is writing concurrently. Returns `true` if
     *  it wrote. */
    suspend fun saveIfAbsent(appWidgetId: Int, config: T): Boolean =
        prefs.saveIfAbsent(appWidgetId, config)

    /** Remove the config for `appWidgetId`. No-op if absent. */
    suspend fun delete(appWidgetId: Int) = prefs.delete(appWidgetId)
}
