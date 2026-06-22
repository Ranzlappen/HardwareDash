package dev.ranzlappen.gadget.feature.radios.wifi.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.radios.wifi.WifiEnabledMetricSource
import dev.ranzlappen.gadget.feature.radios.wifi.WifiSignalMetricSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WifiModule {

    @Binds
    @Singleton
    @IntoMap
    @StringKey(WifiEnabledMetricSource.METRIC_KEY)
    abstract fun bindWifiEnabledMetricSource(impl: WifiEnabledMetricSource): MetricSource

    @Binds
    @Singleton
    @IntoMap
    @StringKey(WifiSignalMetricSource.METRIC_KEY)
    abstract fun bindWifiSignalMetricSource(impl: WifiSignalMetricSource): MetricSource
}
