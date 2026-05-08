package com.gadget.root.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import com.gadget.storage.DropCachesConfig
import com.gadget.storage.FstrimConfig
import com.gadget.storage.StorageControllerResult
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private val DEMO_FSTRIM_PARTITIONS = listOf("/data", "/cache")

/**
 * Storage root extras Card. Rendered inside `FileMetadataScreen` near
 * the bottom. The screen-exit revert is shape-identical to the other
 * Batch-7/8 surfaces but is a no-op since fstrim and drop_caches are
 * intrinsically non-reversible.
 */
@Composable
fun StorageRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val storage = entryPoint.storageController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)
    var status by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            MainScope().launch { storage.revertOnScreenExit() }
        }
    }

    Column(modifier = modifier) {
        RootExtrasDisclaimerCard(text = S.StorageRootExtras.disclaimer(lang))
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = S.StorageRootExtras.cardTitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = S.StorageRootExtras.body(lang),
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(storage.dumpDiskstats(persist = true))
                        }
                    },
                ) { Text(S.StorageRootExtras.dumpDiskstats(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(storage.enumerateMounts())
                        }
                    },
                ) { Text(S.StorageRootExtras.enumerateMounts(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(
                                storage.trimFilesystem(
                                    FstrimConfig(partitions = DEMO_FSTRIM_PARTITIONS),
                                ),
                            )
                        }
                    },
                ) { Text(S.StorageRootExtras.fstrim(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(storage.dropKernelCaches(DropCachesConfig()))
                        }
                    },
                ) { Text(S.StorageRootExtras.dropCaches(lang)) }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(storage.resetAllStorageMutations())
                        }
                    },
                ) { Text(S.StorageRootExtras.resetAll(lang)) }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

private fun describe(result: StorageControllerResult): String = when (result) {
    is StorageControllerResult.Ok -> result.statusNote ?: "OK"
    StorageControllerResult.Unsupported -> "Unsupported on this device"
    StorageControllerResult.OptedOut -> "Disabled — enable in Settings"
    is StorageControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is StorageControllerResult.HardwareError -> "Error: ${result.message}"
    is StorageControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is StorageControllerResult.DiskstatsExcerpt ->
        "Read ${result.excerpt.length} bytes" + (result.persistedFile?.let { " → $it" } ?: "")
    is StorageControllerResult.MountList ->
        "Enumerated ${result.mounts.size} mounts"
}
