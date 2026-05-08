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
}
