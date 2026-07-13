package dev.ranzlappen.gadget.core.datastore

import kotlinx.serialization.Serializable

/**
 * A user-defined accent palette (W9), active when
 * [CustomThemeOption.Custom] is selected. Three ARGB colors
 * (`0xAARRGGBB` as a `Long`, matching `GadgetColorPicker`'s contract) that the
 * app layer copies onto the canonical dark/light `ColorScheme` — an accent
 * override, not a full Monet-style tonal generation (no seed→scheme generator
 * ships in the app).
 *
 * Defaults are the brand accents (teal / magenta / amber), so selecting
 * "Custom" before editing looks identical to the default theme rather than
 * black. Persisted as a JSON blob in [UserPreferences.customPalette].
 */
@Serializable
data class CustomPalette(
    val primaryArgb: Long = 0xFF00E5C8,
    val secondaryArgb: Long = 0xFFE872C0,
    val tertiaryArgb: Long = 0xFFFFC857,
)
