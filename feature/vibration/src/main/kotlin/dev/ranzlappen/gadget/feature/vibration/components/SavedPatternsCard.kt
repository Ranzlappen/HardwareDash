package dev.ranzlappen.gadget.feature.vibration.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.CompactCard
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetIconButton
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.VibrationPattern

/**
 * Lists the user's saved [VibrationPattern]s with play + delete actions. Each
 * row is a [CompactCard] (name + duration subtitle). Mirror of the torch
 * widget-list style for list rows.
 */
@Composable
internal fun SavedPatternsCard(
    patterns: List<VibrationPattern>,
    onPlay: (VibrationPattern) -> Unit,
    onDelete: (VibrationPattern) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        title = stringResource(R.string.vibration_patterns_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth(),
        icon = Icons.Outlined.SaveAlt,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            if (patterns.isEmpty()) {
                GadgetEmptyState(
                    title = stringResource(R.string.vibration_patterns_empty_title),
                    subtitle = stringResource(R.string.vibration_patterns_empty_subtitle),
                    icon = Icons.Outlined.SaveAlt,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                patterns.forEach { pattern ->
                    CompactCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = pattern.name,
                        subtitle = stringResource(R.string.vibration_patterns_duration, pattern.totalMillis),
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GadgetIconButton(
                                    onClick = { onPlay(pattern) },
                                    icon = Icons.Outlined.PlayArrow,
                                    contentDescription = stringResource(R.string.vibration_patterns_play),
                                )
                                GadgetIconButton(
                                    onClick = { onDelete(pattern) },
                                    icon = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.vibration_patterns_delete),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun SavedPatternsCardPreview() = GadgetThemedPreview {
    SavedPatternsCard(
        patterns = listOf(
            VibrationPattern("a", "Heartbeat", listOf(0L, 100L, 100L, 100L), listOf(0, 200, 0, 255)),
            VibrationPattern("b", "Ramp", listOf(0L, 500L), listOf(0, 180)),
        ),
        onPlay = {},
        onDelete = {},
        expanded = true,
        onExpandedChange = {},
    )
}
