package dev.ranzlappen.gadget.core.monitoring.di

import dagger.Module
import dagger.multibindings.Multibinds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.core.monitoring.MonitorWidgetNotifier

/**
 * Declares the cross-module multibinding maps so they're injectable even
 * when no feature has contributed yet (empty map). Features add entries
 * via `@Binds @IntoMap @StringKey(metricKey)` in their own modules.
 */
@Module
@InstallIn(SingletonComponent::class)
interface MonitoringBindsModule {

    @Multibinds
    fun metricSources(): Map<String, MetricSource>

    @Multibinds
    fun monitorWidgetNotifiers(): Map<String, MonitorWidgetNotifier>
}
