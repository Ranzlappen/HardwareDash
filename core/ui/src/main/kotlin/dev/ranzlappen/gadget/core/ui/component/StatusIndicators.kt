package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetSpacing
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Selectable filter chip with Gadget styling.
 *
 * Wraps Material 3 [FilterChip] with theme-derived colours and the
 * design-system's extraSmall shape token (6 dp radius — chips read as
 * "pill" without going fully rounded). Long labels truncate
 * single-line with [TextOverflow.Ellipsis] — chips don't grow
 * vertically.
 *
 * For multi-select rows use a [androidx.compose.foundation.layout.Row]
 * (or `FlowRow` if available) of [GadgetChip]s, each managing its own
 * [selected] state. The chip doesn't enforce single-vs-multi-select
 * semantics — that's the caller's responsibility.
 */
@Composable
fun GadgetChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
        },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(ChipLeadingIconSize),
                )
            }
        } else null,
        shape = MaterialTheme.shapes.extraSmall,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

/**
 * Compact badge — small numeric / text counter or dot indicator.
 *
 * If [text] is `null` the badge renders as a tiny circular dot (think
 * "unread" indicator on an icon). If [text] is non-null it renders
 * the string inside a pill-shaped surface. Pair with
 * [androidx.compose.material3.BadgedBox] to anchor the badge to
 * another composable (e.g. an icon button).
 *
 * Colours default to the M3 error tone (red-ish) for the conventional
 * "attention required" badge; override [containerColor] /
 * [contentColor] for neutral counters.
 *
 * Accessibility:
 * - The dot variant (`text = null`) is decorative-only. Anchor it to
 *   a labelled composable via [androidx.compose.material3.BadgedBox]
 *   so a screen reader announces the parent + "has notification".
 * - The text variant exposes the value via [stateDescription] so
 *   screen readers announce "3 unread" / "99 plus unread" in
 *   context. Override [stateDescriptionOverride] for a different
 *   announcement (e.g. "3 errors").
 */
@Composable
fun GadgetBadge(
    modifier: Modifier = Modifier,
    text: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.error,
    contentColor: Color = MaterialTheme.colorScheme.onError,
    stateDescriptionOverride: String? = null,
) {
    if (text == null) {
        Box(
            modifier = modifier
                .size(BadgeDotSize)
                .clip(CircleShape)
                .background(containerColor),
        )
    } else {
        val announcement = stateDescriptionOverride ?: "$text unread"
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(containerColor)
                .padding(BadgePillPadding)
                .semantics { stateDescription = announcement },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
        }
    }
}

/**
 * Colored status dot — semantic "online / offline / warning / error"
 * indicator. Pure presentation: caller is responsible for picking the
 * [color] semantic-meaning mapping.
 *
 * Pair with a label via a [androidx.compose.foundation.layout.Row]
 * for the conventional "● Online" affordance. For a dot inside a
 * larger element (icon, avatar) prefer [GadgetBadge] with
 * [androidx.compose.material3.BadgedBox].
 *
 * Accessibility:
 * - [contentDescription] is **required**. Pass a meaningful string
 *   ("Online", "Offline", "Warning — battery low") so screen readers
 *   announce the dot's semantic. Pass `null` only when a sibling
 *   labelled composable carries the accessibility label (e.g.
 *   `Row { GadgetStatusDot(null); Text("Online") }` — the Text node
 *   provides the label and the dot is decorative).
 */
@Composable
fun GadgetStatusDot(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = StatusDotDefaultSize,
) {
    val a11yModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }
    Box(
        modifier = a11yModifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

// ─── Internals ──────────────────────────────────────────────────────

/** Fixed-size design token: leading icon graphic inside a chip. */
private val ChipLeadingIconSize: Dp = 18.dp

/** Fixed-size design token: dot-only badge diameter. */
private val BadgeDotSize: Dp = 8.dp

/** Default status-dot diameter when no override is passed. */
private val StatusDotDefaultSize: Dp = 8.dp

/** Internal padding for pill-shaped (text-bearing) badges. */
private val BadgePillPadding: PaddingValues = PaddingValues(
    horizontal = GadgetSpacing.Tiny,
    vertical = GadgetSpacing.Pico,
)

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun StatusIndicatorsPreview() = GadgetThemedPreview {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            GadgetChip(selected = false, onClick = {}, label = "All")
            GadgetChip(selected = true, onClick = {}, label = "Active")
            GadgetChip(selected = false, onClick = {}, label = "Archived")
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            GadgetStatusDot(contentDescription = null, color = MaterialTheme.colorScheme.primary)
            Text(text = "Online", style = MaterialTheme.typography.bodyMedium)
            GadgetStatusDot(contentDescription = null, color = MaterialTheme.colorScheme.error)
            Text(text = "Offline", style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            GadgetBadge()
            GadgetBadge(text = "3")
            GadgetBadge(text = "99+")
        }
    }
}
