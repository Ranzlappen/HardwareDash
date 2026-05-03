package com.gadget.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * A named color preset offering four ColorScheme variants:
 * standard light/dark and high-contrast light/dark.
 *
 * Resolved at runtime by [GadgetTheme] based on system dark mode and the
 * user's high-contrast accessibility flag.
 */
data class ColorPreset(
    val id: String,
    val displayName: String,
    val light: ColorScheme,
    val dark: ColorScheme,
    val highContrastLight: ColorScheme,
    val highContrastDark: ColorScheme,
    val swatchPrimary: Color,
    val swatchSecondary: Color,
)

// ─── Shared structural colors (background / surface stack / error) ────────────

private val DarkBgConst       = Color(0xFF0D0D0D)
private val DarkSurfaceConst  = Color(0xFF1A1A2E)
private val DarkCardConst     = Color(0xFF1B2838)
private val LightBgConst      = Color(0xFFF5F5F5)
private val LightSurfaceConst = Color.White
private val LightCardConst    = Color(0xFFE8EAF0)

// ─── Builders that hold structural colors constant ────────────────────────────

private fun buildDark(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    secondary: Color,
    onSecondary: Color,
    tertiary: Color,
    onTertiary: Color,
    inversePrimary: Color,
): ColorScheme = darkColorScheme(
    primary              = primary,
    onPrimary            = onPrimary,
    primaryContainer     = primaryContainer,
    secondary            = secondary,
    onSecondary          = onSecondary,
    tertiary             = tertiary,
    onTertiary           = onTertiary,
    background           = DarkBgConst,
    onBackground         = Color(0xFFE0E0E0),
    surface              = DarkSurfaceConst,
    onSurface            = Color(0xFFE0E0E0),
    surfaceVariant       = DarkCardConst,
    onSurfaceVariant     = Color(0xFFB0BEC5),
    surfaceContainerLow  = Color(0xFF131320),
    surfaceContainer     = Color(0xFF1A1A2E),
    surfaceContainerHigh = Color(0xFF222240),
    outline              = Color(0xFF37474F),
    outlineVariant       = Color(0xFF263238),
    inverseSurface       = Color(0xFFE0E0E0),
    inversePrimary       = inversePrimary,
    error                = Color(0xFFCF6679),
)

private fun buildLight(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    secondary: Color,
    onSecondary: Color,
    tertiary: Color,
    onTertiary: Color,
    inversePrimary: Color,
): ColorScheme = lightColorScheme(
    primary              = primary,
    onPrimary            = onPrimary,
    primaryContainer     = primaryContainer,
    secondary            = secondary,
    onSecondary          = onSecondary,
    tertiary             = tertiary,
    onTertiary           = onTertiary,
    background           = LightBgConst,
    onBackground         = Color(0xFF1C1B1F),
    surface              = LightSurfaceConst,
    onSurface            = Color(0xFF1C1B1F),
    surfaceVariant       = LightCardConst,
    onSurfaceVariant     = Color(0xFF49454F),
    surfaceContainerLow  = Color(0xFFF0F0F5),
    surfaceContainer     = Color(0xFFEAEAF0),
    surfaceContainerHigh = Color(0xFFE0E0E8),
    outline              = Color(0xFF79747E),
    outlineVariant       = Color(0xFFCAC4D0),
    inverseSurface       = Color(0xFF313033),
    inversePrimary       = inversePrimary,
    error                = Color(0xFFB3261E),
)

private fun buildHcDark(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    secondary: Color,
    onSecondary: Color,
    tertiary: Color,
    onTertiary: Color,
    inversePrimary: Color,
): ColorScheme = darkColorScheme(
    primary              = primary,
    onPrimary            = onPrimary,
    primaryContainer     = primaryContainer,
    secondary            = secondary,
    onSecondary          = onSecondary,
    tertiary             = tertiary,
    onTertiary           = onTertiary,
    background           = DarkBgConst,
    onBackground         = Color(0xFFF5F5F5),
    surface              = DarkSurfaceConst,
    onSurface            = Color(0xFFF5F5F5),
    surfaceVariant       = Color(0xFF263238),
    onSurfaceVariant     = Color(0xFFCFD8DC),
    surfaceContainerLow  = Color(0xFF131320),
    surfaceContainer     = Color(0xFF1A1A2E),
    surfaceContainerHigh = Color(0xFF222240),
    outline              = Color(0xFF607D8B),
    outlineVariant       = Color(0xFF37474F),
    inverseSurface       = Color(0xFFF5F5F5),
    inversePrimary       = inversePrimary,
    error                = Color(0xFFFF8A80),
)

private fun buildHcLight(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    secondary: Color,
    onSecondary: Color,
    tertiary: Color,
    onTertiary: Color,
    inversePrimary: Color,
): ColorScheme = lightColorScheme(
    primary              = primary,
    onPrimary            = onPrimary,
    primaryContainer     = primaryContainer,
    secondary            = secondary,
    onSecondary          = onSecondary,
    tertiary             = tertiary,
    onTertiary           = onTertiary,
    background           = LightBgConst,
    onBackground         = Color(0xFF000000),
    surface              = LightSurfaceConst,
    onSurface            = Color(0xFF000000),
    surfaceVariant       = LightCardConst,
    onSurfaceVariant     = Color(0xFF1C1B1F),
    surfaceContainerLow  = Color(0xFFF0F0F5),
    surfaceContainer     = Color(0xFFEAEAF0),
    surfaceContainerHigh = Color(0xFFE0E0E8),
    outline              = Color(0xFF49454F),
    outlineVariant       = Color(0xFF79747E),
    inverseSurface       = Color(0xFF000000),
    inversePrimary       = inversePrimary,
    error                = Color(0xFF8B0000),
)

// ─── Preset definitions ──────────────────────────────────────────────────────

private val PresetCyan = ColorPreset(
    id = "cyan",
    displayName = "Cyan",
    light = buildLight(
        primary = Color(0xFF00838F), onPrimary = Color.White,
        primaryContainer = Color(0xFFB2EBF2),
        secondary = Color(0xFF388E3C), onSecondary = Color.White,
        tertiary = Color(0xFFF57F17), onTertiary = Color.White,
        inversePrimary = Color(0xFF80DEEA),
    ),
    dark = buildDark(
        primary = Color(0xFF00BCD4), onPrimary = Color.Black,
        primaryContainer = Color(0xFF003F47),
        secondary = Color(0xFF4CAF50), onSecondary = Color.Black,
        tertiary = Color(0xFFFFC107), onTertiary = Color.Black,
        inversePrimary = Color(0xFF00838F),
    ),
    highContrastLight = buildHcLight(
        primary = Color(0xFF006064), onPrimary = Color.White,
        primaryContainer = Color(0xFFB2EBF2),
        secondary = Color(0xFF2E7D32), onSecondary = Color.White,
        tertiary = Color(0xFFE65100), onTertiary = Color.White,
        inversePrimary = Color(0xFF80DEEA),
    ),
    highContrastDark = buildHcDark(
        primary = Color(0xFF4DD0E1), onPrimary = Color.Black,
        primaryContainer = Color(0xFF004D54),
        secondary = Color(0xFF66BB6A), onSecondary = Color.Black,
        tertiary = Color(0xFFFFD54F), onTertiary = Color.Black,
        inversePrimary = Color(0xFF006064),
    ),
    swatchPrimary = Color(0xFF00BCD4),
    swatchSecondary = Color(0xFF4CAF50),
)

private val PresetForest = ColorPreset(
    id = "forest",
    displayName = "Forest",
    light = buildLight(
        primary = Color(0xFF2E7D32), onPrimary = Color.White,
        primaryContainer = Color(0xFFC8E6C9),
        secondary = Color(0xFF6D4C41), onSecondary = Color.White,
        tertiary = Color(0xFF827717), onTertiary = Color.White,
        inversePrimary = Color(0xFF81C784),
    ),
    dark = buildDark(
        primary = Color(0xFF66BB6A), onPrimary = Color.Black,
        primaryContainer = Color(0xFF1B5E20),
        secondary = Color(0xFFA1887F), onSecondary = Color.Black,
        tertiary = Color(0xFFCDDC39), onTertiary = Color.Black,
        inversePrimary = Color(0xFF2E7D32),
    ),
    highContrastLight = buildHcLight(
        primary = Color(0xFF1B5E20), onPrimary = Color.White,
        primaryContainer = Color(0xFFC8E6C9),
        secondary = Color(0xFF4E342E), onSecondary = Color.White,
        tertiary = Color(0xFF558B2F), onTertiary = Color.White,
        inversePrimary = Color(0xFF81C784),
    ),
    highContrastDark = buildHcDark(
        primary = Color(0xFF81C784), onPrimary = Color.Black,
        primaryContainer = Color(0xFF1B5E20),
        secondary = Color(0xFFBCAAA4), onSecondary = Color.Black,
        tertiary = Color(0xFFDCE775), onTertiary = Color.Black,
        inversePrimary = Color(0xFF1B5E20),
    ),
    swatchPrimary = Color(0xFF66BB6A),
    swatchSecondary = Color(0xFFCDDC39),
)

private val PresetSunset = ColorPreset(
    id = "sunset",
    displayName = "Sunset",
    light = buildLight(
        primary = Color(0xFFD84315), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFCCBC),
        secondary = Color(0xFFC2185B), onSecondary = Color.White,
        tertiary = Color(0xFFF9A825), onTertiary = Color.Black,
        inversePrimary = Color(0xFFFF8A65),
    ),
    dark = buildDark(
        primary = Color(0xFFFF7043), onPrimary = Color.Black,
        primaryContainer = Color(0xFF6A1B0A),
        secondary = Color(0xFFEC407A), onSecondary = Color.Black,
        tertiary = Color(0xFFFFD54F), onTertiary = Color.Black,
        inversePrimary = Color(0xFFD84315),
    ),
    highContrastLight = buildHcLight(
        primary = Color(0xFFBF360C), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFCCBC),
        secondary = Color(0xFF880E4F), onSecondary = Color.White,
        tertiary = Color(0xFFE65100), onTertiary = Color.White,
        inversePrimary = Color(0xFFFF8A65),
    ),
    highContrastDark = buildHcDark(
        primary = Color(0xFFFF8A65), onPrimary = Color.Black,
        primaryContainer = Color(0xFF6A1B0A),
        secondary = Color(0xFFF06292), onSecondary = Color.Black,
        tertiary = Color(0xFFFFE082), onTertiary = Color.Black,
        inversePrimary = Color(0xFFBF360C),
    ),
    swatchPrimary = Color(0xFFFF7043),
    swatchSecondary = Color(0xFFEC407A),
)

private val PresetMono = ColorPreset(
    id = "mono",
    displayName = "Mono",
    light = buildLight(
        primary = Color(0xFF212121), onPrimary = Color.White,
        primaryContainer = Color(0xFFE0E0E0),
        secondary = Color(0xFF616161), onSecondary = Color.White,
        tertiary = Color(0xFF9E9E9E), onTertiary = Color.Black,
        inversePrimary = Color(0xFFBDBDBD),
    ),
    dark = buildDark(
        primary = Color(0xFFE0E0E0), onPrimary = Color.Black,
        primaryContainer = Color(0xFF424242),
        secondary = Color(0xFFBDBDBD), onSecondary = Color.Black,
        tertiary = Color(0xFF9E9E9E), onTertiary = Color.Black,
        inversePrimary = Color(0xFF424242),
    ),
    highContrastLight = buildHcLight(
        primary = Color(0xFF000000), onPrimary = Color.White,
        primaryContainer = Color(0xFFE0E0E0),
        secondary = Color(0xFF212121), onSecondary = Color.White,
        tertiary = Color(0xFF616161), onTertiary = Color.White,
        inversePrimary = Color(0xFFBDBDBD),
    ),
    highContrastDark = buildHcDark(
        primary = Color(0xFFFFFFFF), onPrimary = Color.Black,
        primaryContainer = Color(0xFF616161),
        secondary = Color(0xFFE0E0E0), onSecondary = Color.Black,
        tertiary = Color(0xFFBDBDBD), onTertiary = Color.Black,
        inversePrimary = Color(0xFF212121),
    ),
    swatchPrimary = Color(0xFFE0E0E0),
    swatchSecondary = Color(0xFF9E9E9E),
)

private val PresetRoyal = ColorPreset(
    id = "royal",
    displayName = "Royal",
    light = buildLight(
        primary = Color(0xFF512DA8), onPrimary = Color.White,
        primaryContainer = Color(0xFFD1C4E9),
        secondary = Color(0xFF1976D2), onSecondary = Color.White,
        tertiary = Color(0xFFAB47BC), onTertiary = Color.White,
        inversePrimary = Color(0xFFB39DDB),
    ),
    dark = buildDark(
        primary = Color(0xFFB39DDB), onPrimary = Color.Black,
        primaryContainer = Color(0xFF311B92),
        secondary = Color(0xFF64B5F6), onSecondary = Color.Black,
        tertiary = Color(0xFFCE93D8), onTertiary = Color.Black,
        inversePrimary = Color(0xFF512DA8),
    ),
    highContrastLight = buildHcLight(
        primary = Color(0xFF311B92), onPrimary = Color.White,
        primaryContainer = Color(0xFFD1C4E9),
        secondary = Color(0xFF0D47A1), onSecondary = Color.White,
        tertiary = Color(0xFF6A1B9A), onTertiary = Color.White,
        inversePrimary = Color(0xFFB39DDB),
    ),
    highContrastDark = buildHcDark(
        primary = Color(0xFFD1C4E9), onPrimary = Color.Black,
        primaryContainer = Color(0xFF311B92),
        secondary = Color(0xFF90CAF9), onSecondary = Color.Black,
        tertiary = Color(0xFFE1BEE7), onTertiary = Color.Black,
        inversePrimary = Color(0xFF311B92),
    ),
    swatchPrimary = Color(0xFFB39DDB),
    swatchSecondary = Color(0xFF64B5F6),
)

private val PresetCrimson = ColorPreset(
    id = "crimson",
    displayName = "Crimson",
    light = buildLight(
        primary = Color(0xFFC62828), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFCDD2),
        secondary = Color(0xFF455A64), onSecondary = Color.White,
        tertiary = Color(0xFFAD1457), onTertiary = Color.White,
        inversePrimary = Color(0xFFEF9A9A),
    ),
    dark = buildDark(
        primary = Color(0xFFEF5350), onPrimary = Color.Black,
        primaryContainer = Color(0xFF8E0000),
        secondary = Color(0xFF90A4AE), onSecondary = Color.Black,
        tertiary = Color(0xFFF06292), onTertiary = Color.Black,
        inversePrimary = Color(0xFFC62828),
    ),
    highContrastLight = buildHcLight(
        primary = Color(0xFF8E0000), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFCDD2),
        secondary = Color(0xFF263238), onSecondary = Color.White,
        tertiary = Color(0xFF6A1B4E), onTertiary = Color.White,
        inversePrimary = Color(0xFFEF9A9A),
    ),
    highContrastDark = buildHcDark(
        primary = Color(0xFFEF9A9A), onPrimary = Color.Black,
        primaryContainer = Color(0xFF8E0000),
        secondary = Color(0xFFB0BEC5), onSecondary = Color.Black,
        tertiary = Color(0xFFF8BBD0), onTertiary = Color.Black,
        inversePrimary = Color(0xFF8E0000),
    ),
    swatchPrimary = Color(0xFFEF5350),
    swatchSecondary = Color(0xFFF06292),
)

/** All available color presets, in display order. */
val ColorPresets: List<ColorPreset> = listOf(
    PresetCyan, PresetForest, PresetSunset, PresetMono, PresetRoyal, PresetCrimson,
)

/** Lookup by id, falling back to the default Cyan preset. */
fun colorPresetById(id: String?): ColorPreset =
    ColorPresets.firstOrNull { it.id == id } ?: PresetCyan

/** The default preset (Cyan) — matches the original hardcoded theme. */
val DefaultColorPreset: ColorPreset = PresetCyan
