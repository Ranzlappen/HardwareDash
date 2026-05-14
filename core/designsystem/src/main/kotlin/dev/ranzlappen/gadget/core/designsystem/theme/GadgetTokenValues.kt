package dev.ranzlappen.gadget.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Data-class wrappers for the design-system spacing / motion / glass
 * tokens. These mirror the existing `object GadgetSpacing` /
 * `object GadgetMotion` / `Glass.kt` constants 1:1 in their `Defaults`
 * companion, so wrapping the existing values in `GadgetThemeData`
 * never changes runtime behaviour on the default theme.
 *
 * Why the wrapper layer: a CompositionLocal needs a value the host
 * can override per-theme (e.g. a future "Compact" theme that halves
 * every spacing token). Compose can't override a Kotlin `object`'s
 * properties at runtime, hence the data classes here.
 *
 * Each value class is marked `@Immutable` so Compose can skip
 * recompositions when the value is unchanged across recompositions
 * — required by the design-system spec's "Performance: @Stable /
 * @Immutable on all data classes; minimize recompositions" clause.
 */

@Immutable
data class GadgetSpacingValues(
    val hairline: Dp = 1.dp,
    val pico: Dp = 2.dp,
    val micro: Dp = 4.dp,
    val tiny: Dp = 8.dp,
    val small: Dp = 12.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val huge: Dp = 48.dp,
) {
    companion object {
        /**
         * Default spacing scale — matches the static
         * `object GadgetSpacing` constants in `tokens/GadgetTokens.kt`.
         */
        val Defaults: GadgetSpacingValues = GadgetSpacingValues()
    }
}

@Immutable
data class GadgetMotionValues(
    // Spring parameters per preset. Kept as primitive damping/stiffness
    // pairs rather than pre-built `SpringSpec<T>` instances because
    // SpringSpec is generic on T — the spec is materialised per call
    // site by the `springX()` helpers below.
    val standardDamping: Float = Spring.DampingRatioNoBouncy,
    val standardStiffness: Float = Spring.StiffnessMedium,
    val gentleDamping: Float = Spring.DampingRatioLowBouncy,
    val gentleStiffness: Float = Spring.StiffnessLow,
    val bouncyDamping: Float = Spring.DampingRatioMediumBouncy,
    val bouncyStiffness: Float = Spring.StiffnessMediumLow,
    val snappyDamping: Float = Spring.DampingRatioNoBouncy,
    val snappyStiffness: Float = Spring.StiffnessHigh,
    // Material 3 expressive easings.
    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f),
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f),
    val standardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    // Animation durations (ms). Tuned to feel responsive on mid-range
    // hardware.
    val durationShort: Int = 200,
    val durationMedium: Int = 300,
    val durationLong: Int = 500,
) {
    /**
     * Workhorse spring. No bounce, medium stiffness — feels
     * "interactive" for value changes without jittering.
     */
    fun <T> springStandard() = spring<T>(
        dampingRatio = standardDamping,
        stiffness = standardStiffness,
    )

    /**
     * Slower, slightly bouncy. For container-level transitions where
     * the user should notice the change.
     */
    fun <T> springGentle() = spring<T>(
        dampingRatio = gentleDamping,
        stiffness = gentleStiffness,
    )

    /**
     * Visibly bouncy. For playful microinteractions — toggle
     * acknowledgements, "you just unlocked X" toasts.
     */
    fun <T> springBouncy() = spring<T>(
        dampingRatio = bouncyDamping,
        stiffness = bouncyStiffness,
    )

    /**
     * Snappy, no overshoot. For state-driven sizing changes that
     * should feel instantaneous but still smoothed (chevron rotation,
     * expand/collapse arrows).
     */
    fun <T> springSnappy() = spring<T>(
        dampingRatio = snappyDamping,
        stiffness = snappyStiffness,
    )

    companion object {
        /**
         * Default motion specs — matches the static
         * `object GadgetMotion` constants in `tokens/GadgetTokens.kt`.
         */
        val Defaults: GadgetMotionValues = GadgetMotionValues()
    }
}

@Immutable
data class GadgetGlassValues(
    val defaultCornerRadius: Dp = 18.dp,
    // Subtle preset — content panels with long text, lists.
    val subtleTopAlpha: Float = 0.85f,
    val subtleBottomAlpha: Float = 0.70f,
    val subtleBorderAlpha: Float = 0.30f,
    // Standard preset — dashboard cards, settings tiles.
    val standardTopAlpha: Float = 0.65f,
    val standardBottomAlpha: Float = 0.40f,
    val standardBorderAlpha: Float = 0.50f,
    // Vivid preset — hero surfaces over a vibrant backdrop.
    val vividTopAlpha: Float = 0.40f,
    val vividBottomAlpha: Float = 0.18f,
    val vividBorderAlpha: Float = 0.65f,
) {
    companion object {
        /**
         * Default glass tuning — matches the values hard-coded in
         * `core/designsystem/Glass.kt`'s `GlassIntensity` enum.
         */
        val Defaults: GadgetGlassValues = GadgetGlassValues()
    }
}
