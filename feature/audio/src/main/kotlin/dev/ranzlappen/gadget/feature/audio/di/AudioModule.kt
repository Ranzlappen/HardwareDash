package dev.ranzlappen.gadget.feature.audio.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.audio.DbMeterMetricSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    @IntoMap
    @StringKey(DbMeterMetricSource.METRIC_KEY)
    abstract fun bindDbMeterMetricSource(impl: DbMeterMetricSource): MetricSource
}
