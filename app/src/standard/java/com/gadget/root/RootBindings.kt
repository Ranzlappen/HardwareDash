package com.gadget.root

import com.gadget.root.companion.CompanionModuleDetector
import com.gadget.root.core.RootDetector
import com.gadget.root.core.RootService
import com.gadget.root.core.RootShell
import com.gadget.root.launch.LaunchGate
import com.gadget.battery.BatteryController
import com.gadget.battery.StandardBatteryController
import com.gadget.camera.CameraController
import com.gadget.camera.StandardCameraController
import com.gadget.microphone.MicrophoneController
import com.gadget.microphone.StandardMicrophoneController
import com.gadget.root.sysfs.StandardSysfsMutationLog
import com.gadget.root.sysfs.SysfsMutationLog
import com.gadget.sensors.SensorsController
import com.gadget.sensors.StandardSensorsController
import com.gadget.torch.StandardTorchController
import com.gadget.torch.TorchController
import com.gadget.vibration.StandardVibrationController
import com.gadget.vibration.VibrationController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Standard-flavor Hilt bindings for the rooted-features safety framework and
 * root core layer. Every binding resolves to a no-op so shared code can
 * inject the same interfaces in both flavors without branching on
 * `BuildConfig.IS_ROOTED`.
 *
 * The fully-qualified class name MUST match the rooted flavor's binding file
 * (`com.gadget.root.RootBindings`) — Gradle source-set merging picks one of
 * the two based on the active product flavor, so a name mismatch would let
 * both compile into the rooted APK and cause a duplicate-binding crash.
 */
@Module
@InstallIn(SingletonComponent::class)
object RootBindings {

    @Provides
    @Singleton
    fun provideRootCapabilityRegistry(): RootCapabilityRegistry =
        NoOpRootCapabilityRegistry()

    @Provides
    @Singleton
    fun provideRootSafetyGate(): RootSafetyGate = NoOpRootSafetyGate()

    @Provides
    @Singleton
    fun provideRootSoftLimiter(): RootSoftLimiter = NoOpRootSoftLimiter()

    @Provides
    @Singleton
    fun provideRootFeatureToggles(): RootFeatureToggles = NoOpRootFeatureToggles()

    // ──── Batch 2: root core layer ────

    @Provides
    @Singleton
    fun provideRootDetector(): RootDetector = NoOpRootDetector()

    @Provides
    @Singleton
    fun provideRootShell(): RootShell = NoOpRootShell()

    @Provides
    @Singleton
    fun provideRootService(): RootService = NoOpRootService()

    @Provides
    @Singleton
    fun provideLaunchGate(): LaunchGate = NoOpLaunchGate()

    @Provides
    @Singleton
    fun provideCompanionModuleDetector(): CompanionModuleDetector =
        NoOpCompanionModuleDetector()

    // ──── Batch 3: feature controllers ────

    @Provides
    @Singleton
    fun provideTorchController(impl: StandardTorchController): TorchController = impl

    @Provides
    @Singleton
    fun provideVibrationController(impl: StandardVibrationController): VibrationController = impl

    @Provides
    @Singleton
    fun provideCameraController(impl: StandardCameraController): CameraController = impl

    @Provides
    @Singleton
    fun provideMicrophoneController(impl: StandardMicrophoneController): MicrophoneController = impl

    // ──── Batch 5: sensors + battery + sysfs mutation log ────

    @Provides
    @Singleton
    fun provideSysfsMutationLog(impl: StandardSysfsMutationLog): SysfsMutationLog = impl

    @Provides
    @Singleton
    fun provideSensorsController(impl: StandardSensorsController): SensorsController = impl

    @Provides
    @Singleton
    fun provideBatteryController(impl: StandardBatteryController): BatteryController = impl
}
