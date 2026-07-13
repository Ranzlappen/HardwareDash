package dev.ranzlappen.gadget.feature.metricwidget.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetTheme
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.core.widgetkit.provider.ContentWidgetUpdater
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.core.widgetkit.ui.ContentWidgetCustomizationSheet
import dev.ranzlappen.gadget.feature.metricwidget.MetricWidgetConfig
import dev.ranzlappen.gadget.feature.metricwidget.MetricWidgetDisplay
import dev.ranzlappen.gadget.feature.metricwidget.R
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `APPWIDGET_CONFIGURE` activity shown by the launcher after a metric widget is
 * dropped from the tray (and re-openable via the launcher's reconfigure
 * affordance — the provider declares `widgetFeatures="reconfigurable"`).
 *
 * Hosts the kit's [ContentWidgetCustomizationSheet]: pick the metric from the
 * app-wide `MetricSource` registry + a display mode + background / tint / label
 * / size, then write the per-`appWidgetId` [MetricWidgetConfig] to the kit
 * [WidgetConfigStore], repaint, and finish `RESULT_OK`. Defaults to
 * `RESULT_CANCELED` so a back-press discards a half-placed (unbound) widget.
 */
@AndroidEntryPoint
class MetricWidgetConfigActivity : ComponentActivity() {

    @Inject lateinit var configStore: WidgetConfigStore<MetricWidgetConfig>

    @Inject lateinit var metricSources: Map<String, @JvmSuppressWildcards MetricSource>

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        lifecycleScope.launch {
            val existing = configStore.get(appWidgetId)
            setContent {
                GadgetTheme {
                    MetricWidgetConfigScreen(
                        existing = existing,
                        metrics = metricSources.toMetricOptions(),
                        onCancel = { finish() },
                        onConfirm = ::saveAndFinish,
                    )
                }
            }
        }
    }

    private fun saveAndFinish(config: MetricWidgetConfig) {
        lifecycleScope.launch {
            configStore.save(appWidgetId, config)
            ContentWidgetUpdater.requestUpdate(
                this@MetricWidgetConfigActivity,
                MetricWidgetProvider.PROVIDER_CLASS,
            )
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}

/** One selectable metric in the picker, pre-flattened from the source map. */
internal data class MetricOption(
    val metricKey: String,
    val displayName: String,
    val unit: String,
    val category: String,
)

/** Flatten + stably sort the injected source map into picker options. */
internal fun Map<String, @JvmSuppressWildcards MetricSource>.toMetricOptions(): List<MetricOption> =
    map { (key, source) ->
        MetricOption(
            metricKey = key,
            displayName = source.descriptor.displayName,
            unit = source.descriptor.unit,
            category = source.descriptor.category.name,
        )
    }.sortedWith(compareBy({ it.category }, { it.displayName }))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetricWidgetConfigScreen(
    existing: MetricWidgetConfig?,
    metrics: List<MetricOption>,
    onCancel: () -> Unit,
    onConfirm: (MetricWidgetConfig) -> Unit,
) {
    var metricKey by remember { mutableStateOf(existing?.metricKey ?: MetricWidgetConfig.NO_METRIC) }
    var display by remember { mutableStateOf(existing?.display ?: MetricWidgetDisplay.ValueAndBar) }
    var name by remember { mutableStateOf(existing?.displayName.orEmpty()) }
    var appearance by remember { mutableStateOf(existing?.appearance ?: WidgetAppearance()) }
    var tintArgb by remember { mutableStateOf(existing?.tintArgb) }
    var showLabel by remember { mutableStateOf(existing?.showLabel ?: true) }
    var sizePreset by remember { mutableStateOf(existing?.sizePreset ?: WidgetSizePreset.Medium) }

    val selected = metrics.firstOrNull { it.metricKey == metricKey }

    ContentWidgetCustomizationSheet(
        name = name,
        onNameChange = { name = it },
        appearance = appearance,
        onAppearanceChange = { appearance = it },
        tintArgb = tintArgb,
        onTintChange = { tintArgb = it },
        showLabel = showLabel,
        onShowLabelChange = { showLabel = it },
        sizePreset = sizePreset,
        onSizePresetChange = { sizePreset = it },
        isExisting = existing != null,
        confirmEnabled = metricKey != MetricWidgetConfig.NO_METRIC,
        onDismiss = onCancel,
        onConfirm = {
            onConfirm(
                MetricWidgetConfig(
                    metricKey = metricKey,
                    display = display,
                    showLabel = showLabel,
                    tintArgb = tintArgb,
                    sizePreset = sizePreset,
                    displayName = name.ifBlank { selected?.displayName.orEmpty() },
                    appearance = appearance,
                ),
            )
        },
        content = {
            MetricPicker(
                metrics = metrics,
                selectedKey = metricKey,
                onSelect = { option ->
                    metricKey = option.metricKey
                    if (name.isBlank()) name = option.displayName
                },
            )
            DisplayModePicker(display = display, onSelect = { display = it })
        },
        preview = { MetricWidgetPreview(option = selected) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetricPicker(
    metrics: List<MetricOption>,
    selectedKey: String,
    onSelect: (MetricOption) -> Unit,
) {
    if (metrics.isEmpty()) {
        GadgetEmptyState(
            title = stringResource(R.string.metric_widget_no_metrics),
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    val spacing = LocalGadgetTheme.current.spacing
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Text(
            text = stringResource(R.string.metric_widget_pick_metric),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        metrics.groupBy { it.category }.forEach { (category, options) ->
            Text(
                text = category,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
                verticalArrangement = Arrangement.spacedBy(spacing.tiny),
            ) {
                options.forEach { option ->
                    GadgetChip(
                        selected = option.metricKey == selectedKey,
                        onClick = { onSelect(option) },
                        label = if (option.unit.isBlank()) {
                            option.displayName
                        } else {
                            "${option.displayName} (${option.unit})"
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DisplayModePicker(
    display: MetricWidgetDisplay,
    onSelect: (MetricWidgetDisplay) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        Text(
            text = stringResource(R.string.metric_widget_display_mode),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
            GadgetChip(
                selected = display == MetricWidgetDisplay.Value,
                onClick = { onSelect(MetricWidgetDisplay.Value) },
                label = stringResource(R.string.metric_widget_display_value),
            )
            GadgetChip(
                selected = display == MetricWidgetDisplay.ValueAndBar,
                onClick = { onSelect(MetricWidgetDisplay.ValueAndBar) },
                label = stringResource(R.string.metric_widget_display_value_bar),
            )
        }
    }
}

@Composable
private fun MetricWidgetPreview(option: MetricOption?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = option?.displayName ?: stringResource(R.string.metric_widget_pick_metric),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
