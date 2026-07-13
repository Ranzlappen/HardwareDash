package dev.ranzlappen.gadget.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Gadget brand palette.
 *
 * Dark-first. Hardware-app users overwhelmingly run in dark mode (less
 * heat, less battery on OLED, easier on the eyes during late-night
 * sensor tinkering), so dark is the canonical theme. Light mode is
 * supported but secondary.
 *
 * Primary: electric teal-cyan — bright enough to read like an
 * indicator LED, saturated enough to land at-glance, but not so loud
 * it competes with sensor data. The same hue family appears across
 * Tesla, Rivian, and OEM hardware dashboards for the same reason.
 *
 * Secondary: warm magenta — distinguishes interactive accents from
 * primary brand cues (e.g. tertiary-action chips, secondary FABs).
 *
 * Tertiary: amber — reserved for warnings, soft attention cues, and
 * the "rooted" badge surface so root features visually scream
 * "different territory" without going red.
 */
object GadgetPalette {
    // Brand — electric teal/cyan
    val BrandTeal = Color(0xFF00E5C8)
    val BrandTealStrong = Color(0xFF00B89E)
    val BrandTealDim = Color(0xFF003D33)
    val BrandTealSoft = Color(0xFFA8FFEC)

    // Accent — magenta
    val BrandMagenta = Color(0xFFE872C0)
    val BrandMagentaStrong = Color(0xFFAE4D90)
    val BrandMagentaDim = Color(0xFF55214A)
    val BrandMagentaSoft = Color(0xFFFCD2EE)

    // Warm — amber for warnings + rooted badge
    val BrandAmber = Color(0xFFFFC857)
    val BrandAmberStrong = Color(0xFFD49B26)
    val BrandAmberDim = Color(0xFF5A3F0E)
    val BrandAmberSoft = Color(0xFFFFE7B5)

    // Dark surfaces (canonical theme)
    val DarkBackground = Color(0xFF06080A)
    val DarkSurface = Color(0xFF0E1217)
    val DarkSurfaceDim = Color(0xFF0A0D11)
    val DarkSurfaceBright = Color(0xFF1B2129)
    val DarkSurfaceContainerLowest = Color(0xFF05070A)
    val DarkSurfaceContainerLow = Color(0xFF0B0E12)
    val DarkSurfaceContainer = Color(0xFF0F1419)
    val DarkSurfaceContainerHigh = Color(0xFF161B22)
    val DarkSurfaceContainerHighest = Color(0xFF1F252E)
    val DarkOutline = Color(0xFF3A424E)
    val DarkOutlineVariant = Color(0xFF272D36)
    val DarkOnBackground = Color(0xFFE2E6EE)
    val DarkOnSurface = Color(0xFFE2E6EE)
    val DarkOnSurfaceVariant = Color(0xFFA5ACB8)
    val DarkError = Color(0xFFFF6B6B)
    val DarkOnError = Color(0xFF1A0808)
    val DarkErrorContainer = Color(0xFF5A1A1A)
    val DarkOnErrorContainer = Color(0xFFFFD9D9)

    // Light surfaces (secondary support — less time in the styling oven)
    val LightBackground = Color(0xFFFAFBFD)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceContainer = Color(0xFFF1F3F8)
    val LightSurfaceContainerHigh = Color(0xFFE6E9F0)
    val LightSurfaceContainerHighest = Color(0xFFDADEE6)
    val LightOutline = Color(0xFFA8AEB8)
    val LightOutlineVariant = Color(0xFFD0D4DC)
    val LightOnBackground = Color(0xFF0E1217)
    val LightOnSurface = Color(0xFF0E1217)
    val LightOnSurfaceVariant = Color(0xFF3F4651)
    val LightError = Color(0xFFC52B2B)
    val LightOnError = Color.White
    val LightErrorContainer = Color(0xFFFFD9D9)
    val LightOnErrorContainer = Color(0xFF410002)
}

/**
 * Canonical dark color scheme for Gadget. Use through [GadgetTheme] —
 * this is exposed publicly so previews + tests can reference it
 * directly.
 */
val GadgetDarkColorScheme = darkColorScheme(
    primary = GadgetPalette.BrandTeal,
    onPrimary = GadgetPalette.DarkBackground,
    primaryContainer = GadgetPalette.BrandTealDim,
    onPrimaryContainer = GadgetPalette.BrandTealSoft,
    inversePrimary = GadgetPalette.BrandTealStrong,
    secondary = GadgetPalette.BrandMagenta,
    onSecondary = GadgetPalette.DarkBackground,
    secondaryContainer = GadgetPalette.BrandMagentaDim,
    onSecondaryContainer = GadgetPalette.BrandMagentaSoft,
    tertiary = GadgetPalette.BrandAmber,
    onTertiary = GadgetPalette.DarkBackground,
    tertiaryContainer = GadgetPalette.BrandAmberDim,
    onTertiaryContainer = GadgetPalette.BrandAmberSoft,
    background = GadgetPalette.DarkBackground,
    onBackground = GadgetPalette.DarkOnBackground,
    surface = GadgetPalette.DarkSurface,
    onSurface = GadgetPalette.DarkOnSurface,
    surfaceVariant = GadgetPalette.DarkSurfaceContainerHigh,
    onSurfaceVariant = GadgetPalette.DarkOnSurfaceVariant,
    surfaceDim = GadgetPalette.DarkSurfaceDim,
    surfaceBright = GadgetPalette.DarkSurfaceBright,
    surfaceContainerLowest = GadgetPalette.DarkSurfaceContainerLowest,
    surfaceContainerLow = GadgetPalette.DarkSurfaceContainerLow,
    surfaceContainer = GadgetPalette.DarkSurfaceContainer,
    surfaceContainerHigh = GadgetPalette.DarkSurfaceContainerHigh,
    surfaceContainerHighest = GadgetPalette.DarkSurfaceContainerHighest,
    outline = GadgetPalette.DarkOutline,
    outlineVariant = GadgetPalette.DarkOutlineVariant,
    error = GadgetPalette.DarkError,
    onError = GadgetPalette.DarkOnError,
    errorContainer = GadgetPalette.DarkErrorContainer,
    onErrorContainer = GadgetPalette.DarkOnErrorContainer,
    scrim = Color.Black,
)

/**
 * Light color scheme. Functional but less polished than dark — most
 * Gadget tinkering happens at night.
 */
val GadgetLightColorScheme = lightColorScheme(
    primary = GadgetPalette.BrandTealStrong,
    onPrimary = Color.White,
    primaryContainer = GadgetPalette.BrandTealSoft,
    onPrimaryContainer = Color(0xFF002923),
    inversePrimary = GadgetPalette.BrandTeal,
    secondary = GadgetPalette.BrandMagentaStrong,
    onSecondary = Color.White,
    secondaryContainer = GadgetPalette.BrandMagentaSoft,
    onSecondaryContainer = Color(0xFF3D0F30),
    tertiary = GadgetPalette.BrandAmberStrong,
    onTertiary = Color.White,
    tertiaryContainer = GadgetPalette.BrandAmberSoft,
    onTertiaryContainer = Color(0xFF3D2A08),
    background = GadgetPalette.LightBackground,
    onBackground = GadgetPalette.LightOnBackground,
    surface = GadgetPalette.LightSurface,
    onSurface = GadgetPalette.LightOnSurface,
    surfaceVariant = GadgetPalette.LightSurfaceContainerHigh,
    onSurfaceVariant = GadgetPalette.LightOnSurfaceVariant,
    surfaceDim = GadgetPalette.LightSurfaceContainerHighest,
    surfaceBright = GadgetPalette.LightSurface,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = GadgetPalette.LightBackground,
    surfaceContainer = GadgetPalette.LightSurfaceContainer,
    surfaceContainerHigh = GadgetPalette.LightSurfaceContainerHigh,
    surfaceContainerHighest = GadgetPalette.LightSurfaceContainerHighest,
    outline = GadgetPalette.LightOutline,
    outlineVariant = GadgetPalette.LightOutlineVariant,
    error = GadgetPalette.LightError,
    onError = GadgetPalette.LightOnError,
    errorContainer = GadgetPalette.LightErrorContainer,
    onErrorContainer = GadgetPalette.LightOnErrorContainer,
    scrim = Color.Black,
)

// ─── Phase-3 user-selectable custom themes ──────────────────────────────────
// Each is a `.copy()` derivation of a base scheme so every required slot is
// inherited and only the intentionally-different colours are overridden.

/** High-contrast dark: pure-black grounds, white text, brightened accents, stronger outlines. */
val GadgetHighContrastDarkColorScheme: ColorScheme = GadgetDarkColorScheme.copy(
    primary = GadgetPalette.BrandTealSoft,
    onPrimary = Color.Black,
    secondary = GadgetPalette.BrandMagentaSoft,
    onSecondary = Color.Black,
    tertiary = GadgetPalette.BrandAmberSoft,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFE4E7EC),
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF222831),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0C0F),
    surfaceContainer = Color(0xFF111418),
    surfaceContainerHigh = Color(0xFF191D23),
    surfaceContainerHighest = Color(0xFF222831),
    outline = Color(0xFF9AA2AE),
    outlineVariant = Color(0xFF5C6470),
)

/** High-contrast light: pure-white grounds, black text, darkened accents, stronger outlines. */
val GadgetHighContrastLightColorScheme: ColorScheme = GadgetLightColorScheme.copy(
    primary = Color(0xFF00564A),
    onPrimary = Color.White,
    secondary = Color(0xFF7A2A62),
    onSecondary = Color.White,
    tertiary = Color(0xFF6E4E12),
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF1A1D22),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F6FA),
    surfaceContainer = Color(0xFFEDEFF4),
    surfaceContainerHigh = Color(0xFFE2E5EC),
    surfaceContainerHighest = Color(0xFFD6DAE2),
    outline = Color(0xFF2B2F36),
    outlineVariant = Color(0xFF6B707A),
)

/** AMOLED true-black: every surface paints #000000 so OLED pixels switch fully off. */
val GadgetAmoledColorScheme: ColorScheme = GadgetDarkColorScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF101316),
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF15191E),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color(0xFF080A0C),
    surfaceContainerHigh = Color(0xFF101316),
    surfaceContainerHighest = Color(0xFF181C21),
    outlineVariant = Color(0xFF1C2025),
)

// Pastel accents — soft, desaturated hues.
private val PastelTeal = Color(0xFF7FD8C8)
private val PastelPink = Color(0xFFE6A8D0)
private val PastelAmber = Color(0xFFF2D08A)

/** Pastel dark: muted slate grounds with soft accents. */
val GadgetPastelDarkColorScheme: ColorScheme = GadgetDarkColorScheme.copy(
    primary = PastelTeal,
    onPrimary = Color(0xFF06251F),
    primaryContainer = Color(0xFF234A42),
    secondary = PastelPink,
    onSecondary = Color(0xFF2E0E25),
    tertiary = PastelAmber,
    onTertiary = Color(0xFF2E2408),
    background = Color(0xFF14161A),
    surface = Color(0xFF181B20),
    surfaceContainer = Color(0xFF1B1F25),
    surfaceContainerHigh = Color(0xFF222730),
)

/** Pastel light: soft off-white grounds with gentle accents. */
val GadgetPastelLightColorScheme: ColorScheme = GadgetLightColorScheme.copy(
    primary = Color(0xFF4FA593),
    onPrimary = Color.White,
    secondary = Color(0xFFB56FA0),
    onSecondary = Color.White,
    tertiary = Color(0xFFC9A24E),
    onTertiary = Color.White,
    background = Color(0xFFF7F4FA),
    surface = Color(0xFFFFFDFF),
    surfaceContainer = Color(0xFFF1ECF5),
)

/**
 * Resolve the [ColorScheme] for a [GadgetCustomTheme] at the given brightness,
 * or `null` for [GadgetCustomTheme.Default] (the caller then falls back to the
 * dynamic / canonical resolution). AMOLED is a dark-only concept, so its light
 * variant uses the standard light palette.
 */
fun GadgetCustomTheme.colorScheme(dark: Boolean): ColorScheme? = when (this) {
    GadgetCustomTheme.Default -> null
    GadgetCustomTheme.HighContrast ->
        if (dark) GadgetHighContrastDarkColorScheme else GadgetHighContrastLightColorScheme
    GadgetCustomTheme.AmoledTrue ->
        if (dark) GadgetAmoledColorScheme else GadgetLightColorScheme
    GadgetCustomTheme.Pastel ->
        if (dark) GadgetPastelDarkColorScheme else GadgetPastelLightColorScheme
}

/**
 * Materialize a **user-defined accent palette** (W9) onto the canonical
 * [base] scheme: overrides only `primary` / `secondary` / `tertiary` (and picks
 * a legible on-color per accent by luminance), leaving surfaces and the rest of
 * the scheme intact. An accent override — not a full tonal generation — because
 * the app ships no seed→scheme (HCT) generator. The three accents come from the
 * user's `CustomPalette` as `Color`s (designsystem stays free of `:core:datastore`).
 */
fun customColorScheme(
    base: ColorScheme,
    primary: Color,
    secondary: Color,
    tertiary: Color,
): ColorScheme = base.copy(
    primary = primary,
    onPrimary = onColorFor(primary),
    secondary = secondary,
    onSecondary = onColorFor(secondary),
    tertiary = tertiary,
    onTertiary = onColorFor(tertiary),
)

/** Black or white, whichever reads on [color] (simple luminance threshold). */
private fun onColorFor(color: Color): Color =
    if (color.luminance() > 0.5f) Color.Black else Color.White
