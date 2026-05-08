package com.gadget.display

/**
 * Configures a backlight-override request. The helper clamps the
 * effective raw value to 130 % of the device-reported `max_brightness`
 * regardless of caller input. [activeWindowMillis] is hard-capped to
 * 60 s by the helper.
 */
data class BrightnessOverrideConfig(
    val percent: Int,
    val activeWindowMillis: Long,
)

/**
 * Configures a refresh-rate override request. [targetModeId] maps to
 * a `cmd display set-display-mode` argument; the helper hard-caps the
 * implied refresh rate at 165 Hz by inspecting the active mode list
 * and rejecting modes whose refresh-rate exceeds that cap.
 */
data class RefreshRateOverrideConfig(
    val displayId: Int = 0,
    val targetModeId: Int,
)

/**
 * Configures a runtime DPI override. [dpi] is clamped to 120–560 inside
 * the helper.
 */
data class DensityOverrideConfig(
    val dpi: Int,
)
