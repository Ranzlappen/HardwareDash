package dev.ranzlappen.gadget.feature.radios.cell.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.radios.cell.CellSignalMetricSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CellModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(CellSignalMetricSource.METRIC_KEY)
    abstract fun bindCellSignalMetricSource(impl: CellSignalMetricSource): MetricSource
}
