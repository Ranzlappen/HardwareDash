package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.GlassIntensity
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetSpacing
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Horizontal glassy card optimised for list rows.
 *
 * Layout (left → right): optional [leadingIcon] · [title] over
 * optional [subtitle] · optional [trailingContent]. The card uses
 * [GlassIntensity.Subtle] by default — list rows usually sit over
 * scrolling content and need more opacity for readability than hero
 * dashboard tiles.
 *
 * Long text in the title / subtitle truncates to a single line each
 * with [TextOverflow.Ellipsis]; pass `singleLineTitle = false` to
 * allow the title to wrap to two lines (subtitle stays single-line
 * regardless).
 *
 * For the dashboard-tile shape (vertical title + content + sparkline)
 * keep using [DashCard]. For a bare glass container with no chrome
 * use [GlassSurface].
 */
@Composable
fun CompactCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    intensity: GlassIntensity = GlassIntensity.Subtle,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = GadgetSpacing.Medium,
        vertical = GadgetSpacing.Small,
    ),
    singleLineTitle: Boolean = true,
) {
    GlassSurface(
        modifier = modifier,
        intensity = intensity,
        onClick = onClick,
        contentPadding = contentPadding,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GadgetSpacing.Small),
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(LeadingIconSize),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(GadgetSpacing.Pico),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (singleLineTitle) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = !singleLineTitle,
                    )
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(GadgetSpacing.Small))
                trailingContent()
            }
        }
    }
}

/** Fixed-size design token: leading icon graphic inside a CompactCard. */
private val LeadingIconSize: Dp = 24.dp

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@Composable
private fun CompactCardPreview() = GadgetThemedPreview {
    Column(verticalArrangement = Arrangement.spacedBy(GadgetSpacing.Small)) {
        CompactCard(
            title = "Battery",
            subtitle = "87% · charging · 1h 22m to full",
            leadingIcon = Icons.Outlined.BatteryFull,
        )
        CompactCard(
            title = "Wi-Fi",
            subtitle = "Connected · −56 dBm",
            leadingIcon = Icons.Outlined.BatteryFull,
            trailingContent = {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(LeadingIconSize),
                )
            },
            onClick = {},
        )
        CompactCard(
            title = "A title with significantly longer copy than usual to exercise the truncation",
            subtitle = "Long subtitle that also overflows so the layout doesn't grow vertically",
            leadingIcon = Icons.Outlined.BatteryFull,
        )
    }
}
