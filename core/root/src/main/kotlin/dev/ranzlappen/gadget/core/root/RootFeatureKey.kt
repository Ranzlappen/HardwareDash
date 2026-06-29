package dev.ranzlappen.gadget.core.root

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
    /** Hidden `BluetoothDevice.getBatteryLevel()` reflection call — covers
     *  BLE and Classic devices the stack already polls internally. */
    data object BluetoothHiddenBatteryApi : RootFeatureKey("bluetooth_hidden_battery_api")
    /** Reflection on `BluetoothA2dp.getCodecStatus()` for negotiated codec. */
    data object BluetoothA2dpCodecReflection : RootFeatureKey("bluetooth_a2dp_codec_reflection")

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

    // ──── Batch-7 Automation features ────
    data object AutomationPrivilegedIntent : RootFeatureKey("automation_privileged_intent")
    data object AutomationSystemSettingsOverride : RootFeatureKey("automation_system_settings_override")
    data object AutomationDumpsysSnapshot : RootFeatureKey("automation_dumpsys_snapshot")

    // ──── Batch-7 Notification features ────
    data object NotificationStickyOverride : RootFeatureKey("notification_sticky_override")
    data object NotificationListenerAccess : RootFeatureKey("notification_listener_access")
    data object NotificationLockScreenOverlay : RootFeatureKey("notification_lock_screen_overlay")

    // ──── Batch-7 Keep-Alive features ────
    data object KeepAliveDozeBypass : RootFeatureKey("keep_alive_doze_bypass")
    data object KeepAlivePmGrant : RootFeatureKey("keep_alive_pm_grant")

    // ──── Batch-8 Storage features ────
    data object StorageDumpDiskstats : RootFeatureKey("storage_dump_diskstats")
    data object StorageEnumerateMounts : RootFeatureKey("storage_enumerate_mounts")
    data object StorageFstrim : RootFeatureKey("storage_fstrim")
    data object StorageDropCaches : RootFeatureKey("storage_drop_caches")

    // ──── Batch-8 Display features ────
    data object DisplayBrightnessOverride : RootFeatureKey("display_brightness_override")
    data object DisplayRefreshRateOverride : RootFeatureKey("display_refresh_rate_override")
    data object DisplayDensityOverride : RootFeatureKey("display_density_override")
    data object DisplaySurfaceFlingerSnapshot : RootFeatureKey("display_surface_flinger_snapshot")

    // ──── Batch-8 Audio routing features ────
    data object AudioStreamVolumeBypass : RootFeatureKey("audio_stream_volume_bypass")
    data object AudioForceRouting : RootFeatureKey("audio_force_routing")
    data object AudioMuteAllStreams : RootFeatureKey("audio_mute_all_streams")
    data object AudioDumpAudioPolicy : RootFeatureKey("audio_dump_audio_policy")

    // ──── Batch-9 Battery deep-dive features ────
    data object BatteryHoldSoc : RootFeatureKey("battery_hold_soc")
    data object BatteryHealthDeepDump : RootFeatureKey("battery_health_deep_dump")
    data object BatteryWirelessCoilCurrent : RootFeatureKey("battery_wireless_coil_current")

    // ──── Batch-9 ADB Debugging features ────
    data object AdbToggleEnabled : RootFeatureKey("adb_toggle_enabled")
    data object AdbOverNetwork : RootFeatureKey("adb_over_network")
    data object AdbDumpProperties : RootFeatureKey("adb_dump_properties")
    data object AdbSetpropOverride : RootFeatureKey("adb_setprop_override")

    // ──── Batch-9 USB Debugging features ────
    data object UsbSwitchFunction : RootFeatureKey("usb_switch_function")
    data object UsbDumpUsb : RootFeatureKey("usb_dump_usb")
    data object UsbDumpSerialService : RootFeatureKey("usb_dump_serial_service")
    data object UsbDumpUsbDevicesDebug : RootFeatureKey("usb_dump_usb_devices_debug")

    // ──── Batch-10 Diagnostics features ────
    data object DiagnosticsTailLogcat : RootFeatureKey("diagnostics_tail_logcat")
    data object DiagnosticsDumpMemInfo : RootFeatureKey("diagnostics_dump_mem_info")
    data object DiagnosticsDumpCpuInfo : RootFeatureKey("diagnostics_dump_cpu_info")
    data object DiagnosticsDumpProcstats : RootFeatureKey("diagnostics_dump_procstats")

    // ──── Batch-13 GPS spoofing features ────
    // Test-provider-driven mock location with rooted permission grant.
    data object GpsLocationOverride : RootFeatureKey("gps_location_override")
    // GPX/KML/route playback driving the override.
    data object GpsRouteSimulation : RootFeatureKey("gps_route_simulation")
    // One-shot installer for the bundled LSPosed module that hides
    // isFromMockProvider()/isMock() from third-party apps.
    data object GpsLsposedHookInstall : RootFeatureKey("gps_lsposed_hook_install")

    // ──── Batch-14 Lock overlay features ────
    // Draw a bounded `TYPE_APPLICATION_OVERLAY` above the secure keyguard,
    // granting SYSTEM_ALERT_WINDOW via root appops first so it works even
    // without the user toggling the Settings permission.
    data object LockSecureOverlay : RootFeatureKey("lock_secure_overlay")

    // ──── Batch-15 Flipper features ────
    // Relax the attached Flipper's USB device-node permissions via root so the
    // app opens the CDC-ACM port without the per-attach permission dialog.
    data object FlipperUsbGrant : RootFeatureKey("flipper_usb_grant")
}
