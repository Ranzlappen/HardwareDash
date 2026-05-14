package dev.ranzlappen.gadget.feature.torch.widget

import dev.ranzlappen.gadget.core.datastore.FeaturePreferences
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import kotlinx.coroutines.flow.Flow
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

    /** Reactive snapshot of every saved widget config, keyed by
     *  `appWidgetId`. Hot — collected by [TorchViewModel] for the
     *  in-app widget list. */
    val all: Flow<Map<Int, TorchWidgetConfig>> = prefs.all

    /** One-shot read of all configs. Used by widget providers'
     *  `onUpdate` to render every instance in a single pass. */
    suspend fun getAll(): Map<Int, TorchWidgetConfig> = prefs.getAll()

    /** Single-config one-shot read. Used by [StrobeWidgetProvider]
     *  to fetch the rate/SOS settings before starting the service. */
    suspend fun get(appWidgetId: Int): TorchWidgetConfig? = prefs.get(appWidgetId)

    /** Persist a config keyed by `appWidgetId`. Replaces any
     *  existing entry. */
    suspend fun save(appWidgetId: Int, config: TorchWidgetConfig) =
        prefs.save(appWidgetId, config)

    /** Remove the config for `appWidgetId`. No-op if absent. */
    suspend fun delete(appWidgetId: Int) = prefs.delete(appWidgetId)
}
