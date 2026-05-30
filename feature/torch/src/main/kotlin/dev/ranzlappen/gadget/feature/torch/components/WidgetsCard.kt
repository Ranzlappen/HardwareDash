package dev.ranzlappen.gadget.feature.torch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.SavedTorchWidget
import dev.ranzlappen.gadget.feature.torch.ui.WidgetAppearancePreview
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource

@Composable
internal fun WidgetsCard(
    widgets: List<SavedTorchWidget>,
    onResolveIcon: (String) -> WidgetIconSource,
    onAddFlashlight: () -> Unit,
    onAddStrobe: () -> Unit,
    onQuickPinFlashlight: () -> Unit,
    onQuickPinStrobe: () -> Unit,
    onEditWidget: (SavedTorchWidget) -> Unit,
    onDeleteWidget: (SavedTorchWidget) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        title = stringResource(R.string.torch_section_widgets_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            // Each Add line pairs the full-width sheet-opening button with a
            // bolt icon button that pins the widget at default settings.
            AddWidgetRow(
                addText = stringResource(R.string.torch_widget_add_flashlight),
                addIcon = Icons.Outlined.FlashlightOn,
                onAdd = onAddFlashlight,
                onQuickPin = onQuickPinFlashlight,
                quickPinContentDescription = stringResource(R.string.torch_widget_quick_pin_flashlight),
            )
            AddWidgetRow(
                addText = stringResource(R.string.torch_widget_add_strobe),
                addIcon = Icons.Outlined.Bolt,
                onAdd = onAddStrobe,
                onQuickPin = onQuickPinStrobe,
                quickPinContentDescription = stringResource(R.string.torch_widget_quick_pin_strobe),
            )
            if (widgets.isEmpty()) {
                GadgetEmptyState(
                    title = stringResource(R.string.torch_widget_list_empty_title),
                    subtitle = stringResource(R.string.torch_widget_list_empty_subtitle),
                    icon = Icons.Outlined.FlashlightOn,
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

/**
 * A single "Add … widget" row: the sheet-opening
 * [GadgetSecondaryButton] on the left + a 48dp [GadgetIconButton]
 * on the right that pins the widget at default settings without
 * opening the configuration sheet (R4 #29 / P2-15).
 */
@Composable
private fun AddWidgetRow(
    addText: String,
    addIcon: ImageVector,
    onAdd: () -> Unit,
    onQuickPin: () -> Unit,
    quickPinContentDescription: String,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        GadgetSecondaryButton(
            onClick = onAdd,
            text = addText,
            leadingIcon = addIcon,
            modifier = Modifier.weight(1f),
        )
        GadgetIconButton(
            onClick = onQuickPin,
            icon = Icons.Outlined.PushPin,
            contentDescription = quickPinContentDescription,
        )
    }
}

/**
 * One row in the in-app widget list. Shows a live [WidgetAppearancePreview]
 * — the exact thumbnail the home-screen widget renders — followed by edit
 * and delete actions. There's deliberately no title/subtitle text: the
 * preview is the identifier, which removes the old (unhelpful) label
 * column entirely.
 */
@Composable
private fun WidgetListRow(
    widget: SavedTorchWidget,
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
                modifier = Modifier.semantics {
                    contentDescription = widget.config.displayName
                },
            )
            Spacer(modifier = Modifier.weight(1f))
            GadgetIconButton(
                onClick = onEdit,
                icon = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.torch_widget_list_action_edit),
            )
            GadgetIconButton(
                onClick = onDelete,
                icon = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.torch_widget_list_action_delete),
            )
        }
    }
}
