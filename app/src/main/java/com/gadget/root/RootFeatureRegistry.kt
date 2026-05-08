package com.gadget.root

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val EXTREME_OPS_LOW_CAP = 3
private const val EXTREME_OPS_MED_CAP = 5
private const val EXTREME_OPS_HIGH_CAP = 10
private const val SINGLE_INVOCATION_CAP = 1

private const val CAMERA_HEAVY_WINDOW_SECONDS = 120
private const val CAMERA_RAW_WINDOW_SECONDS = 30
private const val CAMERA_SHUTTER_SOUND_WINDOW_SECONDS = 300
private const val MIC_SYSTEM_AUDIO_WINDOW_SECONDS = 120

/**
 * Single source of truth mapping every [RootFeatureKey] to its
 * [RootFeatureDescriptor]. Both flavors read this; the standard flavor's
 * gate ignores the descriptors but the registry compiles in both source
 * sets so feature-detection UI can render uniformly.
 *
 * Adding a new rooted feature: declare a [RootFeatureKey], add an entry
 * here, expose it through the relevant controller. No flavor-specific code
 * changes elsewhere.
 */
@Singleton
class RootFeatureRegistry @Inject constructor() {

    private val descriptors: Map<RootFeatureKey, RootFeatureDescriptor> = mapOf(
        RootFeatureKey.BackupFullData to RootFeatureDescriptor(
            key = RootFeatureKey.BackupFullData,
            defaultOn = false,
            limit = null,
            requiresExplicitConfirm = true,
        ),
        RootFeatureKey.WifiInternalTools to RootFeatureDescriptor(
            key = RootFeatureKey.WifiInternalTools,
            defaultOn = false,
            limit = null,
            requiresExplicitConfirm = true,
        ),
        RootFeatureKey.TorchExtremeBrightness to RootFeatureDescriptor(
            key = RootFeatureKey.TorchExtremeBrightness,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = true,
        ),
        RootFeatureKey.TorchHighFrequencyStrobe to RootFeatureDescriptor(
            key = RootFeatureKey.TorchHighFrequencyStrobe,
            defaultOn = false,
            limit = RootLimitPolicy(window = 30.seconds, maxInvocations = EXTREME_OPS_LOW_CAP),
            requiresExplicitConfirm = true,
        ),
        RootFeatureKey.TorchMultiLed to RootFeatureDescriptor(
            key = RootFeatureKey.TorchMultiLed,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
        ),
        RootFeatureKey.TorchThermalOverride to RootFeatureDescriptor(
            key = RootFeatureKey.TorchThermalOverride,
            defaultOn = false,
            limit = RootLimitPolicy(window = 5.minutes, maxInvocations = SINGLE_INVOCATION_CAP),
            requiresExplicitConfirm = true,
        ),
        RootFeatureKey.VibrationExtremeAmplitude to RootFeatureDescriptor(
            key = RootFeatureKey.VibrationExtremeAmplitude,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
        ),
        RootFeatureKey.VibrationDirectPwm to RootFeatureDescriptor(
            key = RootFeatureKey.VibrationDirectPwm,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_HIGH_CAP),
            requiresExplicitConfirm = false,
        ),
        RootFeatureKey.VibrationDualActuator to RootFeatureDescriptor(
            key = RootFeatureKey.VibrationDualActuator,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
        ),
        RootFeatureKey.VibrationSustainedRumble to RootFeatureDescriptor(
            key = RootFeatureKey.VibrationSustainedRumble,
            defaultOn = false,
            limit = RootLimitPolicy(window = 5.minutes, maxInvocations = SINGLE_INVOCATION_CAP),
            requiresExplicitConfirm = true,
        ),

        // ──── Batch-4 Camera features ────
        RootFeatureKey.CameraHighFps to RootFeatureDescriptor(
            key = RootFeatureKey.CameraHighFps,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
        ),
        RootFeatureKey.CameraManualOverride to RootFeatureDescriptor(
            key = RootFeatureKey.CameraManualOverride,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_HIGH_CAP),
            requiresExplicitConfirm = false,
        ),
        RootFeatureKey.CameraRawCapture to RootFeatureDescriptor(
            key = RootFeatureKey.CameraRawCapture,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = CAMERA_RAW_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_MED_CAP,
            ),
            requiresExplicitConfirm = false,
        ),
        RootFeatureKey.CameraMultiSimultaneous to RootFeatureDescriptor(
            key = RootFeatureKey.CameraMultiSimultaneous,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = CAMERA_HEAVY_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
        ),
        RootFeatureKey.CameraHalBypass to RootFeatureDescriptor(
            key = RootFeatureKey.CameraHalBypass,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = CAMERA_HEAVY_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
        ),
        RootFeatureKey.CameraShutterSoundOverride to RootFeatureDescriptor(
            key = RootFeatureKey.CameraShutterSoundOverride,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = CAMERA_SHUTTER_SOUND_WINDOW_SECONDS.seconds,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
        ),

        // ──── Batch-4 Microphone features ────
        RootFeatureKey.MicGainBoost to RootFeatureDescriptor(
            key = RootFeatureKey.MicGainBoost,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
        ),
        RootFeatureKey.MicDirectPcm to RootFeatureDescriptor(
            key = RootFeatureKey.MicDirectPcm,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
        ),
        RootFeatureKey.MicCustomSampleRate to RootFeatureDescriptor(
            key = RootFeatureKey.MicCustomSampleRate,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_LOW_CAP),
            requiresExplicitConfirm = true,
        ),
        RootFeatureKey.MicMultiMicRaw to RootFeatureDescriptor(
            key = RootFeatureKey.MicMultiMicRaw,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_LOW_CAP),
            requiresExplicitConfirm = false,
        ),
        RootFeatureKey.MicNoiseFloorOverride to RootFeatureDescriptor(
            key = RootFeatureKey.MicNoiseFloorOverride,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
        ),
        RootFeatureKey.MicSystemAudioCapture to RootFeatureDescriptor(
            key = RootFeatureKey.MicSystemAudioCapture,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = MIC_SYSTEM_AUDIO_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
        ),
    )

    fun descriptor(key: RootFeatureKey): RootFeatureDescriptor =
        descriptors[key] ?: error("No descriptor registered for $key")

    fun allDescriptors(): Collection<RootFeatureDescriptor> = descriptors.values
}
