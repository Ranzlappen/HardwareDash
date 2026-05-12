package dev.ranzlappen.gadget.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism intensity presets.
 *
 * Choose by visual purpose:
 *
 * - [Subtle] — content panels (long-running text, lists). High opacity
 *   keeps readability; the glass effect is felt more than seen.
 * - [Standard] — dashboard cards, settings tiles. Balanced.
 * - [Vivid] — hero surfaces over a vibrant gradient or content layer
 *   below. Most translucent; assumes there's something interesting
 *   behind it.
 */
@Immutable
enum class GlassIntensity(
    val topAlpha: Float,
    val bottomAlpha: Float,
    val borderAlpha: Float,
) {
    Subtle(topAlpha = 0.85f, bottomAlpha = 0.70f, borderAlpha = 0.30f),
    Standard(topAlpha = 0.65f, bottomAlpha = 0.40f, borderAlpha = 0.50f),
    Vivid(topAlpha = 0.40f, bottomAlpha = 0.18f, borderAlpha = 0.65f),
}

/**
 * Apply a glassy surface to a composable.
 *
 * The glass look is built from three independent layers:
 *
 *   1. A vertical gradient between `colorScheme.surface` at the top and
 *      `colorScheme.surfaceContainer` at the bottom, both with the
 *      [intensity]'s configured opacity.
 *   2. A hairline `colorScheme.outlineVariant` border, opacity-modulated
 *      by the same [intensity]. Catches reflections in the design
 *      language while staying readable.
 *   3. A rounded clip at [cornerRadius] applied to both the background
 *      and the border so the gradient never bleeds beyond the silhouette.
 *
 * Backdrop blur is intentionally not applied here — true backdrop blur
 * on Android requires a `RenderEffect` on API 31+ plus a snapshot of
 * the underlying layer, which costs enough to deserve its own dedicated
 * batch. The gradient + opacity + border read convincingly as glass
 * when stacked over the dark theme's near-black background, and on
 * older devices the look is preserved without the additional cost.
 *
 * Surfaces with no readable content beneath them should use
 * [GlassIntensity.Subtle] so users don't perceive a "hole" in the UI.
 */
@Composable
fun Modifier.glassSurface(
    cornerRadius: Dp = 18.dp,
    intensity: GlassIntensity = GlassIntensity.Standard,
    showBorder: Boolean = true,
): Modifier {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(cornerRadius)
    val gradient = Brush.verticalGradient(
        colors = listOf(
            scheme.surface.copy(alpha = intensity.topAlpha),
            scheme.surfaceContainer.copy(alpha = intensity.bottomAlpha),
        ),
    )
    val withBackground = this
        .clip(shape)
        .background(brush = gradient, shape = shape)
    return if (showBorder) {
        withBackground.border(
            width = 1.dp,
            color = scheme.outlineVariant.copy(alpha = intensity.borderAlpha),
            shape = shape,
        )
    } else {
        withBackground
    }
}
