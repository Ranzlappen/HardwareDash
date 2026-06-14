package dev.ranzlappen.gadget.feature.gps.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.gps.GpsAltitudeMetricSource
import dev.ranzlappen.gadget.feature.gps.GpsSpeedMetricSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GpsModule {

    @Binds
    @Singleton
    @IntoMap
    @StringKey(GpsSpeedMetricSource.METRIC_KEY)
    abstract fun bindSpeedSource(impl: GpsSpeedMetricSource): MetricSource

    @Binds
    @Singleton
    @IntoMap
    @StringKey(GpsAltitudeMetricSource.METRIC_KEY)
    abstract fun bindAltitudeSource(impl: GpsAltitudeMetricSource): MetricSource
}
