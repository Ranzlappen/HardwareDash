package dev.ranzlappen.gadget.feature.metricwidget.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.core.widgetkit.provider.ContentWidgetUpdater
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.metricwidget.MetricWidgetConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repaints every placed metric widget whenever one of the metrics they're
 * bound to changes — the content-source → repaint side of the kit's content
 * archetype, driven through [ContentWidgetUpdater].
 *
 * Tracks the live set of bound metric keys from the config store and, for that
 * set, merges each source's push [MetricSource.stream] with a low-frequency
 * ticker (so poll-only sources, which have no stream, still refresh). The
 * `flatMapLatest` restarts the merge whenever a widget is added, removed, or
 * re-bound to a different metric. Eagerly instantiated for the process lifetime
 * (the app startup path injects it, mirroring `BatteryWidgetController`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class MetricWidgetController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configStore: WidgetConfigStore<MetricWidgetConfig>,
    private val metricSources: Map<String, @JvmSuppressWildcards MetricSource>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            configStore.all
                .map { configs ->
                    configs.values.filter { !it.removed && it.isBound }.map { it.metricKey }.toSet()
                }
                .distinctUntilChanged()
                .flatMapLatest { keys -> repaintSignals(keys) }
                .collect {
                    ContentWidgetUpdater.requestUpdate(context, MetricWidgetProvider.PROVIDER_CLASS)
                }
        }
    }

    /** Unit signals to repaint on: each bound push source's changes + a ticker. */
    private fun repaintSignals(keys: Set<String>): Flow<Unit> {
        if (keys.isEmpty()) return emptyFlow()
        val streams = keys.mapNotNull { key -> metricSources[key]?.stream()?.map { } }
        val ticker = flow {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                emit(Unit)
            }
        }
        return merge(ticker, *streams.toTypedArray())
    }

    companion object {
        /** Refresh cadence for poll-only bound metrics (push sources update live). */
        private const val REFRESH_INTERVAL_MS = 30_000L
    }
}
