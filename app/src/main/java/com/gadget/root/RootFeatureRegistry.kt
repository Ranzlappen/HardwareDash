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

private const val AUTOMATION_DESTRUCTIVE_WINDOW_MINUTES = 5
private const val AUTOMATION_INTENT_WINDOW_SECONDS = 60
private const val AUTOMATION_DUMPSYS_WINDOW_SECONDS = 60
private const val NOTIFICATION_OVERLAY_WINDOW_SECONDS = 30
private const val KEEPALIVE_PRIVILEGED_WINDOW_MINUTES = 5

private const val STORAGE_TRIM_WINDOW_MINUTES = 5
private const val STORAGE_DUMP_WINDOW_SECONDS = 60
private const val DISPLAY_BRIGHTNESS_WINDOW_SECONDS = 60
private const val DISPLAY_OVERRIDE_WINDOW_SECONDS = 30
private const val DISPLAY_DENSITY_WINDOW_MINUTES = 5
private const val AUDIO_VOLUME_BYPASS_WINDOW_SECONDS = 60
private const val AUDIO_ROUTING_WINDOW_SECONDS = 30
private const val AUDIO_MUTE_WINDOW_MINUTES = 5
private const val AUDIO_DUMP_WINDOW_SECONDS = 60

private const val BATTERY_HOLD_SOC_WINDOW_MINUTES = 5
private const val BATTERY_HEALTH_DEEP_DUMP_WINDOW_SECONDS = 60
private const val BATTERY_WIRELESS_COIL_WINDOW_MINUTES = 5
private const val ADB_TOGGLE_WINDOW_SECONDS = 60
private const val ADB_NETWORK_WINDOW_MINUTES = 5
private const val ADB_DUMP_WINDOW_SECONDS = 60
private const val ADB_SETPROP_WINDOW_SECONDS = 60
private const val USB_FUNCTION_WINDOW_SECONDS = 60
private const val USB_DUMP_WINDOW_SECONDS = 60
private const val DIAGNOSTICS_DUMP_WINDOW_SECONDS = 60

private const val GPS_OVERRIDE_WINDOW_SECONDS = 60
private const val GPS_OVERRIDE_INVOCATION_CAP = 60
private const val GPS_ROUTE_WINDOW_SECONDS = 60
private const val GPS_ROUTE_INVOCATION_CAP = 30
private const val GPS_LSPOSED_INSTALL_WINDOW_MINUTES = 5
private const val GPS_LSPOSED_INSTALL_CAP = 1

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
            requiresExplicitConfirm = true,
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
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.VibrationDirectPwm to RootFeatureDescriptor(
            key = RootFeatureKey.VibrationDirectPwm,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_HIGH_CAP),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.VibrationDualActuator to RootFeatureDescriptor(
            key = RootFeatureKey.VibrationDualActuator,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_MED_CAP),
            requiresExplicitConfirm = true,
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
            requiresExplicitConfirm = true,
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
            requiresExplicitConfirm = true,
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
            requiresExplicitConfirm = true,
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
            requiresExplicitConfirm = true,
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
            requiresExplicitConfirm = true,
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
            requiresExplicitConfirm = true,
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
            requiresExplicitConfirm = true,
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
            requiresExplicitConfirm = true,
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

        // ──── Batch-7 Automation features ────
        RootFeatureKey.AutomationPrivilegedIntent to RootFeatureDescriptor(
            key = RootFeatureKey.AutomationPrivilegedIntent,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = AUTOMATION_INTENT_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.AutomationSystemSettingsOverride to RootFeatureDescriptor(
            key = RootFeatureKey.AutomationSystemSettingsOverride,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = AUTOMATION_DESTRUCTIVE_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.AutomationDumpsysSnapshot to RootFeatureDescriptor(
            key = RootFeatureKey.AutomationDumpsysSnapshot,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = AUTOMATION_DUMPSYS_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_HIGH_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),

        // ──── Batch-7 Notification features ────
        RootFeatureKey.NotificationStickyOverride to RootFeatureDescriptor(
            key = RootFeatureKey.NotificationStickyOverride,
            defaultOn = false,
            limit = RootLimitPolicy(window = 60.seconds, maxInvocations = EXTREME_OPS_LOW_CAP),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.NotificationListenerAccess to RootFeatureDescriptor(
            key = RootFeatureKey.NotificationListenerAccess,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = AUTOMATION_DESTRUCTIVE_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.NotificationLockScreenOverlay to RootFeatureDescriptor(
            key = RootFeatureKey.NotificationLockScreenOverlay,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = NOTIFICATION_OVERLAY_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),

        // ──── Batch-7 Keep-Alive features ────
        RootFeatureKey.KeepAliveDozeBypass to RootFeatureDescriptor(
            key = RootFeatureKey.KeepAliveDozeBypass,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = KEEPALIVE_PRIVILEGED_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.KeepAlivePmGrant to RootFeatureDescriptor(
            key = RootFeatureKey.KeepAlivePmGrant,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = KEEPALIVE_PRIVILEGED_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),

        // ──── Batch-8 Storage features ────
        RootFeatureKey.StorageDumpDiskstats to RootFeatureDescriptor(
            key = RootFeatureKey.StorageDumpDiskstats,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = STORAGE_DUMP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_MED_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
        RootFeatureKey.StorageEnumerateMounts to RootFeatureDescriptor(
            key = RootFeatureKey.StorageEnumerateMounts,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = STORAGE_DUMP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_HIGH_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
        RootFeatureKey.StorageFstrim to RootFeatureDescriptor(
            key = RootFeatureKey.StorageFstrim,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = STORAGE_TRIM_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.StorageDropCaches to RootFeatureDescriptor(
            key = RootFeatureKey.StorageDropCaches,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = STORAGE_DUMP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),

        // ──── Batch-8 Display features ────
        RootFeatureKey.DisplayBrightnessOverride to RootFeatureDescriptor(
            key = RootFeatureKey.DisplayBrightnessOverride,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = DISPLAY_BRIGHTNESS_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.DisplayRefreshRateOverride to RootFeatureDescriptor(
            key = RootFeatureKey.DisplayRefreshRateOverride,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = DISPLAY_OVERRIDE_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.DisplayDensityOverride to RootFeatureDescriptor(
            key = RootFeatureKey.DisplayDensityOverride,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = DISPLAY_DENSITY_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.DisplaySurfaceFlingerSnapshot to RootFeatureDescriptor(
            key = RootFeatureKey.DisplaySurfaceFlingerSnapshot,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = DISPLAY_BRIGHTNESS_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_MED_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),

        // ──── Batch-8 Audio routing features ────
        RootFeatureKey.AudioStreamVolumeBypass to RootFeatureDescriptor(
            key = RootFeatureKey.AudioStreamVolumeBypass,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = AUDIO_VOLUME_BYPASS_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.AudioForceRouting to RootFeatureDescriptor(
            key = RootFeatureKey.AudioForceRouting,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = AUDIO_ROUTING_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.AudioMuteAllStreams to RootFeatureDescriptor(
            key = RootFeatureKey.AudioMuteAllStreams,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = AUDIO_MUTE_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.AudioDumpAudioPolicy to RootFeatureDescriptor(
            key = RootFeatureKey.AudioDumpAudioPolicy,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = AUDIO_DUMP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_MED_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),

        // ──── Batch-9 Battery deep-dive features ────
        RootFeatureKey.BatteryHoldSoc to RootFeatureDescriptor(
            key = RootFeatureKey.BatteryHoldSoc,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = BATTERY_HOLD_SOC_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.BatteryHealthDeepDump to RootFeatureDescriptor(
            key = RootFeatureKey.BatteryHealthDeepDump,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = BATTERY_HEALTH_DEEP_DUMP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_MED_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
        RootFeatureKey.BatteryWirelessCoilCurrent to RootFeatureDescriptor(
            key = RootFeatureKey.BatteryWirelessCoilCurrent,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = BATTERY_WIRELESS_COIL_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),

        // ──── Batch-9 ADB Debugging features ────
        RootFeatureKey.AdbToggleEnabled to RootFeatureDescriptor(
            key = RootFeatureKey.AdbToggleEnabled,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = ADB_TOGGLE_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.AdbOverNetwork to RootFeatureDescriptor(
            key = RootFeatureKey.AdbOverNetwork,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = ADB_NETWORK_WINDOW_MINUTES.minutes,
                maxInvocations = SINGLE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.AdbDumpProperties to RootFeatureDescriptor(
            key = RootFeatureKey.AdbDumpProperties,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = ADB_DUMP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_HIGH_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
        RootFeatureKey.AdbSetpropOverride to RootFeatureDescriptor(
            key = RootFeatureKey.AdbSetpropOverride,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = ADB_SETPROP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),

        // ──── Batch-9 USB Debugging features ────
        RootFeatureKey.UsbSwitchFunction to RootFeatureDescriptor(
            key = RootFeatureKey.UsbSwitchFunction,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = USB_FUNCTION_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_LOW_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.UsbDumpUsb to RootFeatureDescriptor(
            key = RootFeatureKey.UsbDumpUsb,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = USB_DUMP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_HIGH_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
        RootFeatureKey.UsbDumpSerialService to RootFeatureDescriptor(
            key = RootFeatureKey.UsbDumpSerialService,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = USB_DUMP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_HIGH_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
        RootFeatureKey.UsbDumpUsbDevicesDebug to RootFeatureDescriptor(
            key = RootFeatureKey.UsbDumpUsbDevicesDebug,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = USB_DUMP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_MED_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),

        // ──── Batch-10 Diagnostics features ────
        RootFeatureKey.DiagnosticsTailLogcat to RootFeatureDescriptor(
            key = RootFeatureKey.DiagnosticsTailLogcat,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = DIAGNOSTICS_DUMP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_HIGH_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
        RootFeatureKey.DiagnosticsDumpMemInfo to RootFeatureDescriptor(
            key = RootFeatureKey.DiagnosticsDumpMemInfo,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = DIAGNOSTICS_DUMP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_HIGH_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
        RootFeatureKey.DiagnosticsDumpCpuInfo to RootFeatureDescriptor(
            key = RootFeatureKey.DiagnosticsDumpCpuInfo,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = DIAGNOSTICS_DUMP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_HIGH_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),
        RootFeatureKey.DiagnosticsDumpProcstats to RootFeatureDescriptor(
            key = RootFeatureKey.DiagnosticsDumpProcstats,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = DIAGNOSTICS_DUMP_WINDOW_SECONDS.seconds,
                maxInvocations = EXTREME_OPS_MED_CAP,
            ),
            requiresExplicitConfirm = false,
            isWriteCapable = false,
        ),

        // ──── Batch-13 GPS spoofing features ────
        RootFeatureKey.GpsLocationOverride to RootFeatureDescriptor(
            key = RootFeatureKey.GpsLocationOverride,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = GPS_OVERRIDE_WINDOW_SECONDS.seconds,
                maxInvocations = GPS_OVERRIDE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.GpsRouteSimulation to RootFeatureDescriptor(
            key = RootFeatureKey.GpsRouteSimulation,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = GPS_ROUTE_WINDOW_SECONDS.seconds,
                maxInvocations = GPS_ROUTE_INVOCATION_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
        RootFeatureKey.GpsLsposedHookInstall to RootFeatureDescriptor(
            key = RootFeatureKey.GpsLsposedHookInstall,
            defaultOn = false,
            limit = RootLimitPolicy(
                window = GPS_LSPOSED_INSTALL_WINDOW_MINUTES.minutes,
                maxInvocations = GPS_LSPOSED_INSTALL_CAP,
            ),
            requiresExplicitConfirm = true,
            isWriteCapable = true,
        ),
    )

    fun descriptor(key: RootFeatureKey): RootFeatureDescriptor =
        descriptors[key] ?: error("No descriptor registered for $key")

    fun allDescriptors(): Collection<RootFeatureDescriptor> = descriptors.values
}
