package dev.ranzlappen.gadget.feature.vibration.standard.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.feature.vibration.VibrationRootCapabilities
import dev.ranzlappen.gadget.feature.vibration.standard.StandardVibrationRootCapabilities
import javax.inject.Singleton

/**
 * Hilt module wiring the standard-flavor (no-op) Vibration root capabilities —
 * the mirror of `:feature:vibration-rooted`'s `RootedVibrationModule`.
 *
 * Lives in `:feature:vibration-standard` (pulled in via `standardImplementation`)
 * so the standard-Vibration graph is self-contained in the feature module,
 * matching the `:feature:torch-standard` blueprint. Because each flavor's
 * sibling module contributes only to its own build variant, exactly one
 * `@Binds VibrationRootCapabilities` is on the classpath per APK — no
 * duplicate-binding clash.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StandardVibrationModule {

    @Binds
    @Singleton
    abstract fun bindVibrationRootCapabilities(
        impl: StandardVibrationRootCapabilities,
    ): VibrationRootCapabilities
}
