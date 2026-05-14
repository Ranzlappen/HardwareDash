package dev.ranzlappen.gadget.core.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetTheme
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetSpacing

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
@Composable
fun GadgetThemedPreview(
    darkTheme: Boolean = true,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    GadgetTheme(useDarkTheme = darkTheme, useDynamicColor = useDynamicColor) {
        val spacing = LocalGadgetTheme.current.spacing
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(spacing.medium),
        ) {
            content()
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
