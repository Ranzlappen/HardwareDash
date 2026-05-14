package dev.ranzlappen.gadget.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable

/**
 * Umbrella value object exposed by `LocalGadgetTheme.current`. Holds
 * every design-system token through a single, stable reference so a
 * downstream consumer can override any field per-theme via
 * `CompositionLocalProvider(LocalGadgetTheme provides …) { … }`.
 *
 * The five token slots break down as:
 *
 * - [identifier] — the active theme variant. Phase 0 ships only
 *   [GadgetCustomTheme.Default]; Phase 3 will plug high-contrast /
 *   amoled-true / pastel here.
 * - [colors] — Material 3 [ColorScheme]. Defaults to the resolved
 *   dark/light/dynamic palette computed inside [GadgetTheme].
 * - [typography] — Material 3 [Typography]. Defaults to
 *   [GadgetTypography] from `theme/GadgetType.kt`.
 * - [shapes] — Material 3 [Shapes]. Defaults to [GadgetShapes] from
 *   `theme/GadgetShape.kt`.
 * - [spacing] / [motion] / [glass] — Gadget-specific value classes
 *   from `theme/GadgetTokenValues.kt`. Each has a `Defaults`
 *   companion that mirrors the existing static constants 1:1.
 *
 * Marked `@Immutable` so Compose can skip recompositions when the
 * value is unchanged. Equality is structural (data class), so two
 * themes with the same field values compare equal and produce no
 * spurious recompositions.
 */
@Immutable
data class GadgetThemeData(
    val colors: ColorScheme,
    val typography: Typography,
    val shapes: Shapes,
    val identifier: GadgetCustomTheme = GadgetCustomTheme.Default,
    val spacing: GadgetSpacingValues = GadgetSpacingValues.Defaults,
    val motion: GadgetMotionValues = GadgetMotionValues.Defaults,
    val glass: GadgetGlassValues = GadgetGlassValues.Defaults,
)
