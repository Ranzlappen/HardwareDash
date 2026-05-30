package dev.ranzlappen.gadget.core.widgetkit.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.a11y.LocalReducedMotion
import dev.ranzlappen.gadget.core.widgetkit.config.BackgroundMode
import dev.ranzlappen.gadget.core.widgetkit.config.TapAnimation
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.core.widgetkit.render.iconTintArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Approximate, live render of how a 1×1 kit-built widget will look with
 * the given [appearance]. Drawn in pure Compose (not RemoteViews) but
 * mirrors the [dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer]
 * mapping — same background shapes/colours and the shared [iconTintArgb]
 * tint — so the preview tracks the placed widget.
 *
 * Shows the active-state [icon] (callers resolve
 * `appearance.iconStyle.activeKey` to a [WidgetIconSource] via their
 * feature's icon resolver, e.g. torch's `WidgetIconCatalog`). A
 * [WidgetIconSource.CustomFile] is decoded off the main thread and
 * rendered untinted (it carries its own colours); a
 * [WidgetIconSource.Resource] is tinted per the icon style.
 *
 * When [interactive] is true (the widget editor sets this) the preview
 * becomes pressable and **demonstrates the configured tap animation** so
 * the user can see what each option does without pinning the widget
 * first — Flash whitens the icon, Pulse dips its alpha, Scale shrinks it,
 * and Ripple shows the launcher-style ripple. The list-row thumbnail
 * leaves [interactive] off and stays static. Animations honour
 * [LocalReducedMotion]. (The real on-widget press frame is a best-effort
 * RemoteViews swap; some launchers throttle it, so this preview is the
 * reliable demonstration.)
 */
@Composable
fun WidgetAppearancePreview(
    appearance: WidgetAppearance,
    icon: WidgetIconSource,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
) {
    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tapEnabled = interactive && appearance.tap.enabled
    val animation = appearance.tap.animation

    // 0 = resting, 1 = fully pressed. Pinned to 0 under reduced motion
    // (a Ripple press still shows, it just doesn't scale/fade/flash).
    val pressProgress by animateFloatAsState(
        targetValue = if (tapEnabled && isPressed && !reducedMotion) 1f else 0f,
        label = "widget-preview-press",
    )

    val baseTint = Color(iconTintArgb(LocalContext.current, appearance.iconStyle))
    val iconTint = if (animation == TapAnimation.Flash) lerp(baseTint, Color.White, pressProgress) else baseTint
    val iconScale = if (animation == TapAnimation.Scale) 1f - SCALE_PRESS_DROP * pressProgress else 1f
    val iconAlpha = if (animation == TapAnimation.Pulse) 1f - PULSE_PRESS_DROP * pressProgress else 1f

    val shape = RoundedCornerShape(PreviewDefaults.Corner)
    val interaction = if (tapEnabled) {
        Modifier.clickable(
            interactionSource = interactionSource,
            // Ripple uses the themed indication; the frame animations
            // isolate their own visual, so suppress the ripple for them.
            indication = if (animation == TapAnimation.Ripple) LocalIndication.current else null,
            onClick = {},
        )
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(PreviewDefaults.CellSize)
            .clip(shape)
            .then(interaction),
        contentAlignment = Alignment.Center,
    ) {
        when (appearance.background) {
            BackgroundMode.GlassSurface -> Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(GlassFill)
                    .border(PreviewDefaults.GlassStroke, GlassStrokeColor, shape),
            )
            BackgroundMode.Solid -> Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(Color(appearance.solidColor)),
            )
            BackgroundMode.Transparent -> Unit
        }
        val iconModifier = Modifier
            .matchParentSize()
            .padding(PreviewDefaults.IconPadding)
            .scale(iconScale)
            .alpha(iconAlpha)
        when (icon) {
            is WidgetIconSource.Resource -> Image(
                painter = painterResource(icon.resId),
                contentDescription = null,
                colorFilter = ColorFilter.tint(iconTint),
                contentScale = ContentScale.Fit,
                modifier = iconModifier,
            )
            is WidgetIconSource.CustomFile -> {
                // Decode off the main thread; a user image renders untinted.
                val bitmap by produceState<ImageBitmap?>(initialValue = null, icon.path) {
                    value = withContext(Dispatchers.IO) {
                        runCatching { BitmapFactory.decodeFile(icon.path)?.asImageBitmap() }.getOrNull()
                    }
                }
                bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = iconModifier,
                    )
                }
            }
        }
    }
}

/** Glass-surface approximation — mirrors the fill + outline of
 *  `widget_background_glass.xml` (#33222222 fill, #33FFFFFF stroke). */
private val GlassFill = Color(0x33222222)
private val GlassStrokeColor = Color(0x33FFFFFF)

/** How far the icon shrinks / fades at full press for the Scale / Pulse
 *  preview animations (fractions of the resting value). */
private const val SCALE_PRESS_DROP = 0.2f
private const val PULSE_PRESS_DROP = 0.6f

/** Fixed preview geometry. ~72 dp approximates a 1×1 home-screen cell;
 *  the corner radius matches the widget background drawables (16 dp) and
 *  the icon padding matches the widget layouts (12 dp). Per-file
 *  design-token constants per the repo's no-raw-dp rule. */
private object PreviewDefaults {
    val CellSize = 72.dp
    val Corner = 16.dp
    val IconPadding = 12.dp
    val GlassStroke = 1.dp
}
