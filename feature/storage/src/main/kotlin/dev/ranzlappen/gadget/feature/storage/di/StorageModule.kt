package dev.ranzlappen.gadget.feature.storage.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.storage.StorageFreeGbMetricSource
import dev.ranzlappen.gadget.feature.storage.StorageUsedPercentMetricSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    @IntoMap
    @StringKey(StorageUsedPercentMetricSource.METRIC_KEY)
    abstract fun bindUsedPercentSource(impl: StorageUsedPercentMetricSource): MetricSource

    @Binds
    @Singleton
    @IntoMap
    @StringKey(StorageFreeGbMetricSource.METRIC_KEY)
    abstract fun bindFreeGbSource(impl: StorageFreeGbMetricSource): MetricSource
}
