package dev.ranzlappen.gadget.feature.adbdebug.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.adbdebug.AdbEnabledMetricSource
import javax.inject.Singleton

/**
 * Contributes the adbdebug feature's [MetricSource]s into the app-wide
 * `Map<String, MetricSource>` consumed by `:core:monitoring` (chart/history)
 * and `:core:automation` (trigger evaluation).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AdbDebugMonitorModule {

    @Binds
    @Singleton
    @IntoMap
    @StringKey(AdbEnabledMetricSource.METRIC_KEY)
    abstract fun bindAdbEnabledMetricSource(impl: AdbEnabledMetricSource): MetricSource
}
