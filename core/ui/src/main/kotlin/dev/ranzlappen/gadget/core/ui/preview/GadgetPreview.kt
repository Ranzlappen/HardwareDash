package dev.ranzlappen.gadget.core.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetTheme
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetSpacing
import dev.ranzlappen.gadget.core.ui.adaptive.LocalWindowSizeClass

/**
 * Standard preview wrapper: wraps [content] in [GadgetTheme] over the
 * theme's background color so previews match the live app's defaults.
 *
 * `useDynamicColor` defaults to **false** in preview — Android Studio's
 * preview renderer can't sample a wallpaper, so dynamic color would
 * fall back to seed colors and previews would diverge from on-device
 * appearance. Pass `useDynamicColor = true` only when explicitly
 * previewing the dynamic-color path.
 *
 * Usage:
 *
 * ```kotlin
 * @GadgetPreviewLightDark
 * @Composable
 * private fun DashCardPreview() = GadgetThemedPreview {
 *     DashCard(title = "Battery") { Text("87%") }
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun GadgetThemedPreview(
    darkTheme: Boolean = true,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    GadgetTheme(useDarkTheme = darkTheme, useDynamicColor = useDynamicColor) {
        val spacing = LocalGadgetTheme.current.spacing
        // Derive a WindowSizeClass from the preview canvas and provide it via
        // LocalWindowSizeClass. Screens built on ModuleScreenScaffold read it
        // through rememberLayoutMode(); outside GadgetApp (Android Studio
        // previews AND the Roborazzi gallery render) the local's default
        // `error(...)` would otherwise throw. Deriving from the actual
        // constraints keeps @GadgetPreviewSizeClasses meaningful — a 700/1024dp
        // preview still resolves Medium/Expanded → TwoPane/ThreePane. GadgetApp
        // supplies the real value at runtime; this wrapper is preview-only.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            val sizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
            CompositionLocalProvider(LocalWindowSizeClass provides sizeClass) {
                Box(modifier = Modifier.padding(spacing.medium)) {
                    content()
                }
            }
        }
    }
}

/**
 * Multi-preview annotation that renders the same composable twice —
 * once in the Gadget dark theme, once in the Gadget light theme —
 * with the matching theme background color. Apply alongside the
 * `@Composable` annotation on a preview function.
 *
 * Background colors are hardcoded to avoid a dependency on
 * [dev.ranzlappen.gadget.core.designsystem.theme.GadgetPalette] from
 * annotation argument lists (the latter would require the constants
 * to be `const`, which `Color` isn't). They mirror
 * `GadgetPalette.DarkBackground` / `GadgetPalette.LightBackground`.
 */
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF06080A,
)
@Preview(
    name = "Light",
    showBackground = true,
    backgroundColor = 0xFFFAFBFD,
)
annotation class GadgetPreviewLightDark
