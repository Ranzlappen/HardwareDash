package dev.ranzlappen.gadget.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import dev.ranzlappen.gadget.core.designsystem.a11y.LocalReducedMotion
import dev.ranzlappen.gadget.core.designsystem.a11y.LocalReducedTransparency
import dev.ranzlappen.gadget.core.designsystem.a11y.rememberSystemReducedMotion

/**
 * Top-level Gadget Material 3 theme.
 *
 * Defaults to dark-first (the hardware-tinkerer aesthetic) but obeys
 * the system theme when [useDarkTheme] is not overridden. On Android
 * 12+ (`Build.VERSION_CODES.S`), dynamic color is enabled by default
 * so the theme picks up the user's wallpaper-derived palette while
 * keeping our typography, shapes, and motion tokens.
 *
 * Accessibility-local overrides:
 * - [reducedMotionOverride] — pass `true` / `false` to force the
 *   `LocalReducedMotion` value regardless of the system setting.
 *   `null` (the default) means "follow system" — the local is
 *   populated from `Settings.Global.ANIMATOR_DURATION_SCALE` via
 *   [rememberSystemReducedMotion].
 * - [reducedTransparency] — drives `LocalReducedTransparency`.
 *   Android has no system-wide reduce-transparency setting, so this
 *   defaults to `false` and is meant to be wired from a user
 *   preference (Settings → Accessibility → Reduce transparency).
 *
 * Provides [LocalGadgetTheme] = a [GadgetThemeData] umbrella value
 * holding every design-system token (colors, typography, shapes,
 * spacing, motion, glass). Downstream consumers wanting to override
 * one slot (e.g. a "Compact" theme that halves spacing) wrap their
 * subtree in:
 *
 * ```kotlin
 * CompositionLocalProvider(
 *     LocalGadgetTheme provides LocalGadgetTheme.current.copy(
 *         spacing = GadgetSpacingValues(medium = 8.dp, …)
 *     ),
 * ) { … }
 * ```
 *
 * Usage:
 *
 * ```kotlin
 * setContent {
 *     GadgetTheme {
 *         GadgetNavHost(...)
 *     }
 * }
 * ```
 */
@Composable
fun GadgetTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    reducedMotionOverride: Boolean? = null,
    reducedTransparency: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> GadgetDarkColorScheme
        else -> GadgetLightColorScheme
    }
    val themeData = GadgetThemeData(
        colors = colorScheme,
        typography = GadgetTypography,
        shapes = GadgetShapes,
        identifier = GadgetCustomTheme.Default,
    )
    val systemReducedMotion = rememberSystemReducedMotion()
    val effectiveReducedMotion = reducedMotionOverride ?: systemReducedMotion

    CompositionLocalProvider(
        LocalGadgetTheme provides themeData,
        LocalReducedMotion provides effectiveReducedMotion,
        LocalReducedTransparency provides reducedTransparency,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GadgetTypography,
            shapes = GadgetShapes,
            content = content,
        )
    }
}

/**
 * Composition local exposing the active [GadgetThemeData]. Read via
 * `LocalGadgetTheme.current` from any `@Composable` that needs a
 * design-system token (spacing, motion, glass, custom-theme
 * identifier). The default raises an `error` if no [GadgetTheme]
 * wraps the call site — every entry point into Compose content
 * (MainActivity, previews, tests) must wrap content in `GadgetTheme`.
 */
val LocalGadgetTheme = staticCompositionLocalOf<GadgetThemeData> {
    error("No GadgetTheme provided. Wrap your content in GadgetTheme { … }.")
}

/**
 * Identifies the active custom theme. Lives on [GadgetThemeData] as
 * the [GadgetThemeData.identifier] field so consumers can branch on
 * the named theme variant (e.g. pick between two backdrop images
 * based on `LocalGadgetTheme.current.identifier`).
 *
 * Phase 0 ships only [Default]. Marked [Immutable] so Compose can
 * skip recompositions when the value is unchanged.
 */
@Immutable
enum class GadgetCustomTheme {
    Default,
    // Placeholders for the user-selectable themes coming in Phase 3:
    // HighContrast,
    // AmoledTrue,
    // Pastel,
}
