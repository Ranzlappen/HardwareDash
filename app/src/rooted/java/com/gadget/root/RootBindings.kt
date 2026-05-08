package com.gadget.root

import com.gadget.root.companion.CompanionModuleDetector
import com.gadget.root.companion.RootedCompanionModuleDetector
import com.gadget.root.core.RootDetector
import com.gadget.root.core.RootService
import com.gadget.root.core.RootShell
import com.gadget.root.core.RootedRootDetector
import com.gadget.root.core.RootedRootService
import com.gadget.root.core.RootedRootShell
import com.gadget.root.launch.LaunchGate
import com.gadget.root.launch.RootedLaunchGate
import com.gadget.camera.CameraController
import com.gadget.camera.RootedCameraController
import com.gadget.microphone.MicrophoneController
import com.gadget.microphone.RootedMicrophoneController
import com.gadget.torch.RootedTorchController
import com.gadget.torch.TorchController
import com.gadget.vibration.RootedVibrationController
import com.gadget.vibration.VibrationController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Rooted-flavor Hilt bindings. Same fully-qualified name as the standard
 * flavor's binding file; Gradle picks one based on the active product flavor
 * so the two never collide in a single APK.
 *
 * Uses `@Binds` instead of `@Provides` because each implementation is itself
 * `@Singleton` and constructor-injected — Hilt builds them, this module
 * just wires the interface → impl mapping.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RootBindings {

    @Binds @Singleton
    abstract fun bindRootCapabilityRegistry(impl: RootedRootCapabilityRegistry): RootCapabilityRegistry

    @Binds @Singleton
    abstract fun bindRootSafetyGate(impl: RootedRootSafetyGate): RootSafetyGate

    @Binds @Singleton
    abstract fun bindRootSoftLimiter(impl: RootedRootSoftLimiter): RootSoftLimiter

    @Binds @Singleton
    abstract fun bindRootFeatureToggles(impl: RootedRootFeatureToggles): RootFeatureToggles

    // ──── Batch 2: root core layer ────

    @Binds @Singleton
    abstract fun bindRootDetector(impl: RootedRootDetector): RootDetector

    @Binds @Singleton
    abstract fun bindRootShell(impl: RootedRootShell): RootShell

    @Binds @Singleton
    abstract fun bindRootService(impl: RootedRootService): RootService

    @Binds @Singleton
    abstract fun bindLaunchGate(impl: RootedLaunchGate): LaunchGate

    @Binds @Singleton
    abstract fun bindCompanionModuleDetector(impl: RootedCompanionModuleDetector): CompanionModuleDetector

    // ──── Batch 3: feature controllers ────

    @Binds @Singleton
    abstract fun bindTorchController(impl: RootedTorchController): TorchController

    @Binds @Singleton
    abstract fun bindVibrationController(impl: RootedVibrationController): VibrationController

    @Binds @Singleton
    abstract fun bindCameraController(impl: RootedCameraController): CameraController

    @Binds @Singleton
    abstract fun bindMicrophoneController(impl: RootedMicrophoneController): MicrophoneController
}
