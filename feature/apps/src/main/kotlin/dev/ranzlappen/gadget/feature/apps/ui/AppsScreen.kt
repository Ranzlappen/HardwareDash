@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package dev.ranzlappen.gadget.feature.apps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.data.apps.Folder
import dev.ranzlappen.gadget.core.designsystem.GlassIntensity
import dev.ranzlappen.gadget.core.ui.component.GadgetDialog
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.ui.component.GadgetFab
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.ui.component.GlassSurface
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.apps.R
import kotlinx.coroutines.launch

/**
 * Top-level App-Organizer screen (Hilt route): a grid of folders. Tap opens the
 * folder editor; long-press deletes (with confirm). The FAB creates a folder.
 * Each folder can be exposed as a home-screen widget from its editor.
 *
 * Thin wrapper over the Hilt-free [AppsScreenContent] (instrumented-tested).
 */
@Composable
fun AppsScreen(onOpenFolder: (Long) -> Unit) {
    val viewModel = hiltViewModel<AppsViewModel>()
    val folders by viewModel.folders.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val importLegacyLabel = stringResource(R.string.apps_import_legacy)
    val importSuccessTemplate = stringResource(R.string.apps_import_success)
    val importNothingMsg = stringResource(R.string.apps_import_nothing)
    val importErrorMsg = stringResource(R.string.apps_import_error)
    AppsScreenContent(
        folders = folders,
        onOpenFolder = onOpenFolder,
        onCreateFolder = { name -> viewModel.createFolder(name, DEFAULT_FOLDER_COLOR) },
        onDeleteFolder = viewModel::deleteFolder,
        snackbarHostState = snackbarHostState,
        showLegacyImport = viewModel.legacyDbExists,
        onImportLegacy = {
            scope.launch {
                val msg = runCatching { viewModel.importLegacy() }
                    .fold(
                        onSuccess = { result ->
                            if (result.isEmpty) importNothingMsg
                            else importSuccessTemplate.format(result.folderCount, result.appCount)
                        },
                        onFailure = { importErrorMsg },
                    )
                snackbarHostState.showSnackbar(msg)
            }
        },
    )
}

/** Stateless folder-grid content. Holds only ephemeral dialog UI state; no
 *  ViewModel/Hilt, so it's preview- and instrumented-test-friendly. */
@Composable
fun AppsScreenContent(
    folders: List<Folder>,
    onOpenFolder: (Long) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    showLegacyImport: Boolean = false,
    onImportLegacy: () -> Unit = {},
) {
    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Folder?>(null) }
    var showOverflow by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.apps_title)) },
                actions = {
                    if (showLegacyImport) {
                        Box {
                            IconButton(onClick = { showOverflow = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = null,
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflow,
                                onDismissRequest = { showOverflow = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.apps_import_legacy)) },
                                    onClick = {
                                        showOverflow = false
                                        onImportLegacy()
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            GadgetFab(
                onClick = { showCreate = true },
                icon = Icons.Filled.Add,
                contentDescription = stringResource(R.string.apps_create_folder),
            )
        },
    ) { padding ->
        if (folders.isEmpty()) {
            GadgetEmptyState(
                title = stringResource(R.string.apps_no_folders),
                subtitle = stringResource(R.string.apps_description),
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(folders, key = { it.id }) { folder ->
                    FolderTile(
                        folder = folder,
                        onClick = { onOpenFolder(folder.id) },
                        onLongClick = { pendingDelete = folder },
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateFolderDialog(
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                onCreateFolder(name)
                showCreate = false
            },
        )
    }

    pendingDelete?.let { folder ->
        GadgetDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(R.string.apps_delete_folder_confirm),
            text = folder.name,
            confirmButton = {
                TextButton(onClick = {
                    onDeleteFolder(folder.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.apps_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.apps_cancel))
                }
            },
        )
    }
}

@Composable
private fun FolderTile(folder: Folder, onClick: () -> Unit, onLongClick: () -> Unit) {
    GlassSurface(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        intensity = GlassIntensity.Standard,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(folder.baseColorArgb)),
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CreateFolderDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    // M3 AlertDialog (themed) rather than GadgetDialog because the kit dialog
    // takes a String body, not an input slot.
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.apps_new_folder)) },
        text = {
            GadgetTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.apps_folder_name),
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name) }) {
                Text(stringResource(R.string.apps_create_folder))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.apps_cancel)) }
        },
    )
}

private const val DEFAULT_FOLDER_COLOR: Int = 0xFF6750A4.toInt()

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun FolderTilePreview() = GadgetThemedPreview {
    FolderTile(
        folder = Folder(
            id = 1L,
            name = "Games",
            baseColorArgb = DEFAULT_FOLDER_COLOR,
            coverIcon = "auto",
            sortOrder = 0,
            locked = false,
            createdAt = 0L,
        ),
        onClick = {},
        onLongClick = {},
    )
}
