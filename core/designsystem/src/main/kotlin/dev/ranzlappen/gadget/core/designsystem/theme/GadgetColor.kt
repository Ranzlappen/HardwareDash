package dev.ranzlappen.gadget.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

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
