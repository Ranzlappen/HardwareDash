package dev.ranzlappen.gadget.feature.diagnostics.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.diagnostics.MemoryMetricSource
import javax.inject.Singleton

/**
 * Contributes the diagnostics feature's [MetricSource]s into the app-wide
 * `Map<String, MetricSource>` consumed by `:core:monitoring` (chart/history)
 * and `:core:automation` (trigger evaluation).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticsMonitorModule {

    @Binds
    @Singleton
    @IntoMap
    @StringKey(MemoryMetricSource.METRIC_KEY)
    abstract fun bindMemoryMetricSource(impl: MemoryMetricSource): MetricSource
}
