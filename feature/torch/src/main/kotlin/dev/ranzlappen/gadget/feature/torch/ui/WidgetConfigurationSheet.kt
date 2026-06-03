package dev.ranzlappen.gadget.feature.torch.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconChoice
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunction
import dev.ranzlappen.gadget.core.widgetkit.ui.WidgetCustomizationResult
import dev.ranzlappen.gadget.core.widgetkit.ui.WidgetCustomizationSheet
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig

/**
 * Thin torch shell over the kit-generic [WidgetCustomizationSheet].
 *
 * Now that a widget binds to a [WidgetFunction] (picked from torch's
 * [dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetFunctionCatalog]) and
 * carries its params generically, the entire dialog — name, function picker,
 * per-function param editors, size, appearance / tap / feedback, and the live
 * preview — lives in the kit. This wrapper only maps the torch
 * [TorchWidgetConfig] the sheet opened with into the kit's `initial*` params
 * and the [WidgetCustomizationResult] back out via [onConfirm]. The old
 * torch-specific strobe rate / Morse fields are gone — they're now generic
 * `params` rendered automatically from each function's `ActionParam` schema.
 */
@Composable
fun WidgetConfigurationSheet(
    initial: TorchWidgetConfig,
    isExisting: Boolean,
    functions: List<WidgetFunction>,
    onDismiss: () -> Unit,
    onConfirm: (WidgetCustomizationResult) -> Unit,
    resolveIcon: (String) -> WidgetIconSource,
    onImportCustomIcon: suspend (Uri) -> String?,
    iconChoices: List<WidgetIconChoice>,
    modifier: Modifier = Modifier,
) {
    WidgetCustomizationSheet(
        initialName = initial.displayName,
        initialActionKey = initial.actionKey,
        initialParams = initial.params,
        initialSizePreset = initial.sizePreset,
        initialAppearance = initial.appearance,
        functions = functions,
        isExisting = isExisting,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        iconChoices = iconChoices,
        resolveIcon = resolveIcon,
        onImportCustomIcon = onImportCustomIcon,
        modifier = modifier,
    )
}
