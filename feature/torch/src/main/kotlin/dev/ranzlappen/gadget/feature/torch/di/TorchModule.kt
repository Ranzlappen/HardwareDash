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
 * implementation.
 *
 * Phase 2 / Batch 1 + 1.1 ship only the standard-flavor
 * [StandardTorchController] (Camera2-based). Rooted extras
 * (DutyCycleStrobe, MultiLedOrchestrator, ThermalOverrideController)
 * are deferred — they need `RootCapabilityRegistry` + `RootSafetyGate`
 * to land first.
 *
 * Future flavor-specific binding pattern: rooted-only torch surface
 * ships as a sibling `:feature:torch-rooted` Gradle module (mirroring
 * `:feature:lock-rooted` / `:feature:diagnostics-rooted` etc.) wired
 * into `:app` via `rootedImplementation`. The sibling module ships
 * its own Hilt module that supersedes this binding for the rooted
 * variant. The standard APK is physically unable to compile against
 * the rooted module — see CLAUDE.md's "Standard-APK leak gate"
 * section.
 *
 * In-module `src/standard/` + `src/rooted/` source sets are NOT used
 * for feature-level rooted code — the codebase convention is sibling
 * `<name>-rooted` modules. See
 * https://github.com/Ranzlappen/HardwareDash/issues/94 for the
 * pickup plan.
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
