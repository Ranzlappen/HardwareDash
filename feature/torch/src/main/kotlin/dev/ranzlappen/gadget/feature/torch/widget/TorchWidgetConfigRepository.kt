package dev.ranzlappen.gadget.feature.torch.widget

import dev.ranzlappen.gadget.core.datastore.FeaturePreferences
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-feature repository over [FeaturePreferences] for
 * [TorchWidgetConfig] entries keyed by `appWidgetId`.
 *
 * Each home-screen torch widget owns exactly one entry in this
 * repository. The entry is created when the user pins the widget
 * via [TorchWidgetCreator]; it's deleted when the widget is dragged
 * off the home screen and [FlashlightWidgetProvider.onDeleted] /
 * [StrobeWidgetProvider.onDeleted] fires.
 *
 * Backed by the `torch_widgets` Preferences DataStore file, created
 * by [FeaturePreferencesFactory]. Other future features (Vibration
 * patterns, Sound presets, etc.) consume the same factory with their
 * own file name.
 *
 * **Hot caching:** the `all` flow is upgraded to a [StateFlow] via
 * `stateIn(...)` with an internal supervisor scope. Widget providers
 * read this synchronously from a [BroadcastReceiver.onReceive]
 * pathway via `all.value` (peeking the latest emission), and Compose
 * subscribers share a single cold-collection of the underlying
 * DataStore rather than each remounting their own. The scope is
 * process-lifetime because the repository is `@Singleton`.
 */
@Singleton
class TorchWidgetConfigRepository @Inject constructor(
    factory: FeaturePreferencesFactory,
) {
    private val prefs: FeaturePreferences<TorchWidgetConfig> = factory.create(
        fileName = "torch_widgets",
        keyPrefix = "widget_",
        serializer = TorchWidgetConfig.serializer(),
    )

    /** Internal hot scope. Survives ViewModel-scope teardown so the
     *  StateFlow stays warm for widget providers that read between
     *  UI sessions. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Reactive snapshot of every saved widget config, keyed by
     *  `appWidgetId`. Hot — shared across UI subscribers AND
     *  synchronously peek-able by widget providers via `.value`. */
    val all: StateFlow<Map<Int, TorchWidgetConfig>> = prefs.all.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap(),
    )

    /** One-shot read of all configs. Used by widget providers'
     *  `onUpdate` to render every instance in a single pass. */
    suspend fun getAll(): Map<Int, TorchWidgetConfig> = prefs.getAll()

    /** Single-config one-shot read. Reads from the cached StateFlow
     *  when warm (microsecond cost) and falls back to a DataStore read
     *  when the StateFlow hasn't emitted yet. */
    suspend fun get(appWidgetId: Int): TorchWidgetConfig? =
        all.value[appWidgetId] ?: prefs.get(appWidgetId)

    /** Authoritative single-config read straight from DataStore,
     *  bypassing the hot `all` cache. Used by [StrobeService] right
     *  after a pin so a stale cache entry (e.g. a self-healed default
     *  the StateFlow hasn't replaced yet) can't strand the first tap on
     *  the wrong config. */
    suspend fun getFresh(appWidgetId: Int): TorchWidgetConfig? =
        prefs.get(appWidgetId)

    /** Persist a config keyed by `appWidgetId`. Replaces any
     *  existing entry. */
    suspend fun save(appWidgetId: Int, config: TorchWidgetConfig) =
        prefs.save(appWidgetId, config)

    /** Persist a config only if `appWidgetId` has none yet. Atomic, so
     *  a provider's self-heal can't clobber a real config that the
     *  pin-success receiver is writing concurrently. Returns `true` if
     *  it wrote. */
    suspend fun saveIfAbsent(appWidgetId: Int, config: TorchWidgetConfig): Boolean =
        prefs.saveIfAbsent(appWidgetId, config)

    /** Remove the config for `appWidgetId`. No-op if absent. */
    suspend fun delete(appWidgetId: Int) = prefs.delete(appWidgetId)
}
