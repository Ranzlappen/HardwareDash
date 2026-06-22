package dev.ranzlappen.gadget.feature.ambient.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.ambient.AmbientLightMetricSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AmbientModule {

    @Binds
    @Singleton
    @IntoMap
    @StringKey(AmbientLightMetricSource.METRIC_KEY)
    abstract fun bindAmbientLightMetricSource(impl: AmbientLightMetricSource): MetricSource
}
