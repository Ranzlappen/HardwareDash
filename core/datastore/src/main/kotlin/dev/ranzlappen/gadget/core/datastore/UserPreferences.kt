package dev.ranzlappen.gadget.core.datastore

import androidx.compose.runtime.Immutable

/**
 * Strongly-typed user preferences surface backed by DataStore.
 *
 * `@Immutable` so Compose can skip recompositions when the value
 * is structurally unchanged. Default constructor values represent
 * the canonical first-launch state: dark-first, dynamic colour on,
 * no a11y overrides.
 *
 * Phase 2 / Batch 1 introduces only the slots required by the
 * Settings v1 screen (Appearance + Accessibility sections). Future
 * batches grow this data class as more features need persisted
 * config — adding a field here + a key in [UserPreferencesKeys]
 * + a getter/setter in [UserPreferencesRepository] is the full
 * recipe.
 */
@Immutable
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
) {
    companion object {
        /** Initial strobe rate before the user touches the slider. 5 Hz
         *  is well below the Camera2 rate cliff on most OEMs. */
        const val DEFAULT_STROBE_RATE_HZ: Float = 5f
    }
}

/**
 * Dark-theme selector. `FollowSystem` defers to the OS setting via
 * [androidx.compose.foundation.isSystemInDarkTheme]; explicit
 * [Light] / [Dark] overrides force the theme regardless of the
 * system setting.
 */
@Immutable
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
@Immutable
enum class TriStatePreference {
    On,
    Off,
    FollowSystem,
}
