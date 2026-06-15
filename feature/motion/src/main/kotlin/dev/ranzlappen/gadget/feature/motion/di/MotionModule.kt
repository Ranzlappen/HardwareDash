package dev.ranzlappen.gadget.feature.motion.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.motion.MotionDetectedMetricSource
import dev.ranzlappen.gadget.feature.motion.RotationRateMetricSource
import dev.ranzlappen.gadget.feature.motion.StepCounterMetricSource
import javax.inject.Singleton

/**
 * Contributes the motion feature's readable signals into the shared
 * `Map<String, MetricSource>` multibinding — one definition each, consumed
 * by monitoring, the automation engine, and `:core:hardware`'s registry
 * (sensors' `SensorsModule` binding is the reference).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MotionModule {

    @Binds
    @Singleton
    @IntoMap
    @StringKey(RotationRateMetricSource.METRIC_KEY)
    abstract fun bindRotationRate(impl: RotationRateMetricSource): MetricSource

    @Binds
    @Singleton
    @IntoMap
    @StringKey(StepCounterMetricSource.METRIC_KEY)
    abstract fun bindStepCounter(impl: StepCounterMetricSource): MetricSource

    @Binds
    @Singleton
    @IntoMap
    @StringKey(MotionDetectedMetricSource.METRIC_KEY)
    abstract fun bindMotionDetected(impl: MotionDetectedMetricSource): MetricSource
}
