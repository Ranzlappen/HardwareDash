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
)

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
