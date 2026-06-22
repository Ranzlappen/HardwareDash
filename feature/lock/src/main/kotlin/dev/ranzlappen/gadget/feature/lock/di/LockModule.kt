package dev.ranzlappen.gadget.feature.lock.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.lock.LockStateMetricSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LockModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(LockStateMetricSource.METRIC_KEY)
    abstract fun bindLockStateMetricSource(impl: LockStateMetricSource): MetricSource
}
