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

private const val SENSORS_OVERCLOCK_WINDOW_MINUTES = 5
private const val SENSORS_EXPERT_POLL_WINDOW_MINUTES = 5
private const val BATTERY_DANGEROUS_OPS_WINDOW_MINUTES = 10
private const val BATTERY_FULL_DUMP_WINDOW_SECONDS = 60

private const val WIFI_REGULATORY_WINDOW_MINUTES = 5
private const val BT_REGULATORY_WINDOW_MINUTES = 5
private const val IR_BURST_WINDOW_SECONDS = 60
private const val NFC_NCI_WINDOW_SECONDS = 60

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
            isWriteCapable = true,
        ),
        RootFeatureKey.TorchHighFrequencyStrobe to RootFeatureDescriptor(
            key = RootFeatureKey.TorchHighFrequencyStrobe,
            defaultOn = false,
            limit = RootLimitPolicy(window = 30.seconds, maxInvocations = EXTREME_OPS_LOW_CAP),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.TorchMultiLed to RootFeatureDescriptor(
            key = RootFeatureKey.TorchMultiLed,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = true,
        ),
        RootFeatureKey.TorchThermalOverride to RootFeatureDescriptor(
            key = RootFeatureKey.TorchThermalOverride,
            defaultOn = false,
            limit = RootLimitPolicy(window = 5.minutes, maxInvocations = SINGLE_INVOCATION_CAP),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.VibrationExtremeAmplitude to RootFeatureDescriptor(
            key = RootFeatureKey.VibrationExtremeAmplitude,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = true,
        ),
        RootFeatureKey.VibrationDirectPwm to RootFeatureDescriptor(
            key = RootFeatureKey.VibrationDirectPwm,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_HIGH_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = true,
        ),
        RootFeatureKey.VibrationDualActuator to RootFeatureDescriptor(
            key = RootFeatureKey.VibrationDualActuator,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = true,
        ),
        RootFeatureKey.VibrationSustainedRumble to RootFeatureDescriptor(
            key = RootFeatureKey.VibrationSustainedRumble,
            defaultOn = false,
            limit = RootLimitPolicy(window = 5.minutes, maxInvocations = SINGLE_INVOCATION_CAP),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
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
            isWriteCapable = true,
        ),

        // ──── Batch-4 Microphone features ────
        RootFeatureKey.MicGainBoost to RootFeatureDescriptor(
            key = RootFeatureKey.MicGainBoost,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = true,
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
            isWriteCapable = true,
        ),
        RootFeatureKey.MicSystemAudioCapture to RootFeatureDescriptor(
            key = RootFeatureKey.MicSystemAudioCapture,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = MIC_SYSTEM_AUDIO_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),

        // ──── Batch-5 Sensors features ────
        RootFeatureKey.SensorsHighPolling to RootFeatureDescriptor(
            key = RootFeatureKey.SensorsHighPolling,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = true,
        ),
        RootFeatureKey.SensorsHighPollingExpert to RootFeatureDescriptor(
            key = RootFeatureKey.SensorsHighPollingExpert,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = SENSORS_EXPERT_POLL_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.SensorsRawUnfiltered to RootFeatureDescriptor(
            key = RootFeatureKey.SensorsRawUnfiltered,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_HIGH_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = true,
        ),
        RootFeatureKey.SensorsSysfsRead to RootFeatureDescriptor(
            key = RootFeatureKey.SensorsSysfsRead,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_HIGH_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
        RootFeatureKey.SensorsOverclock to RootFeatureDescriptor(
            key = RootFeatureKey.SensorsOverclock,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = SENSORS_OVERCLOCK_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.SensorsFusionOverride to RootFeatureDescriptor(
            key = RootFeatureKey.SensorsFusionOverride,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = true,
        ),
        RootFeatureKey.SensorsHiddenEnumeration to RootFeatureDescriptor(
            key = RootFeatureKey.SensorsHiddenEnumeration,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_HIGH_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),

        // ──── Batch-5 Battery features ────
        RootFeatureKey.BatteryFuelGaugeRaw to RootFeatureDescriptor(
            key = RootFeatureKey.BatteryFuelGaugeRaw,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_HIGH_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
        RootFeatureKey.BatteryCellMonitor to RootFeatureDescriptor(
            key = RootFeatureKey.BatteryCellMonitor,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_HIGH_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
        RootFeatureKey.BatteryChargingProfile to RootFeatureDescriptor(
            key = RootFeatureKey.BatteryChargingProfile,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = BATTERY_DANGEROUS_OPS_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.BatteryThermalBypass to RootFeatureDescriptor(
            key = RootFeatureKey.BatteryThermalBypass,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = BATTERY_DANGEROUS_OPS_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.BatteryChargingTypeOverride to RootFeatureDescriptor(
            key = RootFeatureKey.BatteryChargingTypeOverride,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_LOW_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = true,
        ),
        RootFeatureKey.BatteryFullDump to RootFeatureDescriptor(
            key = RootFeatureKey.BatteryFullDump,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = BATTERY_FULL_DUMP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_MED_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),

        // ──── Batch-6 Wi-Fi features ────
        RootFeatureKey.WifiRfkillToggle to RootFeatureDescriptor(
            key = RootFeatureKey.WifiRfkillToggle,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_LOW_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = true,
        ),
        RootFeatureKey.WifiTxPowerOverride to RootFeatureDescriptor(
            key = RootFeatureKey.WifiTxPowerOverride,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = WIFI_REGULATORY_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.WifiChannelOverride to RootFeatureDescriptor(
            key = RootFeatureKey.WifiChannelOverride,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_LOW_CAP),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.WifiInjectionProbe to RootFeatureDescriptor(
            key = RootFeatureKey.WifiInjectionProbe,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_HIGH_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),

        // ──── Batch-6 Bluetooth features ────
        RootFeatureKey.BluetoothRfkillToggle to RootFeatureDescriptor(
            key = RootFeatureKey.BluetoothRfkillToggle,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_LOW_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = true,
        ),
        RootFeatureKey.BluetoothTxPowerOverride to RootFeatureDescriptor(
            key = RootFeatureKey.BluetoothTxPowerOverride,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = BT_REGULATORY_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.BluetoothHciSnoopDump to RootFeatureDescriptor(
            key = RootFeatureKey.BluetoothHciSnoopDump,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_HIGH_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),

        // ──── Batch-6 NFC features ────
        RootFeatureKey.NfcRawNciCommand to RootFeatureDescriptor(
            key = RootFeatureKey.NfcRawNciCommand,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = NFC_NCI_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_MED_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),

        // ──── Batch-6 IR features ────
        RootFeatureKey.IrCustomCarrier to RootFeatureDescriptor(
            key = RootFeatureKey.IrCustomCarrier,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = IR_BURST_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.IrRawGpioPattern to RootFeatureDescriptor(
            key = RootFeatureKey.IrRawGpioPattern,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = IR_BURST_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),

        // ──── Batch-6 Cellular features ────
        RootFeatureKey.CellRawModemDump to RootFeatureDescriptor(
            key = RootFeatureKey.CellRawModemDump,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
        RootFeatureKey.CellSignalDeepDump to RootFeatureDescriptor(
            key = RootFeatureKey.CellSignalDeepDump,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_HIGH_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),

        // ──── Batch-6 GPS features ────
        RootFeatureKey.GpsNmeaRawTap to RootFeatureDescriptor(
            key = RootFeatureKey.GpsNmeaRawTap,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
        RootFeatureKey.GpsConstellationDump to RootFeatureDescriptor(
            key = RootFeatureKey.GpsConstellationDump,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_HIGH_CAP),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
    )

    fun descriptor(key: RootFeatureKey): RootFeatureDescriptor =
        descriptors[key] ?: error("No descriptor registered for $key")

    fun allDescriptors(): Collection<RootFeatureDescriptor> = descriptors.values
}
