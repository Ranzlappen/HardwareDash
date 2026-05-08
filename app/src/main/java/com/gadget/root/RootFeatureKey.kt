package com.gadget.root

/**
 * Stable identifier for every rooted-only capability the app may gate behind
 * [RootSafetyGate]. New entries are added when their feature module lands.
 *
 * `id` is used as the suffix of DataStore preference keys, so it must never
 * change once shipped — renaming a key would silently reset the user's choice.
 */
sealed class RootFeatureKey(val id: String) {
    // ──── Original Batch-1 placeholders (kept for stability) ────
    data object BackupFullData : RootFeatureKey("backup_full_data")
    data object WifiInternalTools : RootFeatureKey("wifi_internal_tools")

    // ──── Batch-3 Torch features ────
    data object TorchExtremeBrightness : RootFeatureKey("torch_extreme_brightness")
    data object TorchHighFrequencyStrobe : RootFeatureKey("torch_high_freq_strobe")
    data object TorchMultiLed : RootFeatureKey("torch_multi_led")
    data object TorchThermalOverride : RootFeatureKey("torch_thermal_override")

    // ──── Batch-3 Vibration features ────
    data object VibrationExtremeAmplitude : RootFeatureKey("vibration_extreme_amplitude")
    data object VibrationDirectPwm : RootFeatureKey("vibration_direct_pwm")
    data object VibrationDualActuator : RootFeatureKey("vibration_dual_actuator")
    data object VibrationSustainedRumble : RootFeatureKey("vibration_sustained_rumble")

    // ──── Batch-4 Camera features ────
    data object CameraHighFps : RootFeatureKey("camera_high_fps")
    data object CameraManualOverride : RootFeatureKey("camera_manual_override")
    data object CameraRawCapture : RootFeatureKey("camera_raw_capture")
    data object CameraMultiSimultaneous : RootFeatureKey("camera_multi_simultaneous")
    data object CameraHalBypass : RootFeatureKey("camera_hal_bypass")
    data object CameraShutterSoundOverride : RootFeatureKey("camera_shutter_sound_override")

    // ──── Batch-4 Microphone features ────
    data object MicGainBoost : RootFeatureKey("mic_gain_boost")
    data object MicDirectPcm : RootFeatureKey("mic_direct_pcm")
    data object MicCustomSampleRate : RootFeatureKey("mic_custom_sample_rate")
    data object MicMultiMicRaw : RootFeatureKey("mic_multi_mic_raw")
    data object MicNoiseFloorOverride : RootFeatureKey("mic_noise_floor_override")
    data object MicSystemAudioCapture : RootFeatureKey("mic_system_audio_capture")

    // ──── Batch-5 Sensors features ────
    data object SensorsHighPolling : RootFeatureKey("sensors_high_polling")
    data object SensorsHighPollingExpert : RootFeatureKey("sensors_high_polling_expert")
    data object SensorsRawUnfiltered : RootFeatureKey("sensors_raw_unfiltered")
    data object SensorsSysfsRead : RootFeatureKey("sensors_sysfs_read")
    data object SensorsOverclock : RootFeatureKey("sensors_overclock")
    data object SensorsFusionOverride : RootFeatureKey("sensors_fusion_override")
    data object SensorsHiddenEnumeration : RootFeatureKey("sensors_hidden_enumeration")

    // ──── Batch-5 Battery features ────
    data object BatteryFuelGaugeRaw : RootFeatureKey("battery_fuel_gauge_raw")
    data object BatteryCellMonitor : RootFeatureKey("battery_cell_monitor")
    data object BatteryChargingProfile : RootFeatureKey("battery_charging_profile")
    data object BatteryThermalBypass : RootFeatureKey("battery_thermal_bypass")
    data object BatteryChargingTypeOverride : RootFeatureKey("battery_charging_type_override")
    data object BatteryFullDump : RootFeatureKey("battery_full_dump")

    // ──── Batch-6 Wi-Fi features ────
    data object WifiRfkillToggle : RootFeatureKey("wifi_rfkill_toggle")
    data object WifiTxPowerOverride : RootFeatureKey("wifi_tx_power_override")
    data object WifiChannelOverride : RootFeatureKey("wifi_channel_override")
    data object WifiInjectionProbe : RootFeatureKey("wifi_injection_probe")

    // ──── Batch-6 Bluetooth features ────
    data object BluetoothRfkillToggle : RootFeatureKey("bluetooth_rfkill_toggle")
    data object BluetoothTxPowerOverride : RootFeatureKey("bluetooth_tx_power_override")
    data object BluetoothHciSnoopDump : RootFeatureKey("bluetooth_hci_snoop_dump")

    // ──── Batch-6 NFC features ────
    data object NfcRawNciCommand : RootFeatureKey("nfc_raw_nci_command")

    // ──── Batch-6 IR features ────
    data object IrCustomCarrier : RootFeatureKey("ir_custom_carrier")
    data object IrRawGpioPattern : RootFeatureKey("ir_raw_gpio_pattern")

    // ──── Batch-6 Cellular features ────
    data object CellRawModemDump : RootFeatureKey("cell_raw_modem_dump")
    data object CellSignalDeepDump : RootFeatureKey("cell_signal_deep_dump")

    // ──── Batch-6 GPS features ────
    data object GpsNmeaRawTap : RootFeatureKey("gps_nmea_raw_tap")
    data object GpsConstellationDump : RootFeatureKey("gps_constellation_dump")
}
