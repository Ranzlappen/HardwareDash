package dev.ranzlappen.gadget.feature.radios.nfc.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.radios.nfc.NfcEnabledMetricSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NfcModule {

    @Binds
    @Singleton
    @IntoMap
    @StringKey(NfcEnabledMetricSource.METRIC_KEY)
    abstract fun bindNfcEnabledMetricSource(impl: NfcEnabledMetricSource): MetricSource
}
