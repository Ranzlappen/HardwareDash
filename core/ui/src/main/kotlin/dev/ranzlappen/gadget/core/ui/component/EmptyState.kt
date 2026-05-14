package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetSpacing
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewSizeClasses
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Centered "nothing to show" placeholder — the conventional empty
 * list / no results / first-run-haven't-set-this-up-yet composable.
 *
 * Layout (top → bottom): optional hero [icon] · [title] · optional
 * [subtitle] · optional [action] slot. All slots after [title] are
 * conditional — passing `null` omits them from the column entirely
 * (no empty spacer left behind by `Arrangement.spacedBy`).
 *
 * [title] truncates to 2 lines with [TextOverflow.Ellipsis]; the
 * subtitle wraps freely up to 4 lines then truncates. The [action]
 * slot accepts any composable — typically a [GadgetPrimaryButton]
 * for a primary CTA ("Add your first sensor") or a
 * [GadgetTertiaryButton] for a lower-emphasis call ("Learn more").
 *
 * The composable centres itself in the parent. Wrap in a
 * [Modifier.fillMaxSize] caller if you want it to vertically centre
 * within a full screen; otherwise it sizes to its content.
 */
@Composable
fun GadgetEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(GadgetSpacing.Large),
) {
    val spacing = LocalGadgetTheme.current.spacing
    Box(
        modifier = modifier.padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // A11y: merge the title + optional subtitle + optional
                // action into one a11y node so the empty state is
                // announced as a single coherent unit rather than
                // three separate stops.
                .semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(HeroIconSize),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = SubtitleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (action != null) {
                Spacer(modifier = Modifier.height(spacing.small))
                action()
            }
        }
    }
}

// ─── Internals ──────────────────────────────────────────────────────

/** Fixed-size design token: hero icon at the top of the empty state. */
private val HeroIconSize: Dp = 64.dp

/** Maximum subtitle lines before truncation kicks in. */
private const val SubtitleMaxLines: Int = 4

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@GadgetPreviewSizeClasses
@Composable
private fun EmptyStatePreview() = GadgetThemedPreview {
    GadgetEmptyState(
        title = "No sensors yet",
        subtitle = "Add your first sensor to start tracking live readouts on the dashboard.",
        icon = Icons.Outlined.Sensors,
        action = {
            GadgetPrimaryButton(onClick = {}, text = "Add sensor")
        },
    )
}
