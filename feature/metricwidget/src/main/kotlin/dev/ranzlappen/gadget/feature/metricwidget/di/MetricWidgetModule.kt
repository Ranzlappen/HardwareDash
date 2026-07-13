package dev.ranzlappen.gadget.feature.metricwidget.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.metricwidget.MetricWidgetConfig
import javax.inject.Singleton

/**
 * Hilt bindings for the metric widget's per-`appWidgetId` config store.
 * Top-level `object` module for `@Provides` (repo convention; mirrors
 * `BatteryWidgetModule`).
 */
@Module
@InstallIn(SingletonComponent::class)
object MetricWidgetModule {

    @Provides
    @Singleton
    fun provideMetricWidgetConfigStore(
        factory: FeaturePreferencesFactory,
    ): WidgetConfigStore<MetricWidgetConfig> {
        val prefs = factory.create(
            fileName = "metric_widgets",
            keyPrefix = "metric_widget_",
            serializer = MetricWidgetConfig.serializer(),
        )
        return WidgetConfigStore(prefs)
    }
}
