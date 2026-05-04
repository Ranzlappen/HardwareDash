package com.gadget.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gadget.data.db.apps.Folder
import com.gadget.localization.S
import com.gadget.ui.apps.AppsViewModel

/**
 * Top-level screen for the App-Organizer module: lists user-defined folders
 * and exposes a FAB for creating new ones. Tapping a folder is a no-op for
 * this micro-batch — the editor route lands in 6c.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    onFolderClick: (Long) -> Unit = {},
) {
    val viewModel = hiltViewModel<AppsViewModel>()
    val folders by viewModel.folders.collectAsState()
    val apps = S.apps
    val common = S.common

    var showNewDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Folder?>(null) }

    val defaultFolderColor = MaterialTheme.colorScheme.primary.toArgb()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = apps.createFolder)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = apps.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = apps.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            if (folders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = apps.noFolders,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(folders, key = { it.id }) { folder ->
                        FolderListRow(
                            folder = folder,
                            onClick = { onFolderClick(folder.id) },
                            onDeleteClick = { pendingDelete = folder },
                        )
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        NewFolderDialog(
            onDismiss = { showNewDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(name, defaultFolderColor)
                showNewDialog = false
            },
        )
    }

    pendingDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(apps.deleteFolderConfirm) },
            text = { Text(folder.name) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folder.id)
                    pendingDelete = null
                }) { Text(common.delete) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(common.cancel) }
            },
        )
    }
}

@Composable
private fun FolderListRow(
    folder: Folder,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                color = Color(folder.baseColorArgb),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Text(
                text = folder.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = S.common.delete,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val apps = S.apps
    val common = S.common

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(apps.newFolder) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(apps.folderName) },
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name) },
            ) { Text(common.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(common.cancel) }
        },
    )
}
