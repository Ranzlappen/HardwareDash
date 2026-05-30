package dev.ranzlappen.gadget.feature.torch.rooted.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.feature.torch.TorchRootCapabilities
import dev.ranzlappen.gadget.feature.torch.legacy.LegacyTorchController
import dev.ranzlappen.gadget.feature.torch.rooted.RootedTorchController
import dev.ranzlappen.gadget.feature.torch.rooted.RootedTorchRootCapabilities
import javax.inject.Singleton

/**
 * Hilt module that wires the rooted-flavor Torch implementations.
 *
 * Two bindings:
 *  - [LegacyTorchController] -> [RootedTorchController]: the libsu-backed
 *    sysfs controller the legacy rooted-extras UI consumes via
 *    `RootFeaturesEntryPoint.legacyTorchController()`.
 *  - [TorchRootCapabilities] -> [RootedTorchRootCapabilities]: the
 *    modular adapter the new `:feature:torch` screen consumes for its
 *    rooted-tier extras (DutyCycleStrobe, MultiLed, ThermalOverride
 *    cards).
 *
 * Lives in `:feature:torch-rooted` (not `app/src/rooted/.../root/RootBindings.kt`)
 * so the rooted-Torch graph is self-contained inside the feature module,
 * matching the established `:feature:torch` blueprint pattern.
 * `RootBindings` keeps the remaining cross-feature bindings (audio, vibration,
 * camera, …) until each feature follows the same migration.
 *
 * **InstallIn(SingletonComponent::class)** so both controllers stay alive
 * for the process lifetime — the libsu Shell.RootService binder doesn't
 * tolerate per-Activity scoping, and the modular adapter holds a hot
 * StateFlow subscription on the modular `TorchController.state`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RootedTorchModule {

    @Binds
    @Singleton
    abstract fun bindLegacyTorchController(
        impl: RootedTorchController,
    ): LegacyTorchController

    @Binds
    @Singleton
    abstract fun bindTorchRootCapabilities(
        impl: RootedTorchRootCapabilities,
    ): TorchRootCapabilities
}
