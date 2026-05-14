package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.GlassIntensity
import dev.ranzlappen.gadget.core.designsystem.a11y.LocalReducedMotion
import dev.ranzlappen.gadget.core.designsystem.glassSurface
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetSpacing
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Public-API defaults for Gadget button family.
 *
 * The four `Dp` constants below correspond to design-system fixed-size
 * tokens (Material 3 accessibility minimum touch target, canonical FAB
 * diameter, etc.) rather than magic numbers — they're allowed to
 * appear here per the design-system spec's "design token explicitly
 * requires a fixed-size variant" carve-out. Everything else in this
 * file pulls from [GadgetSpacing] / `LocalGadgetTheme.current.motion`
 * / [MaterialTheme].
 */
object GadgetButtonDefaults {
    /** Minimum touch-target height. Matches Material a11y guidance. */
    val MinHeight: Dp = 48.dp

    /** Square hit-target diameter for icon-only buttons. */
    val IconButtonSize: Dp = 48.dp

    /** Canonical FAB diameter (regular variant). */
    val FabSize: Dp = 56.dp

    /** Default icon size painted inside a labelled button. */
    val InternalIconSize: Dp = 20.dp

    /** Stroke width for the in-button [CircularProgressIndicator]. */
    val ProgressStrokeWidth: Dp = 2.dp

    /** Default content padding for filled/outlined/ghost labels. */
    val ContentPadding: PaddingValues = PaddingValues(
        horizontal = GadgetSpacing.Large,
        vertical = GadgetSpacing.Tiny,
    )

    /** Disabled-state alpha applied to container + content colours. */
    const val DisabledAlpha: Float = 0.38f
}

// ─── Public API ─────────────────────────────────────────────────────

/**
 * Primary filled button — high-emphasis call to action.
 *
 * Backed by [MaterialTheme.colorScheme.primary]; long labels are
 * truncated single-line with ellipsis unless [singleLine] is set to
 * false (opt-in two-line behaviour for unusually long copy).
 */
@Composable
fun GadgetPrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentPadding: PaddingValues = GadgetButtonDefaults.ContentPadding,
) {
    GlassyLabelledButton(
        onClick = onClick,
        text = text,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        singleLine = singleLine,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        contentPadding = contentPadding,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        border = null,
    )
}

/**
 * Secondary outlined glassy button — medium emphasis.
 *
 * Container is the M3 surfaceVariant tone with a 1 dp primary-coloured
 * border. Suitable for sibling actions to a [GadgetPrimaryButton].
 */
@Composable
fun GadgetSecondaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentPadding: PaddingValues = GadgetButtonDefaults.ContentPadding,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GlassyLabelledButton(
        onClick = onClick,
        text = text,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        singleLine = singleLine,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        contentPadding = contentPadding,
        // useGlass=true → the surface paint comes from
        // Modifier.glassSurface(intensity=Standard) inside the helper,
        // not a solid surfaceVariant fill. The hairline border still
        // paints in colorScheme.outline so the button keeps its
        // "outlined glassy" visual identity.
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(
            width = spacing.hairline,
            color = MaterialTheme.colorScheme.outline,
        ),
        useGlass = true,
    )
}

/**
 * Tertiary / ghost button — low emphasis, no container fill.
 *
 * Renders as text + optional icons only; the ripple still surfaces on
 * press. Use for inline actions inside cards / lists where a filled
 * surface would feel heavy.
 */
@Composable
fun GadgetTertiaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentPadding: PaddingValues = GadgetButtonDefaults.ContentPadding,
) {
    GlassyLabelledButton(
        onClick = onClick,
        text = text,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        singleLine = singleLine,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        contentPadding = contentPadding,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        border = null,
    )
}

/**
 * Icon-only square button — 48 dp hit target.
 *
 * Use for chrome / toolbar actions where a label would be redundant.
 *
 * Accessibility:
 * - [contentDescription] feeds the accessibility tree. Pass `null`
 *   **only** when a sibling element (a `Text(...)` next to the
 *   button in the same `Row`, an `IconButton` inside a `BadgedBox`,
 *   etc.) provides the accessible label. A `null`
 *   `contentDescription` on a standalone icon button means a screen
 *   reader announces nothing — the button becomes invisible to
 *   non-sighted users.
 */
@Composable
fun GadgetIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val motion = LocalGadgetTheme.current.motion
    val pressScale by animateFloatAsState(
        targetValue = if (!reducedMotion && isPressed && enabled) PressedScale else 1f,
        animationSpec = motion.springStandard(),
        label = "icon-button-press-scale",
    )
    Surface(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(
                minWidth = GadgetButtonDefaults.IconButtonSize,
                minHeight = GadgetButtonDefaults.IconButtonSize,
            )
            .scale(pressScale),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        contentColor = if (enabled) tint else tint.copy(alpha = GadgetButtonDefaults.DisabledAlpha),
        interactionSource = interactionSource,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(GadgetButtonDefaults.InternalIconSize),
            )
        }
    }
}

/**
 * Floating action button.
 *
 * If [text] is supplied the FAB renders as an extended FAB (icon +
 * label, wraps content width). Otherwise a 56 dp circular FAB with
 * just the icon. Both honour [enabled] and the standard press-scale
 * spring animation.
 */
@Composable
fun GadgetFab(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    text: String? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val theme = LocalGadgetTheme.current
    val motion = theme.motion
    val spacing = theme.spacing
    val pressScale by animateFloatAsState(
        targetValue = if (!reducedMotion && isPressed && enabled) PressedScale else 1f,
        animationSpec = motion.springStandard(),
        label = "fab-press-scale",
    )
    val resolvedContainer = if (enabled) containerColor
    else containerColor.copy(alpha = GadgetButtonDefaults.DisabledAlpha)
    val resolvedContent = if (enabled) contentColor
    else contentColor.copy(alpha = GadgetButtonDefaults.DisabledAlpha)

    Surface(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(
                minWidth = GadgetButtonDefaults.FabSize,
                minHeight = GadgetButtonDefaults.FabSize,
            )
            .scale(pressScale),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = resolvedContainer,
        contentColor = resolvedContent,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (text != null) spacing.medium else spacing.tiny,
                vertical = spacing.tiny,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(GadgetButtonDefaults.InternalIconSize),
            )
            if (text != null) {
                Spacer(modifier = Modifier.width(spacing.tiny))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
        }
    }
}

// ─── Private internals ──────────────────────────────────────────────

/** Scale factor applied while a button is in pressed state. */
private const val PressedScale: Float = 0.97f

@Composable
private fun GlassyLabelledButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier,
    enabled: Boolean,
    loading: Boolean,
    singleLine: Boolean,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    contentPadding: PaddingValues,
    containerColor: Color,
    contentColor: Color,
    border: BorderStroke?,
    useGlass: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val theme = LocalGadgetTheme.current
    val motion = theme.motion
    val spacing = theme.spacing
    val pressScale by animateFloatAsState(
        targetValue = if (!reducedMotion && isPressed && enabled && !loading) PressedScale else 1f,
        animationSpec = motion.springStandard(),
        label = "labelled-button-press-scale",
    )
    val resolvedContainer = if (enabled) containerColor
    else containerColor.copy(alpha = GadgetButtonDefaults.DisabledAlpha)
    val resolvedContent = if (enabled) contentColor
    else contentColor.copy(alpha = GadgetButtonDefaults.DisabledAlpha)

    // When useGlass is true the surface paint comes from
    // Modifier.glassSurface — the M3 Surface's `color` slot stays
    // transparent so the glass overlay shows through. The glass
    // modifier reads its alphas from LocalGadgetTheme.current.glass,
    // so a custom theme that retunes glassmorphism flows through
    // automatically. When false, the Surface paints `containerColor`
    // as a solid fill — the existing primary / tertiary behaviour.
    val surfaceModifier = modifier
        .defaultMinSize(minHeight = GadgetButtonDefaults.MinHeight)
        .scale(pressScale)
        .let { base ->
            if (useGlass) {
                base.glassSurface(
                    intensity = GlassIntensity.Standard,
                    showBorder = false,
                )
            } else {
                base
            }
        }
    val surfaceColor = if (useGlass) Color.Transparent else resolvedContainer

    Surface(
        onClick = onClick,
        modifier = surfaceModifier,
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.small,
        color = surfaceColor,
        contentColor = resolvedContent,
        border = border,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(GadgetButtonDefaults.InternalIconSize),
                    color = resolvedContent,
                    strokeWidth = GadgetButtonDefaults.ProgressStrokeWidth,
                )
            } else {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(GadgetButtonDefaults.InternalIconSize),
                    )
                    Spacer(modifier = Modifier.width(spacing.tiny))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = if (singleLine) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = !singleLine,
                    modifier = Modifier.wrapContentWidth(),
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(spacing.tiny))
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(GadgetButtonDefaults.InternalIconSize),
                    )
                }
            }
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@Composable
private fun GadgetButtonsPreview() = GadgetThemedPreview {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        GadgetPrimaryButton(onClick = {}, text = "Primary action")
        GadgetSecondaryButton(onClick = {}, text = "Secondary")
        GadgetTertiaryButton(onClick = {}, text = "Tertiary / Ghost")
        GadgetPrimaryButton(
            onClick = {},
            text = "Disabled",
            enabled = false,
        )
        GadgetPrimaryButton(
            onClick = {},
            text = "Loading",
            loading = true,
        )
        GadgetPrimaryButton(
            onClick = {},
            text = "A label that runs longer than usual and exercises the singleLine ellipsis fallback",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            GadgetIconButton(
                onClick = {},
                icon = Icons.Outlined.Search,
                contentDescription = "Search",
            )
            GadgetFab(
                onClick = {},
                icon = Icons.Outlined.Add,
                contentDescription = "Add sensor",
                text = "Add sensor",
            )
        }
    }
}
