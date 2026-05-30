package com.gadget.root.ui

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.gadget.localization.Language
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import dev.ranzlappen.gadget.core.root.RootFeatureDescriptor
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import com.gadget.root.RootFeaturesEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

/**
 * Settings card listing every rooted feature with a per-feature opt-in
 * switch. Renders nothing on the standard flavor or on a rooted device
 * without root granted — the parent layout sees an empty Composable and
 * the rest of the Settings screen stacks normally.
 */
@Composable
fun RootedFeatureTogglesCard(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val toggles = entryPoint.featureToggles()
    val descriptors = remember(entryPoint) {
        entryPoint.featureRegistry().allDescriptors().toList()
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)
    val safetyMode by toggles.isMonitorSafetyMode().collectAsState(initial = true)
    var pendingConfirm by remember { mutableStateOf<RootFeatureDescriptor?>(null) }

    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Rooted feature toggles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Each feature ships OFF. Enable individually after reading the safety notes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Safety mode",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Blocks every write-capable feature; read-only diagnostics stay on.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = safetyMode,
                    onCheckedChange = { newValue ->
                        scope.launch { toggles.setMonitorSafetyMode(newValue) }
                    },
                )
            }
            HorizontalDivider()
            descriptors.forEach { descriptor ->
                val enabled by toggles.isEnabled(descriptor.key)
                    .collectAsState(initial = false)
                val gatedBySafetyMode = safetyMode && descriptor.isWriteCapable
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = descriptor.key.id.replace('_', ' '),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (descriptor.requiresExplicitConfirm) {
                            Text(
                                text = "⚠ Hardware risk — read before enabling",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        if (gatedBySafetyMode) {
                            Text(
                                text = "Disabled by Safety mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Switch(
                        checked = enabled && !gatedBySafetyMode,
                        enabled = !gatedBySafetyMode,
                        onCheckedChange = { newValue ->
                            if (newValue && descriptor.requiresExplicitConfirm) {
                                pendingConfirm = descriptor
                            } else {
                                scope.launch { toggles.setEnabled(descriptor.key, newValue) }
                            }
                        },
                    )
                }
            }
        }
    }

    val confirmTarget = pendingConfirm
    if (confirmTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingConfirm = null },
            title = {
                Text(
                    text = S.RootFeatureRisk.dialogTitle(lang),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = confirmTarget.key.id.replace('_', ' '),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = riskBucketTextFor(confirmTarget.key, lang),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val key = confirmTarget.key
                        pendingConfirm = null
                        scope.launch { toggles.setEnabled(key, true) }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(S.RootFeatureRisk.confirmEnable(lang)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirm = null }) {
                    Text(S.RootFeatureRisk.cancel(lang))
                }
            },
        )
    }
}

private fun riskBucketTextFor(key: RootFeatureKey, lang: Language): String {
    val id = key.id
    return when {
        id.startsWith("torch_") ||
            id.startsWith("vibration_") ||
            id.startsWith("display_") ->
            S.RootFeatureRisk.thermal(lang)
        id.startsWith("battery_") ->
            S.RootFeatureRisk.batteryCell(lang)
        id.startsWith("audio_") || id.startsWith("mic_") ->
            S.RootFeatureRisk.hearing(lang)
        id.startsWith("sensors_") || id.startsWith("camera_") ->
            S.RootFeatureRisk.sensorIntegrity(lang)
        id.startsWith("wifi_") ||
            id.startsWith("bluetooth_") ||
            id.startsWith("cell_") ||
            id.startsWith("nfc_") ||
            id.startsWith("ir_") ||
            id.startsWith("gps_") ->
            S.RootFeatureRisk.radioRegulatory(lang)
        id.startsWith("storage_") ->
            S.RootFeatureRisk.storageNonReversible(lang)
        id.startsWith("automation_") ||
            id.startsWith("notification_") ||
            id.startsWith("keep_alive_") ->
            S.RootFeatureRisk.uxDeadlock(lang)
        id.startsWith("adb_") || id.startsWith("usb_") ->
            S.RootFeatureRisk.attackSurface(lang)
        id.startsWith("diagnostics_") ->
            S.RootFeatureRisk.infoDisclosure(lang)
        else -> S.RootFeatureRisk.generic(lang)
    }
}

@Composable
internal fun rememberRootFeatures(): RootFeaturesEntryPoint {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            RootFeaturesEntryPoint::class.java,
        )
    }
}

