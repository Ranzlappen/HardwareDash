package dev.ranzlappen.gadget.feature.battery.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.battery.BatteryLevelMetricSource
import dev.ranzlappen.gadget.feature.battery.BatteryTemperatureMetricSource
import dev.ranzlappen.gadget.feature.battery.BatteryVoltageMetricSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BatteryModule {

    @Binds @Singleton @IntoMap @StringKey(BatteryLevelMetricSource.METRIC_KEY)
    abstract fun bindLevelSource(impl: BatteryLevelMetricSource): MetricSource

    @Binds @Singleton @IntoMap @StringKey(BatteryTemperatureMetricSource.METRIC_KEY)
    abstract fun bindTemperatureSource(impl: BatteryTemperatureMetricSource): MetricSource

    @Binds @Singleton @IntoMap @StringKey(BatteryVoltageMetricSource.METRIC_KEY)
    abstract fun bindVoltageSource(impl: BatteryVoltageMetricSource): MetricSource
}
