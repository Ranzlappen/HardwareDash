package dev.ranzlappen.gadget.feature.flipper.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.flipper.monitor.FlipperBatteryMetricSource
import dev.ranzlappen.gadget.feature.flipper.monitor.FlipperConnectedMetricSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FlipperModule {

    @Binds
    @Singleton
    @IntoMap
    @StringKey(FlipperConnectedMetricSource.METRIC_KEY)
    abstract fun bindFlipperConnectedMetricSource(impl: FlipperConnectedMetricSource): MetricSource

    @Binds
    @Singleton
    @IntoMap
    @StringKey(FlipperBatteryMetricSource.METRIC_KEY)
    abstract fun bindFlipperBatteryMetricSource(impl: FlipperBatteryMetricSource): MetricSource
}
