package com.gadget.root

import com.gadget.torch.TorchController
import com.gadget.vibration.VibrationController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point for the rooted-features module. Composable code in
 * `src/main` reaches the controllers + capability/toggle services via
 * `EntryPointAccessors.fromApplication(...)` rather than `@Inject`, since
 * `@Composable` functions can't take constructor parameters.
 *
 * Mirrors the `AppsEntryPoint` shape.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RootFeaturesEntryPoint {
    fun capabilityRegistry(): RootCapabilityRegistry
    fun featureRegistry(): RootFeatureRegistry
    fun featureToggles(): RootFeatureToggles
    fun torchController(): TorchController
    fun vibrationController(): VibrationController
}
