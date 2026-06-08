package com.gadget.backup.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gadget.R
import com.gadget.backup.BackupManagerEntryPoint
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetCircularProgress
import dev.ranzlappen.gadget.core.ui.component.GadgetDialog
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings "Backup & restore" card. Exports the whole-app backup ZIP to a
 * user-chosen document and restores from one (including **legacy** backups
 * produced by the monolithic app — `BackupManager` lifts their `gadget_db`
 * App-Organizer rows into `apps.db` on the next launch).
 *
 * Lives in `:app` because [com.gadget.backup.BackupManager] depends on the
 * legacy `GadgetDatabase`; it's dropped into the modular Settings screen via the
 * `backupSection` slot (the leaf-module-can't-see-`:app` seam). Reaches the
 * manager through [BackupManagerEntryPoint].
 *
 * Restore replaces all current data and applies the modular DBs only on the
 * next process start, so a successful restore prompts a restart.
 */
@Composable
fun BackupCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember(context) { BackupManagerEntryPoint.get(context) }

    var status by remember { mutableStateOf<BackupStatus>(BackupStatus.Idle) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestartDialog by remember { mutableStateOf(false) }
    val working = status == BackupStatus.Working

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME),
    ) { uri ->
        if (uri != null) {
            status = BackupStatus.Working
            scope.launch {
                status = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { backupManager.createBackup(it) }
                        ?: error("output stream unavailable")
                }.fold(
                    onSuccess = { BackupStatus.Exported },
                    onFailure = { BackupStatus.Error(it.message ?: "export failed") },
                )
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingRestoreUri = uri }

    DashCard(title = stringResource(R.string.backup_title), modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.backup_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GadgetPrimaryButton(
                    onClick = { exportLauncher.launch(defaultBackupFileName()) },
                    text = stringResource(R.string.backup_export),
                    enabled = !working,
                    modifier = Modifier.weight(1f),
                )
                GadgetSecondaryButton(
                    onClick = { importLauncher.launch(BACKUP_OPEN_MIME_TYPES) },
                    text = stringResource(R.string.backup_restore),
                    enabled = !working,
                    modifier = Modifier.weight(1f),
                )
            }
            when (val s = status) {
                BackupStatus.Working -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GadgetCircularProgress(modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.backup_working),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                BackupStatus.Exported -> StatusLine(
                    text = stringResource(R.string.backup_exported),
                    color = MaterialTheme.colorScheme.primary,
                )
                is BackupStatus.Error -> StatusLine(
                    text = stringResource(R.string.backup_error, s.message),
                    color = MaterialTheme.colorScheme.error,
                )
                BackupStatus.Idle -> Unit
            }
        }
    }

    pendingRestoreUri?.let { uri ->
        GadgetDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = stringResource(R.string.backup_restore_confirm_title),
            text = stringResource(R.string.backup_restore_confirm_body),
            confirmButton = {
                GadgetPrimaryButton(
                    onClick = {
                        pendingRestoreUri = null
                        status = BackupStatus.Working
                        scope.launch {
                            val result = runCatching {
                                context.contentResolver.openInputStream(uri)?.use {
                                    backupManager.restoreBackup(it)
                                } ?: error("input stream unavailable")
                            }
                            result.fold(
                                onSuccess = { status = BackupStatus.Idle; showRestartDialog = true },
                                onFailure = { status = BackupStatus.Error(it.message ?: "restore failed") },
                            )
                        }
                    },
                    text = stringResource(R.string.backup_restore),
                )
            },
            dismissButton = {
                GadgetTertiaryButton(
                    onClick = { pendingRestoreUri = null },
                    text = stringResource(R.string.backup_cancel),
                )
            },
        )
    }

    if (showRestartDialog) {
        GadgetDialog(
            onDismissRequest = { showRestartDialog = false },
            title = stringResource(R.string.backup_restart_title),
            text = stringResource(R.string.backup_restart_body),
            confirmButton = {
                GadgetPrimaryButton(
                    onClick = { restartApp(context) },
                    text = stringResource(R.string.backup_restart_now),
                )
            },
            dismissButton = {
                GadgetTertiaryButton(
                    onClick = { showRestartDialog = false },
                    text = stringResource(R.string.backup_later),
                )
            },
        )
    }
}

@Composable
private fun StatusLine(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
}

private sealed interface BackupStatus {
    data object Idle : BackupStatus
    data object Working : BackupStatus
    data object Exported : BackupStatus
    data class Error(val message: String) : BackupStatus
}

private fun defaultBackupFileName(): String {
    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
    return "gadget-backup-$stamp.zip"
}

/** Cold-restart the app so the restored modular databases are reopened. */
private fun restartApp(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    if (launch != null) context.startActivity(launch)
    Runtime.getRuntime().exit(0)
}

private const val BACKUP_MIME = "application/zip"

/** Permissive open filter — OEM file pickers report ZIPs under several MIME
 *  types, so accept the common ones rather than miss the user's backup. */
private val BACKUP_OPEN_MIME_TYPES = arrayOf(
    "application/zip",
    "application/octet-stream",
    "application/x-zip-compressed",
)
