package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ranzlappen.gadget.core.designsystem.GlassIntensity
import dev.ranzlappen.gadget.core.designsystem.a11y.LocalReducedTransparency
import dev.ranzlappen.gadget.core.designsystem.glassSurface
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetSpacing

/**
 * Low-level glassmorphic container — the design-system primitive that
 * [DashCard] and [CompactCard] both build on.
 *
 * Use this directly only when you need glass styling without any
 * built-in chrome (no title slot, no icon slot, no header). For the
 * common dashboard-tile shape see [DashCard]; for list-row layouts see
 * [CompactCard].
 *
 * Inherits the `Modifier.glassSurface(...)` extension's defaults
 * (medium corner radius, 1 dp outline border at the intensity-derived
 * alpha). Override via [intensity] / [showBorder] for a different
 * surface tier, or wrap the content in a custom [Modifier.clip] /
 * [Modifier.border] if you need a non-default radius.
 *
 * The receiver of [content] is a [BoxScope] so callers can use
 * `Modifier.align(...)` and `Modifier.matchParentSize()` without
 * needing to nest an extra [Box].
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    intensity: GlassIntensity = GlassIntensity.Standard,
    showBorder: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(GadgetSpacing.Medium),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    // Respect LocalReducedTransparency: when the user has opted out
    // of translucent surfaces, swap any Standard/Vivid intensity to
    // Subtle (highest-opacity preset) so the glass surface stays
    // distinguishable but content beneath bleeds through less.
    val effectiveIntensity = if (LocalReducedTransparency.current && intensity != GlassIntensity.Subtle) {
        GlassIntensity.Subtle
    } else {
        intensity
    }
    val baseModifier = modifier.glassSurface(intensity = effectiveIntensity, showBorder = showBorder)
    val interactiveModifier = if (onClick != null) {
        baseModifier.clickable(onClick = onClick)
    } else {
        baseModifier
    }
    Box(
        modifier = interactiveModifier.padding(contentPadding),
        content = content,
    )
}
