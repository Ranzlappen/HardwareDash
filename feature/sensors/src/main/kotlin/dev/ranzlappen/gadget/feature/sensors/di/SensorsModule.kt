package dev.ranzlappen.gadget.feature.sensors.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.sensors.AccelerationMetricSource
import dev.ranzlappen.gadget.feature.sensors.LightMetricSource
import dev.ranzlappen.gadget.feature.sensors.ProximityMetricSource
import javax.inject.Singleton

/**
 * Contributes the sensors feature's readable signals into the shared
 * `Map<String, MetricSource>` multibinding — one definition each, consumed
 * by monitoring, the automation engine, and `:core:hardware`'s registry
 * (torch's `TorchMetricSource` binding is the reference).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SensorsModule {

    @Binds
    @Singleton
    @IntoMap
    @StringKey(ProximityMetricSource.METRIC_KEY)
    abstract fun bindProximityMetricSource(impl: ProximityMetricSource): MetricSource

    @Binds
    @Singleton
    @IntoMap
    @StringKey(LightMetricSource.METRIC_KEY)
    abstract fun bindLightMetricSource(impl: LightMetricSource): MetricSource

    @Binds
    @Singleton
    @IntoMap
    @StringKey(AccelerationMetricSource.METRIC_KEY)
    abstract fun bindAccelerationMetricSource(impl: AccelerationMetricSource): MetricSource
}
