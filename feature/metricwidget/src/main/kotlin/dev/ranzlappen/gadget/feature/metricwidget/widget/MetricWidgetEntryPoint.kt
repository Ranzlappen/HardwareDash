package dev.ranzlappen.gadget.feature.metricwidget.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.metricwidget.MetricWidgetConfig

/**
 * Hilt entry point for the metric widget's `AppWidgetProvider`, which is
 * system-instantiated and can't receive `@Inject`. Reached via
 * `EntryPointAccessors.fromApplication(context, MetricWidgetEntryPoint::class.java)`.
 *
 * [metricSources] is the app-wide `@IntoMap` multibinding — Hilt aggregates
 * every feature's contributions at the `SingletonComponent`, so the provider
 * can resolve whichever metric the user bound this instance to, without the
 * widget module depending on any feature.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MetricWidgetEntryPoint {
    fun metricWidgetConfigStore(): WidgetConfigStore<MetricWidgetConfig>
    fun widgetAppearanceRenderer(): WidgetAppearanceRenderer
    fun metricSources(): Map<String, @JvmSuppressWildcards MetricSource>
}
