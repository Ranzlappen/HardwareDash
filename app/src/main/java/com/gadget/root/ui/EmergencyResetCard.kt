package com.gadget.root.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import dev.ranzlappen.gadget.core.root.emergency.EmergencyResetCoordinatorResult
import dev.ranzlappen.gadget.core.root.emergency.EmergencyResetOptions
import kotlinx.coroutines.launch

/**
 * Global Emergency Reset Card. Rendered as the last section of
 * `SettingsScreen` (Section 9). Visible only when root is granted.
 *
 * The reset itself bypasses `RootSafetyGate` — the dialog confirmation
 * is the safety wrapper. Two checkboxes let the user opt into stopping
 * the keep-alive service (on by default) and clearing per-feature
 * opt-outs (off by default).
 */
@Composable
fun EmergencyResetCard(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val coordinator = entryPoint.emergencyResetCoordinator()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)

    var showDialog by remember { mutableStateOf(false) }
    var stopKeepAlive by remember { mutableStateOf(true) }
    var resetOptOuts by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = S.EmergencyReset.cardTitle(lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = S.EmergencyReset.body(lang),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = S.EmergencyReset.disclaimer(lang),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text(S.EmergencyReset.trigger(lang)) }
            status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = S.EmergencyReset.dialogTitle(lang),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = S.EmergencyReset.dialogBody(lang),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = stopKeepAlive,
                            onCheckedChange = { stopKeepAlive = it },
                        )
                        Text(
                            text = S.EmergencyReset.checkboxStopKeepAlive(lang),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = resetOptOuts,
                            onCheckedChange = { resetOptOuts = it },
                        )
                        Text(
                            text = S.EmergencyReset.checkboxResetOptOuts(lang),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        val options = EmergencyResetOptions(
                            stopKeepAliveService = stopKeepAlive,
                            resetAllPerFeatureOptOuts = resetOptOuts,
                        )
                        scope.launch {
                            status = describeEmergencyResetResult(
                                coordinator.resetEverything(options),
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(S.EmergencyReset.confirmButton(lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(S.EmergencyReset.cancelButton(lang))
                }
            },
        )
    }
}

private fun describeEmergencyResetResult(result: EmergencyResetCoordinatorResult): String =
    when (result) {
        EmergencyResetCoordinatorResult.Unsupported -> "Unsupported on this device"
        is EmergencyResetCoordinatorResult.Ok -> describeOk(result)
        is EmergencyResetCoordinatorResult.HardwareError -> {
            val partial = result.partial
            if (partial != null) {
                "Partial: ${describeOk(partial)} — error: ${result.message}"
            } else {
                "Error: ${result.message}"
            }
        }
    }

private fun describeOk(ok: EmergencyResetCoordinatorResult.Ok): String =
    "Reverted ${ok.sysfsMutationsRestored} sysfs (failed=${ok.sysfsMutationsFailed}); " +
        "keepAlive=${ok.keepAliveStopped}; doze=${ok.dozeReset}; " +
        "appOps=${ok.batteryOptimizationReset}; optOutsCleared=${ok.perFeatureOptOutsCleared}"
