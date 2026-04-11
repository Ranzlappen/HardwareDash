package com.gadget.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

/**
 * Creates a scaled copy of [AppTypography] by multiplying all fontSize and lineHeight
 * values by the given [scale]. Used for the large-text accessibility setting.
 */
fun scaledTypography(scale: Float): Typography = Typography(
    headlineLarge = AppTypography.headlineLarge.copy(
        fontSize = (28 * scale).sp, lineHeight = (36 * scale).sp,
    ),
    headlineMedium = AppTypography.headlineMedium.copy(
        fontSize = (24 * scale).sp, lineHeight = (32 * scale).sp,
    ),
    titleLarge = AppTypography.titleLarge.copy(
        fontSize = (20 * scale).sp, lineHeight = (28 * scale).sp,
    ),
    titleMedium = AppTypography.titleMedium.copy(
        fontSize = (16 * scale).sp, lineHeight = (24 * scale).sp,
    ),
    titleSmall = AppTypography.titleSmall.copy(
        fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp,
    ),
    bodyLarge = AppTypography.bodyLarge.copy(
        fontSize = (16 * scale).sp, lineHeight = (24 * scale).sp,
    ),
    bodyMedium = AppTypography.bodyMedium.copy(
        fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp,
    ),
    bodySmall = AppTypography.bodySmall.copy(
        fontSize = (12 * scale).sp, lineHeight = (16 * scale).sp,
    ),
    labelLarge = AppTypography.labelLarge.copy(
        fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp,
    ),
    labelMedium = AppTypography.labelMedium.copy(
        fontSize = (12 * scale).sp, lineHeight = (16 * scale).sp,
    ),
    labelSmall = AppTypography.labelSmall.copy(
        fontSize = (11 * scale).sp, lineHeight = (16 * scale).sp,
    ),
)
