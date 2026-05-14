package dev.ranzlappen.gadget.core.testing

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetTheme
import dev.ranzlappen.gadget.core.ui.adaptive.LocalWindowSizeClass

/**
 * Test composition wrapper that applies the production [GadgetTheme]
 * **and** pre-populates `LocalWindowSizeClass` so tests don't have to
 * stand up a real activity to read either CompositionLocal.
 *
 * Typical usage inside a JUnit / Compose UI test:
 *
 * ```kotlin
 * @Test
 * fun primaryButton_emitsClick() {
 *     var clicked = false
 *     composeTestRule.setContent {
 *         GadgetTestTheme {
 *             GadgetPrimaryButton(onClick = { clicked = true }, text = "OK")
 *         }
 *     }
 *     composeTestRule.onNodeWithText("OK").performClick()
 *     assert(clicked)
 * }
 * ```
 *
 * Defaults match the most common phone case (compact width / compact
 * height — `WindowWidthSizeClass.Compact` / `WindowHeightSizeClass.Compact`).
 * Override [windowSize] to exercise medium / expanded layouts:
 *
 * ```kotlin
 * GadgetTestTheme(windowSize = DpSize(800.dp, 1280.dp)) { … }
 * ```
 *
 * [darkTheme] defaults to `true` to match the design-system's
 * dark-first orientation.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun GadgetTestTheme(
    darkTheme: Boolean = true,
    useDynamicColor: Boolean = false,
    windowSize: DpSize = DefaultPhoneSize,
    content: @Composable () -> Unit,
) {
    val windowSizeClass = WindowSizeClass.calculateFromSize(windowSize)
    GadgetTheme(useDarkTheme = darkTheme, useDynamicColor = useDynamicColor) {
        CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
            content()
        }
    }
}

/** Default test window size: representative compact phone. */
private val DefaultPhoneSize: DpSize = DpSize(width = 360.dp, height = 800.dp)

/** Medium-class window size (foldable open / 7" tablet portrait). */
val MediumWindowSize: DpSize = DpSize(width = 700.dp, height = 900.dp)

/** Expanded-class window size (tablet landscape / Chromebook). */
val ExpandedWindowSize: DpSize = DpSize(width = 1024.dp, height = 768.dp)
