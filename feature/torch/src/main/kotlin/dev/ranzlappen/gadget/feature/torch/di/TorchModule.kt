package dev.ranzlappen.gadget.feature.torch.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.feature.torch.StandardTorchController
import dev.ranzlappen.gadget.feature.torch.TorchController
import javax.inject.Singleton

/**
 * Hilt module that binds the singleton [TorchController]
 * implementation. Phase 2 / Batch 1 ships only the standard-flavor
 * [StandardTorchController] (Camera2-based); a rooted-flavor module
 * will override this binding from the `rooted` source set when the
 * RootCapabilityRegistry infrastructure is ported.
 *
 * `@InstallIn(SingletonComponent::class)` so the controller's
 * lifecycle matches the app's — its CameraManager.TorchCallback
 * subscription stays live for the whole process.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TorchModule {

    @Binds
    @Singleton
    abstract fun bindTorchController(
        impl: StandardTorchController,
    ): TorchController
}
