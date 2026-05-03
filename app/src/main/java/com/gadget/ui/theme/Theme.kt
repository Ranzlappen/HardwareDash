package com.gadget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue

// ─── Theme entry point ────────────────────────────────────────────────────────
@Composable
fun GadgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val accessibilityPrefs by AccessibilityPreferencesManager.prefs
    val themePrefs by ThemePreferencesManager.prefs

    val preset = colorPresetById(themePrefs.presetId)

    val colors = when {
        darkTheme && accessibilityPrefs.highContrast -> preset.highContrastDark
        darkTheme                                    -> preset.dark
        accessibilityPrefs.highContrast              -> preset.highContrastLight
        else                                         -> preset.light
    }

    val typography = if (accessibilityPrefs.largeText) scaledTypography(1.2f) else AppTypography

    CompositionLocalProvider(
        LocalAccessibilityPreferences provides accessibilityPrefs,
        LocalThemePreferences provides themePrefs,
    ) {
        MaterialTheme(
            colorScheme = colors,
            shapes = AppShapes,
            typography = typography,
            content = content,
        )
    }
}
