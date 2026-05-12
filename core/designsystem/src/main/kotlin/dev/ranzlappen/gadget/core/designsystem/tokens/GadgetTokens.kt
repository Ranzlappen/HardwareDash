package dev.ranzlappen.gadget.core.designsystem.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp

/**
 * Gadget design tokens: spacing, elevation, motion.
 *
 * Importing structural values from a single place is the lever that
 * makes future redesigns cheap — every padding/spring/elevation in the
 * app reaches into here, so retuning the design system means touching
 * this file. Resist the urge to inline raw dp values at call sites.
 */

/**
 * Spacing scale. Multiplied off a 4dp grid except [Hairline] (for
 * dividers and 1dp borders).
 *
 * Typical usage at the screen edges: [Medium] (16dp).
 * Typical usage between cards in a grid: [Small] (12dp).
 * Typical usage between major sections: [Large] (24dp).
 */
object GadgetSpacing {
    val Hairline = 1.dp
    val Pico = 2.dp
    val Micro = 4.dp
    val Tiny = 8.dp
    val Small = 12.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
    val Huge = 48.dp
}

/**
 * Material 3 elevation levels. The glass design system uses elevation
 * sparingly — most surfaces are flat-with-gradient rather than
 * stacked-with-shadow — but levels are exposed for surfaces that
 * genuinely need depth (FABs, dialogs, app bars on scroll).
 */
object GadgetElevation {
    val None = 0.dp
    val Level1 = 1.dp
    val Level2 = 3.dp
    val Level3 = 6.dp
    val Level4 = 8.dp
    val Level5 = 12.dp
}

/**
 * Motion specs. Springs are parameterised as factory functions because
 * `spring<T>()` is generic — the type parameter is inferred at the
 * call site from the surrounding animation API (e.g. `animateFloatAsState`
 * pins T = Float).
 *
 * Easings mirror Material 3 Expressive: emphasized for entries and
 * exits, standard for transient state changes.
 */
object GadgetMotion {
    /**
     * Workhorse spring. No bounce, medium stiffness — feels
     * "interactive" for value changes (number tickers, progress) but
     * not jittery.
     */
    fun <T> springStandard() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /**
     * Slower, slightly bouncy. For container-level transitions where
     * you want the user to notice the change.
     */
    fun <T> springGentle() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow,
    )

    /**
     * Visibly bouncy. For playful microinteractions — toggle
     * acknowledgements, "you just unlocked X" toasts.
     */
    fun <T> springBouncy() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /**
     * Snappy, no overshoot. For state-driven sizing changes that should
     * feel instantaneous but still smoothed (chevron rotation,
     * expand/collapse arrows).
     */
    fun <T> springSnappy() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    // M3 expressive easings
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    // Durations (ms). Tuned to feel responsive on mid-range devices.
    const val DurationShort = 200
    const val DurationMedium = 300
    const val DurationLong = 500
}
