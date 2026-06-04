package dev.ranzlappen.gadget.feature.vibration.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.ranzlappen.gadget.core.designsystem.GlassIntensity
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetIconButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GlassSurface
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.core.widgetkit.ui.WidgetAppearancePreview
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.SavedVibrationWidget

/**
 * The in-app vibration widget list: a single "Add widget" button that opens the
 * generic customization sheet (where the user picks the function — one-shot or
 * saved pattern — plus size + appearance), plus edit / delete for each placed
 * widget. Mirror of torch's `WidgetsCard`.
 */
@Composable
internal fun WidgetsCard(
    widgets: List<SavedVibrationWidget>,
    onResolveIcon: (String) -> WidgetIconSource,
    onAddWidget: () -> Unit,
    onEditWidget: (SavedVibrationWidget) -> Unit,
    onDeleteWidget: (SavedVibrationWidget) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        title = stringResource(R.string.vibration_section_widgets_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            GadgetSecondaryButton(
                onClick = onAddWidget,
                text = stringResource(R.string.vibration_widget_add),
                leadingIcon = Icons.Outlined.Vibration,
                modifier = Modifier.fillMaxWidth(),
            )
            if (widgets.isEmpty()) {
                GadgetEmptyState(
                    title = stringResource(R.string.vibration_widget_list_empty_title),
                    subtitle = stringResource(R.string.vibration_widget_list_empty_subtitle),
                    icon = Icons.Outlined.Vibration,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                widgets.forEach { widget ->
                    WidgetListRow(
                        widget = widget,
                        onResolveIcon = onResolveIcon,
                        onEdit = { onEditWidget(widget) },
                        onDelete = { onDeleteWidget(widget) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetListRow(
    widget: SavedVibrationWidget,
    onResolveIcon: (String) -> WidgetIconSource,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        intensity = GlassIntensity.Subtle,
        contentPadding = PaddingValues(spacing.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            WidgetAppearancePreview(
                appearance = widget.config.appearance,
                icon = onResolveIcon(widget.config.appearance.iconStyle.activeKey),
                modifier = Modifier.semantics { contentDescription = widget.config.displayName },
            )
            Spacer(modifier = Modifier.weight(1f))
            GadgetIconButton(
                onClick = onEdit,
                icon = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.vibration_widget_list_action_edit),
            )
            GadgetIconButton(
                onClick = onDelete,
                icon = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.vibration_widget_list_action_delete),
            )
        }
    }
}
