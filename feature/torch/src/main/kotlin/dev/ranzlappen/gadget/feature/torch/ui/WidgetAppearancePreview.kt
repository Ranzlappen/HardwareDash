package dev.ranzlappen.gadget.feature.torch.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.feature.torch.widget.customization.BackgroundMode
import dev.ranzlappen.gadget.feature.torch.widget.customization.WidgetAppearance
import dev.ranzlappen.gadget.feature.torch.widget.customization.iconTintArgb

/**
 * Approximate, live render of how a 1×1 torch widget will look with the
 * given [appearance]. Drawn in pure Compose (not RemoteViews) but mirrors
 * the [dev.ranzlappen.gadget.feature.torch.widget.customization.WidgetAppearanceRenderer]
 * mapping — same background shapes/colours and the shared [iconTintArgb]
 * tint — so the preview tracks the placed widget.
 *
 * Shows the active-state icon ([iconResId] should resolve
 * `appearance.iconStyle.activeKey`). Tap animation, SOS and rate are
 * runtime behaviours and aren't depicted here.
 */
@Composable
fun WidgetAppearancePreview(
    appearance: WidgetAppearance,
    iconResId: Int,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(PreviewDefaults.Corner)
    Box(
        modifier = modifier.size(PreviewDefaults.CellSize),
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
        Image(
            painter = painterResource(iconResId),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color(iconTintArgb(LocalContext.current, appearance.iconStyle))),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .matchParentSize()
                .padding(PreviewDefaults.IconPadding),
        )
    }
}

/** Glass-surface approximation — mirrors the fill + outline of
 *  `widget_background_glass.xml` (#33222222 fill, #33FFFFFF stroke). */
private val GlassFill = Color(0x33222222)
private val GlassStrokeColor = Color(0x33FFFFFF)

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
