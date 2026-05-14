package dev.ranzlappen.gadget.core.ui.preview

import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi-preview annotation: right-to-left locale (Arabic). Pair with
 * `@Composable` to verify that a component's layout, padding, and
 * iconography flip correctly under RTL. Use for any composable with
 * leading/trailing icons, ordered text, or asymmetric padding.
 */
@Preview(
    name = "RTL (ar)",
    locale = "ar",
    showBackground = true,
    backgroundColor = 0xFF06080A,
)
annotation class GadgetPreviewRtl

/**
 * Multi-preview annotation: 200% font scale. Verifies that long-text
 * truncation, button minimum heights, and column spacing all hold up
 * under the accessibility-maxed font scale.
 */
@Preview(
    name = "200% font",
    fontScale = 2f,
    showBackground = true,
    backgroundColor = 0xFF06080A,
)
annotation class GadgetPreviewLargeFont

/**
 * Multi-preview annotation: three width breakpoints corresponding to
 * the [androidx.compose.material3.windowsizeclass.WindowWidthSizeClass]
 * thresholds — Compact (<600 dp), Medium (600–840 dp), Expanded
 * (>840 dp). Use on screen-level previews where layout decisions
 * branch on `LocalWindowSizeClass.current.widthSizeClass`.
 */
@Preview(
    name = "Compact (360 dp)",
    widthDp = 360,
    showBackground = true,
    backgroundColor = 0xFF06080A,
)
@Preview(
    name = "Medium (700 dp)",
    widthDp = 700,
    showBackground = true,
    backgroundColor = 0xFF06080A,
)
@Preview(
    name = "Expanded (1024 dp)",
    widthDp = 1024,
    showBackground = true,
    backgroundColor = 0xFF06080A,
)
annotation class GadgetPreviewSizeClasses
