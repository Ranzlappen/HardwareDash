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

/**
 * Top-level Gadget Material 3 theme.
 *
 * Defaults to dark-first (the hardware-tinkerer aesthetic) but obeys
 * the system theme when [useDarkTheme] is not overridden. On Android
 * 12+ (`Build.VERSION_CODES.S`), dynamic color is enabled by default
 * so the theme picks up the user's wallpaper-derived palette while
 * keeping our typography, shapes, and motion tokens.
 *
 * Custom-theme seam: the theme reads from [LocalGadgetTheme], which a
 * downstream consumer can override via
 * `CompositionLocalProvider(LocalGadgetTheme provides MyCustomTheme) { … }`.
 * Phase 0 only ships the [GadgetCustomTheme.Default] entry; later
 * phases will expose user-selectable themes from the settings screen
 * (high-contrast / amoled-true / pastel are the obvious first three),
 * and each will plug in here without API changes downstream.
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

    CompositionLocalProvider(
        LocalGadgetTheme provides GadgetCustomTheme.Default,
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
 * Composition local holding the active custom theme. Future-proofs the
 * design system for user-defined themes wired from settings. Read it
 * via `LocalGadgetTheme.current` in any @Composable that wants to
 * branch on the active theme (e.g. choose between two backdrop images).
 */
val LocalGadgetTheme = staticCompositionLocalOf { GadgetCustomTheme.Default }

/**
 * Identifies the active custom theme.
 *
 * Phase 0 ships only [Default]. Marked [Immutable] so Compose can
 * skip recompositions when the value is unchanged across recompositions.
 */
@Immutable
enum class GadgetCustomTheme {
    Default,
    // Placeholders for the user-selectable themes coming in Phase 3:
    // HighContrast,
    // AmoledTrue,
    // Pastel,
}
