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
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme

/**
 * Glassmorphism intensity presets.
 *
 * Identity tier — `Subtle` / `Standard` / `Vivid` — picks the visual
 * weight. The actual alpha values for each tier come from
 * `LocalGadgetTheme.current.glass` (a [GadgetGlassValues] data class),
 * so a custom theme can re-tune the glassmorphism without forking
 * components.
 *
 * Choose by visual purpose:
 *
 * - [Subtle] — content panels (long-running text, lists). High
 *   opacity keeps readability; the glass effect is felt more than
 *   seen.
 * - [Standard] — dashboard cards, settings tiles. Balanced.
 * - [Vivid] — hero surfaces over a vibrant gradient or content layer
 *   below. Most translucent; assumes there's something interesting
 *   behind it.
 */
@Immutable
enum class GlassIntensity {
    Subtle,
    Standard,
    Vivid,
}

/**
 * Apply a glassy surface to a composable.
 *
 * The glass look is built from three independent layers:
 *
 *   1. A vertical gradient between `colorScheme.surface` at the top
 *      and `colorScheme.surfaceContainer` at the bottom, both with
 *      the [intensity]'s configured opacity (sourced from
 *      `LocalGadgetTheme.current.glass`).
 *   2. A hairline `colorScheme.outlineVariant` border, opacity-
 *      modulated by the same intensity tier.
 *   3. A rounded clip at [cornerRadius] applied to both the
 *      background and the border so the gradient never bleeds beyond
 *      the silhouette.
 *
 * Backdrop blur is intentionally not applied here — true backdrop
 * blur on Android requires a `RenderEffect` on API 31+ plus a
 * snapshot of the underlying layer, which costs enough to deserve
 * its own dedicated batch. The gradient + opacity + border read
 * convincingly as glass when stacked over the dark theme's near-
 * black background, and on older devices the look is preserved
 * without the additional cost.
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
    val glassValues = LocalGadgetTheme.current.glass
    val topAlpha: Float
    val bottomAlpha: Float
    val borderAlpha: Float
    when (intensity) {
        GlassIntensity.Subtle -> {
            topAlpha = glassValues.subtleTopAlpha
            bottomAlpha = glassValues.subtleBottomAlpha
            borderAlpha = glassValues.subtleBorderAlpha
        }
        GlassIntensity.Standard -> {
            topAlpha = glassValues.standardTopAlpha
            bottomAlpha = glassValues.standardBottomAlpha
            borderAlpha = glassValues.standardBorderAlpha
        }
        GlassIntensity.Vivid -> {
            topAlpha = glassValues.vividTopAlpha
            bottomAlpha = glassValues.vividBottomAlpha
            borderAlpha = glassValues.vividBorderAlpha
        }
    }
    val shape = RoundedCornerShape(cornerRadius)
    val gradient = Brush.verticalGradient(
        colors = listOf(
            scheme.surface.copy(alpha = topAlpha),
            scheme.surfaceContainer.copy(alpha = bottomAlpha),
        ),
    )
    val withBackground = this
        .clip(shape)
        .background(brush = gradient, shape = shape)
    return if (showBorder) {
        withBackground.border(
            width = 1.dp,
            color = scheme.outlineVariant.copy(alpha = borderAlpha),
            shape = shape,
        )
    } else {
        withBackground
    }
}
