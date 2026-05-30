package dev.ranzlappen.gadget.feature.torch.standard.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.feature.torch.TorchRootCapabilities
import dev.ranzlappen.gadget.feature.torch.standard.StandardTorchRootCapabilities
import dev.ranzlappen.gadget.feature.torch.standard.StandardTorchSysfsController
import dev.ranzlappen.gadget.feature.torch.sysfs.TorchSysfsController
import javax.inject.Singleton

/**
 * Hilt module that wires the standard-flavor (no-op) Torch implementations —
 * the mirror of `:feature:torch-rooted`'s `RootedTorchModule`.
 *
 * Two bindings, both inert on standard:
 *  - [TorchSysfsController] -> [StandardTorchSysfsController]: every privileged
 *    method returns `Unsupported` (the standard APK has no root shell).
 *  - [TorchRootCapabilities] -> [StandardTorchRootCapabilities]: the modular
 *    adapter the `:feature:torch` screen consumes; reports availability
 *    `Unavailable` so the rooted controls stay hidden and the per-function
 *    badges read red ("requires the rooted app version").
 *
 * Lives in `:feature:torch-standard` (not `app/src/standard/.../root/RootBindings.kt`)
 * so the standard-Torch graph is self-contained inside the feature module,
 * matching the `:feature:torch-rooted` blueprint pattern. `RootBindings` keeps
 * the remaining cross-feature no-ops (audio, vibration, camera, …) until each
 * feature follows the same migration.
 *
 * Because each flavor's sibling module (`-rooted` via `rootedImplementation`,
 * `-standard` via `standardImplementation`) contributes only to its own build
 * variant, exactly one of the two `@Binds` for each interface is on the
 * classpath per APK — no duplicate-binding clash.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StandardTorchModule {

    @Binds
    @Singleton
    abstract fun bindTorchSysfsController(
        impl: StandardTorchSysfsController,
    ): TorchSysfsController

    @Binds
    @Singleton
    abstract fun bindTorchRootCapabilities(
        impl: StandardTorchRootCapabilities,
    ): TorchRootCapabilities
}
