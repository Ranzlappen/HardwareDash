package dev.ranzlappen.gadget.feature.vibration.rooted.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.feature.vibration.VibrationRootCapabilities
import dev.ranzlappen.gadget.feature.vibration.rooted.RootedVibrationRootCapabilities
import javax.inject.Singleton

/**
 * Hilt module wiring the rooted-flavor Vibration capabilities.
 *
 * Binds [VibrationRootCapabilities] -> [RootedVibrationRootCapabilities] (the
 * libsu/sysfs-backed adapter the `:feature:vibration` screen consumes for its
 * extreme-tier controls). Lives in `:feature:vibration-rooted` (pulled in via
 * `rootedImplementation`) so the rooted-Vibration graph is self-contained,
 * matching the `:feature:torch-rooted` blueprint.
 *
 * `InstallIn(SingletonComponent::class)` so the controller stays alive for the
 * process lifetime — the libsu shell binder doesn't tolerate per-Activity
 * scoping and the adapter holds hot StateFlows for the monitoring metric.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RootedVibrationModule {

    @Binds
    @Singleton
    abstract fun bindVibrationRootCapabilities(
        impl: RootedVibrationRootCapabilities,
    ): VibrationRootCapabilities
}
