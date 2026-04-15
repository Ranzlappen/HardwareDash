package com.gadget.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Colour palette ──────────────────────────────────────────────────────────
private val CyanAccent  = Color(0xFF00BCD4)
private val GreenAccent = Color(0xFF4CAF50)
private val AmberAccent = Color(0xFFFFC107)
private val DarkBg      = Color(0xFF0D0D0D)
private val DarkSurface = Color(0xFF1A1A2E)
private val DarkCard    = Color(0xFF1B2838)

private val DarkColors = darkColorScheme(
    primary            = CyanAccent,
    onPrimary          = Color.Black,
    primaryContainer   = Color(0xFF003F47),
    secondary          = GreenAccent,
    onSecondary        = Color.Black,
    tertiary           = AmberAccent,
    onTertiary         = Color.Black,
    background         = DarkBg,
    onBackground       = Color(0xFFE0E0E0),
    surface            = DarkSurface,
    onSurface          = Color(0xFFE0E0E0),
    surfaceVariant     = DarkCard,
    onSurfaceVariant   = Color(0xFFB0BEC5),
    surfaceContainerLow  = Color(0xFF131320),
    surfaceContainer     = Color(0xFF1A1A2E),
    surfaceContainerHigh = Color(0xFF222240),
    outline            = Color(0xFF37474F),
    outlineVariant     = Color(0xFF263238),
    inverseSurface     = Color(0xFFE0E0E0),
    inversePrimary     = Color(0xFF00838F),
    error              = Color(0xFFCF6679),
)

private val LightColors = lightColorScheme(
    primary            = Color(0xFF00838F),
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFB2EBF2),
    secondary          = Color(0xFF388E3C),
    onSecondary        = Color.White,
    tertiary           = Color(0xFFF57F17),
    onTertiary         = Color.White,
    background         = Color(0xFFF5F5F5),
    onBackground       = Color(0xFF1C1B1F),
    surface            = Color.White,
    onSurface          = Color(0xFF1C1B1F),
    surfaceVariant     = Color(0xFFE8EAF0),
    onSurfaceVariant   = Color(0xFF49454F),
    surfaceContainerLow  = Color(0xFFF0F0F5),
    surfaceContainer     = Color(0xFFEAEAF0),
    surfaceContainerHigh = Color(0xFFE0E0E8),
    outline            = Color(0xFF79747E),
    outlineVariant     = Color(0xFFCAC4D0),
    inverseSurface     = Color(0xFF313033),
    inversePrimary     = Color(0xFF80DEEA),
    error              = Color(0xFFB3261E),
)

// ─── High-contrast colour schemes ────────────────────────────────────────────

private val HighContrastDarkColors = darkColorScheme(
    primary            = Color(0xFF4DD0E1),   // Lighter cyan — ~10.5:1 on #0D0D0D
    onPrimary          = Color.Black,
    primaryContainer   = Color(0xFF004D54),
    secondary          = Color(0xFF66BB6A),   // Brighter green
    onSecondary        = Color.Black,
    tertiary           = Color(0xFFFFD54F),   // Brighter amber
    onTertiary         = Color.Black,
    background         = DarkBg,
    onBackground       = Color(0xFFF5F5F5),   // Near-white for max contrast
    surface            = DarkSurface,
    onSurface          = Color(0xFFF5F5F5),
    surfaceVariant     = Color(0xFF263238),    // Slightly lighter cards
    onSurfaceVariant   = Color(0xFFCFD8DC),   // Boosted from #B0BEC5
    surfaceContainerLow  = Color(0xFF131320),
    surfaceContainer     = Color(0xFF1A1A2E),
    surfaceContainerHigh = Color(0xFF222240),
    outline            = Color(0xFF607D8B),    // More visible borders
    outlineVariant     = Color(0xFF37474F),
    inverseSurface     = Color(0xFFF5F5F5),
    inversePrimary     = Color(0xFF006064),
    error              = Color(0xFFFF8A80),    // Brighter error for visibility
)

private val HighContrastLightColors = lightColorScheme(
    primary            = Color(0xFF006064),    // Darker teal — stronger on white
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFB2EBF2),
    secondary          = Color(0xFF2E7D32),    // Darker green
    onSecondary        = Color.White,
    tertiary           = Color(0xFFE65100),    // Darker orange
    onTertiary         = Color.White,
    background         = Color(0xFFF5F5F5),
    onBackground       = Color(0xFF000000),    // Pure black text
    surface            = Color.White,
    onSurface          = Color(0xFF000000),
    surfaceVariant     = Color(0xFFE8EAF0),
    onSurfaceVariant   = Color(0xFF1C1B1F),   // Near-black
    surfaceContainerLow  = Color(0xFFF0F0F5),
    surfaceContainer     = Color(0xFFEAEAF0),
    surfaceContainerHigh = Color(0xFFE0E0E8),
    outline            = Color(0xFF49454F),    // Darker borders
    outlineVariant     = Color(0xFF79747E),
    inverseSurface     = Color(0xFF000000),
    inversePrimary     = Color(0xFF80DEEA),
    error              = Color(0xFF8B0000),     // Darker red for contrast
)

// ─── Theme entry point ────────────────────────────────────────────────────────
@Composable
fun GadgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val accessibilityPrefs by AccessibilityPreferencesManager.prefs

    val colors = when {
        darkTheme && accessibilityPrefs.highContrast  -> HighContrastDarkColors
        darkTheme                                      -> DarkColors
        accessibilityPrefs.highContrast                -> HighContrastLightColors
        else                                           -> LightColors
    }

    val typography = if (accessibilityPrefs.largeText) scaledTypography(1.2f) else AppTypography

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colors.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAccessibilityPreferences provides accessibilityPrefs) {
        MaterialTheme(
            colorScheme = colors,
            shapes = AppShapes,
            typography = typography,
            content = content,
        )
    }
}
