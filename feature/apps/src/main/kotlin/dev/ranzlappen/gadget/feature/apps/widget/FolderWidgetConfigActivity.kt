package dev.ranzlappen.gadget.feature.apps.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.core.data.apps.Folder
import dev.ranzlappen.gadget.core.designsystem.GlassIntensity
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetTheme
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.CompactCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.ui.component.GlassSurface
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.core.widgetkit.provider.ContentWidgetUpdater
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.core.widgetkit.ui.ContentWidgetCustomizationSheet
import dev.ranzlappen.gadget.feature.apps.R
import dev.ranzlappen.gadget.feature.apps.icons.MaterialSymbol
import dev.ranzlappen.gadget.feature.apps.ui.AppsViewModel
import dev.ranzlappen.gadget.feature.apps.widget.customization.FolderWidgetIconCatalog
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `APPWIDGET_CONFIGURE` activity shown by the launcher after a folder widget is
 * dropped from the tray (and re-openable later via the launcher's reconfigure
 * affordance — the provider declares `widgetFeatures="reconfigurable"`).
 *
 * Hosts the kit's [ContentWidgetCustomizationSheet]: pick the folder + tune
 * background / accent tint / name-label / starting size, then write the
 * per-`appWidgetId` [FolderWidgetConfig] to the kit [WidgetConfigStore],
 * repaint, and finish `RESULT_OK`.
 *
 * Defaults to `RESULT_CANCELED` so a back-press / swipe-away discards a
 * half-placed widget (no orphan bound to no folder).
 */
@AndroidEntryPoint
class FolderWidgetConfigActivity : ComponentActivity() {

    @Inject lateinit var configStore: WidgetConfigStore<FolderWidgetConfig>
    @Inject lateinit var iconCatalog: FolderWidgetIconCatalog

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
            // Seed from an existing config when re-configuring a placed widget;
            // otherwise this is a fresh tray-drop with defaults.
            val existing = configStore.get(appWidgetId)
            setContent {
                GadgetTheme {
                    FolderWidgetConfigScreen(
                        existing = existing,
                        iconCatalog = iconCatalog,
                        onCancel = { finish() },
                        onConfirm = ::saveAndFinish,
                    )
                }
            }
        }
    }

    private fun saveAndFinish(config: FolderWidgetConfig) {
        lifecycleScope.launch {
            configStore.save(appWidgetId, config)
            ContentWidgetUpdater.requestUpdate(
                this@FolderWidgetConfigActivity,
                FolderWidgetProvider.PROVIDER_CLASS,
            )
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderWidgetConfigScreen(
    existing: FolderWidgetConfig?,
    iconCatalog: FolderWidgetIconCatalog,
    onCancel: () -> Unit,
    onConfirm: (FolderWidgetConfig) -> Unit,
) {
    val viewModel = hiltViewModel<AppsViewModel>()
    val folders by viewModel.folders.collectAsState()
    val scope = rememberCoroutineScope()

    var folderId by remember { mutableStateOf(existing?.folderId ?: FolderWidgetConfig.NO_FOLDER) }
    var name by remember { mutableStateOf(existing?.displayName.orEmpty()) }
    var appearance by remember { mutableStateOf(existing?.appearance ?: WidgetAppearance()) }
    var tintArgb by remember {
        mutableStateOf(
            existing?.coverTintArgb?.takeIf { it != FolderWidgetConfig.FOLLOW_FOLDER_COLOR },
        )
    }
    var showLabel by remember { mutableStateOf(existing?.showLabel ?: true) }
    var sizePreset by remember { mutableStateOf(existing?.sizePreset ?: WidgetSizePreset.Medium) }
    var iconKey by remember { mutableStateOf(existing?.iconKey) }

    val selectedFolder = folders.firstOrNull { it.id == folderId }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch { iconKey = iconCatalog.importCustomIcon(uri) }
        }
    }

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
        confirmEnabled = folderId != FolderWidgetConfig.NO_FOLDER,
        showTapAnimation = true,
        onDismiss = onCancel,
        onConfirm = {
            val folderName = selectedFolder?.name.orEmpty()
            onConfirm(
                FolderWidgetConfig(
                    folderId = folderId,
                    sizePreset = sizePreset,
                    showLabel = showLabel,
                    coverTintArgb = tintArgb ?: FolderWidgetConfig.FOLLOW_FOLDER_COLOR,
                    iconKey = iconKey,
                    displayName = name.ifBlank { folderName },
                    appearance = appearance,
                ),
            )
        },
        content = {
            FolderPicker(
                folders = folders,
                selectedId = folderId,
                onSelect = { folder ->
                    folderId = folder.id
                    if (name.isBlank()) name = folder.name
                },
            )
            WidgetIconPicker(
                catalog = iconCatalog,
                selectedKey = iconKey,
                onSelect = { iconKey = it },
                onClear = { iconKey = null },
                onImportCustom = {
                    importLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )
        },
        preview = { FolderWidgetPreview(folder = selectedFolder) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WidgetIconPicker(
    catalog: FolderWidgetIconCatalog,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    onImportCustom: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        Text(
            text = stringResource(R.string.apps_cover_icon),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
            verticalArrangement = Arrangement.spacedBy(spacing.tiny),
        ) {
            // "None" chip clears the widget-specific icon (falls back to folder cover).
            GadgetChip(
                selected = selectedKey == null,
                onClick = onClear,
                label = stringResource(R.string.apps_cover_none),
            )
            catalog.entries.forEach { entry ->
                GadgetChip(
                    selected = selectedKey == entry.key,
                    onClick = { onSelect(entry.key) },
                    label = entry.displayName,
                    leadingIcon = MaterialSymbol.fromId(entry.key)?.icon,
                )
            }
        }
        // Import a custom image from the gallery.
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            intensity = GlassIntensity.Subtle,
            onClick = onImportCustom,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                Text(
                    text = stringResource(R.string.apps_cover_pick_image),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                if (selectedKey != null && catalog.isCustom(selectedKey)) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.apps_cover_clear))
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderPicker(
    folders: List<Folder>,
    selectedId: Long,
    onSelect: (Folder) -> Unit,
) {
    if (folders.isEmpty()) {
        GadgetEmptyState(
            title = stringResource(R.string.apps_no_folders),
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        folders.forEach { folder ->
            CompactCard(
                title = folder.name,
                onClick = { onSelect(folder) },
                trailingContent = if (folder.id == selectedId) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(R.string.apps_widget_folder_selected),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun FolderWidgetPreview(folder: Folder?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = folder?.name ?: stringResource(R.string.apps_widget_pick_folder),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
