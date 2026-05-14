package dev.ranzlappen.gadget.core.ui.adaptive

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Active [WindowSizeClass] for the current Compose subtree.
 *
 * Populated by `GadgetApp` from the host [android.app.Activity] via
 * `calculateWindowSizeClass(activity)`. Read in layout-aware
 * components / screens that branch on Compact / Medium / Expanded
 * (and the matching height classes), e.g.:
 *
 * ```kotlin
 * val sizeClass = LocalWindowSizeClass.current
 * when (sizeClass.widthSizeClass) {
 *     WindowWidthSizeClass.Compact -> CompactDashboardLayout()
 *     WindowWidthSizeClass.Medium  -> TwoPaneDashboardLayout()
 *     WindowWidthSizeClass.Expanded -> ThreePaneDashboardLayout()
 * }
 * ```
 *
 * The default raises an `error` if no provider sits above the call
 * site — every entry point into Compose content (MainActivity,
 * previews, tests) must wrap content in `GadgetApp` (or in a custom
 * `CompositionLocalProvider(LocalWindowSizeClass provides …)` for
 * previews and tests that want to exercise a specific size class).
 */
val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    error(
        "LocalWindowSizeClass not provided. Wrap your content in " +
            "GadgetApp { … }, or for previews/tests provide a value via " +
            "CompositionLocalProvider(LocalWindowSizeClass provides …).",
    )
}
