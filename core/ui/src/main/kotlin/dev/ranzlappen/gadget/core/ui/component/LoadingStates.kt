package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.a11y.LocalReducedMotion
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetSpacing
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Circular progress indicator with Gadget styling.
 *
 * Wraps M3 [CircularProgressIndicator]. Passing [progress] in the
 * `0f..1f` range renders the determinate variant; passing `null` (the
 * default) renders the indeterminate spinner.
 *
 * The composable applies no internal size — pass a size on the
 * [modifier] (e.g. `Modifier.size(GadgetButtonDefaults.InternalIconSize)`).
 * Matches the design-system contract that components never lock
 * dimensions internally.
 */
@Composable
fun GadgetCircularProgress(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = ProgressIndicatorDefaults.circularTrackColor,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
) {
    if (progress != null) {
        val clamped = progress.coerceIn(0f, 1f)
        CircularProgressIndicator(
            progress = { clamped },
            modifier = modifier.semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = clamped,
                    range = 0f..1f,
                )
            },
            color = color,
            trackColor = trackColor,
            strokeWidth = strokeWidth,
        )
    } else {
        CircularProgressIndicator(
            modifier = modifier,
            color = color,
            trackColor = trackColor,
            strokeWidth = strokeWidth,
        )
    }
}

/**
 * Linear progress indicator with Gadget styling.
 *
 * Wraps M3 [LinearProgressIndicator]. Passing [progress] in the
 * `0f..1f` range renders the determinate variant; passing `null`
 * renders the indeterminate sweep.
 *
 * Pass a width via [modifier] (typically `Modifier.fillMaxWidth()`);
 * the height is fixed by M3 defaults.
 */
@Composable
fun GadgetLinearProgress(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    if (progress != null) {
        val clamped = progress.coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { clamped },
            modifier = modifier.semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = clamped,
                    range = 0f..1f,
                )
            },
            color = color,
            trackColor = trackColor,
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier,
            color = color,
            trackColor = trackColor,
        )
    }
}

/**
 * Shimmer placeholder block — animated linear-gradient sweep over a
 * shaped surface. Use as a content placeholder while real data
 * loads.
 *
 * Pass a size on the [modifier] (e.g.
 * `Modifier.fillMaxWidth().height(16.dp)` for a text-line skeleton,
 * `Modifier.size(64.dp)` for an avatar skeleton). The composable
 * does NOT lock a default size — caller controls dimensions.
 *
 * The gradient sweep direction is diagonal `(0, 0) → (translateX,
 * translateX)` with a 1.5-second linear loop. The translation target
 * is **measured per-instance** via [BoxWithConstraints] — the
 * gradient sweeps from `0` to `maxWidth.toPx() * 2`, so a 32 dp
 * avatar and a 1024 dp banner both get the same visually-uniform
 * shimmer cadence. Without this, a fixed-pixel sweep either flashes
 * imperceptibly on small surfaces or cycles glacially on wide ones.
 *
 * Three colour stops give a smooth shimmer band; intensities derive
 * from [MaterialTheme.colorScheme.surfaceVariant] / `surface` so the
 * effect blends across light + dark + dynamic-colour themes.
 */
@Composable
fun GadgetShimmerBlock(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
) {
    // A11y: announce that we're loading. Polite live region — screen
    // readers wait for the user to finish their current sentence
    // before announcing instead of interrupting.
    val a11yModifier = modifier.semantics {
        contentDescription = "Loading"
        liveRegion = LiveRegionMode.Polite
    }
    // Respect LocalReducedMotion: render a static surfaceVariant
    // block (still legible as "loading") instead of an animated
    // gradient sweep. Caller's intent (placeholder skeleton) is
    // preserved; the motion is suppressed.
    if (LocalReducedMotion.current) {
        Box(
            modifier = a11yModifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        return
    }
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val shimmerColors = listOf(
        base.copy(alpha = ShimmerLowAlpha),
        highlight.copy(alpha = ShimmerHighAlpha),
        base.copy(alpha = ShimmerLowAlpha),
    )
    BoxWithConstraints(modifier = a11yModifier) {
        // constraints.maxWidth is in pixels. For an unbounded parent
        // (no width constraint) fall back to a sensible default so
        // the shimmer still animates rather than freezing.
        val targetPx = if (constraints.maxWidth in 1 until Int.MAX_VALUE) {
            constraints.maxWidth.toFloat() * ShimmerSweepWidthMultiplier
        } else {
            ShimmerFallbackTargetPx
        }
        val transition = rememberInfiniteTransition(label = "shimmer-transition")
        val translateX by transition.animateFloat(
            initialValue = ShimmerStartOffset,
            targetValue = targetPx,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = ShimmerDurationMillis,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmer-translate",
        )
        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateX, y = translateX),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(brush = brush),
        )
    }
}

// ─── Internals ──────────────────────────────────────────────────────

/** Low alpha used for the dim ends of the shimmer gradient. */
private const val ShimmerLowAlpha: Float = 0.6f

/** High alpha used for the bright middle of the shimmer gradient. */
private const val ShimmerHighAlpha: Float = 0.2f

/** Starting position of the shimmer gradient sweep, in pixels. */
private const val ShimmerStartOffset: Float = 0f

/**
 * Multiplier applied to the block's pixel width to compute the
 * gradient sweep endpoint. `2.0f` means the highlight band fully
 * traverses the visible area + an extra block-width of "off-screen"
 * travel, giving a smooth wraparound to the start position when the
 * animation restarts.
 */
private const val ShimmerSweepWidthMultiplier: Float = 2f

/**
 * Sweep endpoint used when the parent's width constraint is
 * [Int.MAX_VALUE] / unmeasurable (unbounded layout — rare but
 * possible inside a `LazyRow` cell measured before placement). 1000
 * px gives a visually plausible sweep on most screen sizes.
 */
private const val ShimmerFallbackTargetPx: Float = 1000f

/** Full loop duration of the shimmer animation, in milliseconds. */
private const val ShimmerDurationMillis: Int = 1500

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@Composable
private fun LoadingStatesPreview() = GadgetThemedPreview {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
            GadgetCircularProgress(modifier = Modifier.size(32.dp))
            GadgetCircularProgress(modifier = Modifier.size(32.dp), progress = 0.6f)
        }
        GadgetLinearProgress(modifier = Modifier.fillMaxWidth())
        GadgetLinearProgress(modifier = Modifier.fillMaxWidth(), progress = 0.3f)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            GadgetShimmerBlock(modifier = Modifier.size(64.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.tiny),
            ) {
                GadgetShimmerBlock(modifier = Modifier.fillMaxWidth().height(16.dp))
                GadgetShimmerBlock(modifier = Modifier.fillMaxWidth().height(12.dp))
            }
        }
    }
}
