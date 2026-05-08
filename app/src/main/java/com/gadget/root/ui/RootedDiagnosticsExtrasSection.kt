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
import com.gadget.diagnostics.DiagnosticsControllerResult
import com.gadget.diagnostics.LogcatBuffer
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import kotlinx.coroutines.launch

/**
 * Diagnostics root extras Card. Rendered inside `BugReportScreen` above
 * the Batch-9 USB extras section. Read-only — no auto-revert hook
 * needed since this surface performs zero writes.
 */
@Composable
fun DiagnosticsRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val diag = entryPoint.diagnosticsController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)
    var status by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier) {
        RootExtrasDisclaimerCard(text = S.DiagnosticsRootExtras.disclaimer(lang))
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
                    text = S.DiagnosticsRootExtras.cardTitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = S.DiagnosticsRootExtras.body(lang),
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeDiagnosticsResult(
                                diag.tailLogcat(LogcatBuffer.MAIN, persist = true),
                            )
                        }
                    },
                ) { Text(S.DiagnosticsRootExtras.tailLogcatMain(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeDiagnosticsResult(
                                diag.tailLogcat(LogcatBuffer.RADIO, persist = true),
                            )
                        }
                    },
                ) { Text(S.DiagnosticsRootExtras.tailLogcatRadio(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeDiagnosticsResult(diag.dumpMemInfo(persist = true))
                        }
                    },
                ) { Text(S.DiagnosticsRootExtras.dumpMemInfo(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeDiagnosticsResult(diag.dumpCpuInfo(persist = true))
                        }
                    },
                ) { Text(S.DiagnosticsRootExtras.dumpCpuInfo(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeDiagnosticsResult(diag.dumpProcstats(persist = true))
                        }
                    },
                ) { Text(S.DiagnosticsRootExtras.dumpProcstats(lang)) }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeDiagnosticsResult(diag.resetAllDiagnosticsMutations())
                        }
                    },
                ) { Text(S.DiagnosticsRootExtras.resetAll(lang)) }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

private fun describeDiagnosticsResult(result: DiagnosticsControllerResult): String = when (result) {
    is DiagnosticsControllerResult.Ok -> result.statusNote ?: "OK"
    DiagnosticsControllerResult.Unsupported -> "Unsupported on this device"
    DiagnosticsControllerResult.OptedOut -> "Disabled — enable in Settings"
    is DiagnosticsControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is DiagnosticsControllerResult.HardwareError -> "Error: ${result.message}"
    is DiagnosticsControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is DiagnosticsControllerResult.LogcatExcerpt ->
        "logcat[${result.buffer.wireName}]: ${result.excerpt.length} bytes; " +
            "persisted=${result.persistedFile ?: "no"}"
    is DiagnosticsControllerResult.MemInfoExcerpt ->
        "meminfo: ${result.excerpt.length} bytes; persisted=${result.persistedFile ?: "no"}"
    is DiagnosticsControllerResult.CpuInfoExcerpt ->
        "cpuinfo: ${result.excerpt.length} bytes; persisted=${result.persistedFile ?: "no"}"
    is DiagnosticsControllerResult.ProcstatsExcerpt ->
        "procstats: ${result.excerpt.length} bytes; persisted=${result.persistedFile ?: "no"}"
}
