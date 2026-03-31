package com.hardwaredash.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
private val DarkCard    = Color(0xFF16213E)

private val DarkColors = darkColorScheme(
    primary          = CyanAccent,
    onPrimary        = Color.Black,
    primaryContainer = Color(0xFF003F47),
    secondary        = GreenAccent,
    onSecondary      = Color.Black,
    tertiary         = AmberAccent,
    onTertiary       = Color.Black,
    background       = DarkBg,
    onBackground     = Color(0xFFE0E0E0),
    surface          = DarkSurface,
    onSurface        = Color(0xFFE0E0E0),
    surfaceVariant   = DarkCard,
    outline          = Color(0xFF37474F),
    error            = Color(0xFFCF6679),
)

private val LightColors = lightColorScheme(
    primary          = Color(0xFF00838F),
    onPrimary        = Color.White,
    secondary        = Color(0xFF388E3C),
    tertiary         = Color(0xFFF57F17),
    background       = Color(0xFFF5F5F5),
    surface          = Color.White,
)

// ─── Theme entry point ────────────────────────────────────────────────────────
@Composable
fun HardwareDashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colors, content = content)
}
