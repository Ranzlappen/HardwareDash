package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.GlassIntensity
import dev.ranzlappen.gadget.core.designsystem.a11y.LocalReducedMotion
import dev.ranzlappen.gadget.core.designsystem.glassSurface
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetSpacing
import dev.ranzlappen.gadget.core.ui.R
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewSizeClasses
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/** Fixed-size design tokens for the expandable card chrome. */
private object GadgetExpandableCardDefaults {
    /** Header icon / chevron size — matches [DashCard]'s 20 dp header icon. */
    val IconSize = 20.dp
    /** Minimum header tap-target height (Material accessibility minimum). */
    val HeaderMinHeight = 48.dp
}

/**
 * A collapsible sibling of [DashCard]: the same glass surface and
 * icon-plus-title header, but the header is a toggle that shows/hides the
 * [content] body, with a chevron that rotates to reflect state.
 *
 * **Stateless** — `expanded` is hoisted via [onExpandedChange]. Pair it with
 * a persisted store (see `:core:monitoring`'s `CollapseStateRepository`) when
 * the collapsed state must survive process death; for ephemeral state a
 * `rememberSaveable { mutableStateOf(true) }` is enough.
 *
 * Accessibility: the header is a single `toggleable` node (role Button) whose
 * `stateDescription` announces "Expanded"/"Collapsed"; the chevron is
 * decorative. Honors [LocalReducedMotion] — the chevron rotation and the body
 * enter/exit are pinned (instant) when reduced motion is requested.
 */
@Composable
fun GadgetExpandableCard(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    intensity: GlassIntensity = GlassIntensity.Standard,
    contentPadding: PaddingValues = PaddingValues(GadgetSpacing.Medium),
    content: @Composable () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val reducedMotion = LocalReducedMotion.current
    val stateDesc = stringResource(
        if (expanded) R.string.expandable_card_state_expanded
        else R.string.expandable_card_state_collapsed,
    )
    val chevronTarget = if (expanded) CHEVRON_EXPANDED_DEG else CHEVRON_COLLAPSED_DEG
    val chevronRotation = if (reducedMotion) {
        chevronTarget
    } else {
        animateFloatAsState(targetValue = chevronTarget, label = "chevronRotation").value
    }

    Box(modifier = modifier.glassSurface(intensity = intensity)) {
        Column(modifier = Modifier.padding(contentPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = GadgetExpandableCardDefaults.HeaderMinHeight)
                    .toggleable(
                        value = expanded,
                        role = Role.Button,
                        onValueChange = onExpandedChange,
                    )
                    .semantics { stateDescription = stateDesc },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(GadgetExpandableCardDefaults.IconSize),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(spacing.tiny))
                }
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(GadgetExpandableCardDefaults.IconSize)
                        .rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val body: @Composable () -> Unit = {
                Column {
                    Spacer(modifier = Modifier.height(spacing.small))
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        content()
                    }
                }
            }
            if (reducedMotion) {
                if (expanded) body()
            } else {
                AnimatedVisibility(visible = expanded) { body() }
            }
        }
    }
}

private const val CHEVRON_EXPANDED_DEG = 180f
private const val CHEVRON_COLLAPSED_DEG = 0f

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@GadgetPreviewSizeClasses
@Composable
private fun GadgetExpandableCardPreview() = GadgetThemedPreview {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        GadgetExpandableCard(
            title = "Expanded section",
            expanded = true,
            onExpandedChange = {},
            icon = Icons.Filled.KeyboardArrowDown,
        ) {
            Text("Body content shown while expanded.")
        }
        GadgetExpandableCard(
            title = "Collapsed section",
            expanded = false,
            onExpandedChange = {},
        ) {
            Text("Hidden until expanded.")
        }
    }
}
