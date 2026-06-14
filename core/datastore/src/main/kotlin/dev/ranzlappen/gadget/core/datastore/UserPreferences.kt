package dev.ranzlappen.gadget.core.datastore

/**
 * Strongly-typed user preferences surface backed by DataStore.
 *
 * Default constructor values represent the canonical first-launch
 * state: dark-first, dynamic colour on, no a11y overrides. Compose
 * treats this data class as stable (all fields are themselves
 * stable primitives or enums), so no `@Immutable` annotation is
 * needed — and `:core:datastore` deliberately doesn't pull in a
 * Compose dependency.
 *
 * Phase 2 / Batch 1 introduces only the slots required by the
 * Settings v1 screen (Appearance + Accessibility sections). Future
 * batches grow this data class as more features need persisted
 * config — adding a field here + a key in [UserPreferencesKeys]
 * + a getter/setter in [UserPreferencesRepository] is the full
 * recipe.
 */
data class UserPreferences(
    val darkThemeMode: DarkThemeMode = DarkThemeMode.FollowSystem,
    val dynamicColor: Boolean = true,
    val reducedMotionOverride: TriStatePreference = TriStatePreference.FollowSystem,
    val reducedTransparency: Boolean = false,
    val largeTextOverride: Boolean = false,
    /**
     * Default strobe rate (Hz) applied to new flashlight-strobe
     * widgets at pin time. The TorchScreen slider writes this; each
     * widget captures the value at creation and persists its own
     * copy thereafter, so changing the slider doesn't retroactively
     * alter existing widgets. Range pinned to `1f..20f` in the UI.
     */
    val defaultStrobeRateHz: Float = DEFAULT_STROBE_RATE_HZ,
    /**
     * Persistent text the in-app Morse controls convert to a looping
     * Morse strobe. Remembered across launches so the user types their
     * message once. Per-widget Morse widgets store their own text.
     */
    val morseText: String = DEFAULT_MORSE_TEXT,
    /**
     * Default torch brightness applied when the torch is turned on and
     * the device supports `CameraManager.turnOnTorchWithStrengthLevel`
     * (API 33+). Range `0f..1f`; `1f` = maximum intensity.
     */
    val defaultTorchBrightness: Float = DEFAULT_TORCH_BRIGHTNESS,
    /**
     * Whether the floating torch-toggle overlay button is enabled.
     * The overlay requires `SYSTEM_ALERT_WINDOW` permission (checked at
     * runtime in Settings); `true` here does not mean the overlay is
     * currently visible — it means the user asked for it and permission
     * was granted. `TorchOverlayService` reads this and stops itself
     * when the value flips to `false`.
     */
    val floatingTorchButtonEnabled: Boolean = false,
) {
    companion object {
        /** Initial strobe rate before the user touches the slider. 5 Hz
         *  is well below the Camera2 rate cliff on most OEMs. */
        const val DEFAULT_STROBE_RATE_HZ: Float = 5f

        /** Initial Morse message before the user edits the field. */
        const val DEFAULT_MORSE_TEXT: String = "SOS"

        /** Initial torch brightness (full intensity). */
        const val DEFAULT_TORCH_BRIGHTNESS: Float = 1f
    }
}

/**
 * Dark-theme selector. `FollowSystem` defers to the OS setting via
 * [androidx.compose.foundation.isSystemInDarkTheme]; explicit
 * [Light] / [Dark] overrides force the theme regardless of the
 * system setting.
 */
enum class DarkThemeMode {
    Light,
    Dark,
    FollowSystem,
}

/**
 * Three-valued preference for accessibility toggles that have a
 * meaningful "follow what the system tells us" default. For
 * example, reduced motion: `FollowSystem` means the value is
 * derived from `Settings.Global.ANIMATOR_DURATION_SCALE`; explicit
 * [On] / [Off] forces the override regardless.
 */
enum class TriStatePreference {
    On,
    Off,
    FollowSystem,
}
