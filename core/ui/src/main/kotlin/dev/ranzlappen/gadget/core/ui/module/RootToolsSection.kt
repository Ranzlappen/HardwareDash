package dev.ranzlappen.gadget.core.ui.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusDot
import dev.ranzlappen.gadget.core.ui.component.color
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * The reusable **rooted-tools section** every feature screen drops in to
 * surface its rooted controller's interactive UI behind the root gate — the
 * standardized substrate for the #94 / W6 epic ("re-surface rooted UX
 * natively in every feature screen"). Torch/vibration hand-rolled their own
 * `RootToolsCard`; this hoists the shared shape into `:core:ui` so the
 * dormant features (battery, storage, gps, audio, camera, diagnostics,
 * radios-*, lock, bugreport, keepalive) fill in [content] rather than
 * re-inventing the chrome.
 *
 * The section renders inside a collapsible [GadgetExpandableCard]. When
 * [available] (the feature's `RootReady` snapshot — resolved through the
 * `:core:root` Hilt seam, **never** `BuildConfig.IS_ROOTED`) is false, it
 * shows [unavailableMessage] instead of the controls, so a standard build
 * or an un-rooted device sees an honest "requires the rooted app" state.
 *
 * All copy is passed in already-resolved (the [ModuleInfo] convention), so
 * feature strings stay in each feature's resources.
 */
@Composable
fun RootToolsSection(
    title: String,
    available: Boolean,
    unavailableMessage: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        title = title,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth(),
        icon = Icons.Outlined.Bolt,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            if (available) {
                content()
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    GadgetStatusDot(
                        contentDescription = null,
                        color = GadgetStatusKind.Warning.color(),
                    )
                    Text(
                        text = unavailableMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * One rooted action row inside a [RootToolsSection]: a labeled control with
 * an optional [description] and an optional [statusMessage] (the last
 * `*ControllerResult` mapped to a human string, tinted by [statusKind]) plus
 * a run button. Reusable across every dormant feature's read-only rooted
 * actions (diskstats dump, NMEA tap, mount enumeration, …).
 */
@Composable
fun RootActionRow(
    label: String,
    runLabel: String,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    statusMessage: String? = null,
    statusKind: GadgetStatusKind = GadgetStatusKind.Success,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            GadgetSecondaryButton(onClick = onRun, text = runLabel, enabled = enabled)
        }
        if (statusMessage != null) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = statusKind.color(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@GadgetPreviewLightDark
@GadgetPreviewRtl
@Composable
private fun RootToolsSectionAvailablePreview() = GadgetThemedPreview {
    RootToolsSection(
        title = "Rooted tools",
        available = true,
        unavailableMessage = "Requires the rooted app version.",
        expanded = true,
        onExpandedChange = {},
    ) {
        RootActionRow(
            label = "Dump diskstats",
            description = "Kernel I/O statistics per block device.",
            runLabel = "Run",
            onRun = {},
            statusMessage = "Captured 12 devices.",
            statusKind = GadgetStatusKind.Success,
        )
        RootActionRow(
            label = "Enumerate mounts",
            description = "Full mount table via /proc/mountinfo.",
            runLabel = "Run",
            onRun = {},
        )
    }
}

@GadgetPreviewLightDark
@Composable
private fun RootToolsSectionUnavailablePreview() = GadgetThemedPreview {
    RootToolsSection(
        title = "Rooted tools",
        available = false,
        unavailableMessage = "Requires the rooted app version.",
        expanded = true,
        onExpandedChange = {},
    ) {}
}
