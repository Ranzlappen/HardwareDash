package dev.ranzlappen.gadget.feature.radios.bt.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.radios.bt.BtEnabledMetricSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BtModule {

    @Binds
    @Singleton
    @IntoMap
    @StringKey(BtEnabledMetricSource.METRIC_KEY)
    abstract fun bindBtEnabledMetricSource(impl: BtEnabledMetricSource): MetricSource
}
